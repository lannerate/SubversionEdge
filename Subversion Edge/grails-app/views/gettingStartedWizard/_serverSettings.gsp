<p><g:message code="wizard.GettingStarted.ServerSettings.p1"/></p>
<p><g:message code="wizard.GettingStarted.ServerSettings.p2"/></p>
<div id="portModal" class="modal hide fade" style="display: none; width: 800px;">
  <div class="modal-header">
    <a class="close" data-dismiss="modal" data-target="#portModal">&times;</a>
    <h3><g:message code="wizard.GettingStarted.ServerSettings.modal.port.header" /></h3>
  </div>
  <div class="modal-body">
    <g:if test="${isSolaris}">
      <p><g:message code="server.page.edit.solarisStandardPorts.instructions" /></p>
      <ul>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions1" args="${[console_user, params.port ?: server.port]}" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions2" args="${[console_user]}" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions3" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions4" /></li>
      </ul>
        <p class="standardPort" style="display: none;"><g:message code="server.page.edit.solarisStandardPorts.altInstructions" /></p>
        <p class="restrictedPort" style="display: none;"><g:message code="server.page.edit.solarisPrivatePorts.altInstructions" /></p>
    </g:if>
    <g:else>
      <p class="standardPort" style="display: none;"><g:message code="server.page.edit.standardPorts.instructions" /></p>
      <p class="restrictedPort" style="display: none;"><g:message code="server.page.edit.privatePorts.instructions" /></p>
    </g:else>
    <ul>
      <li id="modalStandardPortLink" class="standardPort" style="display: none;"><g:message code="server.page.edit.httpdBind" /> <a id="modalToggleBind" href="#"
          onclick="var el = $('#modalBindInstructions'); el.toggle(); if (!el.is(':hidden')) { $(this).text('<g:message code="general.hide" />'); } else { $(this).text('<g:message code="server.page.edit.showCommands" />'); } return false;"> <g:message code="server.page.edit.showCommands" /></a>
        <div id="modalBindInstructions" style="border: 1px;">
          <p><g:message code="server.page.edit.httpdBind.instructions" /> <em><g:message code="server.page.edit.httpdBind.asRoot" /></em></p>
          <blockquote>
            <code>chown root:${httpd_group} ${csvnHome}/lib/httpd_bind/httpd_bind
            <br/>
            chmod u+s ${csvnHome}/lib/httpd_bind/httpd_bind</code>
          </blockquote>
        </div>
      </li>
      <li><g:message code="server.page.edit.httpd.asSudo" /> <a id="modalToggleSudo" href="#" 
          onclick="var el = $('#modalSudoInstructions'); el.toggle(); if (!el.is(':hidden')) { $(this).text('<g:message code="general.hide" />'); } else { $(this).text('<g:message code="server.page.edit.showCommands" />'); } return false;"> <g:message code="server.page.edit.showCommands" /></a>
        <div id="modalSudoInstructions" style="border: 1px;">
          <p><g:message code="server.page.edit.httpd.asSudo.instruction" /></p>
          <ul>
            <li><g:message code="server.page.edit.httpd.asSudo.command" args="${['<code>/usr/sbin/visudo</code>']}" /><br/><br/>
              <code>Defaults env_keep += "PYTHONPATH"<br/>
              ${console_user}    ALL=(ALL) NOPASSWD: ${csvnHome}/bin/httpd</code>
            </li>
          </ul>
        </div>
      </li>
    </ul>
    <script type="text/javascript">
      $('#modalSudoInstructions').hide();
      $('#modalBindInstructions').hide();
    </script>
  </div>
  <div class="modal-footer">
      <a href="#" class="btn btn-primary" data-dismiss="modal" data-target="#portModal">Close</a>
  </div>
</div>
<g:javascript>
/* <![CDATA[ */

  $('#portModal').on('hidden', function (e) {
    stopEvent(e);
  });

  $('#portModal').on('hide', function (e) {
    stopEvent(e);
    $.ajax({
      url: '/csvn/gettingStarted/isDefaultPortAllowed',
      type: "GET",
      success: function(data, textStatus, jqXHR) {
        isDefaultPortAllowed = data.isDefaultPortAllowed;
        if (isDefaultPortAllowed) {
          showRepoParentPopup();
        } else {
          showPort();
        }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        alert(textStatus + '\n\n' + errorThrown);
      }
    });
  });

  function continueButton(clickHandler) {
    return '<button class="btn" onclick="' + clickHandler + '(); return false;"><g:message code="wizard.GettingStarted.continue"/></button>';
  }

  var hostnameText = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.hostname"/></p>' +
    '<div id="availableHostnames"></div>' + continueButton('continueHostname');

  var sslText = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.useSsl"/></p>' +
    ' <p><a id="gettingStartedSslLink" href="#" onclick="enableSsl(); continueSsl(); return false;">';
  sslText += '<g:message code="wizard.GettingStarted.ServerSettings.help.field.useSsl.enable"/></a></p>' +
    continueButton('continueSsl');
   
  var serverPortRecommendDefault = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.recommendDefault"/></p>' +
    '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.alternativeToDefault"/></p>';
     
  var serverPortRecommendHighPort = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.recommendHighPort"/></p>' +
    '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.alternativeToHighPort"/></p>' +
    ' <ul><li><a href="#" onclick="$(\'#port\').val(\'18080\'); continuePort(); return false;"><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.highPortLink"/></a></li>' +
    ' <li><a href="#" onclick="$(\'#port\').val(\'__JS_PORT__\'); continuePort(); return false;"><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.defaultPortLink"/></a></li></ul>';
     
  var serverPortDefaultUnavailable = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.port.defaultPortUnavailable"/></p>';

  function enableSsl() {
    $('#useSsl').prop('checked', true);
    var port = $("#port");
     if (port.val() == '80') {
       port.val('443');
     } 
  }
    
   function toggleSsl() {
     var sslCheckbox = $('#useSsl');
     var isChecked = !sslCheckbox.prop('checked');
     sslCheckbox.prop('checked', isChecked);
     var port = $("#port");
     if (isChecked && port.val() == '80') {
       port.val('443');
     } else if (!isChecked && port.val() == '443') {
       port.val('80');
     }
   }
     
   var portButton = continueButton('continuePort');
   var isDefaultPortAllowed = ${isDefaultPortAllowed};
   var isPort80Available = ${isPort80Available};
   var isPort443Available = ${isPort443Available};
   
   var repoLocationText = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.repoParentDir"/></p>' +
     ' <button class="btn" onclick="continue2()"><g:message code="wizard.GettingStarted.saveAndContinue"/></button>';
     
  function createHtmlList(items) {
      var html = null;
      if (items.length > 0) {
          html = '<ul>';
          for (var i = 0; i < items.length; i++) {
              html += '<li>' + items[i] + '</li>'
          }
          html += '</ul>';
      }
      return html;
  }
     
  function linkifyCopyValue(selector, items, continueFunction) {
    var links = [];
    for (var i = 0; i < items.length; i++) {
      var link = '<a href="#" onclick="$(\'' + selector + '\').val(\'' + 
          items[i] + '\');';
      if (continueFunction) {
          link += continueFunction + ';'
      }
      link += ' return false;">' + items[i] + '</a>';
      links[i] = link;
    }
    return links;
  }
  
  function retrieveHostnames() {
    $.ajax({
      url: '/csvn/gettingStarted/availableHostnames',
      type: "GET",
      success: function(data, textStatus, jqXHR) {
        processHostnameList(data);
      },
      error: function(jqXHR, textStatus, errorThrown) {
        alert(textStatus + '\n\n' + errorThrown);
      }
    });
  }
  
  var ipAddressHostnameText = '<g:message code="wizard.GettingStarted.ServerSettings.help.field.hostname.ipAddress"/>';
      
  function processHostnameList(hostnames) {
    var listHtml = createHtmlList(linkifyCopyValue('#hostname', hostnames, 
        'continueHostname()'));
    if (listHtml) {
      var html = '<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.hostname.list"/></p>' + listHtml;
      var reachable = false;
      for (var i = 0; i < hostnames.length && !reachable; i++) {
        if (hostnames[i].indexOf('.') > 0) {
          reachable = true;
        } 
      }
      if (!reachable) {
        html += '<p>' + ipAddressHostnameText + '</p>';
      }
      $('#availableHostnames').html(html);
    } else {
      $('#availableHostnames').html('<p><g:message code="wizard.GettingStarted.ServerSettings.help.field.hostname.none"/> ' + 
          ipAddressHostnameText + '</p>');
    }
  }

  function serverPortContent() {
    var serverPortText;
    var isSslChecked = $('#useSsl').prop('checked');
    if (isDefaultPortAllowed) {
      if ((isSslChecked && isPort443Available) || 
          (!isSslChecked && isPort80Available)) {
        serverPortText = serverPortRecommendDefault + portButton;
      } else {
        serverPortText = serverPortDefaultUnavailable + portButton;
      }
    } else {
      serverPortText = serverPortRecommendHighPort + portButton;
    }
    
    var defaultPort = '80';
    var httpScheme = 'http';
    if (isSslChecked) {
      defaultPort = '443';
      httpScheme = 'https';
    }
    serverPortText = serverPortText.replace(/__JS_PORT__/g, defaultPort)
     .replace(/__JS_HTTP_SCHEME__/g, httpScheme);
    return serverPortText;
  }

  function continueHostname() {
    var location = targetElement('#hostname');
    location.popover('hide');
    
    location = targetElement('#useSsl');
    var options = { trigger: 'manual', placement: 'bottom', html: true};
    options.title = '<g:message code="wizard.GettingStarted.ServerSettings.help.field.useSsl.title"/>';
    options.content = sslText;
    location.popover(options);
    location.popover('show');
  }

  function continueSsl() {
    var location = targetElement('#useSsl');
    location.popover('hide');
    showPort();
  }
  
  function showPort() {
    var location = targetElement('#port');
    var options = { trigger: 'manual', placement: 'bottom', html: true};
    options.title = '<g:message code="wizard.GettingStarted.ServerSettings.help.field.port.title"/>';
    options.content = serverPortContent();
    location.popover(options);
    location.popover('show');  
  }

  function continuePort() {
    var location = targetElement('#port');
    location.popover('hide');
  <g:if test="${isDefaultPortAllowed}">
    showRepoParentPopup();
  </g:if>
  <g:else>
    var portModal = $('#portModal');
    var portValue = $('#port').val();
    if (portModal.length && portValue < 1024) {
      if (portValue == '80' || portValue == '443') {
        $('.standardPort').show();
        $('.restrictedPort').hide();
      } else {
        $('.standardPort').hide();
        $('.restrictedPort').show();
      }
      portModal.modal('show');
    }
    else {
      showRepoParentPopup();
    }
  </g:else>
  }
  
  function showRepoParentPopup() {
      var location = targetElement('#repoParentDir');
      var options = { trigger: 'manual', placement: 'bottom', html: true};
      options.title = '<g:message code="wizard.GettingStarted.ServerSettings.help.field.repoParentDir.title"/>';
      options.content = repoLocationText;
      location.popover(options);
      location.popover('show');  
  }
  
  function continue2() {
    $('#updateButton').click();
  }
  $(document).ready(function() {
    startWizard();
    $('#wizard${activeWizard.label}').on('hidden', function () {
      targetElement('#hostname').popover('hide');
      targetElement('#useSsl').popover('hide');
      targetElement('#port').popover('hide');
      targetElement('#repoParentDir').popover('hide');
    });
  });
  
  function startWizard() {
    retrieveHostnames();
    var location = targetElement('#hostname');
    var options = { trigger: 'manual', placement: 'bottom', html: true};
    options.title = '<g:message code="wizard.GettingStarted.ServerSettings.help.field.hostname.title"/>';
    options.content = hostnameText;
    location.popover(options);
    location.popover('show');
  }
  
  function targetElement(selector) {
     var isPhoneMode = !$('#tip').is(':visible');
     var target = $(selector).parent().children('.help-block').children('.help-marker');
     if (target.length == 0 || isPhoneMode) {
       target = $(selector).parent().children('.help-block');
     }
     if (target.length == 0) {
       target = $(selector);
       if (isPhoneMode) {
         target = target.parent();
       }
     }
     return target;
  }  
  
/* ]]> */
</g:javascript>

