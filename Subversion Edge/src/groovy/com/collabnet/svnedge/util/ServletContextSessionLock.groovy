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
package com.collabnet.svnedge.util

import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.http.HttpSessionBindingListener
import javax.servlet.ServletContext

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

/**
 * Object used for locking a resource via servletContext, so that only
 * one session may hold the lock at a time
 */
public class ServletContextSessionLock implements HttpSessionBindingListener {

    static Log log = LogFactory.getLog(ServletContextSessionLock.class)

    int userId
    final Date createdOn
    private final String attributeKey
    private boolean isLocked
    
    private ServletContextSessionLock(String attributeKey) {
        this.attributeKey = attributeKey
        this.createdOn = new Date()
        this.isLocked = false
    }

    /**
     * This method will create the lock or it will return the existing lock,
     * if it is owned by the calling session.
     * 
     * @param session
     * @param key identifies the purpose of the lock
     * @return the lock if possible; otherwise returns null, if another session
     * already has the lock
     */
    static ServletContextSessionLock obtain(HttpSession session, String key) {
        
        ServletContextSessionLock lock = session[key]
        ServletContext context = session.servletContext
        synchronized (context) {
            if (lock && lock != context[key]) {
                lock = null
            } else {
                if (!context[key]) {
                    lock = new ServletContextSessionLock(key)
                    context[key] = lock
                    session[key] = lock
                    lock.isLocked = true
                }
            }
        }
        return lock
    }

    /**
     * Access the lock if it exists, regardless of who owns it
     * 
     * @param session
     * @param key identifies the purpose of the lock
     * @return the lock or null if no one has obtained the lock
     */
    static ServletContextSessionLock peek(HttpSession session, String key) {    
        ServletContext context = session.servletContext
        synchronized (context) {
            return context[key]
        }
    }
    
    /**
     * Makes the lock available, should be called once the purpose of the lock
     * is complete.
     * 
     * @param session
     */
    void release(HttpSession session) {
        if (isLocked) {
            session[attributeKey] = null
            // the above will fire HttpSessionBindingEvent calling valueUnbound
        }
    }

    /**
     * No-op
     */
    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        // Handled in obtain()
    }

    /**
     * Will release the lock when release() is called or if the session expires
     */
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        if (event.name != attributeKey) {
            throw new IllegalStateException("Lock did not use expected key")
        }
        ServletContext context = event.session.servletContext
        synchronized (context) {
            if (context[attributeKey] == this) {
                context[attributeKey] = null
            }
        }
    }
}
