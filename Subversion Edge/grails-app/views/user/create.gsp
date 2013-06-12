<head>
    <meta name="layout" content="main" />
    <g:javascript>

    $(function() {

      $('#passwd').on('keyup', passwordConfirm);
      $('#passwordConfirm').on('keyup', passwordConfirm);
      $('.requirePasswordConfirm').on('click', passwordConfirm);
      passwordConfirm();
    });

    function passwordConfirm() {
      var b = $('#passwd').val() == $('#passwordConfirm').val();
      $('#passwordConfirmMessage').css("display", b ? 'none' : 'inline');
      $('.requirePasswordConfirm').prop('disabled', !b);
      return b;
    }

    </g:javascript>
</head>


<content tag="title">
  <g:message code="user.page.create.title"/>
</content>

<g:render template="leftNav" />

<body>
  <g:form class="form-horizontal" action="save">
    <g:propTextField bean="${userInstance}" field="username" required="true" prefix="user"/>
    <g:propTextField bean="${userInstance}" field="realUserName" required="true" prefix="user"/>
    <g:propControlsBody bean="${userInstance}" field="passwd" required="true" prefix="user">
      <input type="password" id="passwd" name="passwd"
         value="${fieldValue(bean: userInstance, field: 'passwd')}"/>
    </g:propControlsBody>
    <div class="control-group${hasErrors(bean:userInstance,field:'passwd',' error')}">
      <label class="control-label" for="confirmPasswd"><g:message code="user.page.edit.passwd.confirm"/></label>
      <div class="controls">
        <input type="password" id="passwordConfirm" name="passwordConfirm" value=""/>
        <span id="passwordConfirmMessage" class="TextRequired" style="display: none;">
          <img width="15" height="15" alt="Warning" align="bottom"
                src="${resource(dir: 'images/icons', file: 'icon_warning_sml.gif')}" border="0"/>
          <g:message code="setupCloudServices.page.signup.passwordConfirm.notEqual"/>
        </span>
      </div>
    </div>
    <g:propTextField bean="${userInstance}" field="email" required="true" prefix="user"/>
    <g:propTextField bean="${userInstance}" field="description" prefix="user"/>

    <g:propControlsBody bean="${userInstance}" field="authorities" prefix="user">
                          <g:each in="${roleList}" var="role">
                            <g:checkBox id="authority_${role.id}" name="authorities" value="${role.id}"
                            checked="${userInstance.authorities?.contains(role) || params.authorities?.toList()?.contains(role.id.toString())}"
                            disabled="${!authorizedRoleList.contains(role)}"/>
                            <label class="checkbox inline withFor" for="authority_${role.id}">${role.authority} - ${role.description}</label><br/>
                          </g:each>
    </g:propControlsBody>
    
    <div class="form-actions">
      <input class="btn btn-primary requirePasswordConfirm" type="submit" value="${message(code: 'default.button.create.label')}" />
    </div>
  </g:form>
</body>
