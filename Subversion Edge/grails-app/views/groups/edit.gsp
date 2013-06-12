<%@ page import="com.collabnet.svnedge.domain.Groups" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>
<content tag="title">
  <g:message code="group.page.edit.title" args="${[groupInstance.authority]}"/>
</content>

<g:render template="../user/leftNav" />

<body>
<g:hasErrors bean="${groupInstance}">
    <div class="errors">
      <g:renderErrors bean="${groupInstance}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form class="form-horizontal" method="post">
    <g:hiddenField name="id" value="${groupInstance?.id}"/>
    <g:hiddenField name="version" value="${groupInstance?.version}"/>
    <g:propControlsBody bean="${groupInstance}" field="description" prefix="group">
      <g:textArea id="description" name="description" value="${groupInstance?.description}" class="span6"/>
    </g:propControlsBody>        
            
    <div class="control-group">
      <span class="control-label"><g:message code="group.people.label"/></span>
      <div class="controls">
        <g:if test="${userList.size() > 12}"><div style="width: 300px; height: 300px; overflow: auto; border: 1px solid #eee; padding: 5px;"></g:if>
        <g:set var="selectedUsers" value="${groupInstance?.people.collect { it.id }}"/>
        <g:each var="user" in="${userList}">
            <label class="checkbox"><input type="checkbox" name="people" value="${user.id}" <g:if test="${selectedUsers.contains(user.id)}"> checked="checked"</g:if>/>${user.username}</label>
        </g:each>
        <g:if test="${userList.size() > 12}"></div></g:if>
        <div class="help-block"><g:message code="group.page.edit.warning.selfedit"/></div> 
      </div>
    </div>        
            
    <div class="form-actions">
      <g:actionSubmit class="btn btn-primary save" action="update" value="${message(code: 'default.button.update.label')}"/>
    </div>
  </g:form>
</body>
</html>
