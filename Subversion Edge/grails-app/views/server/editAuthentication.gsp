<html>
  <head>
      <meta name="layout" content="main" />

    <g:set var="editAuthConfirmMessage" value="${message(code:'server.page.edit.authentication.confirm')}" />

    <g:javascript>
      /* <![CDATA[ */
        $(document).ready(function() {            
            // toggle the console ldap auth with the general ldapenabled setting
            var ldapChkBox = $('#ldapEnabled');
            ldapChkBox.change(function(event) {
                $('#ldapEnabledConsole').prop('checked', ldapChkBox.prop('checked'));
            });
            
            showHideLdapOptions();
        });

        function showHideLdapOptions() {
            if ($('#ldapEnabled').prop('checked')) {
                $('#ldapDialog').show();
            } else { 
                $('#ldapDialog').hide();
            }
        }
      /* ]]> */
    </g:javascript>
    
  </head>
  <content tag="title">
    <g:message code="server.page.editAuthentication.title" />
  </content>
  
  <g:render template="leftNav" />
  
  <body>


    <div class="message">${result}</div>
    <g:if test="${!isConfigurable}">
    <div class="alert alert-block alert-info">
    <p><g:message code="server.page.edit.missingDirectives" />
    <blockquote>
    <code>
      Include "${csvnConf}/csvn_main_httpd.conf"<br/>
      Include "${csvnConf}/svn_viewvc_httpd.conf"
    </code>
    </blockquote>
    </p>
    </div>
    </g:if>
    
  <g:form class="form-horizontal" method="post" name="serverForm">
      <g:hiddenField name="view" value="editAuthentication"/>
      <g:hiddenField name="id" value="${server.id}" />
      
      <fieldset>
        <g:propCheckBox bean="${server}" field="allowAnonymousReadAccess" prefix="server"/>
        <g:propCheckBox bean="${server}" field="forceUsernameCase" prefix="server"/>

        <g:propControlsBody bean="${server}" field="ldapEnabled" prefix="server" labelCode="server.authenticationMethods.label">
            <g:checkBox name="fileLoginEnabled" value="${server.fileLoginEnabled}"/>
            <label class="checkbox inline withFor" for="fileLoginEnabled"><g:message code="server.fileLoginEnabled.label" /></label><br />
            <g:checkBox name="ldapEnabled" value="${server.ldapEnabled}" onClick="javascript:showHideLdapOptions();"/>
            <label class="checkbox inline withFor" for="ldapEnabled"><g:message code="server.ldapEnabled.label" /></label>
        </g:propControlsBody>

      <div id="ldapDialog">
      
        <g:propControlsBody bean="${server}" field="ldapSecurityLevel" prefix="server" required="true">
          <g:select from="${['NONE', 'SSL', 'TLS', 'STARTTLS']}" value="${fieldValue(bean:server,field:'ldapSecurityLevel')}" name="ldapSecurityLevel"></g:select>
        </g:propControlsBody>
      
        <g:propTextField bean="${server}" field="ldapServerHost" prefix="server" required="true"/>
        <g:propTextField bean="${server}" field="ldapServerPort" prefix="server" required="true" sizeClass="small" integer="true"/>
        <g:propTextField bean="${server}" field="ldapAuthBasedn" prefix="server" sizeClass="span6"/>
        <g:propTextField bean="${server}" field="ldapAuthBinddn" prefix="server" sizeClass="span6"/>
        <g:propControlsBody bean="${server}" field="ldapAuthBindPassword" prefix="server">
          <g:passwordFieldWithChangeNotification name="ldapAuthBindPassword" value="${fieldValue(bean:server,field:'ldapAuthBindPassword')}" size="30"/>
        </g:propControlsBody>
        <g:propTextField bean="${server}" field="ldapLoginAttribute" prefix="server"/>
        
        <g:propControlsBody bean="${server}" field="ldapSearchScope" prefix="server">
            <g:select from="${['sub', 'one']}" value="${fieldValue(bean:server,field:'ldapSearchScope')}" name="ldapSearchScope"></g:select>
        </g:propControlsBody>
        
        <g:propTextField bean="${server}" field="ldapFilter" prefix="server" sizeClass="span6" maxlength="8000"/>

        <g:propCheckBox bean="${server}" field="ldapServerCertVerificationNeeded" prefix="server"/>
        <g:propCheckBox bean="${server}" field="ldapEnabledConsole" prefix="server"/>
        <g:propTextField bean="${server}" field="authHelperPort" prefix="server" sizeClass="small" integer="true"/>

      </fieldset>
      <div class="form-actions">
        <g:actionSubmit action="update" value="${message(code:'server.page.editAuthentication.button.save')}" class="btn btn-primary"/>
        <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
      </div>
    </g:form>             
  </body>
</html>
