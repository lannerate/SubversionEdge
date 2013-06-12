<html>
  <head>
      <meta name="layout" content="main" />
  </head>
  
  <g:if test="${isFreshInstall}">
    <content tag="title"><g:message code="setupTeamForge.page.ctfInfo.title.fresh" /></content>
  </g:if>
  <g:else>
    <content tag="title"><g:message code="setupTeamForge.page.wizard.title" /></content>
  </g:else>

  <g:render template="/server/leftNav" />

  <body>

    <g:if test="${!isFreshInstall}">
      <g:set var="tabArray" value="${[]}" />
      <g:set var="tabArray" value="${tabArray << [active:true, label: message(code:'setupTeamForge.page.tabs.ctfInfo', args:[1])]}" />
      <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.ctfProject', args:[2])]}" />
      <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.ctfUsers', args:[3])]}" />
      <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.confirm', args:[4])]}" />
      <g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}" />
    </g:if>

          <g:if test="${flash.errors}">
            <g:render template="errorList"/>
          </g:if> 
          <g:if test="${connectionErrors && errorCause}">
            <div class="alert alert-block alert-error">
                <%=generalError%>
                <ul>
                    <li><g:message code="ctfConversion.form.ctfInfo.noconnection"/> <%=errorCause%></li>
                </ul>
            </div>
          </g:if>

          <p>
            <g:message code="setupTeamForge.page.ctfInfo.p1"/>
          </p>

    <g:form class="form-horizontal" method="post">
      <g:propTextField bean="${con}" field="ctfURL" required="true" prefix="setupTeamForge.page.ctfInfo"/>
      <g:propTextField bean="${con}" field="ctfUsername" required="true" prefix="setupTeamForge.page.ctfInfo"/>
      <g:propControlsBody bean="${con}" field="ctfPassword" required="true" prefix="setupTeamForge.page.ctfInfo">
        <input type="password" id="ctfPassword" name="ctfPassword" 
             value="${fieldValue(bean:con,field:'ctfPassword')}"/>
      </g:propControlsBody>

      <div class="control-group <g:if test="${con && con.requiresServerKey}"> error</g:if>">
      <label class="control-label"
          for="serverKey"><g:message code="ctfConversionBean.serverKey.label"/></label>
      <div class="controls">
          <input class="input-xlarge" type="text" id="serverKey" name="serverKey" 
              value="${fieldValue(bean: con, field: 'serverKey')}"/>
        <div class="help-block">
            <g:message code="setupReplica.page.apiKey.description" />
            <ul>
              <li><g:message code="setupReplica.page.apiKey.hosted" /></li>
              <li><g:message code="setupReplica.page.apiKey.property" /></li>
            </ul>
          <g:if test="${con && con.requiresServerKey}">
            <div class="alert alert-error">
              <g:message code="setupReplica.action.updateCredentials.invalidApiKey" />
            </div>
          </g:if>
        </div>
      </div>

      <div class="form-actions">
            <g:if test="${isFreshInstall}">
                <g:actionSubmit action="convert" value="${message(code:'setupTeamForge.page.ctfInfo.button.convert')}" class="btn btn-primary"/>
            </g:if>
            <g:else>
                <g:actionSubmit action="confirmCredentials" value="${message(code:'setupTeamForge.page.ctfInfo.button.continue')}" class="btn btn-primary"/>
            </g:else>
     </div>
      </g:form>

    </body>
</html>
  
