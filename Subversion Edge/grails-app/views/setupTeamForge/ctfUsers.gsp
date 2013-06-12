<html>
  <head>
      <meta name="layout" content="main" />

    <script type="text/javascript">
    function showHide() {
      if ($('#importUsers').prop('checked')) {
        $('#userList').show();
        $('#continue_button').removeClass('form-actions');
        $('#continue_button').addClass('pull-right');
      } else {
        $('#userList').hide();
        $('#continue_button').removeClass('pull-right');
        $('#continue_button').addClass('form-actions');
      }
    }
    </script>

  </head>
  <content tag="title"><g:message code="setupTeamForge.page.wizard.title" /></content>

  <g:render template="/server/leftNav" />

  <body>

   <g:set var="tabArray" value="${[]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfInfo', label: message(code:'setupTeamForge.page.tabs.ctfInfo', args:[1])]}" />
   <g:set var="tabArray" value="${tabArray << [action:'ctfProject', label: message(code:'setupTeamForge.page.tabs.ctfProject', args:[2])]}" />
   <g:set var="tabArray" value="${tabArray << [active: true, label: message(code:'setupTeamForge.page.tabs.ctfUsers', args:[3])]}" />
   <g:set var="tabArray" value="${tabArray << [label: message(code:'setupTeamForge.page.tabs.confirm', args:[4])]}" />
   <g:render template="/common/tabs" model="${[tabs: tabArray, pills: true]}" />

<g:form class="form-horizontal" method="post">
 <g:if test="${server.ldapEnabled}">
      <div class="alert">
      <g:message code="setupTeamForge.page.ctfUsers.p1" />
      </div>
 </g:if>
 <g:else>
    <g:if test="${existingUsers.size() == 0 && csvnOnlyUsers.size() == 0}">
      <p><g:message code="setupTeamForge.page.ctfUsers.noUsers" /></p>
    </g:if>
    <g:elseif test="${existingUsers.size() > 0 && csvnOnlyUsers.size() == 0}">
        <p><g:message code="setupTeamForge.page.ctfUsers.managedUsers" /></p>
    </g:elseif>
    <g:else>
        <g:propCheckBox bean="${wizardBean}" field="importUsers" prefix="setupTeamForge.page.ctfUsers"/>
        <g:javascript>$('#importUsers').click(showHide)</g:javascript>
        <div id="userList"<g:if test="${!wizardBean.importUsers}"> style="display: none;"</g:if>>
          <g:propCheckBox bean="${wizardBean}" field="assignMembership" prefix="setupTeamForge.page.ctfUsers"
              tip="${message(code: 'setupTeamForge.page.ctfUsers.importUsers.addMembershipTo', args: (wizardBean.ctfProject ? [1, wizardBean.ctfProject] : [2]), encodeAs: 'HTML')}"/>
        
          <p>
          <g:if  test="${existingUsers.size() == 0}">
             <g:message code="setupTeamForge.page.ctfUsers.importUsers.noConflicts" />
          </g:if>
          <g:else>
             <g:message code="setupTeamForge.page.ctfUsers.importUsers.someExists" />
          </g:else>
          </p>
        <g:if  test="${existingUsers.size() > 0}">
         <table class="table table-bordered table-condensed">
        <!-- <table style="width: 100%; border: 1px solid #333; margin: 10px">
         --> 
          <tr><th><g:message code="setupTeamForge.page.ctfUsers.column.existingUsers" /></th><th><g:message code="setupTeamForge.page.ctfUsers.column.toImport" /></th></tr>
          <tr>
          <td>
            <ul>
              <g:each var="user" in="${existingUsers}">
                <li>${user}</li>
              </g:each>
            </ul>
          </td>
          <td>
            <ul>
              <g:each var="user" in="${csvnOnlyUsers}">
                <li>${user}</li>
              </g:each>
            </ul>
          </td>
          </tr>
         </table>
        </g:if>
        </div>
    </g:else>
  </g:else>
  <div id="continue_button" class="pull-right">
    <g:actionSubmit action="updateUsers" value="${message(code:'setupTeamForge.page.ctfUsers.button.continue')}" class="btn btn-primary"/>
  </div>
      </g:form>
    </body>
</html>
  
