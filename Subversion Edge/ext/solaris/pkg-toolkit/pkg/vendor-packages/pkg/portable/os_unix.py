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
#

"""
Generic implementation of OS support for most unix-derived OSs.
In general, if os.type is "posix", most if not all of these methods
can be used.  For any that are unusable (or where a better
implementation exists), a module can be provided which overrides
these.
"""

import pwd
import grp
import errno
import os
import platform
import shutil
import sys
import util as os_util
# used to cache contents of passwd and group files
users = {}
uids = {}
users_lastupdate = {}
groups = {}
gids = {}
groups_lastupdate = {}

# storage to repeat first call to group routines
__been_here = {}

def already_called():
        callers_name = sys._getframe(1).f_code.co_name
        if callers_name in __been_here:
                return True
        else:
                __been_here[callers_name] = True
                return False

def get_isainfo():
        return platform.uname()[5]	

def get_release():
        return os_util.get_os_release()

def get_platform():
        return platform.uname()[4]

def get_userid():
        # If the software is being executed with pfexec, the uid or euid will
        # likely be 0 which is of no use.  Since the os.getlogin() interface
        # provided by Python breaks in a number of interesting ways, their
        # recommendation is to pull the username from the environment instead.
        user = os.getenv('USER', os.getenv('LOGNAME', os.getenv('USERNAME')))
        if user:
                try:
                        return get_user_by_name(user, None, False)
                except KeyError:
                        # The environment username wasn't valid.
                        pass
        return os.getuid()

def get_username():
        if not already_called():
                get_username()
        return pwd.getpwuid(get_userid()).pw_name

def is_admin():
        return os.getuid() == 0

def get_group_by_name(name, dirpath, use_file):
        if not already_called():
                get_group_by_name(name, dirpath, use_file)

        if not use_file:
                return grp.getgrnam(name).gr_gid
        try: 
                load_groups(dirpath)
                return groups[dirpath][name].gr_gid
        except OSError, e:
                if e.errno != errno.ENOENT:
                        raise
                # If the password file doesn't exist, bootstrap
                # ourselves from the current environment.
                return grp.getgrnam(name).gr_gid
        except KeyError:
                raise KeyError, "group name not found: %s" % name

def get_user_by_name(name, dirpath, use_file):
        if not already_called():
                get_user_by_name(name, dirpath, use_file)

        if not use_file:
                return pwd.getpwnam(name).pw_uid
        try: 
                load_passwd(dirpath)
                return users[dirpath][name].pw_uid
        except OSError, e:
                if e.errno != errno.ENOENT:
                        raise
                # If the password file doesn't exist, bootstrap
                # ourselves from the current environment.
                return pwd.getpwnam(name).pw_uid
        except KeyError:
                raise KeyError, "user name not found: %s" % name

def get_name_by_gid(gid, dirpath, use_file):
        if not already_called():
                get_name_by_gid(gid, dirpath, use_file)

        if not use_file:
                return grp.getgrgid(gid).gr_name
        try: 
                load_groups(dirpath)
                return gids[dirpath][gid].gr_name
        except OSError, e:
                if e.errno != errno.ENOENT:
                        raise
                # If the password file doesn't exist, bootstrap
                # ourselves from the current environment.
                return grp.getgrgid(gid).gr_name
        except KeyError:
                raise KeyError, "group ID not found: %s" % gid

def get_name_by_uid(uid, dirpath, use_file):
        if not already_called():
                get_name_by_uid(uid, dirpath, use_file)

        if not use_file:
                return pwd.getpwuid(uid).pw_name
        try: 
                load_passwd(dirpath)
                return uids[dirpath][uid].pw_name
        except OSError, e:
                if e.errno != errno.ENOENT:
                        raise
                # If the password file doesn't exist, bootstrap
                # ourselves from the current environment.
                return pwd.getpwuid(uid).pw_name
        except KeyError:
                raise KeyError, "user ID not found: %d" % uid

def load_passwd(dirpath):
        # check if we need to reload cache
        passwd_file = os.path.join(dirpath, "etc/passwd")
        passwd_stamp = os.stat(passwd_file).st_mtime
        if passwd_stamp <= users_lastupdate.get(dirpath, -1):
                return
        users[dirpath] = user = {}
        uids[dirpath] = uid = {}
        f = file(passwd_file)
        for line in f:
                arr = line.rstrip().split(":")
                if len(arr) != 7:
                        # Skip any line we can't make sense of.
                        continue
                try:
                        arr[2] = int(arr[2])
                        arr[3] = int(arr[3])
                except ValueError:
                        # Skip any line we can't make sense of.
                        continue
                pw_entry = pwd.struct_passwd(arr)

                user[pw_entry.pw_name] = pw_entry
                # Traditional systems allow multiple users to have the same
                # user id, so only the first one should be mapped to the
                # current pw_entry.
                uid.setdefault(pw_entry.pw_uid, pw_entry)

        users_lastupdate[dirpath] = passwd_stamp
        f.close()

def load_groups(dirpath):
        # check if we need to reload cache
        group_file = os.path.join(dirpath, "etc/group")
        group_stamp = os.stat(group_file).st_mtime
        if group_stamp <= groups_lastupdate.get(dirpath, -1):
                return
        groups[dirpath] = group = {}
        gids[dirpath] = gid = {}
        f = file(group_file)
        for line in f:
                arr = line.rstrip().split(":")
                if len(arr) != 4:
                        # Skip any line we can't make sense of.
                        continue
                try:
                        arr[2] = int(arr[2])
                except ValueError:
                        # Skip any line we can't make sense of.
                        continue
                gr_entry = grp.struct_group(arr)

                group[gr_entry.gr_name] = gr_entry
                # Traditional systems allow multiple groups to have the same
                # group id, so only the first one should be mapped to the
                # current pw_entry.
                gid.setdefault(gr_entry.gr_gid, gr_entry)

        groups_lastupdate[dirpath] = group_stamp
        f.close()

def chown(path, owner, group):
        return os.chown(path, owner, group)

def rename(src, dst):
        os.rename(src, dst)

def remove(path):
        os.unlink(path)

def link(src, dst):
        os.link(src, dst)
        
def split_path(path):
        return path.split('/')

def get_root(path):
        return '/'

def copyfile(src, dst):
        shutil.copyfile(src, dst)

