<head>
    <meta name="layout" content="main" />
</head>

<content tag="title">
    <g:message code="user.page.header"/>
</content>

<g:render template="leftNav" />

<body>

  <g:form class="form-horizontal">
    <div class="control-group">
      <span class="control-label"><g:message code="user.username.label"/></span>
      <div class="controls readonly">${userInstance.username}</div>
    </div>        

   <%-- only showing username and email for editable (local db) users --%>
   <g:if test="${editable}">
    <div class="control-group">
      <span class="control-label"><g:message code="user.realUserName.label"/></span>
      <div class="controls readonly">${userInstance.realUserName}</div>
    </div>        
   </g:if>

    <div class="control-group">
      <span class="control-label"><g:message code="user.email.label"/></span>
      <div class="controls readonly">${email}</div>
    </div>        
   <g:if test="${editable}">
    <div class="control-group">
      <span class="control-label"><g:message code="user.description.label"/></span>
      <div class="controls readonly">${userInstance.description}</div>
    </div>        
   </g:if>


                <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_USERS">

    <div class="control-group">
      <span class="control-label"><g:message code="user.authorities.label"/></span>
      <div class="controls readonly">
                          <ul>
                          <g:each in="${userInstance.authorities}" var="r">
                              <li><g:link controller="role" action="show" id="${r.id}">${r.authority}</g:link> (${r.description})</li>
                          </g:each>
                          </ul>
      </div>
    </div>        

                 </g:ifAnyGranted>

      <div class="form-actions">
          <input type="hidden" name="id" value="${userInstance.id}" />
          <g:if test="${editable}">
            <g:actionSubmit id="editButton" action="edit" class="btn btn-primary edit" value="${message(code:'user.page.edit.button.edit')}" />
          </g:if> 
            <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_USERS">
              <g:actionSubmit action="delete" class="btn delete" id="deleteButton" value="${message(code:'user.page.edit.button.delete')}"
                              data-toggle="modal" data-target="#confirmDelete" />
            </g:ifAnyGranted>
      </div>
  </g:form>

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

    </div>
</body>
