<html>
    <head>
        <meta name="layout" content="main" />
    </head>

<g:render template="/repo/leftNav" />

<content tag="title">
  <g:message code="repoTemplate.page.edit.header.title" />
</content>

<body>
    <g:hasErrors bean="${repoTemplateInstance}">
      <div class="alert alert-error">
        <g:renderErrors bean="${repoTemplateInstance}" as="list" />
      </div>
    </g:hasErrors>
    <g:form class="form-horizontal" method="post" >
      <g:hiddenField name="id" value="${repoTemplateInstance?.id}" />
      <g:hiddenField name="version" value="${repoTemplateInstance?.version}" />
      <g:propTextField bean="${repoTemplateInstance}" field="name" required="true" prefix="repoTemplate" maxlength="120"/>
      <g:propCheckBox bean="${repoTemplateInstance}" field="active" prefix="repoTemplate"/>

      <div class="form-actions">
        <g:actionSubmit class="btn btn-primary save" action="update" value="${message(code: 'default.button.update.label', default: 'Update')}" />
        <g:actionSubmit class="btn delete" id="deleteButton" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                        data-toggle="modal" data-target="#confirmDelete" />
      </div>

      <div id="confirmDelete" class="modal hide fade" style="display: none">
        <div class="modal-header">
          <a class="close" data-dismiss="modal">&times;</a>
          <h3>${message(code: 'default.confirmation.title')}</h3>
        </div>
        <div class="modal-body">
          <p>${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}</p>
        </div>
        <div class="modal-footer">
          <a href="#" class="btn btn-primary ok" 
             onclick="formSubmit($('#deleteButton').closest('form'), '/csvn/repoTemplate/delete')">${message(code: 'default.confirmation.ok')}</a>
          <a href="#" class="btn cancel" data-dismiss="modal">${message(code: 'default.confirmation.cancel')}</a>
        </div>
      </div>

    </g:form>
</body>
</html>
