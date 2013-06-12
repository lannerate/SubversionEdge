<%@ page import="com.collabnet.svnedge.domain.Role" %>
<html>
<head>
  <meta name="layout" content="main"/>
</head>

<content tag="title">
  <g:message code="role.page.header"/>
</content>

<g:render template="../user/leftNav" />

<body>

    <table class="table table-striped table-bordered table-condensed tablesorter">
      <thead>
      <tr>
        <g:sortableColumn property="authority" title="${message(code: 'role.page.list.column.authority')}"/>

        <g:sortableColumn property="description" title="${message(code: 'role.page.list.column.description')}"/>
      </tr>
      </thead>
      <tbody>
      <g:each in="${roleList}" status="i" var="roleInstance">
        <tr>

          <td width="20%"><g:ifAnyGranted role="ROLE_ADMIN,${roleInstance.authority}"><g:link action="show" id="${roleInstance.id}">${fieldValue(bean: roleInstance, field: "authority")}</g:link></g:ifAnyGranted></td>

          <td>${fieldValue(bean: roleInstance, field: "description")}</td>
        </tr>
      </g:each>
      </tbody>
    </table>
  
<g:pagination total="${roleTotal}" />
</body>
</html>
