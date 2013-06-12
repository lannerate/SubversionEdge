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
  <title><g:message code="setupCloudServices.page.confirmation.title"/></title>
  <meta name="layout" content="main"/>
</head>

<content tag="title">
  <g:message code="setupCloudServices.page.confirmation.title"/>
</content>

<g:render template="/server/leftNav"/>

<body>

<div class="row-fluid">
  <div class="span6">
    <p><g:message code="setupCloudServices.page.confirmation.p1"/></p>
    <ol>
       <li><g:message code="setupCloudServices.page.confirmation.service.backup.detail.step1"/></li>
       <li><g:message code="setupCloudServices.page.confirmation.service.backup.detail.step2"/></li>
       <li><g:message code="setupCloudServices.page.confirmation.service.backup.detail.step3"/></li>
     </ol>
    <p><g:message code="setupCloudServices.page.confirmation.getStarted.prompt"/></p>
    <p><a class="btn" target="_blank" href="http://visit.collab.net/SVNsupport.html"><g:message code="setupCloudServices.page.confirmation.support"/> &raquo;</a></p>
  </div>
  <div class="span6">
     <img alt="" src="${resource(dir:'images/cloud',file:'subversion-edge-to-the-cloud.png')}" border="0"/>
  </div>
</div>


</body>
</html>
