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

<%@ page import="com.collabnet.svnedge.domain.ServerMode" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>
<content tag="title"><g:message code="server.page.editIntegration.leftNav.header" /></content>

<g:render template="/server/leftNav"/>

<body>
  <p>
    <g:message code="server.page.editIntegration.p1" args="${['<strong><i>' + cmd.ctfURL + '</i></strong>']}" />
  </p>

   <g:set var="tabArray" value="${[[active: true, controller: 'setupReplica', action: 'editCredentials', label: message(code:'server.page.editIntegration.tab.edit')]]}" />
   <g:if test="${isReplica}">
     <g:set var="tabArray" value="${ tabArray << [controller: 'setupReplica', action: 'editConfig', label: message(code:'server.page.editIntegration.tab.editReplicaConfig')]}" />
  </g:if>
   <g:set var="tabArray" value="${ tabArray << [controller: 'server', action: 'editIntegration', label: message(code:'server.page.editIntegration.tab.convert')]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray]}" />

  <g:if test="${isReplica}">
    <p><g:message code="setupReplica.page.editCredentials.p1"/></p>
  </g:if>

  <g:form class="form-horizontal" method="post">
    <fieldset>
    <g:if test="${isReplica}">

      <g:propControlsBody bean="${cmd}" field="ctfURL" prefix="setupReplica.page.ctfInfo">
        ${cmd.ctfURL}
        <g:hiddenField name="ctfURL" value="${cmd.ctfURL}"/>
      </g:propControlsBody>
      <g:propTextField bean="${cmd}" field="ctfUsername" prefix="setupReplica.page.ctfInfo"/>
      <g:propControlsBody bean="${cmd}" field="ctfPassword" prefix="setupReplica.page.ctfInfo">
        <g:passwordField name="ctfPassword" value="${cmd.ctfPassword}" size="20"/>
      </g:propControlsBody>
    </g:if>

    <g:set var="tip"><em><g:message code="ctfConversionBean.serverKey.error.missing" /></em>
          <div>
            <g:message code="setupReplica.page.apiKey.description" />
            <ul>
              <li><g:message code="setupReplica.page.apiKey.hosted" /></li>
              <li><g:message code="setupReplica.page.apiKey.property" /></li>
            </ul>
          </div>
    </g:set>
    <g:propTextField bean="${cmd}" field="serverKey" prefix="ctfConversionBean" tip="${tip}"/>    
    </fieldset>
    <div class="form-actions">
      <g:actionSubmit action="updateCredentials" value="${message(code:'setupTeamForge.page.ctfInfo.button.continue')}" class="btn btn-primary"/>
    </div>
  </g:form>

</body>
</html>
  
