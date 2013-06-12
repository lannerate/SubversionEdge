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
  <title>CollabNet Subversion Edge <g:message code="admin.page.leftNav.cloudServices"/></title>
  <meta name="layout" content="main"/>
</head>

<content tag="title"><g:message code="setupCloudServices.page.credentials.title"/></content>

<g:render template="/server/leftNav"/>
<body>
<g:form class="form-horizontal">

  <g:if test="${existingCredentials}">
    <div class="control-group required-field">
      <span class="control-label"><g:message code="setupCloudServices.page.signup.domain.label"/></span>
      <div class="controls readonly">${cmd.domain}</div>
    </div>
    <div class="control-group required-field">
      <span class="control-label"><g:message code="setupCloudServices.page.signup.username.label"/></span>
      <div class="controls readonly">${cmd.username}</div>
    </div>    
  </g:if>
  <g:else>
    <g:propTextField bean="${cmd}" field="domain" required="true" prefix="setupCloudServices.page.signup"/>
    <g:propTextField bean="${cmd}" field="username" required="true" prefix="setupCloudServices.page.signup"/>
  </g:else>

  <g:propControlsBody bean="${cmd}" field="password" required="true" prefix="setupCloudServices.page.signup">
    <g:passwordFieldWithChangeNotification name="password"
      value="${fieldValue(bean:cmd,field:'password')}"/>
  </g:propControlsBody>
  <div class="form-actions">  
          <g:actionSubmit id="btnCloudServicesValidate"
                          value="${message(code:'setupCloudServices.page.credentials.button.validate')}"
                          action="updateCredentials" class="btn btn-primary"/>
          <g:if test="${existingCredentials}">
            <g:actionSubmit id="btnCloudServicesRemove"
                            value="${message(code:'setupCloudServices.page.credentials.button.remove')}"
                            action="removeCredentials" class="btn" data-toggle="modal" data-target="#confirmDelete"/>
          </g:if>
  </div>
  
      <div id="confirmDelete" class="modal hide fade" style="display: none">
        <div class="modal-header">
          <a class="close" data-dismiss="modal">&times;</a>
          <h3><g:message code="default.confirmation.title"/></h3>
        </div>
        <div class="modal-body">
          <p><g:message code="setupCloudServices.page.credentials.button.remove.confirm"/></p>
        </div>
        <div class="modal-footer">
          <a href="#" class="btn btn-primary ok" 
             onclick="formSubmit($('#btnCloudServicesRemove').closest('form'), '/csvn/setupCloudServices/removeCredentials')">${message(code: 'default.confirmation.ok')}</a>
          <a href="#" class="btn cancel" data-dismiss="modal">${message(code: 'default.confirmation.cancel')}</a>
        </div>
      </div>  
</g:form>

<g:if test="${existingCredentials}">

<div class="row-fluid">

  <div class="span6">
    <p><img src="${resource(dir:'images/cloud',file:'cloud-backup-logo.png')}" /></p>
    <p><g:message code="setupCloudServices.page.credentials.backup.text"/></p>
    <p><span class="pull-right"><a href="/csvn/repo/bkupScheduleMultiple?type=cloud"
       class="btn btn-primary"><g:message code="setupCloudServices.page.credentials.backup"/> &raquo;</a>
       &nbsp;&nbsp;<a class="btn btn-primary" target="_blank"
       href="https://app.cloudforge.com"><g:message code="setupCloudServices.page.credentials.cloud"/> &raquo;</a>
    </span></p>
  </div>

  <div class="span6">
    <p><img src="${resource(dir:'images/cloud',file:'cloudforge-logo.png')}" /></p>
    <p><g:message code="setupCloudServices.page.index.service.migrate.detail"/></p>
    <p><span class="pull-right"><a target="_blank"
        href="https://app.cloudforge.com/trial_signup/new?source=svnedge" class="btn btn-primary"><g:message code="setupCloudServices.page.index.button.moveToCloud"/></a></span>
    </p>
  </div>

</div>

</g:if>

</body>
</html>
