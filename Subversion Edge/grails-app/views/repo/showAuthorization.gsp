<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
</head>

<content tag="title"><g:message code="repository.page.editAuthorization.title" /></content>

<g:render template="leftNav"/>

<body>
<g:hasErrors bean="${authRulesCommand?.errors}">
    <div class="error">
        <g:renderErrors as="list"/>
    </div>
</g:hasErrors>

  <g:if test="${lock}">
    <div class="alert"><g:message code="repository.page.showAuthorization.isLocked" args="${[lockOwner.realUserName, lock.createdOn]}"/></div>
  </g:if>

<g:if test="${!authzEnabled}">
  <div class="alert">
    <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM"><g:message code="repository.page.editAuthorization.disabled.systemAdmin" args="${[createLink(controller: 'server', action: 'advanced')]}"/></g:ifAnyGranted>
    <g:ifNotGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM"><g:message code="repository.page.editAuthorization.disabled"/></g:ifNotGranted>
  </div>
</g:if>
      <pre>${accessRules}</pre>

<g:if test="${authzEnabled}">
      <g:form action="editAuthorization" method="get">
        <div class="form-buttons">
          <input class="btn btn-primary" type="submit" value="${message(code: 'default.button.edit.label')}" <g:if test="${lock}">disabled="disabled"</g:if> />
        </div>
      </g:form>
</g:if>
</body>
</html>
