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
<g:set var="tabArray" value="${tabArray << [action:'replicaSetup', label: message(code:'setupReplica.page.tabs.replicaInfo', args:[2])]}"/>
<g:set var="tabArray" value="${tabArray << [active: true, label: message(code:'setupReplica.page.tabs.confirm', args:[3])]}"/>
<g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}"/>

<p><strong><g:message code="setupTeamForge.page.confirm.ready" /></strong> <g:message code="setupReplica.page.confirm.p1" /></p>

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
    <div class="span3"><strong><g:message code="setupReplica.page.confirm.svnMasterURL.label"/></strong></div>
    <div class="span8">
      <g:if test="${selectedScmServer.description}">
        <g:message code="setupReplica.page.confirm.nameDescription" args="${[selectedScmServer.title, selectedScmServer.description]}"/>
      </g:if>
      <g:else>${selectedScmServer.title}</g:else>
    </div>
  </div>

  <div class="row-fluid">
    <div class="span3"><strong><g:message code="setupReplica.page.replicaSetup.description.label"/></strong></div>
    <div class="span8">
      <g:if test="${replicaDescription}">
        <g:message code="setupReplica.page.confirm.nameDescription" args="${[replicaTitle, replicaDescription]}"/>
      </g:if>
      <g:else>${replicaTitle}</g:else>
    </div>
  </div>

  <div class="row-fluid">
    <div class="span3"><strong><g:message code="setupReplica.page.replicaSetup.message.label"/></strong></div>
    <div class="span8">${replicaMessageForAdmin}</div>
  </div>
</div>

<g:form method="post">
  <div class="pull-right">
    <g:actionSubmit action="convert" value="${message(code:'setupTeamForge.page.confirm.button.confirm')}" class="btn btn-primary"/>
  </div>
</g:form>

</body>
</html>
  
