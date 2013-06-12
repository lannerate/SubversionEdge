%{--
  - CollabNet Subversion Edge
  - Copyright (C) 2011, CollabNet Inc. All rights reserved.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<html>
    <head>
        <meta name="layout" content="main" />
        <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
    </head>

<g:render template="leftNav" />

<content tag="title">
   <g:message code="repository.page.backup.header" />
</content>

    <body>

    <g:render template="backupScheduleForm"/>

    </body>
<content tag="bottomOfBody">
    <g:javascript library="listView-3.0.0"/>
    <g:javascript library="jquery.dataTables.min"/>
    <g:javascript library="DT_bootstrap"/>
</content>
</html>
