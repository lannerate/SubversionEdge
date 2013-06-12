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
# Copyright 2008 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.

class ApiException(Exception):
        """Base exception class for all server.api exceptions."""
        def __init__(self, *args):
                Exception.__init__(self, *args)
                if args:
                        self.data = args[0]

        def __str__(self):
                return str(self.data)

class VersionException(ApiException):
        """Exception used to indicate that the client's requested api version
        is not supported.
        """
        def __init__(self, expected_version, received_version):
                ApiException.__init__(self)
                self.expected_version = expected_version
                self.received_version = received_version

        def __str__(self):
                return "Incompatible API version '%s' specified; " \
                    "expected: '%s'." % (self.received_version,
                    self.expected_version)

class RedirectException(ApiException):
        """Used to indicate that the client should be redirected to a new
        URI.
        """
        pass

