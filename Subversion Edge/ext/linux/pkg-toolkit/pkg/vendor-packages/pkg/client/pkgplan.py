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

import errno
import itertools
import os

import pkg.manifest as manifest
import pkg.actions.directory as directory
from pkg.misc import msg
from pkg.misc import get_pkg_otw_size
from pkg.misc import EmptyI
from pkg.misc import expanddirs

class PkgPlan(object):
        """A package plan takes two package FMRIs and an Image, and produces the
        set of actions required to take the Image from the origin FMRI to the
        destination FMRI.

        If the destination FMRI is None, the package is removed.
        """

        def __init__(self, image, progtrack, check_cancelation):
                self.origin_fmri = None
                self.destination_fmri = None
                self.actions = []
                self.__repair_actions = []

                self.__origin_mfst = manifest.NullCachedManifest
                self.__destination_mfst = manifest.NullCachedManifest
                self.__legacy_info = {}

                self.image = image
                self.__progtrack = progtrack

                self.__xfersize = -1
                self.__xferfiles = -1

                self.__destination_filters = []

                self.check_cancelation = check_cancelation

        def __str__(self):
                s = "%s -> %s\n" % (self.origin_fmri, self.destination_fmri)

                for src, dest in itertools.chain(*self.actions):
                        s += "  %s -> %s\n" % (src, dest)

                return s

        def propose_reinstall(self, fmri, mfst):
                self.destination_fmri = fmri
                self.__destination_mfst = mfst
                self.__legacy_info["version"] = self.destination_fmri.version
                self.origin_fmri = fmri
                self.__origin_mfst = mfst

                if not self.image.install_file_present(fmri):
                        raise RuntimeError, "not installed"

        def propose_repair(self, fmri, mfst, actions):
                self.propose_reinstall(fmri, mfst)
                self.origin_fmri = None

                # Create a list of (src, dst) pairs for the actions to send to
                # execute_repair.  src is none in this case since we aren't
                # upgrading, just repairing.
                lst = [(None, x) for x in actions]

                # Only install actions, no update or remove
                self.__repair_actions = lst

        def propose_destination(self, fmri, mfst):
                self.destination_fmri = fmri
                self.__destination_mfst = mfst
                self.__legacy_info["version"] = self.destination_fmri.version

                if self.image.install_file_present(fmri):
                        raise RuntimeError, "already installed"

        def propose_removal(self, fmri, mfst):
                self.origin_fmri = fmri
                self.__origin_mfst = mfst

                if not self.image.install_file_present(fmri):
                        raise RuntimeError, "not installed"

        def get_actions(self):
                raise NotImplementedError()

        def get_nactions(self):
                return len(self.actions[0]) + len(self.actions[1]) + \
                    len(self.actions[2])

        def update_pkg_set(self, fmri_set):
                """ updates a set of installed fmris to reflect
                proposed new state"""

                if self.origin_fmri:
                        fmri_set.discard(self.origin_fmri)

                if self.destination_fmri:
                        fmri_set.add(self.destination_fmri)

        def evaluate(self, old_excludes=EmptyI, new_excludes=EmptyI):
                """Determine the actions required to transition the package."""
                # if origin unset, determine if we're dealing with an previously
                # installed version or if we're dealing with the null package
                #
                # XXX Perhaps make the pkgplan creator make this explicit, so we
                # don't have to check?
                f = None

                if not self.origin_fmri:
                        f = self.image.older_version_installed(
                            self.destination_fmri)
                        if f:
                                self.origin_fmri = f
                                self.__origin_mfst = \
				    self.image.get_manifest(f)

                # Assume that origin actions are unique, but make sure that
                # destination ones are.
                ddups = self.__destination_mfst.duplicates(new_excludes)
                if ddups:
                        raise RuntimeError, ["Duplicate actions", ddups]

                self.actions = self.__destination_mfst.difference(
                    self.__origin_mfst, old_excludes, new_excludes)

                # figure out how many implicit directories disappear in this
                # transition and add directory remove actions.  These won't
                # do anything unless no pkgs reference that directory in
                # new state....

                # Retrieving origin_dirs first and then checking it for any
                # entries allows avoiding an unnecessary expanddirs for the
                # destination manifest when it isn't needed.
                origin_dirs = expanddirs(self.__origin_mfst.get_directories(
                    old_excludes))

                if origin_dirs:
                        absent_dirs = origin_dirs - \
                            expanddirs(self.__destination_mfst.get_directories(
                            new_excludes))

                        for a in absent_dirs:
                                self.actions[2].append(
                                    [directory.DirectoryAction(path=a), None])

                # Stash information needed by legacy actions.
                self.__legacy_info["description"] = \
                    self.__destination_mfst.get("pkg.summary",
                    self.__destination_mfst.get("description", "none provided"))

                # Add any repair actions to the update list
                self.actions[1].extend(self.__repair_actions)

                #
                # We cross a point of no return here, and throw away the origin
                # and destination manifests; we also delete them from the
                # image cache.
                self.__origin_mfst = None
                self.__destination_mfst = None

        def get_legacy_info(self):
                """ Returns information needed by the legacy action to
                    populate the SVR4 packaging info. """
                return self.__legacy_info

        def get_xferstats(self):
                if self.__xfersize != -1:
                        return (self.__xferfiles, self.__xfersize)

                self.__xfersize = 0
                self.__xferfiles = 0
                for src, dest in itertools.chain(*self.actions):
                        if dest and dest.needsdata(src):
                                self.__xfersize += get_pkg_otw_size(dest)
                                self.__xferfiles += 1

                return (self.__xferfiles, self.__xfersize)

        def will_xfer(self):
                nf, nb = self.get_xferstats()
                if nf > 0:
                        return True
                else:
                        return False

        def get_xfername(self):
                if self.destination_fmri:
                        return self.destination_fmri.get_name()
                if self.origin_fmri:
                        return self.origin_fmri.get_name()
                return None

        def download(self):
                """Download data for any actions that need it."""
                self.__progtrack.download_start_pkg(self.get_xfername())
                mfile = self.image.transport.multi_file(self.destination_fmri,
                    self.__progtrack, self.check_cancelation)

                if not mfile:
                        self.__progtrack.download_end_pkg()
                        return

                for src, dest in itertools.chain(*self.actions):
                        if dest and dest.needsdata(src):
                                mfile.add_action(dest)

                mfile.wait_files() 
                self.__progtrack.download_end_pkg()

        def gen_install_actions(self):
                for src, dest in self.actions[0]:
                        yield src, dest

        def gen_removal_actions(self):
                for src, dest in self.actions[2]:
                        yield src, dest

        def gen_update_actions(self):
                for src, dest in self.actions[1]:
                        yield src, dest

        def execute_install(self, src, dest):
                """ perform action for installation of package"""
                try:
                        dest.install(self, src)
                except Exception, e:
                        msg("Action install failed for '%s' (%s):\n  %s: %s" % \
                            (dest.attrs.get(dest.key_attr, id(dest)),
                             self.destination_fmri.get_pkg_stem(),
                             e.__class__.__name__, e))
                        raise

        def execute_update(self, src, dest):
                """ handle action updates"""
                try:
                        dest.install(self, src)
                except Exception, e:
                        msg("Action upgrade failed for '%s' (%s):\n %s: %s" % \
                             (dest.attrs.get(dest.key_attr, id(dest)),
                             self.destination_fmri.get_pkg_stem(),
                             e.__class__.__name__, e))
                        raise

        def execute_removal(self, src, dest):
                """ handle action removals"""
                try:
                        src.remove(self)
                except Exception, e:
                        msg("Action removal failed for '%s' (%s):\n  %s: %s" % \
                            (src.attrs.get(src.key_attr, id(src)),
                             self.origin_fmri.get_pkg_stem(),
                             e.__class__.__name__, e))
                        raise

        def postexecute(self):
                """Perform actions required after install or remove of a pkg.

                This method executes each action's postremove() or postinstall()
                methods, as well as any package-wide steps that need to be taken
                at such a time.
                """
                # record that package states are consistent
                for src, dest in itertools.chain(*self.actions):
                        if dest:
                                dest.postinstall(self, src)
                        else:
                                src.postremove(self)

                # For an uninstall or an upgrade, remove the installation
                # turds from the origin's directory.
                # XXX should this just go in preexecute?
                if self.destination_fmri == None or self.origin_fmri != None:
                        self.image.remove_install_file(self.origin_fmri)

                        try:
                                os.unlink("%s/pkg/%s/filters" % (
                                    self.image.imgdir,
                                    self.origin_fmri.get_dir_path()))
                        except EnvironmentError, e:
                                if e.errno != errno.ENOENT:
                                        raise

                if self.destination_fmri != None:
                        self.image.add_install_file(self.destination_fmri)

                        # Save the filters we used to install the package, so
                        # they can be referenced later.
                        if self.__destination_filters:
                                f = file("%s/pkg/%s/filters" % \
                                    (self.image.imgdir,
                                    self.destination_fmri.get_dir_path()), "w")

                                f.writelines([
                                    myfilter + "\n"
                                    for myfilter, code in \
				        self.__destination_filters
                                ])
                                f.close()

