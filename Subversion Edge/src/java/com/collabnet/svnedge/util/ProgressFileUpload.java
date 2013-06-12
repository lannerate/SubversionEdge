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

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

public class ProgressFileUpload extends DelegateFileUpload {
    
    public ProgressFileUpload(FileUpload upload) {
        super(upload);
    }

    @Override
    public List parseRequest(HttpServletRequest req) throws FileUploadException {
        SessionProgressListener spl = null;
        try {
            spl = new SessionProgressListener(req);
            setProgressListener(spl);
            return super.parseRequest(req);
        } catch (Exception e) {
            if (null != spl) {
                spl.clearSession();
            }
            // trap and log the exception as it is commonly caused by the user
            // abandoning an upload
            Logger log = Logger.getLogger(getClass());
            log.debug("Exception parsing multipart stream", e);
            log.warn("Exception parsing multipart stream, stacktrace available via DEBUG level");
        }
        return Collections.EMPTY_LIST;
    }
}
