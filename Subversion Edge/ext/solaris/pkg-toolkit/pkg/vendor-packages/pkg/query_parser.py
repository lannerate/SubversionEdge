#!/usr/bin/python2.4
#
# CDDL HEADER START
#
# The contents of this file are subject to the terms of the
# Common Development and Distribution License (the "License").
# You may not use this file except in compliance with the License.
#
# You can obtain a copy of the license at usr/src/OPENSOLARIS.LICENSE
# or http://www.opensolaris.org/os/licensing.
# See the License for the specific language governing permissions
# and limitations under the License.
#
# When distributing Covered Code, include this CDDL HEADER in each
# file and include the License file at usr/src/OPENSOLARIS.LICENSE.
# If applicable, add the following below this CDDL HEADER, with the
# fields enclosed by brackets "[]" replaced with your own identifying
# information: Portions Copyright [yyyy] [name of copyright owner]
#
# CDDL HEADER END
#

#
# Copyright 2009 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.
#

import os
import fnmatch
import re
import sys
import threading
import copy
import itertools
import errno
import cgi

import ply.lex as lex
import ply.yacc as yacc
import pkg.search_storage as ss
import pkg.search_errors as search_errors
import pkg.fmri as fmri
import pkg.actions as actions
from pkg.choose import choose
from pkg.misc import EmptyI

FILE_OPEN_TIMEOUT_SECS = 1

class QueryLexer(object):
        """This class defines the lexer used to separate parse queries into
        its constituents.  It's written for Ply, a python implementation for
        lex and yacc."""

        # These are the types of tokens that the lexer can produce.
        tokens = ("FTERM", "TERM", "LPAREN", "RPAREN", "AND", "OR", "QUOTE1",
            "QUOTE2", "LBRACE", "RBRACE")

        # These statements define the lexing rules or (, ),', and ". These are
        # all checked after all lexing defined in functions below.
        t_LPAREN = r"\("
        t_RPAREN = r"\)"
        t_QUOTE1 = r"[\']"
        t_QUOTE2 = r'[\"]'

        # This rule causes spaces to break tokens, but the space itself is not
        # reported as a token.
        t_ignore = r" "

        def __init__(self):
                object.__init__(self)
                # Set up a dictionary of strings which have special meaning and
                # are otherwise indistinguishable from normal terms. The
                # mapping is from the string seen in the query to the lexer
                # tokens.
                self.reserved = {
                    "AND" : "AND",
                    "OR" : "OR"
                }

        # Note: Functions are documented using comments instead of docstrings
        # below because Ply uses the docstrings for specific purposes.
                
        def t_LBRACE(self, t):
                # This rule is for lexing the left side of the pkgs<>
                # construction.
                r"([pP]([kK]([gG][sS]?)?)?)?<"
                t.type = "LBRACE"
                return t

        def t_RBRACE(self, t):
                # This rule is for lexing the right side of the pkgs<>
                # construction.
                r">"
                t.type = "RBRACE"
                return t

        def t_FTERM(self, t):
                # This rule breaks up the structured search term
                # pkg_name:action_type:key: into its constituents and builds
                # a FTERM token whose value is a three tuple of the
                # constituents.
                r"([^\(\s][^\s]*)?\:"
                t.type = "FTERM"
                bar = t.value.split(":", 4)
                assert len(bar) >= 2
                assert bar[-1] == ""
                pkg_name = None
                action_type = None
                key = bar[-2]
                if len(bar) >= 3:
                        action_type = bar[-3]
                        if len(bar) >= 4:
                                pkg_name = bar[-4]
                t.value = (pkg_name, action_type, key)
                return t
                
        def t_TERM(self, t):
                # This rule handles the general search terms as well as
                # checking for any reserved words such as AND or OR.
                r'[^\s\(\'\"][^\s]*[^\s\)\'\"\>] | [^\s\(\)\'\"]'
                t.type = self.reserved.get(t.value, "TERM")
                return t

        def t_error(self, t):
                raise RuntimeError("\n".join(
                    [_("An unparseable character in query at position : %d") %
                        (self.get_pos() + 1),
                    "%s" % self.get_string(),
                    "%s" % (" " * self.get_pos() + "^")]))

        def build(self, **kwargs):
                self.lexer = lex.lex(object=self, **kwargs)

        def set_input(self, input):
                self.lexer.input(input)
                
        def token(self):
                return self.lexer.token()

        def get_pos(self):
                return self.lexer.lexpos

        def get_string(self):
                return self.lexer.lexdata
                
        def test(self, data):
                """This is a function useful for testing and debugging as it
                shows the user exactly which tokens are produced from the input
                data."""

                self.lexer.input(data)
                while 1:
                        tok = self.lexer.token()
                        if not tok:
                                break
                        print >> sys.stderr, tok


class QueryParser(object):
        """This class defines the parser which converts a stream of tokens into
        an abstract syntax tree representation of the query.  The AST is able
        to perform the search."""

        # Use the same set of tokens as the lexer.
        tokens = QueryLexer.tokens

        # Define the precendence and associativity of certain tokens to
        # eliminate shift/reduce conflicts in the grammar.
        precedence = (
            ("right", "FTERM"),
            ("left", "TERM"),
            ("right", "AND", "OR"),
            ("right", "QUOTE1", "QUOTE2"),
        )

        # Note: Like the lexer, Ply uses the doctrings of the functions to
        # determine the rules.

        def p_top_level(self, p):
                # This is the top or start node of the AST.
                '''start : xterm'''
                p[0] = self.query_objs["TopQuery"](p[1])

        def p_xterm(self, p):
                # This rule parses xterms.  xterms are terms which can connect
                # smaller terms together.
                ''' xterm : basetermlist
                          | andterm
                          | orterm'''
                p[0] = p[1]

        def p_basetermlist(self, p):
                # basetermlist handles performing the implicit AND operator
                # which is placed between a list of terms.  For example the
                # query 'foo bar' is treated the same as 'foo AND bar'.
                ''' basetermlist : baseterm
                                 | baseterm basetermlist '''
                if len(p) == 3:
                        p[0] = self.query_objs["AndQuery"](p[1], p[2])
                else:
                        p[0] = p[1]

        def p_baseterm(self, p):
                # baseterms are the minimal units of meaning for the parser.
                # Any baseterm is a valid query unto itself.
                ''' baseterm : term
                             | fterm
                             | phraseterm
                             | parenterm
                             | pkgconv'''
                p[0] = p[1]

        def p_term(self, p):
                # terms are the parser's representation of the lexer's TERMs.
                # The TermQuery object performs most of the work of actually
                # performing the search.
                'term : TERM'
                p[0] = self.query_objs["TermQuery"](p[1])
                
        def p_fterm(self, p):
                # fterms are the parser's representation of the lexer's FTERMS
                # (which are field/structured query terms).  In the query
                # 'foo:bar:baz:zap', foo, bar, and baz are part of the FTERM,
                # zap is split into a separate lexer TERM token.  zap flows
                # up from parsing the ftermarg.  A query like 'foo:bar:baz' has
                # no ftermarg though.  In that case, an implict wildcard is
                # explicitly created.
                '''fterm : FTERM ftermarg
                         | FTERM fterm
                         | FTERM'''
                # If the len of p is 3, then one of the first two cases
                # was used.
                if len(p) == 3:
                        p[0] = self.query_objs["FieldQuery"](p[1], p[2])
                # If the length of p isn't 3, then a bare FTERM was found.  It's
                # necessary to make the implicit wildcard explicit in this case.
                else:
                        p[0] = self.query_objs["FieldQuery"](p[1],
                            self.query_objs["TermQuery"]('*'))

        def p_ftermarg(self, p):
                # ftermargs are the terms which are valid after the final
                # colon of a field term.
                '''ftermarg : term
                            | phraseterm'''
                p[0] = p[1]

        def p_phraseterm(self, p):
                # phraseterms are lists of terms enclosed by quotes.
                ''' phraseterm : QUOTE1 term_list QUOTE1
                               | QUOTE2 term_list QUOTE2'''
                p[2].reverse()
                p[0] = self.query_objs["PhraseQuery"](p[2],
                    self.query_objs["TermQuery"])

        def p_term_list(self, p):
                # term_lists consist of one or more space separated TERMs.
                ''' term_list : TERM term_list
                              | TERM '''
                if len(p) == 3:
                        p[2].append(p[1])
                        p[0] = p[2]
                else:
                        p[0] = [p[1]]

        def p_parenterm(self, p):
                # parenterms contain a single xterm surrounded by parens.
                # The p[2] argument is simply passed on because the only
                # role of parens is to perform grouping, which is enforced
                # by the structure of the AST.
                ''' parenterm : LPAREN xterm RPAREN '''
                p[0] = p[2]

        def p_packages(self, p):
                # pkgconv represents the pkgs<> term.
                'pkgconv : LBRACE xterm RBRACE'
                p[0] = self.query_objs["PkgConversion"](p[2])

        def p_andterm(self, p):
                # andterms perform an intersection of the results of its
                # two children.
                ''' andterm : xterm AND xterm'''
                p[0] = self.query_objs["AndQuery"](p[1], p[3])

        def p_orterm(self, p):
                ''' orterm : xterm OR xterm'''
                # orterms returns the union of the results of its
                # two children.
                p[0] = self.query_objs["OrQuery"](p[1], p[3])

        def p_error(self, p):
                raise ParseError(p, self.lexer.get_pos(),
                    self.lexer.get_string())
                        
        def __init__(self, lexer):
                """Build a parser using the lexer given as an argument."""
                self.lexer = lexer
                self.parser = yacc.yacc(module=self, debug=0, write_tables=0)
                # Store the classes used to build the AST so that child classes
                # can replace them where needed with alternate classes.
                self.query_objs = {
                    "AndQuery" : AndQuery,
                    "FieldQuery" : FieldQuery,
                    "OrQuery" : OrQuery,
                    "PhraseQuery" : PhraseQuery,
                    "PkgConversion" : PkgConversion,
                    "TermQuery" : TermQuery,
                    "TopQuery" : TopQuery
                }

        def parse(self, input):
                """Parse the string, input, into an AST using the rules defined
                in this class."""

                self.lexer.set_input(input)
                return self.parser.parse(lexer=self.lexer)


class QueryException(Exception):
        pass


class DetailedValueError(QueryException):

        def __init__(self, name, bad_value, whole_query):
                QueryException.__init__(self)
                self.name = name
                self.bad_value = bad_value
                self.query = whole_query

        def __str__(self):
                return _("In query %(query)s, %(name)s had a bad value of "
                    "'%(bv)s'.") % {
                        "query":self.query,
                        "name":self.name,
                        "bv":self.bad_value
                    }


class IncompleteQuery(QueryException):

        def __init__(self, whole_query):
                QueryException.__init__(self)
                self.query = whole_query

        def __str__(self):
                return _("A query is expected to have five fields: "
                    "case sensitivity, return type, number of results to "
                    "return, the number at which to start returning results, "
                    "and the text of the query.  The query provided lacked at "
                    "least one of those fields:\n%s") % self.query

        
class ParseError(QueryException):
        def __init__(self, parse_object, string_position, input_string):
                QueryException.__init__(self)
                self.p = parse_object
                self.pos = string_position
                self.str = input_string

        def __str__(self, html=False):
                line_break = "\n"
                pre_tab = ""
                end_pre_tab = ""
                if html:
                        line_break = "<br/>"
                        pre_tab = "<pre>"
                        end_pre_tab = "</pre>"
                return line_break.join([_("Could not parse query."),
                    _("Problem occurred with: %s") % self.p,
                    "%s%s" % (pre_tab, cgi.escape(self.str)),
                    "%s%s" % (" " * max(self.pos - 1, 0) + "^", end_pre_tab)])

        def html(self):
                return self.__str__(html=True)


class Query(object):
        """General Query object.  It defines various constants and provides for
        marshalling a Query into and out of a string format."""

        RETURN_EITHER = 0
        RETURN_PACKAGES = 1
        RETURN_ACTIONS = 2
        VALIDATION_STRING = { 1:'Return from search v1\n' }

        def __init__(self, text, case_sensitive, return_type, num_to_return,
            start_point):
                """Construct a query object.

                The "text" parameter is the tokens and syntax of the query.

                The "case_sensitive" parameter is a boolean which determines
                whether the query is case sensitive or not.

                The "return_type" parameter must be either RETURN_PACKAGES or
                RETURN_ACTIONS and determines whether the query is expected to
                return packages or actions to the querier.

                The "num_to_return" paramter is the maximum number of results to
                return.

                The "start_point" parameter is the number of results to skip
                before returning results to the querier."""

                self.__text = text
                self.case_sensitive = case_sensitive
                self.return_type = return_type
                assert self.return_type == Query.RETURN_PACKAGES or \
                    self.return_type == Query.RETURN_ACTIONS
                self.num_to_return = num_to_return
                self.start_point = start_point

        def __str__(self):
                """Return the v1 string representation of this query."""

                return "%s_%s_%s_%s_%s" % (self.case_sensitive,
                    self.return_type, self.num_to_return, self.start_point,
                    self.__text)

        def ver_0(self):
                """Return the v0 string representation of this query."""

                return self.__text

        def encoded_text(self):
                if self.return_type == Query.RETURN_PACKAGES:
                        return "<%s>" % self.__text
                else:
                        return self.__text

        @staticmethod
        def fromstr(s):
                """Take the output of the __str__ method of this class and
                return a Query object from that string."""

                try:
                        case_sensitive, return_type, num_to_return, \
                            start_point, text = s.split("_", 4)
                except ValueError:
                        raise IncompleteQuery(s)
                if case_sensitive == 'True':
                        case_sensitive = True
                elif case_sensitive == 'False':
                        case_sensitive = False
                else:
                        raise DetailedValueError("case_sensitive",
                            case_sensitive, s)
                if num_to_return == 'None':
                        num_to_return = None
                else:
                        try:
                                num_to_return = int(num_to_return)
                        except ValueError:
                                raise DetailedValueError("num_to_return",
                                    num_to_return, s)
                if start_point == 'None':
                        start_point = None
                else:
                        try:
                                start_point = int(start_point)
                        except ValueError:
                                raise DetailedValueError("start_point",
                                    start_point, s)
                try:
                        return_type = int(return_type)
                except ValueError:
                        raise DetailedValueError("return_type", return_type, s)
                if return_type != Query.RETURN_PACKAGES and \
                    return_type != Query.RETURN_ACTIONS:
                        raise DetailedValueError("return_type", return_type, s)
                return Query(text, case_sensitive, return_type,
                    num_to_return, start_point)

        @staticmethod
        def return_action_to_key(k):
                """Method which produces the sort key for an action."""

                at, st, pfmri, fv, l = k
                return pfmri


class UnknownFieldTypeException(Exception):
        def __init__(self, field_kind, field_value):
                Exception.__init__(self)
                self.field_kind = field_kind
                self.field_value = field_value


class BooleanQueryException(QueryException):
        """This exception is used when the two children of a boolean query
        don't agree on whether to return actions or packages."""

        def __init__(self, ac, pc):
                """The parameter "ac" is the child which returned actions
                while "pc" is the child which returned pacakges."""
                QueryException.__init__(self)
                self.ac = ac
                self.pc = pc

        def __str__(self, html=False):
                line_break = "\n"
                pre_tab = ""
                end_pre_tab = ""
                if html:
                        line_break = "<br/>"
                        pre_tab = "<pre>"
                        end_pre_tab = "</pre>"
                ac_s = _("This expression produces action results:")
                ac_q = "%s%s%s" % (pre_tab, self.ac, end_pre_tab)
                pc_s = _("This expression produces package results:")
                pc_q = "%s%s%s" % (pre_tab, self.pc, end_pre_tab)
                return line_break.join([ac_s, ac_q, pc_s, pc_q,
                    _("'AND' and 'OR' require those expressions to produce "
                    "the same type of results.")])

        def html(self):
                return self.__str__(html=True)


class BooleanQuery(object):
        """Superclass for all boolean operations in the AST."""

        def __init__(self, left_query, right_query):
                """The parameters "left_query" and "right_query" are objects
                which implement the query interface.  Specifically, they're
                expected to implement search, allow_version, set_info, and to
                have a public member called return_type."""

                self.lc = left_query
                self.rc = right_query
                self.return_type = self.lc.return_type
                if self.lc.return_type != self.rc.return_type:
                        if self.lc.return_type == Query.RETURN_ACTIONS:
                                raise BooleanQueryException(self.lc, self.rc)
                        else:
                                raise BooleanQueryException(self.rc, self.lc)

        def set_info(self, **kwargs):
                """This function passes information to the terms prior to
                search being executed.  For a boolean query, it only needs to
                pass whatever information exists onto its children."""

                self.lc.set_info(**kwargs)
                self.rc.set_info(**kwargs)
                
        def search(self, *args):
                """Distributes the search to the two children and returns a
                tuple of the results."""

                return set(self.lc.search(None, *args)), \
                    set(self.rc.search(None, *args))

        def sorted(self, res):
                """Sort the results.  If the results are actions, sort by the
                fmris of the packages from which they came."""

                key = None
                if self.return_type == Query.RETURN_ACTIONS:
                        key = Query.return_action_to_key
                return sorted(res, key=key)

        def allow_version(self, v):
                """Returns whether the query supports version v."""

                return v > 0 and self.lc.allow_version(v) and \
                    self.rc.allow_version(v)

class AndQuery(BooleanQuery):
        """Class representing AND queries in the AST."""

        def search(self, restriction, *args):
                """Performs a search over the two children and combines
                the results.

                The "restriction" parameter is a generator of actions, over
                which the search shall be performed.  Only boolean queries will
                set restriction.  Nodes that return packages have, by
                definition, parents that must return packages.  This means that
                only queries contained within a boolean query higher up in the
                AST tree will have restriction set."""

                if self.return_type == Query.RETURN_ACTIONS:
                        # If actions are being returned, the answers from
                        # previous terms must be used as the domain of search.
                        # To do this, restriction is passed to the left
                        # child and the result from that child is passed to
                        # the right child as its domain.
                        lc_it = self.lc.search(restriction, *args)
                        return self.rc.search(lc_it, *args)
                else:
                        # If packages are being returned, holding the names
                        # of all known packages in memory is feasible. By
                        # using sets, and their intersection, duplicates are
                        # also removed from the results.
                        lc_set, rc_set = BooleanQuery.search(self, *args)
                        return self.sorted(lc_set & rc_set)
                

        def __str__(self):
                return "( " + str(self.lc) + " AND " + str(self.rc) + " )"
        
class OrQuery(BooleanQuery):
        """Class representing OR queries in the AST."""

        def search(self, restriction, *args):
                """Performs a search over the two children and combines
                the results.

                The "restriction" parameter is a generator function that returns
                actions within the search domain.  If it's not None,
                then this query is under a higher boolean query which also
                returns actions."""

                if self.return_type == Query.RETURN_PACKAGES:
                        # If packages are being returned, it's feasible to
                        # hold them all in memory.  This allows the use of
                        # sets, and their union, to remove duplicates and
                        # produce a sorted list.
                        lc_set, rc_set = BooleanQuery.search(self, *args)
                        for i in self.sorted(lc_set | rc_set):
                                yield i
                elif not restriction:
                        # If restriction doesn't exist, then chain together
                        # the results from both children.
                        for i in itertools.chain(self.lc.search(restriction,
                            *args), self.rc.search(restriction, *args)):
                                yield i
                else:
                        # If restriction exists, then it must serve as the
                        # domain for the children.  It is a generator so
                        # only one pass may be made over it.  Also, it is not
                        # possible, in general, to hold a list of all items
                        # that the generator will produce in memory.  These
                        # reasons lead to the construction below, which iterates
                        # over the results in restriction and uses each as the
                        # restriction to the child search.  If this turns out
                        # to be a performance bottleneck, it would be possible
                        # to gather N of the results in restriction into a list
                        # and dispatch them to the children results in one shot.
                        # The tradeoff is the memory to hold O(N) results in
                        # memory at each level of ORs in the AST.
                        for i in restriction:
                                for j in itertools.chain(self.lc.search([i],
                                    *args), self.rc.search([i], *args)):
                                        yield j

        def __str__(self):
                return "( " + str(self.lc) + " OR " + str(self.rc) + " )"

class PkgConversion(object):
        """Class representing a change from returning actions to returning
        packages in the AST."""
        
        def __init__(self, query):
                self.query = query
                self.return_type = Query.RETURN_PACKAGES

        def __str__(self):
                return "p<%s>" % str(self.query)

        def set_info(self, **kwargs):
                """This function passes information to the terms prior to
                search being executed.  It only needs to pass whatever
                information exists to its child."""

                self.query.set_info(**kwargs)

        @staticmethod
        def optional_action_to_package(it, return_type, current_type):
                """Based on the return_type and current type, it converts the
                iterator over results, it, to a sorted list of packages.
                return_type is what the caller wants to return and current_type
                is what it is iterating over."""

                if current_type == return_type or return_type == \
                    Query.RETURN_EITHER:
                        return it
                elif return_type == Query.RETURN_PACKAGES and \
                    current_type == Query.RETURN_ACTIONS:
                        return sorted(set(
                            (pfmri for at, st, pfmri, fv, l in it)))
                else:
                        assert 0
                
        def search(self, restriction, *args):
                """Takes the results of its child's search and converts the
                results to be a sorted list of packages.

                The "restriction" parameter is a generator of actions which
                the domain over which search should be performed. It should
                always be None."""

                return self.optional_action_to_package(
                    self.query.search(restriction, *args),
                    Query.RETURN_PACKAGES, self.query.return_type)

        def allow_version(self, v):
                """Returns whether the query supports a query of version v."""

                return v > 0 and self.query.allow_version(v)


class PhraseQuery(object):
        """Class representing a phrase search in the AST"""

        def __init__(self, str_list, term_query_class):
                """The "str_list" parameter is the list of strings which make
                up the phrase to be searched.

                The "term_query_class" parameter is a TermQuery object which
                handles searching for the initial word in the phrase."""

                assert str_list
                self.query = term_query_class(str_list[0])
                self.full_str = " ".join(str_list)
                self.return_type = Query.RETURN_ACTIONS
                if len(str_list) > 1:
                        self.query.add_trailing_wildcard()

        def __str__(self):
                return "Phrase Query:'" + self.full_str + "'"

        def set_info(self, **kwargs):
                """This function passes information to the terms prior to
                search being executed.  It only needs to pass whatever
                information exists to its child."""

                self.query.set_info(**kwargs)

        def filter_res(self, l):
                """Check to see if the phrase is contained in l, the string of
                the original action."""

                return self.full_str in l

        @staticmethod
        def combine(fs, fv):
                """Checks to see if the phrase being searched for is a subtring
                of the value which matched the token.  If it is, use the value
                returned, otherwise use the search phrase."""

                if fs in fv:
                        return fv
                else:
                        return fs
                
        def search(self, restriction, *args):
                """Perform a search for the given phrase.  The child is used to
                find instances of the first word of the phrase.  Those results
                are then filtered by searching for the longer phrase within
                the original line of the action.  restriction is a generator
                function that returns actions within the search domain."""

                it = (
                    (at, st, pfmri, self.combine(self.full_str, fv), l)
                    for at, st, pfmri, fv, l
                    in self.query.search(restriction, *args)
                    if self.filter_res(l)
                )
                return it

        def allow_version(self, v):
                """Returns whether the query supports a query of version v."""

                return v > 0 and self.query.allow_version(v)
                
class FieldQuery(object):
        """Class representing a structured query in the AST."""

        def __init__(self, params, query):
                """Builds a FieldQuery object.

                The "params" parameter are the three parts of the structured
                search term pulled apart during parsing.

                The "query" parameter is the Query object which contains the
                fourth field (the token) of the structured search."""

                # For efficiency, especially on queries which search over
                # '*', instead of filtering the results, this class makes
                # modifications to its child class so that it will do the
                # needed filtering as it does the search.
                self.query = query
                self.return_type = Query.RETURN_ACTIONS
                self.query.pkg_name, self.query.action_type, self.query.key = \
                    params
                self.query.action_type_wildcard = self.__is_wildcard(
                    self.query.action_type)
                self.query.key_wildcard = self.__is_wildcard(self.query.key)
                self.query.pkg_name_wildcard = \
                    self.__is_wildcard(self.query.pkg_name)
                self.query.pkg_name_match = None
                # Because users will rarely want to search on package names
                # by fully specifying them out to their time stamps, package
                # name search is treated as a prefix match.  To accomplish
                # this, a star is appended to the package name if it doesn't
                # already end in one.
                if not self.query.pkg_name_wildcard:
                        if not self.query.pkg_name.endswith("*"):
                                self.query.pkg_name += "*"
                        self.query.pkg_name_match = \
                            re.compile(fnmatch.translate(self.query.pkg_name),
                                re.I).match

        def __str__(self):
                return "( PN:%s AT:%s ST:%s Q:%s)" % (self.query.pkg_name,
                    self.query.action_type, self.query.key, self.query)

        def set_info(self, **kwargs):
                """This function passes information to the terms prior to
                search being executed.  It only needs to pass whatever
                information exists to its child."""

                self.query.set_info(**kwargs)

        @staticmethod
        def __is_wildcard(s):
                return s is None or s == '*' or s == ''
                
        def search(self, restriction, *args):
                """Perform a search for the structured query.  The child has
                been modified so that it is able to do the structured query
                directly."""

                assert self.query.return_type == Query.RETURN_ACTIONS
                return self.query.search(restriction, *args)

        def allow_version(self, v):
                """Returns whether the query supports a query of version v."""

                return v > 0 and self.query.allow_version(v)

class TopQuery(object):
        """Class which must be at the top of all valid ASTs, and may only be
        at the top of an AST.  It handles starting N results in, or only
        showing M items.  It also transforms the internal representations of
        results into the format expected by the callers of search."""

        def __init__(self, query):
                self.query = query
                self.start_point = 0
                self.num_to_return = None

        def __str__(self):
                return "TopQuery(" + str(self.query) +  " )"

        def __keep(self, x):
                """Determines whether the x'th result should be returned."""

                return x >= self.start_point and \
                    (self.num_to_return is None or
                    x < self.num_to_return + self.start_point)
        
        def finalize_results(self, it):
                """Converts the internal result representation to the format
                which is expected by the callers of search.  It also handles
                returning only those results requested by the user."""

                # Need to replace "1" with current search version, or something
                # similar

                if self.query.return_type == Query.RETURN_ACTIONS:
                        return (
                            (1, Query.RETURN_ACTIONS, (pfmri, fv, l))
                            for x, (at, st, pfmri, fv, l)
                            in enumerate(it)
                            if self.__keep(x)
                        )
                else:
                        return (
                            (1, Query.RETURN_PACKAGES, pfmri)
                            for x, pfmri
                            in enumerate(it)
                            if self.__keep(x)
                        )

        def set_info(self, num_to_return, start_point, **kwargs):
                """This function passes information to the terms prior to
                search being executed.  This is also where the starting point
                and number of results to return is set.  Both "num_to_return"
                and "start_point" are expected to be integers."""

                if start_point:
                        self.start_point = start_point
                self.num_to_return = num_to_return
                self.query.set_info(start_point=start_point,
                    num_to_return=num_to_return, **kwargs)
                
                        
        def search(self, *args):
                """Perform search by taking the result of the child's search
                and transforming and subselecting the results.  None is passed
                to the child since initially there is no set of results to
                restrict subsequent searches to."""

                return self.finalize_results(self.query.search(None, *args))

        def allow_version(self, v):
                """Returns whether the query supports a query of version v."""

                return self.query.allow_version(v)

class TermQuery(object):
        """Class representing the a single query term in the AST."""

        # This structure was used to gather all index files into one
        # location. If a new index structure is needed, the files can
        # be added (or removed) from here. Providing a list or
        # dictionary allows an easy approach to opening or closing all
        # index files.

        dict_lock = threading.Lock()

        has_non_wildcard_character = re.compile('.*[^\*\?].*')

        fmris = None
        
        _global_data_dict = {
            "manf":
                    ss.IndexStoreDict(ss.MANIFEST_LIST),
            "token_byte_offset":
                    ss.IndexStoreDictMutable(ss.BYTE_OFFSET_FILE),
            "fmri_offsets":
                    ss.InvertedDict(ss.FMRI_OFFSETS_FILE, None)
        }

        def __init__(self, term):
                """term is a the string for the token to be searched for."""

                term = term.strip()
                self._glob = False
                if '*' in term or '?' in term or '[' in term:
                        self._glob = True
                self._term = term

                self.return_type = Query.RETURN_ACTIONS
                self._file_timeout_secs = FILE_OPEN_TIMEOUT_SECS

                # This block of options is used by FieldQuery to limit the
                # domain of search.
                self.pkg_name = None
                self.action_type = None
                self.key = None
                self.action_type_wildcard = True
                self.key_wildcard = True
                self.pkg_name_wildcard = True
                self.pkg_name_match = None
                self._case_sensitive = None
                self._dir_path = None

                # These variables are set by set_info and are used to hold
                # information specific to the particular search index that this
                # AST is being built for.
                self._manifest_path_func = None
                self._data_manf = None
                self._data_token_offset = None
                self._data_main_dict = None

        def __str__(self):
                return "( TermQuery: " + self._term + " )"

        def add_trailing_wildcard(self):
                """Ensures that the search is a prefix match.  Primarily used
                by the PhraseQuery class."""

                if not self._term.endswith('*'):
                        self._term += "*"
        
        def set_info(self, index_dir, get_manifest_path,
            case_sensitive, **kwargs):
                """Sets the information needed to search which is specific to
                the particular index used to back the search.

                'index_dir' is a path to the base directory of the index.
                
                'get_manifest_path' is a function which when given a
                fully specified fmri returns the path to the manifest file
                for that fmri.

                'case_sensitive' is a boolean which determines whether search
                is case sensitive or not."""

                self._dir_path = index_dir
                assert self._dir_path
                self._manifest_path_func = get_manifest_path
                self._case_sensitive = case_sensitive

                # Take the staic class lock because it's possible we'll
                # modify the shared dictionaries for the class.
                TermQuery.dict_lock.acquire()
                try:
                        self._data_main_dict = \
                            ss.IndexStoreMainDict(ss.MAIN_FILE)
                        if "fmri_offsets" not in TermQuery._global_data_dict:
                                TermQuery._global_data_dict["fmri_offsets"] = \
                                    ss.InvertedDict(ss.FMRI_OFFSETS_FILE, None)
                        # Create a temporary list of dictionaries we need to
                        # open consistently.
                        tmp = TermQuery._global_data_dict.values()
                        tmp.append(self._data_main_dict)
                        try:
                                # Try to open the index files assuming they
                                # were made after the conversion to using the
                                # fmri_offsets.v1 file.
                                ret = ss.consistent_open(tmp, self._dir_path,
                                    self._file_timeout_secs)
                        except search_errors.InconsistentIndexException:
                                # If opening the index fails, try falling
                                # back to the index prior to the conversion
                                # to using the fmri_offsets.v1 file.
                                del TermQuery._global_data_dict[
                                    "fmri_offsets"]
                                tmp = TermQuery._global_data_dict.values()
                                tmp.append(self._data_main_dict)
                                ret = ss.consistent_open(tmp, self._dir_path,
                                    self._file_timeout_secs)
                        if ret == None:
                                raise search_errors.NoIndexException(
                                    self._dir_path)
                        should_reread = False
                        # Check to see if any of the in-memory stores of the
                        # dictionaries are out of date compared to the ones
                        # on disc.
                        for k, d in self._global_data_dict.items():
                                if d.should_reread():
                                        should_reread = True
                                        break
                        try:
                                # If any of the in-memory dictionaries are out
                                # of date, create new copies of the shared
                                # dictionaries and make the shared structure
                                # point at them.  This is to prevent changing
                                # the data structures in any other threads
                                # which may be part way through executing a
                                # search.
                                if should_reread:
                                        for i in tmp:
                                                i.close_file_handle()
                                        TermQuery._global_data_dict = \
                                            dict([
                                                (k, copy.copy(d))
                                                for k, d
                                                in TermQuery._global_data_dict.items()
                                            ])
                                        tmp = \
                                            TermQuery._global_data_dict.values()
                                        tmp.append(self._data_main_dict)
                                        ret = ss.consistent_open(tmp,
                                            self._dir_path,
                                            self._file_timeout_secs)
                                        try:
                                                if ret == None:
                                                        raise search_errors.NoIndexException(self._dir_path)
                                                # Reread the dictionaries and
                                                # store the new information in
                                                # the shared data structure.
                                                for d in TermQuery._global_data_dict.values():
                                                        d.read_dict_file()
                                        except:
                                                self._data_main_dict.close_file_handle()
                                                raise

                        finally:
                                # Ensure that the files are closed no matter
                                # what happens.
                                for d in TermQuery._global_data_dict.values():
                                        d.close_file_handle()
                        self._data_manf = TermQuery._global_data_dict["manf"]

                        self._data_token_offset = \
                            TermQuery._global_data_dict["token_byte_offset"]

                        try:
                                self._data_fmri_offsets = \
                                    TermQuery._global_data_dict["fmri_offsets"]
                        except KeyError:
                                self._data_fmri_offsets = None
                finally:
                        TermQuery.dict_lock.release()

        def allow_version(self, v):
                """Returns whether the query supports a query of version v."""
                return True
                
        def _close_dicts(self):
                """Closes the main dictionary file handle, which is handled
                separately from the other dictionaries since it's not read
                entirely into memory in one shot."""

                self._data_main_dict.close_file_handle()

        @staticmethod
        def flatten(lst):
                """Takes a list which may contain one or more sublists and
                returns a list which contains all the items contained in the
                original list and its sublists, but without any sublists."""

                res = []
                for l in lst:
                        if isinstance(l, list):
                                res.extend(TermQuery.flatten(l))
                        else:
                                res.append(l)
                return res
                
        def _restricted_search_internal(self, restriction):
                """Searches for the given term within a restricted domain of
                search results.  restriction is a generator function that
                returns actions within the search domain."""

                glob = self._glob
                term = self._term
                case_sensitive = self._case_sensitive

                if not case_sensitive:
                        glob = True
                for at, st, fmri_str, fv, l in restriction:
                        # Check if the current action doesn't match any field
                        # query specifications.
                        if not (self.pkg_name_wildcard or
                            self.pkg_name_match(fmri_str)) or \
                            not (self.action_type_wildcard or
                            at == self.action_type) or \
                            not (self.key_wildcard or st == self.key):
                                continue
                        l = l.strip()
                        # Find the possible tokens for this action.
                        act = actions.fromstr(l)
                        toks = [t[2] for t in act.generate_indices()]
                        toks = self.flatten(toks)
                        matches = []
                        if glob:
                                matches = choose(toks, term, case_sensitive)
                        elif term in toks:
                                matches = True
                        # If this search term matches any of the tokens the
                        # action could generate, yield it.  If not, continue
                        # on to the next action.
                        if matches:
                                yield at, st, fmri_str, fv, l

        def __offset_line_read(self, offsets):
                """Takes a group of byte offsets into the main dictionary and
                reads the lines starting at those byte offsets."""

                md_fh = self._data_main_dict.get_file_handle()
                for o in sorted(offsets):
                        md_fh.seek(o)
                        yield md_fh.readline()

        def _read_pkg_dirs(self, fmris):
                """Legacy function used to search indexes which have a pkg
                directory with fmri offset information instead of the
                fmri_offsets.v1 file."""

                if not os.path.isdir(os.path.join(self._dir_path, "pkg")):
                        raise search_errors.InconsistentIndexException(
                            self._dir_path)

                if TermQuery.fmris is None and not self.pkg_name_wildcard:
                        TermQuery.fmris = list(fmris())
                pkg_offsets = set()
                for matching_pfmri in (
                    p
                    for p in TermQuery.fmris
                    if self.pkg_name_match(
                        p.get_fmri(anarchy=True, include_scheme=False))
                ):
                        try:
                                fh = open(os.path.join(
                                    self._dir_path, "pkg",
                                    matching_pfmri.get_name(),
                                    str(matching_pfmri.version)), "rb")
                        except EnvironmentError, e:
                                if e.errno != errno.ENOENT:
                                        raise
                                continue
                        for l in fh:
                                pkg_offsets.add(int(l))
                return pkg_offsets

        def _search_internal(self, fmris):
                """Searches the indexes in dir_path for any matches of query
                and the results in self.res.  The method assumes the
                dictionaries have already been loaded and read appropriately.

                The "fmris" parameter is a generator of fmris of installed
                packages."""

                assert self._data_main_dict.get_file_handle() is not None

                glob = self._glob
                term = self._term
                case_sensitive = self._case_sensitive

                if not case_sensitive:
                        glob = True
                # If offsets is equal to None, match all possible results.  A
                # match with no results is represented by an empty set.
                offsets = None

                if glob:
                        # If the term has at least one non-wildcard character
                        # in it, do the glob search.
                        if TermQuery.has_non_wildcard_character.match(term):
                                keys = self._data_token_offset.get_keys()
                                matches = choose(keys, term, case_sensitive)
                                offsets = set([
                                    self._data_token_offset.get_id(match)
                                    for match in matches
                                ])
                elif self._data_token_offset.has_entity(term):
                        offsets = set([
                            self._data_token_offset.get_id(term)])
                # Restrict results by package name.
                if not self.pkg_name_wildcard:
                        try:
                                pkg_offsets = \
                                    self._data_fmri_offsets.get_offsets(
                                        self.pkg_name_match)
                        except AttributeError:
                                pkg_offsets = self._read_pkg_dirs(fmris)
                        if offsets is None:
                                offsets = pkg_offsets
                        else:
                                offsets &= pkg_offsets
                # Restrict results by action type.
                if not self.action_type_wildcard:
                        tmp_set = set()
                        try:
                                fh = open(os.path.join(self._dir_path,
                                    "__at_" + self.action_type), "rb")
                                for l in fh:
                                        tmp_set.add(int(l.strip()))
                                if offsets is None:
                                        offsets = tmp_set
                                else:
                                        offsets &= tmp_set
                        except EnvironmentError, e:
                                if e.errno != errno.ENOENT:
                                        raise
                                # If the file doesn't exist, then no actions
                                # with that action type were indexed.
                                offsets = set()
                # Restrict results by key.
                if not self.key_wildcard:
                        tmp_set = set()
                        try:
                                fh = open(os.path.join(self._dir_path,
                                    "__st_" + self.key), "rb")
                                for l in fh:
                                        tmp_set.add(int(l))
                                if offsets is None:
                                        offsets = tmp_set
                                else:
                                        offsets &= tmp_set
                        except EnvironmentError, e:
                                if e.errno != errno.ENOENT:
                                        raise
                                # If the file doesn't exist, then no actions
                                # with that key were indexed.
                                offsets = set()
                line_iter = EmptyI
                # If offsets isn't None, then the set of results has been
                # restricted so iterate through those offsets.
                if offsets is not None:
                        line_iter = self.__offset_line_read(offsets)
                # If offsets is None and the term was only wildcard search
                # tokens, return results for every known token.
                elif glob and \
                    not TermQuery.has_non_wildcard_character.match(term):
                        line_iter = self._data_main_dict.get_file_handle()
                        
                for line in line_iter:
                        assert not line == '\n'
                        tok, at_lst = \
                            self._data_main_dict.parse_main_dict_line(line)
                        # Check that the token was what was expected.
                        assert ((term == tok) or 
                            (not case_sensitive and
                            term.lower() == tok.lower()) or
                            (glob and fnmatch.fnmatch(tok, term)) or
                            (not case_sensitive and
                            fnmatch.fnmatch(tok.lower(), term.lower())))
                        # Check the various action types this token was
                        # associated with.
                        for at, st_list in at_lst:
                                if not self.action_type_wildcard and \
                                    at != self.action_type:
                                        continue
                                # Check the key types this token and action type
                                # were associated with.
                                for st, fv_list in st_list:
                                        if not self.key_wildcard and \
                                            st != self.key:
                                                continue
                                        # Get the values which matched this
                                        # token.
                                        for fv, p_list in fv_list:
                                                # Get the fmri id and list of
                                                # offsets into the manifest for
                                                # that fmri id.
                                                for p_id, m_off_set in p_list:
                                                        p_id = int(p_id)
                                                        p_str = self._data_manf.get_entity(p_id)
                                                        # Check that the pkg
                                                        # name matches the
                                                        # restrictions, if any
                                                        # exist.
                                                        if not self.pkg_name_wildcard and not self.pkg_name_match(p_str):
                                                                continue
                                                        int_os = [
                                                            int(o)
                                                            for o
                                                            in m_off_set
                                                        ]
                                                        yield (p_str, int_os,
                                                            at, st, fv)
                # Close the dictionaries since there are no more results to
                # yield.
                self._close_dicts()
                return

        @staticmethod
        def __get_key(k):
                p_id, p_str = k
                return p_str
        
        def _get_results(self, res):
                """Takes the results from search_internal ("res") and reads the
                lines from the manifest files at the provided offsets."""

                for fmri_str, offsets, at, st, fv in res:
                        send_res = []
                        f = fmri.PkgFmri(fmri_str)
                        path = self._manifest_path_func(f)
                        # XXX If action size changes substantially, we should
                        # reexamine whether the buffer size should be changed.
                        file_handle = open(path, "rb", buffering=512)
                        for o in sorted(offsets):
                                file_handle.seek(o)
                                send_res.append((fv, at, st,
                                    file_handle.readline()))
                        file_handle.close()
                        for fv, at, st, l in send_res:
                                yield at, st, fmri_str, fv, l
