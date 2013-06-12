/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.util;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class DelegateFileUpload extends ServletFileUpload {
    private FileUpload delegate;
    
    public DelegateFileUpload(FileUpload upload) {
        this.delegate = upload;
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public FileItemFactory getFileItemFactory() {
        return delegate.getFileItemFactory();
    }

    public long getFileSizeMax() {
        return delegate.getFileSizeMax();
    }

    public String getHeaderEncoding() {
        return delegate.getHeaderEncoding();
    }

    public FileItemIterator getItemIterator(RequestContext ctx) 
            throws FileUploadException, IOException {
        return delegate.getItemIterator(ctx);
    }

    public ProgressListener getProgressListener() {
        return delegate.getProgressListener();
    }

    public long getSizeMax() {
        return delegate.getSizeMax();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public List parseRequest(HttpServletRequest req) throws FileUploadException {
        return delegate.parseRequest(req);
    }

    public List parseRequest(RequestContext arg0) throws FileUploadException {
        return delegate.parseRequest(arg0);
    }

    public void setFileItemFactory(FileItemFactory factory) {
        delegate.setFileItemFactory(factory);
    }

    public void setFileSizeMax(long fileSizeMax) {
        delegate.setFileSizeMax(fileSizeMax);
    }

    public void setHeaderEncoding(String encoding) {
        delegate.setHeaderEncoding(encoding);
    }

    public void setProgressListener(ProgressListener pListener) {
        delegate.setProgressListener(pListener);
    }

    public void setSizeMax(long sizeMax) {
        delegate.setSizeMax(sizeMax);
    }

    public String toString() {
        return delegate.toString();
    }
    

}
