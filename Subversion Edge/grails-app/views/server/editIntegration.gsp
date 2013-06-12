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
    <title>CollabNet Subversion Edge <g:message code="server.page.editIntegration.title" /></title>
      <meta name="layout" content="main" />
  </head>
  <content tag="title"><g:message code="server.page.editIntegration.leftNav.header" /></content>

  <g:render template="leftNav" />

  <body>

    <p>
     <g:message code="server.page.editIntegration.p1" args="${['<strong><i>' + ctfServerBaseUrl + '</i></strong>']}" />
    </p>

   <g:set var="tabArray" value="${[[controller: 'setupReplica', action: 'editCredentials', label: message(code:'server.page.editIntegration.tab.edit')]]}" />
   <g:if test="${isReplica}">
     <g:set var="tabArray" value="${ tabArray << [controller: 'setupReplica', action: 'editConfig', label: message(code:'server.page.editIntegration.tab.editReplicaConfig')]}" />
  </g:if>
   <g:set var="tabArray" value="${ tabArray << [active:true, label: message(code:'server.page.editIntegration.tab.convert')]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray]}" />

           <p>
            <g:message code="server.page.editIntegration.p2"/>
           </p>
            <ul>
                <li><g:message code="server.page.editIntegration.bullet1" /></li>
                <li><g:message code="server.page.editIntegration.bullet2" />
            </ul>

            <p><g:message code="server.page.editIntegration.p3" /></p>
            <ul>
               <li><g:message code="server.page.editIntegration.bullet3" /></li>
            </ul>

          <g:if test="${formError}">
            <div class="alert alert-block alert-error">
                ${formError}
                <g:if test="${errorCause}">
                    <ul>
                        <li>${errorCause}</li>
                    </ul>
                </g:if>
            </div>
          </g:if>
    <br/>
    <g:form class="form-horizontal" method="post" action="revert">
      <fieldset>
      <div class="control-group">
        <span class="control-label"><g:message code="server.page.editIntegration.ctfUrl.label"/></span>
        <div class="controls readonly">${ctfServerBaseUrl}</div>
      </div>      
      <g:propTextField bean="${ctfCredentials}" field="ctfUsername" required="true" prefix="server.page.editIntegration"/>
      <g:propControlsBody bean="${ctfCredentials}" field="ctfPassword" required="true" prefix="server.page.editIntegration">
        <input type="password" id="ctfPassword" name="ctfPassword" 
            value="${fieldValue(bean:ctfCredentials,field:'ctfPassword')}"/>
      </g:propControlsBody>
      </fieldset>
      <div class="form-actions">
        <g:actionSubmit action="revert" value="${message(code:'server.page.editIntegration.button.convert')}" class="btn btn-primary"/>
        <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
      </div>
    </g:form>
  </body>
</html>
