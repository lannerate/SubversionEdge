%{--
  - CollabNet Subversion Edge
  - Copyright (C) 2011, CollabNet Inc. All rights reserved.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  --}%


<html>
<head>
  <meta name="layout" content="main"/>
  <script type="text/javascript">

    var messages = {
      prompt: '<g:message code="setupCloudServices.login.available.prompt"/>',
      checking: '<g:message code="setupCloudServices.login.available.checking"/>'
    }

    var usernameAvailabilityCheckers = []
    $(document).ready(function() {
      $('input.CheckForLoginUniqueness').each(function(s) {
        var usernameField = s;
        // FIXME! Still needs to convert completely to jquery.
        var usernameMsgElement = s.next();
        var checker = new CloudTokenAvailabilityChecker(usernameField, usernameMsgElement, '/csvn/setupCloudServices/checkLoginAvailability', messages.checking, messages.prompt);
        usernameAvailabilityCheckers.push(checker);
        checker.onSuccess = updateActionButtons;
        checker.onFailure = updateActionButtons;

        checker.doAjaxRequest(checker);
        usernameField.keydown(checker.keypressHandler);
      });
      updateActionButtons();
    })

    function updateActionButtons() {
      var enableActions = true
      for (var i = 0; i < usernameAvailabilityCheckers.length; i++) {
        if (usernameAvailabilityCheckers[i].tokenAvailable != true) {
          enableActions = false
          break
        }
      }
      $('input.listViewAction').prop('disabled', !enableActions);
    }

  </script>
</head>
<content tag="title"><g:message code="setupCloudServices.page.selectUsers.title"/></content>

<g:render template="/server/leftNav"/>
<body>

<g:set var="tabArray"
       value="${[[action:'selectUsers', label: message(code:'setupCloudServices.page.tabs.selectUsers', args:[1])]]}"/>
<g:set var="tabArray"
       value="${tabArray << [active: true, label: message(code:'setupCloudServices.page.tabs.createLogins', args:[2])]}"/>
<g:render template="/common/tabs" model="${[tabs: tabArray]}"/>

<g:message code="setupCloudServices.page.createUserLogins.p1"/>
<g:form>
  <table class="table table-striped table-bordered table-condensed">
          <thead>
          <tr class="ItemListHeader">
            <th><g:message code="setupCloudServices.page.selectUsers.username"/></th>
            <th><g:message code="setupCloudServices.page.selectUsers.realUsername"/></th>
            <th><g:message code="setupCloudServices.page.selectUsers.emailAddress"/></th>
            <th><g:message code="setupCloudServices.page.selectUsers.proposedLogin"/></th>
          </tr>
          </thead>
          <tbody>
          <g:each in="${userList}" status="i" var="user">
            <tr>
              <td>${user.username}</td>
              <td>${user.realUserName}</td>
              <td>${user.email}</td>
              <td>
                <g:listViewSelectItem item="${user}" hidden="true"/>
                <input type="text" id="username_${user.id}" name="username_${user.id}"
                       value="${fieldValue(bean: user, field: 'username')}"
                       class="CheckForLoginUniqueness"/>
                <span class="usernameUniqueneMessage" id="usernameUniqueMessage_${user.id}"></span>
              </td>
            </tr>
          </g:each>
          <g:if test="${!userList}">
            <tr>
              <td colspan="5"><p><g:message code="setupCloudServices.page.selectUsers.noUsers"/></p></td>
            </tr>
          </g:if>
          </tbody>
        </table>

        <div class="pull-right">
          <g:actionSubmit id="btnMigrateUsers"
                          value="${message(code:'setupCloudServices.page.selectUsers.migrate')}"
                          controller="setupCloudServices" action="migrateUsers" class="btn btn-primary listViewAction"
                          disabled="disabled"/>
        </div>
</g:form>

</body>
</html>
