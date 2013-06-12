<html>
<head>
  %{--
  - CollabNet Subversion Edge
  - Copyright (C) 2010, CollabNet Inc. All rights reserved.
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

  <meta name="layout" content="main"/>
</head>
<content tag="title">
  <g:message code="setupReplica.page.title"/>
</content>

<g:render template="/server/leftNav"/>

<body>
<p><g:message code="setupReplica.page.convert.p1"/></p>

<g:form method="post">
<div class="well">
  <div class="row-fluid">
    <div class="span3"><strong><g:message code="setupReplica.page.confirm.ctfURL.label"/></strong></div>
    <div class="span8">${ctfURL}</div>
  </div>

  <div class="row-fluid">
    <div class="span3"><strong><g:message code="setupReplica.page.ctfInfo.ctfUsername.label"/></strong></div>
    <div class="span8">${ctfUsername}</div>
  </div>

  <div class="row-fluid">
    <div class="span3"><strong><g:message code="setupReplica.page.confirm.svnReplicaCheckout.label"/></strong></div>
    <div class="span8">${svnReplicaCheckout}</div>
  </div>
</div>
</g:form>
</body>
</html>
  
