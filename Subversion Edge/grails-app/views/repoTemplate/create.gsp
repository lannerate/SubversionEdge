<%@ page import="com.collabnet.svnedge.domain.RepoTemplate" %>
<html>
    <head>
      <meta name="layout" content="main" />
    </head>

<g:render template="/repo/leftNav" />

<content tag="title">
   <g:message code="repoTemplate.page.create.header.title" />
</content>
    
<body>
  <g:uploadForm class="form-horizontal">

    <g:propTextField bean="${repoTemplateInstance}" field="name" required="true" prefix="repoTemplate" maxlength="120"/>
    <g:javascript>$('#name').focus()</g:javascript>
        
    <div class="control-group required-field${hasErrors(bean: repoTemplateInstance, field: 'location', ' error')}">
      <label class="control-label"
            for="templateUpload"><g:message code="repoTemplate.templateUpload.label"/></label>
      <div class="controls">
        <input type="file" name="templateUpload" />
        <div class="help-block">
          <g:message code="repoTemplate.templateUpload.description" />
          <g:hasErrors bean="${repoTemplateInstance}" field="location">
            <div id="location_errors_row">
              <ul><g:eachError bean="${repoTemplateInstance}" field="location">
                <li><g:message error="${it}" encodeAs="HTML"/></li>
              </g:eachError></ul>
            </div>
          </g:hasErrors>            
        </div>
      </div>
    </div>
    <div class="form-actions">    
      <g:actionSubmit action="save" class="btn btn-primary" value="${message(code: 'default.button.create.label')}" />
    </div>
   </g:uploadForm>
</body>
</html>
