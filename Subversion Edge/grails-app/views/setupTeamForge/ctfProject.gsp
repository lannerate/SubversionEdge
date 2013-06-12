<html>
  <head>
      <meta name="layout" content="main" />
  </head>
  <content tag="title"><g:message code="setupTeamForge.page.wizard.title" /></content>

  <g:render template="/server/leftNav" />

  <body>

   <g:set var="tabArray" value="${[]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfInfo', label: message(code:'setupTeamForge.page.tabs.ctfInfo', args:[1])]}" />
   <g:set var="tabArray" value="${tabArray << [active: true, label: message(code:'setupTeamForge.page.tabs.ctfProject', args:[2])]}" />
   <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.ctfUsers', args:[3])]}" />
   <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.confirm', args:[4])]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}" />

          <g:if test="${con.errors.hasGlobalErrors()}">
            <div class="alert alert-block alert-error">
              <ul>
              <g:each in="${con.errors.globalErrors}">
                  <li><g:message error="${it}" /></li>
              </g:each>
              </ul>
            </div>
          </g:if>

          <p>
            <g:message code="setupTeamForge.page.ctfProject.p1" />
          </p>

          <p>
            <g:message code="setupTeamForge.page.ctfProject.p2" />
          </p>

  <g:form class="form-horizontal" method="post">
    <g:hiddenField name="projectType" id="projectTypeSingle" value="single"/>
    <g:propTextField bean="${con}" field="ctfProject" required="true" prefix="setupTeamForge.page.ctfProject"/>
    <g:if test="${invalidRepoNames.containsUpperCaseRepos}">
      <g:propCheckBox bean="${con}" field="lowercaseRepos" prefix="setupTeamForge.page.ctfProject"/>
    </g:if>
    <g:if test="${invalidRepoNames.containsReposWithInvalidFirstChar}">
      <g:propCheckBox bean="${con}" field="repoPrefix" prefix="setupTeamForge.page.ctfProject"/>
    </g:if>
    <div class="form-actions">
      <g:actionSubmit action="updateProject" value="${message(code:'setupTeamForge.page.ctfProject.button.continue')}" class="btn btn-primary"/>
    </div>
    </g:form>

    </body>
</html>
  
