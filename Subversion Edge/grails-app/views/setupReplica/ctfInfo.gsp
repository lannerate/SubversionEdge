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
<g:set var="tabArray" value="${tabArray << [active:true, label: message(code:'setupReplica.page.tabs.ctfInfo', args:[1])]}"/>
<g:set var="tabArray" value="${tabArray << [label: message(code:'setupReplica.page.tabs.replicaInfo', args:[2])]}"/>
<g:set var="tabArray" value="${tabArray << [label: message(code:'setupReplica.page.tabs.confirm', args:[3])]}"/>
<g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}"/>

<p><g:message code="setupReplica.page.ctfInfo.p1"/></p>
<g:form class="form-horizontal" method="post">
  <g:propTextField bean="${cmd}" field="ctfURL" required="true" prefix="setupReplica.page.ctfInfo" skipHtmlEncoding="true"/>
  <g:propTextField bean="${cmd}" field="ctfUsername" required="true" prefix="setupReplica.page.ctfInfo"/>
  <g:propControlsBody bean="${cmd}" field="ctfPassword" required="true" prefix="setupReplica.page.ctfInfo">
    <g:passwordField name="ctfPassword" value="${fieldValue(bean:cmd,field:'ctfPassword')}"/>
  </g:propControlsBody>
  <g:set var="serverKeyTip">
    <em><g:message code="ctfConversionBean.serverKey.error.missing" /></em>
    <div>
    <g:message code="setupReplica.page.apiKey.description" />
    <ul>
      <li><g:message code="setupReplica.page.apiKey.hosted" /></li>
      <li><g:message code="setupReplica.page.apiKey.property" /></li>
    </ul>
    <g:message code="setupReplica.page.apiKey.unavailable" />
    </div>
  </g:set>
  <g:propTextField bean="${cmd}" field="serverKey" prefix="ctfConversionBean" tip="${serverKeyTip}"/>

  <div class="form-actions">
    <g:actionSubmit action="replicaSetup" value="${message(code:'setupTeamForge.page.ctfInfo.button.continue')}" class="btn btn-primary"/>
  </div>
</g:form>

</body>
</html>
  
