<%@ page import="com.collabnet.svnedge.domain.Role" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>
<content tag="title">
  <g:message code="role.page.show.title" args="${[roleInstance.authority]}"/>
</content>

<g:render template="../user/leftNav" />

<body>
  <g:form class="form-horizontal">
    <div class="control-group">
      <span class="control-label"><g:message code="role.description.label"/></span>
      <div class="controls readonly">${roleInstance.description}</div>
    </div>
    <div class="control-group">
      <span class="control-label"><g:message code="role.people.label"/></span>
      <div class="controls readonly">
          <ul>
            <g:each in="${roleInstance.people}" var="p">
              <li><g:link controller="user" action="show" id="${p.id}">${p.realUserName}</g:link> (${p.username})</li>
            </g:each>
          </ul>
      </div>
    </div>
    
  <div class="form-actions">
      <g:hiddenField name="id" value="${roleInstance?.id}"/>
      <span class="button"><g:actionSubmit class="btn btn-primary edit" action="edit" value="${message(code: 'default.button.edit.label')}"/></span>
  </div>
  </g:form>
</body>
</html>
