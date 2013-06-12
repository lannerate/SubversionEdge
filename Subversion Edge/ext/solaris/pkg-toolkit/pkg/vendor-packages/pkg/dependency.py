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
# Copyright 2008 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.
#

REQUIRE = 0
OPTIONAL = 1
INCORPORATE = 10

class Dependency(object):
        """A Dependency object is a relationship between one Package and
        another.  It is a bidirectional expression.

        A package may require a minimum version of another package."""

        def __init__(self, host_pkg_fmri, req_pkg_fmri, type = REQUIRE):
                self.host_pkg_fmri = host_pkg_fmri
                self.req_pkg_fmri = req_pkg_fmri

                assert type == REQUIRE \
                    or type == INCORPORATE \
                    or type == OPTIONAL

                self.type = type

        def satisfied(self, pkg_fmri):
                # compare pkg_fmri to req_pkg_fmri
                # compare versions
                return False

	def __repr__(self):
		if self.type == REQUIRE:
			return "%s => %s" % \
				(self.host_pkg_fmri, self.req_pkg_fmri)
		elif self.type == OPTIONAL:
			return "%s o> %s" % \
				(self.host_pkg_fmri, self.req_pkg_fmri)
		elif self.type == INCORPORATE:
			return "%s >> %s" % \
				(self.host_pkg_fmri, self.req_pkg_fmri)
