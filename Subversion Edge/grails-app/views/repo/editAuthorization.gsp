<html>
<head>
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

<g:render template="/common/fileEditor"
    model="[fileContent: fieldValue(bean:authRulesCommand, field:'fileContent'),
        fileId: 'accessRules',
        saveAction: 'saveAuthorization',
        cancelAction: 'cancelEditAuthorization',
        ajaxCancelUrl: '/csvn/repo/cancelEditAuthorization']" />

</body>
</html>
