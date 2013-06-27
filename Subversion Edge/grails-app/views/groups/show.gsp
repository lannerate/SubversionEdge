<%@ page import="com.collabnet.svnedge.domain.Groups" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>
<content tag="title">
  <g:message code="group.page.show.title" args="${[groupInstance.authority]}"/>
</content>

<g:render template="../user/leftNav" />

<body>
  <g:form class="form-horizontal">
  	<div class="control-group">
      <span class="control-label"><g:message code="group.name.label"/></span>
      <div class="controls readonly">${groupInstance.name}</div>
    </div>
    <div class="control-group">
      <span class="control-label"><g:message code="group.description.label"/></span>
      <div class="controls readonly">${groupInstance.description}</div>
    </div>
    <div class="control-group">
      <span class="control-label"><g:message code="group.people.label"/></span>
      <div class="controls readonly">
          <ul>
            <g:each in="${groupInstance.people}" var="p">
              <li><g:link controller="user" action="show" id="${p.id}">${p.realUserName}</g:link> (${p.username})</li>
            </g:each>
          </ul>
      </div>
    </div>
  <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_USERS"> 
  <div class="form-actions">
      <g:hiddenField name="id" value="${groupInstance?.id}"/>
      <span class="button"><g:actionSubmit class="btn btn-primary edit" action="edit" value="${message(code: 'default.button.edit.label')}"/></span>
       
       <g:hiddenField name="did" value="${groupInstance?.id}"/>
      <span class="button"><g:actionSubmit class="btn delete" action="delete" value="${message(code: 'user.page.edit.button.delete')}"/></span>
  </div>
    </g:ifAnyGranted>
  </g:form>
</body>
</html>
