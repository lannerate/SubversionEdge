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
  <g:javascript>

    var messages = {
      prompt: '<g:message code="setupCloudServices.login.available.prompt"/>',
      checking: '<g:message code="setupCloudServices.login.available.checking"/>'
    }

    $(function() {
      var usernameField = $('#username')
      var usernameMsgElement = $('#usernameUniquenessMessage')
      var loginChecker = new CloudTokenAvailabilityChecker(usernameField, usernameMsgElement, '/csvn/setupCloudServices/checkLoginAvailability', messages.checking, messages.prompt)
      usernameField.on('keydown', function(e) {
        loginChecker.keypressHandler()
      })
      if (usernameField.val().length > 0) {
        loginChecker.keypressHandler();
      }

      var domainField = $('#domain')
      var domainMsgElement = $('#domainUniquenessMessage')
      var domainChecker = new CloudTokenAvailabilityChecker(domainField, domainMsgElement, '/csvn/setupCloudServices/checkDomainAvailability', messages.checking, messages.prompt)
      domainField.on('keydown', function(e) {
        domainChecker.keypressHandler()
      })
      if (domainField.val().length > 0) {
        domainChecker.keypressHandler();
      }
      
      $('#emailAddress').on('keyup', emailConfirm);
      $('#emailAddressConfirm').on('keyup', emailConfirm);
      $('#password').on('keyup', passwordConfirm);
      $('#passwordConfirm').on('keyup', passwordConfirm);
      $('#btnCloudServicesCreateAccout').on('click', function(e) {
        if (!emailConfirm(e)) {
            Event.stop(e);
            alert('<g:message code="setupCloudServices.page.signup.emailAddressConfirm.notEqual"/>');
        }
        if (!passwordConfirm(e)) {
            Event.stop(e);
            alert('<g:message code="setupCloudServices.page.signup.passwordConfirm.notEqual"/>');
        }
      });
    })
    
    function emailConfirm(e) {
      var b = $('#emailAddress').val() == $('#emailAddressConfirm').val();
      $('#confirmEmailMessage').css("display", b ? 'none' : 'inline');
      return b;
    }
    
    function passwordConfirm(e) {
      var b = $('#password').val() == $('#passwordConfirm').val();
      $('#passwordConfirmMessage').css("display", b ? 'none' : 'inline');
      return b;
    }
    $(function() {
      emailConfirm();
      passwordConfirm();
    });
    </g:javascript>       
</head>
<content tag="title"><g:message code="setupCloudServices.page.signup.title"/></content>

<g:render template="/server/leftNav"/>
<body>
<p><g:message code="setupCloudServices.page.signup.p1"/></p>
<g:form class="form-horizontal">
  <g:propTextField bean="${cmd}" field="firstName" required="true" prefix="setupCloudServices.page.signup"/>
  <g:propTextField bean="${cmd}" field="lastName" required="true" prefix="setupCloudServices.page.signup"/>
  <g:propTextField bean="${cmd}" field="emailAddress" required="true" prefix="setupCloudServices.page.signup"/>
  
  <g:propControlsBody bean="${cmd}" field="emailAddressConfirm" required="true" prefix="setupCloudServices.page.signup">
      <input type="text" class="input-xlarge" id="emailAddressConfirm" name="emailAddressConfirm" 
             value="${fieldValue(bean: cmd, field: 'emailAddressConfirm')}"/>
      <span id="confirmEmailMessage" class="TextRequired" style="display: none;">
        <img width="15" height="15" alt="Warning" align="bottom"
                src="${resource(dir: 'images/icons', file: 'icon_warning_sml.gif')}" border="0"/>
        <g:message code="setupCloudServices.page.signup.emailAddressConfirm.notEqual"/>
      </span>
  </g:propControlsBody>

  <g:propTextField bean="${cmd}" field="phoneNumber" prefix="setupCloudServices.page.signup"/>
  <g:propControlsBody bean="${cmd}" field="username" required="true" prefix="setupCloudServices.page.signup">
    <input type="text" class="input-xlarge" id="username" name="username"
        value="${fieldValue(bean: cmd, field: 'username')}"/>
    <span id="usernameUniquenessMessage"></span>
  </g:propControlsBody>
  <g:propControlsBody bean="${cmd}" field="password" required="true" prefix="setupCloudServices.page.signup">
      <input type="password" class="input-xlarge" id="password" name="password"
          value="${fieldValue(bean: cmd, field: 'password')}"/>
  </g:propControlsBody>
  <g:propControlsBody bean="${cmd}" field="passwordConfirm" required="true" prefix="setupCloudServices.page.signup">
      <input type="password" class="input-xlarge" id="passwordConfirm" name="passwordConfirm"
          value="${fieldValue(bean: cmd, field: 'passwordConfirm')}"/>
    <span id="passwordConfirmMessage" class="TextRequired" style="display: none;">
      <img width="15" height="15" alt="Warning" align="bottom"
                src="${resource(dir: 'images/icons', file: 'icon_warning_sml.gif')}" border="0"/>
      <g:message code="setupCloudServices.page.signup.passwordConfirm.notEqual"/>
    </span>
  </g:propControlsBody>
  <g:propTextField bean="${cmd}" field="organization" required="true" prefix="setupCloudServices.page.signup"/>
  <g:propControlsBody bean="${cmd}" field="domain" required="true" prefix="setupCloudServices.page.signup">
    <input type="text" class="input-xlarge" id="domain" name="domain"
        value="${fieldValue(bean: cmd, field: 'domain')}"/>
    <span id="domainUniquenessMessage"></span>
  </g:propControlsBody>
  <g:propCheckBox bean="${cmd}" field="acceptTerms" required="true" prefix="setupCloudServices.page.signup"/>
  <div class="form-actions">
      <g:actionSubmit id="btnCloudServicesCreateAccout"
                      value="${message(code:'setupCloudServices.page.signup.button.continue')}"
                      controller="setupCloudServices" action="createAccount" class="btn btn-primary"/>
      <g:actionSubmit id="btnCloudServicesExistingLogin"
                      value="${message(code:'setupCloudServices.page.signup.button.existingLogin')}"
                      controller="setupCloudServices" action="credentials" class="btn"/>

  </div>
</g:form>

</body>
</html>
