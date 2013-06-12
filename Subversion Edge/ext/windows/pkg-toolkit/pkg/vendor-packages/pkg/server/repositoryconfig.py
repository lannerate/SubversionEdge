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

# Copyright 2009 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.

import ConfigParser
import pkg.misc as misc
import pkg.Uuid25 as uuid

ATTR_TYPE_STR = 0
ATTR_TYPE_INT = 1
ATTR_TYPE_FLOAT = 2
ATTR_TYPE_BOOL = 3
ATTR_TYPE_UUID = 4
ATTR_TYPE_URI = 5
ATTR_TYPE_URI_LIST = 6
ATTR_TYPE_PUB_ALIAS = 7
ATTR_TYPE_PUB_PREFIX = 8
ATTR_TYPE_REPO_COLL_TYPE = 9

class InvalidAttributeError(Exception):
        """Exception class used to indicate an invalid attribute.
        """
        def __init__(self, *args):
                """Standard init override for Exception class."""
                Exception.__init__(self, *args)

class InvalidAttributeValueError(Exception):
        """Exception class used to indicate an invalid attribute value.
        """
        def __init__(self, *args):
                """Standard init override for Exception class."""
                Exception.__init__(self, *args)

class ReadOnlyAttributeError(Exception):
        """Exception class used to indicate when an attempt to set a read-only
        value was made.
        """
        def __init__(self, *args):
                """Standard init override for Exception class."""
                Exception.__init__(self, *args)

class RepositoryConfig(object):
        """A RepositoryConfig object is a collection of configuration
        information and metadata for a repository.
        """

        # This data structure defines the list of possible attributes for a
        # repository along with two optional attributes: default and readonly.
        _attrs = {
            "publisher": {
                "alias": {
                    "type": ATTR_TYPE_PUB_ALIAS,
                },
                "prefix": {
                    "type": ATTR_TYPE_PUB_PREFIX,
                },
            },
            "repository": {
                "collection_type": {
                    "type": ATTR_TYPE_REPO_COLL_TYPE,
                    "default": "core",
                },
                "description": {},
                "detailed_url": {
                    "type": ATTR_TYPE_URI,
                    "default": "http://www.opensolaris.com"
                },
                "legal_uris": {
                    "type": ATTR_TYPE_URI_LIST
                },
                "maintainer": {
                    "default":
                        "Project Indiana <indiana-discuss@opensolaris.org>"
                },
                "maintainer_url": {
                    "type": ATTR_TYPE_URI,
                    "default": "http://www.opensolaris.org/os/project/indiana/"
                },
                "mirrors": {
                    "type": ATTR_TYPE_URI_LIST
                },
                "name": {
                    "default": "package repository"
                },
                "origins": {
                    "type": ATTR_TYPE_URI_LIST
                },
                "refresh_seconds": {
                    "type": ATTR_TYPE_INT,
                    "default": 4 * 60 * 60, # default is 4 hours
                },
                "registration_uri": {
                    "type": ATTR_TYPE_URI,
                },
                "related_uris": {
                    "type": ATTR_TYPE_URI_LIST
                },
            },
            "feed": {
                "id": {
                    "type": ATTR_TYPE_UUID,
                    "readonly": True,
                },
                "name": {
                    "default": "opensolaris.org repository feed"
                },
                "description": {},
                "icon": {
                    "default": "web/_themes/pkg-block-icon.png"
                },
                "logo": {
                    "default": "web/_themes/pkg-block-logo.png"
                },
                "window": {
                    "type": ATTR_TYPE_INT,
                    "default": 24
                },
            },
        }

        def __init__(self, pathname=None):
                """Initializes a RepositoryConfig object.

                Will read existing configuration data from pathname, if
                specified.
                """

                if pathname:
                        # If a pathname was provided, read the data in.
                        self.read(pathname)
                else:
                        # Otherwise, initialize to default state.
                        self.__reset()

        def __str__(self):
                """Returns a string representation of the configuration
                object.
                """
                return "%s" % self.cfg_cache

        def __reset(self):
                """Returns the configuration object to its default state.
                """
                self.cfg_cache = {}
                for section in self._attrs:
                        sattrs = self._attrs[section]
                        for attr in sattrs:
                                info = sattrs[attr]
                                default = info.get("default", None)

                                atype = self.get_attribute_type(section, attr)
                                if default is None and \
                                    atype == ATTR_TYPE_URI_LIST:
                                        default = []

                                if section not in self.cfg_cache:
                                        self.cfg_cache[section] = {}

                                self.cfg_cache[section][attr] = default

        @classmethod
        def is_valid_attribute(cls, section, attr, raise_error=False):
                """Returns a boolean indicating whether the given attribute
                is valid for the specified section.

                This function will raise an exception instead of returning a
                boolean is raise_error=True is specified.
                """
                if section not in cls._attrs:
                        if raise_error:
                                raise InvalidAttributeError("Invalid "
                                    " attribute. Unknown section: %s." % \
                                    (section))
                        else:
                                return False
                if attr not in cls._attrs[section]:
                        if raise_error:
                                raise InvalidAttributeError("Invalid "
                                    "attribute %s.%s." % \
                                    (section, attr))
                        else:
                                return False
                return True

        @classmethod
        def get_attribute_type(cls, section, attr):
                """Returns a numeric value indicating the data type of the
                given attribute for the specified section.

                The return value corresponds to one of the following module
                constants which matches a Python data type:
                    ATTR_TYPE_STR               str
                    ATTR_TYPE_INT               int
                    ATTR_TYPE_FLOAT             float
                    ATTR_TYPE_BOOL              boolean
                    ATTR_TYPE_UUID              str
                    ATTR_TYPE_URI               str
                    ATTR_TYPE_URI_LIST          list of str
                    ATTR_TYPE_PUB_ALIAS         str
                    ATTR_TYPE_PUB_PREFIX        str
                    ATTR_TYPE_REPO_COLL_TYPE    str
                """
                if cls.is_valid_attribute(section, attr, raise_error=True):
                        info = cls._attrs[section][attr]
                        return info.get("type", ATTR_TYPE_STR)
                else:
                        return False

        @classmethod
        def is_valid_attribute_value(cls, section, attr, value,
            raise_error=False):
                """Returns a boolean indicating whether the given attribute
                value is valid for the specified section and attribute.

                This function will raise an exception instead of returning a
                boolean is raise_error=True is specified.
                """

                def validate_uri(uri):
                        try:
                                valid = misc.valid_pub_url(uri)
                        except KeyboardInterrupt:
                                raise
                        except:
                                valid = False

                        if not valid:
                                raise ValueError()

                if cls.is_valid_attribute(section, attr,
                    raise_error=raise_error):
                        atype = cls.get_attribute_type(section, attr)
                        # If the type is string, we always assume it is valid.
                        # For all other types, we attempt a forced conversion
                        # of the value; if it fails, we know the value isn't
                        # valid for the given type.
                        try:
                                if atype == ATTR_TYPE_STR:
                                        return True
                                elif atype == ATTR_TYPE_INT:
                                        int(value)
                                elif atype == ATTR_TYPE_FLOAT:
                                        float(value)
                                elif atype == ATTR_TYPE_BOOL:
                                        if str(value) not in ("True", "False"):
                                                raise TypeError
                                elif atype == ATTR_TYPE_UUID:
                                        # None is valid for configuration
                                        # purposes, even though UUID would
                                        # fail.
                                        if value is not None:
                                                uuid.UUID(hex=str(value))
                                elif atype == ATTR_TYPE_URI:
                                        if value in (None, ""):
                                                return True
                                        validate_uri(value)
                                elif atype == ATTR_TYPE_URI_LIST:
                                        if not isinstance(value, list):
                                                raise TypeError
                                        for u in value:
                                                validate_uri(u)
                                elif atype in (ATTR_TYPE_PUB_ALIAS,
                                    ATTR_TYPE_PUB_PREFIX):
                                        # For now, values are not required.
                                        if value in (None, ""):
                                                return True

                                        # The same rules that apply to publisher
                                        # prefixes also apply to aliases (for
                                        # now).
                                        if not misc.valid_pub_prefix(value):
                                                raise ValueError()
                                elif atype == ATTR_TYPE_REPO_COLL_TYPE:
                                        if str(value) not in ("core",
                                            "supplemental"):
                                                raise TypeError
                                else:
                                        raise RuntimeError(
                                            "Unknown attribute type: %s" % \
                                            atype)
                        except (TypeError, ValueError, OverflowError):
                                if raise_error:
                                        raise InvalidAttributeValueError(
                                            "Invalid value for %s.%s." % \
                                            (section, attr))
                                else:
                                        return False
                else:
                        return False
                return True

        @classmethod
        def is_readonly_attribute(cls, section, attr):
                """Returns a boolean indicating whether the given attribute
                is read-only.
                """
                if cls.is_valid_attribute(section, attr, raise_error=True):
                        info = cls._attrs[section][attr]
                        return info.get("readonly", False)

        @classmethod
        def get_attributes(cls):
                """Returns a dictionary of all attribute sections with each
                section's attributes as a list.
                """
                return dict(
                    (section, [attr for attr in cls._attrs[section]])
                        for section in cls._attrs
                )

        def get_attribute(self, section, attr):
                """Returns the value of the specified attribute for the given
                section.
                """
                if self.is_valid_attribute(section, attr, raise_error=True):
                        return self.cfg_cache[section][attr]

        def _set_attribute(self, section, attr, value):
                """Sets the value of a given configuration attribute for the
                specified section.

                This method does not check the read-only status of an attribute
                and is intended for internal use.
                """
                self.is_valid_attribute_value(section, attr, value,
                    raise_error=True)

                atype = self.get_attribute_type(section, attr)
                if atype == ATTR_TYPE_INT:
                        self.cfg_cache[section][attr] = int(value)
                elif atype == ATTR_TYPE_FLOAT:
                        self.cfg_cache[section][attr] = float(value)
                elif atype == ATTR_TYPE_BOOL:
                        if str(value) == "True":
                                self.cfg_cache[section][attr] = True
                        else:
                                self.cfg_cache[section][attr] = False
                else:
                        # Treat all remaining types as a simple value.
                        self.cfg_cache[section][attr] = value

        def set_attribute(self, section, attr, value):
                """Sets a given configuration attribute to the specified
                value for the specified section.

                This function will raise an exception if the specified
                attribute is read-only.
                """
                if not self.is_readonly_attribute(section, attr):
                        return self._set_attribute(section, attr, value)
                else:
                        raise ReadOnlyAttributeError("%s.%s is read-only." % \
                            (attr, section))

        def read(self, pathname):
                """Reads the specified pathname and populates the configuration
                object based on the data contained within.  The file is
                expected to be in a ConfigParser-compatible format.
                """

                # Reset to initial state to ensure we only have default values
                # so that any values not overwritten by the saved configuration
                # will be correct.
                self.__reset()

                cp = ConfigParser.SafeConfigParser()

                r = cp.read(pathname)
                if len(r) == 0:
                        raise RuntimeError(_("Unable to locate or read the "
                            "specified repository configuration file: "
                            "'%s'.") % pathname)

                assert r[0] == pathname
                for section in self._attrs:
                        for attr in self._attrs[section]:
                                atype = self.get_attribute_type(section, attr)
                                try:
                                        # Retrieve the value as a string first
                                        # to prevent ConfigParser from causing
                                        # an exception.
                                        value = cp.get(section, attr)

                                        # The list types are special in that
                                        # they must be converted first before
                                        # validation.
                                        if atype == ATTR_TYPE_URI_LIST:
                                                uris = []
                                                for u in value.split(","):
                                                        if u:
                                                                uris.append(u)
                                                value = uris

                                        self.is_valid_attribute_value(
                                            section, attr, value,
                                            raise_error=True)

                                        if atype == ATTR_TYPE_INT:
                                                value = cp.getint(section,
                                                    attr)
                                        elif atype == ATTR_TYPE_FLOAT:
                                                value = cp.getfloat(section,
                                                    attr)
                                        elif atype == ATTR_TYPE_BOOL:
                                                value = cp.getboolean(section,
                                                    attr)

                                        self.cfg_cache[section][attr] = value

                                except (ConfigParser.NoSectionError,
                                    ConfigParser.NoOptionError):
                                        # Skip any missing attributes.
                                        continue

        def write(self, pathname):
                """Saves the current configuration object to the specified
                pathname using ConfigParser.
                """
                cp = ConfigParser.SafeConfigParser()

                for section in self._attrs:
                        cp.add_section(section)
                        for attr in self._attrs[section]:
                                value = self.cfg_cache[section][attr]

                                atype = self.get_attribute_type(section, attr)
                                if atype == ATTR_TYPE_URI_LIST:
                                        value = ",".join(value)

                                if value is not None:
                                        cp.set(section, attr, str(value))
                                else:
                                        # Force None to be an empty string.
                                        cp.set(section, attr, "")

                try:
                        f = open(pathname, "w")
                except IOError, (errno, strerror):
                        raise RuntimeError("Unable to open %s for writing: "
                            "%s" % (pathname, strerror))
                cp.write(f)

