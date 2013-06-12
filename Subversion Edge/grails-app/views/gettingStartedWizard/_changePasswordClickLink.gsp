<g:if test="${loggedInUsername() == 'admin'}">
  <p><g:message code="wizard.GettingStarted.ChangePassword.recommend"/></p>
  <p><g:message code="wizard.GettingStarted.ChangePassword.clickUsernameToEdit"/></p>
  <g:javascript>
  function highlightUsername() {
    var options = { trigger: 'manual', placement: 'bottom'};
    options.title = '<g:message code="wizard.GettingStarted.ChangePassword.clickHere"/>';
    var userLinks = $('#loggedInUser, .short-user-menu');
    userLinks.tooltip(options);
    options.placement = 'left';
    $('#userProfileMenuItem').tooltip(options);
    if (userLinks.length > 0) {
      setInterval(function() {
        var userLink = $('#loggedInUser');
        if ($('#wizard${activeWizard.label}CollapseButton').is(':visible')) {
          if (userLink.is(':visible')) {
            $('.short-user-menu').tooltip('hide');
            userLink.tooltip('toggle');
          } else {
            $('#loggedInUser').tooltip('hide');
            if ($('#userProfileMenuItem').is(':visible')) {
              $('#userProfileMenuItem').tooltip('toggle');
              $('.short-user-menu').tooltip('hide');
            } else {
              $('#userProfileMenuItem').tooltip('hide');
              $('.short-user-menu').tooltip('toggle');            
            }
          }
        } else {
            userLink.tooltip('hide');
            $('.short-user-menu').tooltip('hide');
            $('#userProfileMenuItem').tooltip('hide');        
        } 
      }, 1500);
    }
  }
  
  $(document).ready(highlightUsername);
  </g:javascript>
</g:if>
<g:else>
  <p><g:message code="wizard.GettingStarted.ChangePassword.notAdminUser"/></p>
</g:else>
