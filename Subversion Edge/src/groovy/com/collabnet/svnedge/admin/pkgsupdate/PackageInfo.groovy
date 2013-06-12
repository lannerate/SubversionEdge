/*
 * CollabNet Subversion Edge
 * Copyright (C) 2010, CollabNet Inc. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.collabnet.svnedge.admin.pkgsupdate

import com.sun.pkg.client.Image.FmriState
import com.sun.pkg.client.Image
import com.sun.pkg.client.Manifest

import java.text.DecimalFormat

/**
 * The pkg package information wrapper that consolidates most of the package
 * information from the Image.FmriState class.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
public class PackageInfo {

    /**
     * The primary package state 
     */
    private FmriState packageState
    /**
     * The package manifest containing the descriptive properties
     */
    private Manifest packageManifest

    /**
     * Builds a new package info
     * @param packageInfo is the packageInfo from the pkg 
     */
    private PackageInfo(Image pkgImage, FmriState pkgState) {
        this.packageState = pkgState
        this.packageManifest = new Manifest(pkgImage, pkgState.fmri)
    }

    /**
     * Factory method for the package info, given the state
     * @param pkgState is the package state from the 
     * @return
     */
    public static PackageInfo makeNewPackageInfo(Image pkgImage, 
            FmriState pkgState) {
        return new PackageInfo(pkgImage, pkgState)
    }

    /**
     * @return the original package state.
     */
    public FmriState getFmriState() {
        return this.packageState
    }

    /**
     * @return the name of the package
     */
    def String getName() {
        return this.packageState.fmri.name
    }

    /**
     * @return The summary of the package.
     */
    def String getSummary() {
        return this.packageManifest.getAttribute("pkg.summary")
    }

    /**
     * @return The description of the package.
     */
    def String getDescription() {
        return this.packageManifest.getAttribute("pkg.description")
    }

    /**
     * @return the release version of the package. For 1.0.0-445.56 version, the
     * release number is 1.0.0
     */
    def String getRelease() {
        return this.packageState.fmri.getVersion().getRelease().toString()
    }

    /**
     * @return the build version of the package. For 1.0.0-445.56 version, the
     * build number is 445.56.
     */
    def String getBranch() {
        return this.packageState.fmri.getVersion().getBranch().toString()
    }

    /**
     * @return the published data for the package.
     */
    def Date getPublishedDate() {
        return this.packageState.fmri.getVersion().getPublishDate()
    }

    /**
     * @return The version of the package, which consists of the release number,
     * "-", and branch number.
     */
    def String getVersion() {
        return this.getRelease() + "-" + this.getBranch()
    }

    /**
     * @return the size of the package in bytes
     */
    def float getSize() {
        return this.packageManifest.getPackageSize()
    }

    /**
     * @return the size of the package in Mbytes
     */
    def String getSizeInMB() {
        def kBytes = (float)this.packageManifest.getPackageSize() / (float)1024
        def mBytes = kBytes /(float)1024
        return new DecimalFormat("###,##0.00" ).format(mBytes)
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PackageInfo) {
            def o = (PackageInfo) obj
            return this.getFmriState().equals(o.getFmriState())
        } else return false
    }

    @Override
    public int hashCode() {
        return this.getFmriState().hashCode()
    }
}
