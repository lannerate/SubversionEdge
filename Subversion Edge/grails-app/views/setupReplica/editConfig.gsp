%{--
  - CollabNet Subversion Edge
  - Copyright 2012, CollabNet Inc. All rights reserved.
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
  <meta name="layout" content="main"/>
</head>
<content tag="title"><g:message code="server.page.editIntegration.leftNav.header" /></content>

<g:render template="/server/leftNav"/>

<body>
  <p>
    <g:message code="server.page.editIntegration.p1" args="${['<strong><i>' + ctfURL + '</i></strong>']}" />
  </p>

   <g:set var="tabArray" value="${[[controller: 'setupReplica', action: 'editCredentials', label: message(code:'server.page.editIntegration.tab.edit')]]}" />
   <g:set var="tabArray" value="${ tabArray << [active: true, controller: 'setupReplica', action: 'editConfig', label: message(code:'server.page.editIntegration.tab.editReplicaConfig')]}" />
   <g:set var="tabArray" value="${ tabArray << [controller: 'server', action: 'editIntegration', label: message(code:'server.page.editIntegration.tab.convert')]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray]}" />

  <g:form class="form-horizontal" method="post">
    <fieldset>
      <g:propTextField bean="${config}" field="commandRetryAttempts" prefix="setupReplica.page.editConfig" sizeClass="small" integer="true"/>
      <g:propTextField bean="${config}" field="commandRetryWaitSeconds" prefix="setupReplica.page.editConfig" sizeClass="small" integer="true"/>
    </fieldset>
    <div class="form-actions">
      <g:actionSubmit action="updateConfig" value="${message(code:'default.button.save.label')}" class="btn btn-primary"/>
    </div>
  </g:form>

</body>
</html>
  
