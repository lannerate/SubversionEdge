<html>
  <head>
      <meta name="layout" content="main" />
  </head>
  <content tag="title"><g:message code="setupTeamForge.page.convert.title" /></content>

  <g:render template="/server/leftNav" />

  <body>
  <div class="well">
    <g:if test="${ctfProjectLink}">
      <p><g:message code="setupTeamForge.page.convert.p1" /></p>
      <p><g:message code="setupTeamForge.page.convert.project" /> '${wizardBean.ctfProject}' <g:message code="setupTeamForge.page.convert.sourceCode" /> <a href="${ctfProjectLink}">${ctfProjectLink}</a></p>
    </g:if>
    <g:if test="${ctfLink}">
      <p><g:message code="setupTeamForge.page.convert.tfIntegrations" /> <a href="${ctfLink}">${ctfLink}</a></p>
    </g:if>
  </div>

<ul><li><g:message code="setupTeamForge.page.convert.bullet1" /></li> 
<li><g:message code="setupTeamForge.page.convert.bullet2" /></li>
</ul>

<g:message code="setupTeamForge.page.convert.p3" />

<g:if test="${warnings}">
  <div class="alert">
      <g:message code="setupTeamForge.page.convert.warning" />
    <ul>
    <g:each in="${warnings}">
    <li>${it}</li>
    </g:each>
    </ul>
  </div>
</g:if>

    </body>
</html>
  
