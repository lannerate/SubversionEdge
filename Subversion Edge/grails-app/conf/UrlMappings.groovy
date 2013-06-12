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
class UrlMappings {
    static mappings = {
        "/integration/viewvc/viewvc.cgi/$cgiPathInfo**?" {
            controller = "integration"
            action = "viewvc"
            constraints {
                // apply constraints here
            }
        }


        "/api/$apiVersion/$entity/$id?/$cgiPathInfo**?"(parseRequest: false) {
            controller = { "${params.entity}Rest" }
            action = [GET: "restRetrieve", PUT: "restUpdate",
                    DELETE: "restDelete", POST: "restSave"]
            constraints {
                apiVersion(matches: /1/)
            }
        }


        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
            }
        }

        "/" {
            controller = "status"
            action = "index"
        }

        "500"(view: '/error')

    }
}
