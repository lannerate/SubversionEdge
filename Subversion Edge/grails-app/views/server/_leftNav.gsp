<content tag="leftMenu">
 <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM">
  <li class="nav-header"><g:message code="admin.page.leftNav.maintenance"/></li>
  
  <li<g:if test="${controllerName == 'status'}"> class="active"</g:if>>
    <g:link controller="status" action="index"><g:message code="admin.page.leftNav.status" /></g:link>
  </li>
            
  <li<g:if test="${controllerName == 'log' && actionName != 'configure'}"> class="active"</g:if>>
    <g:link controller="log" action="list" params="[sort : 'date', order: 'desc']"><g:message code="admin.page.leftNav.logs" /></g:link>
  </li>
            
  <li<g:if test="${controllerName == 'packagesUpdate'}"> class="active"</g:if>>
    <g:link controller="packagesUpdate" action="available"><g:message code="admin.page.leftNav.updates" /></g:link>
  </li>
  
  <li<g:if test="${controllerName == 'statistics'}"> class="active"</g:if>>
    <g:link controller="statistics" action="index"><g:message code="statistics.main.icon" /></g:link>
  </li>

  <li<g:if test="${controllerName == 'job'}"> class="active"</g:if>>
    <g:link controller="job" action="index"><g:message code="job.main.icon" /></g:link>
  </li>

  <li class="nav-header"><g:message code="admin.page.leftNav.configuration"/></li>

  <li<g:if test="${controllerName == 'server' && (actionName == 'edit' || (actionName == 'update' && params.view == 'edit'))}"> class="active"</g:if>>
    <g:link controller="server" action="edit"><g:message code="admin.page.leftNav.settings" /></g:link>
  </li>
  <li<g:if test="${controllerName == 'server' && (actionName == 'editAuthentication' || (actionName == 'update' && params.view == 'editAuthentication'))}"> class="active"</g:if>>
    <g:link controller="server" action="editAuthentication"><g:message code="admin.page.leftNav.auth" /></g:link>
  </li>
  <li<g:if test="${controllerName == 'server' && ['editProxy', 'updateProxy'].contains(actionName)}"> class="active"</g:if>>
    <g:link controller="server" action="editProxy"><g:message code="admin.page.leftNav.proxy" /></g:link>
  </li>
  <li<g:if test="${controllerName == 'server' && ['editMail', 'updateMail'].contains(actionName)}"> class="active"</g:if>>
    <g:link controller="server" action="editMail"><g:message code="admin.page.leftNav.mail" /></g:link>
  </li>

  <li<g:if test="${controllerName == 'log' && actionName == 'configure'}"> class="active"</g:if>>
    <g:link controller="log" action="configure"><g:message code="admin.page.leftNav.logConfigure" /></g:link>
  </li>

  <li<g:if test="${controllerName == 'server' && ['editMonitoring', 'updateMonitoring'].contains(actionName)}"> class="active"</g:if>>
    <g:link controller="server" action="editMonitoring"><g:message code="admin.page.leftNav.editMonitoring" /></g:link>
  </li>
 </g:ifAnyGranted>
  
<g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_SYSTEM">
  <li class="nav-header"><g:message code="admin.page.leftNav.extensions"/></li>

<g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM">
<g:if test="${!isManagedMode}">
  <li<g:if test="${controllerName == 'setupTeamForge' || controllerName == 'setupReplica'}"> class="active"</g:if>>
    <g:link controller="setupTeamForge" action="index"><g:message code="admin.page.leftNav.teamforge" /></g:link>
  </li>
</g:if>
<g:else>
  <li<g:if test="${['editIntegration','revert','editCredentials','updateCredentials'].contains(actionName) }"> class="active"</g:if>>
    <g:link controller="setupReplica" action="editCredentials"><g:message code="admin.page.leftNav.toStandalone" /></g:link>
  </li>
</g:else>
</g:ifAnyGranted>

<%@ page import="com.collabnet.svnedge.domain.integration.CloudServicesConfiguration" %>
<g:if test="${CloudServicesConfiguration.currentConfig?.enabled}">
  <li<g:if test="${controllerName == 'setupCloudServices'}"> class="active"</g:if>>
    <g:link controller="setupCloudServices" action="index"><g:message code="admin.page.leftNav.cloudServices" /></g:link>
  </li>
</g:if>
</g:ifAnyGranted>
</content>
