#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright 2009 Sun Microsystems, Inc. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License. You can obtain
# a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
# or updatetool/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
# Sun designates this particular file as subject to the "Classpath" exception
# as provided by Sun in the GPL Version 2 section of the License file that
# accompanied this code.  If applicable, add the following below the License
# Header, with the fields enclosed by brackets [] replaced by your own
# identifying information: "Portions Copyrighted [year]
# [name of copyright owner]"
#
# Contributor(s):
#
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

# cmprepo - script for comparing 2 repositories

from urlparse import urlparse
from pkg.misc import versioned_urlopen
import sys
import traceback
import getopt
import os.path

def usage(msg):
    ret = """Usage: cmprepo [-v|--verbose] repo1  repo2
Options:
    -v | --verbose  will print the detailed report of packages missing in
                    repositories.
    repo1, repo2    repositories to compare, this can be a file:// URL, http://
                    or a https:// URL. If you are using HTTP or HTTPS URL make
                    sure the repository is started and running.
    """
    if msg:
        ret = "\n%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret


def main_func():
    verbose = False

    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "v", ['verbose',])
    except getopt.GetoptError, e:
        usage("cmprepo: illegal option -- %s" % e.opt)
        return 1

    for opt, arg in opts:
        if opt in ("-v", "--verbose"):
            verbose = True

    if len(pargs) != 2:
        usage("invalid arguments: 2 repositories must be mentioned for comparison")
        return 1

    catalog1 = get_catalog(pargs[0])
    catalog2 = get_catalog(pargs[1])
    repo1_extra_pkgs = [val for val in catalog1 if val not in catalog2]
    repo2_extra_pkgs = [val for val in catalog2 if val not in catalog1]

    if len(repo1_extra_pkgs) == 0 and len(repo2_extra_pkgs) == 0:
        print "\nRepositories contains the same set of packages."
    else:
        print "\nRepositories do not have the same set of packages."
        if verbose:
           if len(repo1_extra_pkgs) != 0:
               print "\n%s extra packages in repository %s"%(len(repo1_extra_pkgs) , pargs[0])
               for pkg in repo1_extra_pkgs:
                   print "\t",pkg.lstrip('V').rstrip('\n')
           if len(repo2_extra_pkgs) != 0:
               print "\n%s extra packages in repository %s"%(len(repo2_extra_pkgs) , pargs[1])
               for pkg in repo2_extra_pkgs:
                   print "\t",pkg.lstrip('V').rstrip('\n')

    cmp_repo_version(pargs[0], pargs[1])


def get_catalog(repo_url):
    repo = urlparse(repo_url)
    if repo[0] == 'http' or repo[0] == 'https':
        try:
            response, version = versioned_urlopen(repo_url, "catalog", [0])
        except URLError, e:
            if hasattr(e, 'reason'):
                print 'Failed to reach the repo server ',repo_url
                print 'Reason: ', e.reason
            elif hasattr(e, 'code'):
                print 'The repo server %s couldn\'t fulfill the request.'%repo_url
                print 'Error code: ', e.code
            sys.exit(1)
        else:
            catalog = response.read()
            catalog = [val for val in catalog.split('\n') if val.startswith('V')]
    elif repo[0] == 'file':
        try :
            fn = os.path.join(repo[1], repo[2], "catalog/catalog")
            catf = file(fn, 'r')
            catalog = [val for val in catf]
        except IOError, e:
            print e
            sys.exit(1)
    else:
        usage("invalid arguments: repository url can be http://, https:// or file://")
        sys.exit(1)
    return catalog


def cmp_repo_version(repo1_url, repo2_url):
    repo1 = urlparse(repo1_url)
    repo2 = urlparse(repo2_url)
    repo_version = []
    if (repo1[0] == 'http' or repo1[0] == 'https') and (repo2[0] == 'http' or repo2[0] == 'https'):
        for repo in (repo1_url, repo2_url):
            try:
                response, version = versioned_urlopen(repo, "versions", [0])
            except URLError, e:
                if hasattr(e, 'reason'):
                    print 'We failed to reach the repo server ',repo
                    print 'Reason: ', e.reason
                elif hasattr(e, 'code'):
                    print 'The repo server %s couldn\'t fulfill the request.'%repo
                    print 'Error code: ', e.code
                sys.exit(1)
            else:
                 version = response.readline()
                 repo_version.append(version.lstrip('pkg-server ').rstrip('\n'))

        if version.startswith('pkg-server '):
            if repo_version[0] == repo_version[1]:
                print "\nRepositories are running on same version (%s) of pkg(5) software."%repo_version[0]
            else:
                print "\nRepositories are running on different version of pkg(5) software."
                print "Repository '%s' version =%s"%(repo1_url , repo_version[0])
                print "Repository '%s' version =%s"%(repo2_url , repo_version[1])


if __name__ == "__main__":
    try:
        ret = main_func()
    except SystemExit, e:
        raise e
    except:
        traceback.print_exc()
        sys.exit(99)
    sys.exit(ret)

