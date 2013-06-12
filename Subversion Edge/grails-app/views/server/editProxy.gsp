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
  <meta name="layout" content="main"/>
</head>
<content tag="title"><g:message code="server.page.editProxy.title"/></content>

<g:render template="leftNav"/>

<body>
<p><g:message code="server.page.editProxy.message.p1"/></p>
<p><g:message code="server.page.editProxy.message.p2"/></p>

<g:form class="form-horizontal" method="post" name="serverForm" action="updateProxy">
  <g:hiddenField name="view" value="editProxy"/>
  <fieldset>
    <g:propTextField bean="${networkConfig}" field="httpProxyHost" prefix="networkConfiguration" required="true"/>
    <g:propTextField bean="${networkConfig}" field="httpProxyPort" prefix="networkConfiguration" required="true" sizeClass="small" integer="true"/>
    <g:propTextField bean="${networkConfig}" field="httpProxyUsername" prefix="networkConfiguration"/>
    <g:propControlsBody bean="${networkConfig}" field="httpProxyPassword" prefix="networkConfiguration">
      <g:passwordFieldWithChangeNotification name="httpProxyPassword" value="${fieldValue(bean:networkConfig, field:'httpProxyPassword')}" size="30"/>
    </g:propControlsBody>
  </fieldset>
  <div class="form-actions">
    <g:actionSubmit action="updateProxy" value="${message(code:'server.page.editAuthentication.button.save')}"
                    class="btn btn-primary"/>
    <g:if test="${fieldValue(bean: networkConfig, field: 'httpProxyHost')}">
      <g:actionSubmit action="removeProxy" value="${message(code:'server.page.editProxy.button.clear')}"
                      class="btn"/>
    </g:if>
    <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
  </div>
</g:form>
</body>
</html>
