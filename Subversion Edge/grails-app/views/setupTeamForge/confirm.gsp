<html>
  <head>
      <meta name="layout" content="main" />
  </head>
  <content tag="title"><g:message code="setupTeamForge.page.wizard.title" /></content>

  <g:render template="/server/leftNav" />

  <body>

   <g:set var="tabArray" value="${[]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfInfo', label: message(code:'setupTeamForge.page.tabs.ctfInfo', args:[1])]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfProject', label: message(code:'setupTeamForge.page.tabs.ctfProject', args:[2])]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfUsers', label: message(code:'setupTeamForge.page.tabs.ctfUsers', args:[3])]}" />
   <g:set var="tabArray" value="${tabArray << [active: true, label: message(code:'setupTeamForge.page.tabs.confirm', args:[4])]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}" />

        <g:if test="${flash.errors}">
          <g:render template="errorList"/>
        </g:if>
        <g:else>
          <p><strong><g:message code="setupTeamForge.page.confirm.ready" /></strong> <g:message code="setupTeamForge.page.confirm.ready.tip" /></p>
        </g:else>

   <g:form method="post">
    <div class="well">
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.server" /></strong></div>
        <div class="span8">${wizardBean.ctfURL}</div>
      </div>
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.tfVersion" /></strong></div>
        <div class="span8">
          <g:if test="${wizardBean.appVersion == wizardBean.apiVersion}">
            ${wizardBean.apiVersion}
          </g:if>
          <g:else>
            ${wizardBean.appVersion} (API: ${wizardBean.apiVersion})
          </g:else>
        </div>
      </div>
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.project" /></strong></div>
        <div class="span8">
          <g:if test="${wizardBean.isProjectPerRepo}">
             <g:message code="setupTeamForge.page.confirm.sameReposImported" />
          </g:if>
          <g:else>${wizardBean.ctfProject}</g:else>
        </div>
      </div>
    <g:if test="${wizardBean.lowercaseRepos || wizardBean.repoPrefix}">
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.repositories" /></strong></div>
        <div class="span8">
          <g:if test="${wizardBean.lowercaseRepos}">
            <div><g:message code="setupTeamForge.page.confirm.repositoriesConverted" /></div>
          </g:if>
          <g:if test="${wizardBean.repoPrefix}">
            <div><g:message code="setupTeamForge.page.confirm.repositoriesPrefixed" args="${[wizardBean.repoPrefix]}" encodeAs="HTML"/>
            </div>
          </g:if>
        </div>
      </div>
    </g:if>
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.users" /></strong>
        </div>
        <div class="span8">
          <g:if test="${wizardBean.importUsers}">
             <g:message code="setupTeamForge.page.confirm.users.imported" />
          </g:if>
          <g:else>
             <g:message code="setupTeamForge.page.confirm.users.noUsersimported" />
          </g:else>
        </div>
      </div>
    <g:if test="${wizardBean.importUsers}">
      <div class="row-fluid">
        <div class="span3"><strong><g:message code="setupTeamForge.page.confirm.membership" /></strong></div>
        <div class="span8">
          <g:if test="${wizardBean.assignMembership}">
             <g:message code="setupTeamForge.page.confirm.membership.giveMembership" />
          </g:if>
          <g:else>
             <g:message code="setupTeamForge.page.confirm.membership.giveLater" />
          </g:else>
        </div>
      </div>
    </g:if>
    <g:if test="${wizardBean && wizardBean.requiresServerKey}">
       <div style="margin-top: 20px">
         <g:render template="serverKeyField" model="${[con:wizardBean]}"/>
       </div>
    </g:if>
    </div>
    <div class="pull-right">
      <g:actionSubmit action="convert" value="${message(code:'setupTeamForge.page.confirm.button.confirm')}" class="btn btn-primary"/>
    </div>
    </g:form>

    </body>
</html>
  
