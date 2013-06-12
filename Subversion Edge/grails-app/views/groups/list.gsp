<%@ page import="com.collabnet.svnedge.domain.Groups" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>

<content tag="title">
  <g:message code="group.page.header"/>
</content>

<g:render template="../user/leftNav" />

<body>

    <table class="table table-striped table-bordered table-condensed tablesorter">
      <thead>
      <tr>
        <g:sortableColumn property="authority" title="${message(code: 'group.page.list.column.authority')}"/>

        <g:sortableColumn property="description" title="${message(code: 'group.page.list.column.description')}"/>
        
        <g:sortableColumn property="name" title="${message(code: 'group.page.list.column.name')}"/>
      </tr>
      </thead>
      <tbody>
      <g:each in="${groupList}" status="i" var="groupInstance">
        <tr>

          <td width="20%"><g:ifAnyGranted role="ROLE_ADMIN,${groupInstance.authority}"><g:link action="show" id="${groupInstance.id}">${fieldValue(bean: groupInstance, field: "authority")}</g:link></g:ifAnyGranted></td>

          <td>${fieldValue(bean: groupInstance, field: "description")}</td>
        </tr>
      </g:each>
      </tbody>
    </table>
  
<g:pagination total="${groupTotal}" />
</body>
</html>
