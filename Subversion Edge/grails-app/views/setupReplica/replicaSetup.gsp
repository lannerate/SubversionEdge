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

<g:set var="tabArray" value="${[]}"/>
<g:set var="tabArray" value="${tabArray << [action:'ctfInfo', label: message(code:'setupReplica.page.tabs.ctfInfo', args:[1])]}"/>
<g:set var="tabArray" value="${tabArray << [active:true, label: message(code:'setupReplica.page.tabs.replicaInfo', args:[2])]}"/>
<g:set var="tabArray" value="${tabArray << [label: message(code:'setupReplica.page.tabs.confirm', args:[3])]}"/>
<g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}"/>

<p><g:message code="setupReplica.page.replicaSetup.p1"/></p>
<g:form class="form-horizontal" method="post">
  <g:propControlsBody bean="${cmd}" field="masterExternalSystemId" required="true" prefix="setupReplica.page.replicaSetup">
              <select name="masterExternalSystemId" class="autoWidth">
                <g:each in="${integrationServers}" var="scmServer">
                    <option value="${scmServer.id}">${scmServer.title}</option>
                </g:each>
              </select>
  </g:propControlsBody>
  <g:propTextField bean="${cmd}" field="name" required="true" prefix="setupReplica.page.replicaSetup"/>
  <g:propControlsBody bean="${cmd}" field="description" required="true" prefix="setupReplica.page.replicaSetup">
    <textarea name="description" id="description" rows="3" class="span6">${fieldValue(bean: cmd, field: 'description')}</textarea>
  </g:propControlsBody>
  <g:propControlsBody bean="${cmd}" field="message" prefix="setupReplica.page.replicaSetup">
    <textarea name="message" id="message" rows="4" class="span6">${fieldValue(bean: cmd, field: 'message')}</textarea>
  </g:propControlsBody>
  <g:if test="${mailConfig}">
    <g:propTextField bean="${mailConfig}" field="repoSyncToAddress" prefix="mailConfiguration"/>
  </g:if>
  <div class="form-actions">
    <g:actionSubmit action="confirm" value="${message(code:'setupTeamForge.page.ctfInfo.button.continue')}" class="btn btn-primary"/>
  </div>
</g:form>

</body>
</html>
  
