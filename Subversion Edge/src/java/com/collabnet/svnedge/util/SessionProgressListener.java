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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.log4j.Logger;

public class SessionProgressListener implements ProgressListener {
        
    public static final String QUERY_PARAM_KEY = "uploadProgressKey";
    private static final long STEP_SIZE = 2;
    
    private final HttpSession session;
    private final Map<String, Object> stats;
    private final long startTimeMillis;
    private long lastProgress = -1;
    private final String sessionKey;
    private final Logger log;

    public SessionProgressListener(HttpServletRequest request) {
        startTimeMillis = System.currentTimeMillis();
        this.session = request.getSession(false);
        this.stats = new HashMap<String, Object>();
        String qs = request.getQueryString();
        String name = QUERY_PARAM_KEY + "=";
        String key = null;
        if (qs != null) {
            int start = qs.indexOf(name);
            if (start >= 0) {
                int end = qs.indexOf('&', start);
                if (end < start) {
                    end = qs.length();
                }
                key = qs.substring(start + name.length(), end);
            }
        }
        if (null == key) {
            key = "uploadStatsDefaultKey";
        }
        request.setAttribute("uploadStatsSessionKey", key);
        session.setAttribute(key, stats);
        sessionKey = key;
        log = Logger.getLogger(getClass());
    }

    public void clearSession() {
        if (null != sessionKey) {
            session.removeAttribute(sessionKey);
        }
    }
    
    public void update(long bytesRead, long contentLength, int item) {
        long blockSize = contentLength * STEP_SIZE / 100L;
        if (blockSize > 0) {
            long progress = bytesRead / blockSize;
            if (progress > lastProgress) {
                stats.put("bytesRead", bytesRead);
                stats.put("contentLength", contentLength);
                stats.put("fileNumber", item);
                stats.put("startTimeMillis", startTimeMillis);
                stats.put("currentTimeMillis", System.currentTimeMillis());
                stats.put("percentComplete", progress * STEP_SIZE);
                lastProgress = progress;
                if (log.isDebugEnabled()) {
                    log.debug("Upload progress=" + stats);
                }
            }
        }     
    }
}
