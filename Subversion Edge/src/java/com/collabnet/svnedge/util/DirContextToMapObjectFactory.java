/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package com.collabnet.svnedge.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.spi.DirObjectFactory;


/**
 * A JNDI ObjectFactory, which transforms a DirContext object into a map which contains its attributes.
 * This class was taken from http://directory.apache.org/api/groovy-ldap.html and is being 
 * used under the APLv2 license.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirContextToMapObjectFactory implements DirObjectFactory
{

    public Object getObjectInstance( Object obj, Name name, Context ctx, Hashtable env, Attributes attrs )
        throws Exception
    {

        if ( obj instanceof DirContext )
        {

            DirContext dctx = ( DirContext ) obj;

            Map<String, Object> map = new HashMap<String, Object>();
            map.put( "dn", dctx.getNameInNamespace() );

            Attributes as = dctx.getAttributes( "" );
            NamingEnumeration<? extends Attribute> e = as.getAll();
            while ( e.hasMore() )
            {
                Attribute attribute = e.next();
                String attrName = attribute.getID().toLowerCase();

                if ( attribute.size() == 1 )
                {
                    map.put( attrName, attribute.get() );
                }
                else
                {
                    List<Object> l = new ArrayList<Object>();
                    for ( int i = 0; i < attribute.size(); ++i )
                    {
                        l.add( attribute.get( i ) );
                    }
                    map.put( attrName, l );
                }
            }
            return map;
        }
        else
        {
            return null;
        }
    }


    public Object getObjectInstance( Object obj, Name name, Context ctx, Hashtable env )
    {
        return null;
    }
}
