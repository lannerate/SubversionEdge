<head>
    <meta name="layout" content="main" />
</head>


<content tag="title">
  <g:message code="user.page.edit.title"/>
</content>

<g:render template="leftNav" />

<body>
        <g:hasErrors bean="${userInstance}" field="version">
        <div class="alert alert-error">
            <g:renderErrors bean="${userInstance}" field="version" as="list" />
        </div>
        </g:hasErrors>
        <g:form class="form-horizontal">
            <input type="hidden" name="id" value="${userInstance.id}" />
            <input type="hidden" name="version" value="${userInstance.version}" />
            <input type="hidden" id="passwd_change_active" name="passwd_change_active" value="false" />
            
    <div class="control-group">
      <span class="control-label"><g:message code="user.username.label"/></span>
      <div class="controls readonly">
        ${userInstance.username}<g:if test="${!userInstance.isLdapUser()}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span id="passwd_change_link"><a href="#" onclick="showPasswdFields()"><g:message code="user.page.edit.passwdchange"/></a></span></g:if>
      </div>
    </div>        

   <g:if test="${!userInstance.isLdapUser()}">
    <div id="passwd_row">
      <g:propControlsBody bean="${userInstance}" field="passwd" required="true" prefix="user" groupId="passwd_control">
        <input type="password" id="passwd" name="passwd" value="${userInstance.passwd}"/>&nbsp;&nbsp;<span id="cancel_passwd_link"><a href="#" onclick="cancelPasswordChange()"><g:message code="user.page.edit.passwdchange.cancel"/></a></span>
      </g:propControlsBody>
    </div>

    <div id="passwd_confirm_row">
      <div class="control-group${hasErrors(bean:userInstance,field:'passwd',' error')}">
        <label class="control-label" for="confirmPasswd"><g:message code="user.page.edit.passwd.confirm"/></label>
        <div class="controls">
          <input type="password" id="confirmPasswd" name="confirmPasswd" value=""/>
          <span id="passwordConfirmMessage" class="TextRequired" style="display: none;">
            <img width="15" height="15" alt="Warning" align="bottom"
                  src="${resource(dir: 'images/icons', file: 'icon_warning_sml.gif')}" border="0"/>
            <g:message code="setupCloudServices.page.signup.passwordConfirm.notEqual"/>
          </span>
        </div>
      </div>
    </div>
    <g:propTextField bean="${userInstance}" field="realUserName" required="true" prefix="user"/>
   </g:if>
    <g:propTextField bean="${userInstance}" field="email" required="true" prefix="user"/>
   <g:if test="${!userInstance.isLdapUser()}">
    <g:propTextField bean="${userInstance}" field="description" prefix="user"/>
   </g:if>

    <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_USERS">
      <g:if test="${allowEditingRoles}">
        <g:propControlsBody bean="${userInstance}" field="authorities" prefix="user">
                          <g:each in="${roleList}" var="role">
                            <g:checkBox id="authority_${role.id}" name="authorities" value="${role.id}"
                            checked="${userInstance.authorities.contains(role)}"
                            disabled="${!authorizedRoleList.contains(role)}"/>
                            <label class="checkbox inline withFor" for="authority_${role.id}">${role.authority} - ${role.description}</label><br/>
                          </g:each>
        </g:propControlsBody>
      </g:if>
    </g:ifAnyGranted>
          <div class="form-actions">
            <g:actionSubmit action="update" class="btn btn-primary save requirePasswordConfirm" value="${message(code:'user.page.edit.button.save')}" />
            <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_USERS">
              <g:actionSubmit action="delete" class="btn delete" id="deleteButton" value="${message(code:'user.page.edit.button.delete')}"
                              data-toggle="modal" data-target="#confirmDelete" />
            </g:ifAnyGranted>
          </div>

          <div id="confirmDelete" class="modal hide fade" style="display: none">
            <div class="modal-header">
              <a class="close" data-dismiss="modal">&times;</a>
              <h3>${message(code: 'default.confirmation.title')}</h3>
            </div>
            <div class="modal-body">
              <p>${message(code:'user.page.edit.button.delete.confirm')}</p>
            </div>
            <div class="modal-footer">
              <a href="#" class="btn btn-primary ok" onclick="formSubmit($('#deleteButton').closest('form'), '/csvn/user/delete')">${message(code: 'default.confirmation.ok')}</a>
              <a href="#" class="btn cancel" data-dismiss="modal">${message(code: 'default.confirmation.cancel')}</a>
            </div>
          </div>

        </g:form>

<g:javascript>
        <!--
function showPasswdFields() {
    passwdRow.prop('disabled', false);
    passwdRow.show();
    passwdConfirmRow.prop('disabled', false);
    passwdConfirmRow.show();
    $("#passwd_change_link").hide();
    $("#passwd_change_active").val(true);
    passwordConfirm();
}

function cancelPasswordChange() {
    passwdRow.hide();
    passwdRow.prop('disabled', true);    
    passwdConfirmRow.hide();
    passwdConfirmRow.prop('disabled', true);
    $("#passwd_change_link").show();
    $("#passwd_change_active").val(false);
    passwordConfirm();
}

var passwdRow = $("#passwd_row");
var passwdConfirmRow = $("#passwd_confirm_row");
var passwdErrors = $("#passwd_control.error");
if (passwdErrors.length > 0) {
    $("#passwd_change_link").hide();
} else {
    cancelPasswordChange();
}

$(function() {
    $('#passwd').on('keyup', passwordConfirm);
    $('#confirmPasswd').on('keyup', passwordConfirm);
    $('.requirePasswordConfirm').on('click', passwordConfirm);
    passwordConfirm();
});

function passwordConfirm() {
    var isActive = $('#passwd_change_active').val() &&
            $('#passwd_change_active').val() != 'false';
    var b = !isActive || 
            $('#passwd').val() == $('#confirmPasswd').val();
    $('#passwordConfirmMessage').css("display", b ? 'none' : 'inline');
    $('.requirePasswordConfirm').prop('disabled', !b);
    return b;
}

//-->
    </g:javascript>
</body>
