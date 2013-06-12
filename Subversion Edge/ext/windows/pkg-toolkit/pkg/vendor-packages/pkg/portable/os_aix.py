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
#

"""
Most if not all of the os_unix methods apply on AIX. The methods
below override the definitions from os_unix
"""

import os
import errno
from os_unix import \
    get_isainfo, get_release, get_platform, get_group_by_name, \
    get_user_by_name, get_name_by_gid, get_name_by_uid, is_admin, get_userid, \
    get_username, rename, remove, link, split_path, get_root, copyfile

def chown(path, owner, group):
        # The "nobody" user on AIX has uid -2, which is an invalid UID on NFS
        # file systems mounted from non-AIX hosts.
        # However, we don't want to fail an install because of this.
        try:
                return os.chown(path, owner, group)
        except EnvironmentError, e:
                if owner == -2 and e.errno == errno.EINVAL:
                        return os.chown(path, -1, group)
                raise



