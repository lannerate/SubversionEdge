<%@ page import="com.collabnet.svnedge.domain.ServerMode" %>
<html>
  <head>
      <meta name="layout" content="main" />

    <g:set var="editAuthConfirmMessage" value="${message(code:'server.page.edit.authentication.confirm')}" />

    <g:javascript>
    /* <![CDATA[ */
        
        $(document).ready(function() {
            // toggle standard server ports with useSsl field
            $("#useSsl").click(function(event) {
                var sslChkbox = $("#useSsl");
                var port = $("#port");
                if (sslChkbox.prop('checked') && port.val() == '80') {
                    port.val('443');
                }
                else if (!sslChkbox.prop('checked') && port.val() == '443') {
                    port.val('80');
                }
           });
        });

      /* ]]> */
    </g:javascript>
    
  </head>
  <content tag="title"><g:message code="admin.page.leftNav.settings" /></content>

  <g:render template="leftNav" />

  <body>
    <div class="message">${result}</div>
<g:if test="${!isConfigurable}">
<div class="alert alert-block alert-info">
    <p><g:message code="server.page.edit.missingDirectives" />
    <blockquote>
    <code>
    Include "data/conf/csvn_main_httpd.conf"<br/>
    Include "data/conf/svn_viewvc_httpd.conf"
    </code>
    </blockquote>
    </p>
</div>
</g:if>
<g:if test="${privatePortInstructions}">
<div class="alert alert-block alert-info">
    <g:if test ="${isStandardPort}">
      <i><g:message code="server.page.edit.standardPorts.header" /></i>
    </g:if>
    <g:else>
      <i><g:message code="server.page.edit.privatePorts.header" /></i>
    </g:else>
    <g:if test="${isSolaris}">
      <p><g:message code="server.page.edit.solarisStandardPorts.instructions" /></p>
      <ul>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions1" args="${[console_user, params.port ?: server.port]}" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions2" args="${[console_user]}" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions3" /></li>
        <li><g:message code="server.page.edit.solarisStandardPorts.instructions4" /></li>
      </ul>
      <g:if test ="${isStandardPort}">
        <p><g:message code="server.page.edit.solarisStandardPorts.altInstructions" /></p>
      </g:if>
      <g:else>
        <p><g:message code="server.page.edit.solarisPrivatePorts.altInstructions" /></p>
      </g:else>
    </g:if>
    <g:elseif test ="${isStandardPort}">
      <p><g:message code="server.page.edit.standardPorts.instructions" /></p>
    </g:elseif>
    <g:else>
      <p><g:message code="server.page.edit.privatePorts.instructions" /></p>
    </g:else>
<ul>
<g:if test ="${isStandardPort}"> 
    <li><g:message code="server.page.edit.httpdBind" /> <a id="toggleBind" href="#"
      onclick="var el = $('#bindInstructions'); el.toggle(); if (!el.is(':hidden')) { $(this).text('<g:message code="general.hide" />'); } else { $(this).text('<g:message code="server.page.edit.showCommands" />'); } return false;"> <g:message code="server.page.edit.showCommands" /></a>
    <div id="bindInstructions" style="border: 1px;">
    <p><g:message code="server.page.edit.httpdBind.instructions" /> <em><g:message code="server.page.edit.httpdBind.asRoot" /></em>
    </p>
    <blockquote>
    <code>chown root:${httpd_group} ${csvnHome}/lib/httpd_bind/httpd_bind
    <br/>
    chmod u+s ${csvnHome}/lib/httpd_bind/httpd_bind</code>
    </blockquote>
    </div>
    </li>
</g:if>
<li><g:message code="server.page.edit.httpd.asSudo" /> <a id="toggleSudo" href="#" 
  onclick="var el = $('#sudoInstructions'); el.toggle(); if (!el.is(':hidden')) { $(this).text('<g:message code="general.hide" />'); } else { $(this).text('<g:message code="server.page.edit.showCommands" />'); } return false;"> <g:message code="server.page.edit.showCommands" /></a>
<div id="sudoInstructions" style="border: 1px;">
<p>
<g:message code="server.page.edit.httpd.asSudo.instruction" />
</p>
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
$('#sudoInstructions').hide();
$('#bindInstructions').hide();
</script>
    </div>
</g:if>

  <g:form class="form-horizontal" method="post" name="serverForm" id="serverForm">
      <g:hiddenField name="view" value="edit"/>
      <g:hiddenField name="id" value="${server.id}" />

      <fieldset>            
        <g:propTextField bean="${server}" field="hostname" required="true" prefix="server"/>
        <g:propCheckBox bean="${server}" field="useSsl" prefix="server"/>
        <g:hiddenField name="sslConfigFinal" value="UNCHANGED"/>
        <g:javascript>
        function useSslHandler() {
          var useSslElement = $('#useSsl');
          if ($('#useSsl').prop('checked')) {
            var useSslParent = useSslElement.parent();
            var sslTip = useSslParent.children('label.withFor');
            sslTip.html(sslTip.html() + '<span id="sslCustomLink">&nbsp;&nbsp;<a href="#sslConfigModal" data-toggle="modal"><g:message code="server.page.edit.useSsl.advancedConfiguration"/></a></span>');
            sslTip.after('<div id="saveSslConfigMessage"></div>');
          } else {
            $('#sslCustomLink').remove();
            $('#saveSslConfigMessage').remove();
            $('#sslConfigFinal').val('UNCHANGED');
          }
        }
        useSslHandler();
        $('#useSsl').click(useSslHandler);        
        </g:javascript>
        
        <div id="sslConfigModal" class="modal hide fade" style="display: none;">
          <div class="modal-header">
            <a class="close" data-dismiss="modal">&times;</a>
            <h3><g:message code="server.page.edit.useSsl.advancedConfigurationModalTitle"/></h3>
          </div>
          <div class="modal-body">
                  <g:if test="${sslConfig != null}">
                    <p><g:message code="server.page.edit.useSsl.nonDefaultConfig"/> 
                    <br/><a href="#" onclick="$('#sslConfig').val('${defaultSslConfig.replace('\n', '\\n')}')"><g:message code="server.page.edit.useSsl.restoreDefault"/></a></p>
                    <g:set var="sslConfigValue" value="${sslConfig}"/>
                  </g:if>
                  <g:else>
                    <p><g:message code="server.page.edit.useSsl.defaultConfig"/></p>
                    <g:set var="sslConfigValue" value="${defaultSslConfig}"/>
                  </g:else>
                  <g:textArea name="sslConfig" style="width: 95%; height: 8em">${sslConfigValue}</g:textArea>
                  <g:javascript>
                    var initialSslConfigValue = $('#sslConfig').val().trim();
                    //$('#sslConfigFinal').val(initialSslConfigValue);
                    $('#sslConfigModal').on('hidden', function () {
                      if ($('#sslConfigFinal').val() != 'UNCHANGED') {
                        //alert($('#sslConfigFinal').val().replace('\r', '\\r')).replace('\n', '\\n') + '\n\n' + $('#initialSslConfigValue').val().replace('\r', '\\r')).replace('\n', '\\n'));
                        $('#saveSslConfigMessage').addClass('alert');
                        $('#saveSslConfigMessage').html("<g:message code="server.page.edit.useSsl.saveChangesAlert"/>");
                      }
                    });
                    
                    function transferSslConfig() {
                      var newValue = $('#sslConfig').val().trim();
                      if (newValue != initialSslConfigValue) {
                        $('#sslConfigFinal').val(newValue);
                      }
                    }
                  </g:javascript>
          </div>
          <div class="modal-footer">
            <a href="#" onclick="$('#sslConfig').val(initialSslConfigValue); $('#sslConfigModal').modal('hide')" class="btn"><g:message code="default.button.cancel.label"/></a>
            <a href="#" onclick="transferSslConfig(); $('#sslConfigModal').modal('hide')" class="btn btn-primary"><g:message code="default.button.done.label"/></a>
          </div>
        </div>
        
        <g:set var='portTip' value=""/>
        <g:if test="${privatePortInstructions}">
          <g:set var='portTip' value="server.port.label.tip"/>
        </g:if>
        <g:else>
          <g:if test="${(server.useSsl && server.port != 443) || server.port != 80}">
            <g:set var='portTip' value="server.port.label.tip.standardPorts"/>
          </g:if>
        </g:else>
        <g:propTextField bean="${server}" field="port" required="true" 
            integer="true" sizeClass="small" prefix="server" tipCode="${portTip}"/>

        <g:propTextField bean="${server}" field="repoParentDir" required="true" 
            sizeClass="span6" prefix="server"/>

        <g:propTextField bean="${server}" field="dumpDir" required="true" 
            sizeClass="span6" prefix="server"/>

        <g:propTextField bean="${server}" field="adminName" prefix="server"/>
        <g:propTextField bean="${server}" field="adminEmail" required="true" prefix="server"/>
        <g:propTextField bean="${server}" field="adminAltContact" prefix="server"/>

        <g:propCheckBox bean="${server}" field="useSslConsole" prefix="server"/>
        <g:if test="${server.defaultStart}">
          <g:propCheckBox bean="${server}" field="defaultStart" prefix="server"/>
          <div class="alert alert-warning"><g:message code="server.defaultStart.notRecommended"/></div>
        </g:if>
      </fieldset>
      <div class="form-actions">
        <g:actionSubmit id="updateButton" action="update" value="${message(code:'server.page.edit.button.save')}" class="btn btn-primary"/>
        <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
        
      </div>
    </g:form>             
  <g:javascript>
  $('.page-header').html('<div style="float: right"><a href="advanced"><g:message code="server.page.advanced.navLabel" /></a></div>' + $('.page-header').html());
  </g:javascript>
  </body>
</html>
