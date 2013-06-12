<%@ page import="com.collabnet.svnedge.domain.MailConfiguration" %>
<%@ page import="com.collabnet.svnedge.domain.MailAuthMethod" %>
<%@ page import="com.collabnet.svnedge.domain.MailSecurityMethod" %>
<html>
<head>
  <meta name="layout" content="main" />
  <g:javascript>
    /* <![CDATA[ */
$(document).ready(function() {            
    if (elementExists($('#testButton'))) {
        $('#testButton').click(testMailHandler);
    }
});

function testMailHandler() {
    $('#spinner').show();
    $('#testButton').val("${message(code: 'server.page.editMail.button.testInProgress')}");
    $('#saveButton').prop('disabled', true);
    window.setTimeout("$('#testButton').prop('disabled', true)", 500);
}

function fetchResult() {
    var repeatTestMailResult = true;
    new Ajax.Request('/csvn/server/testMailResult?iefix=' + new Date(), {
        method:'get',
        asynchronous: false,
        requestHeaders: {Accept: 'text/json'},
        onSuccess: function(transport) {
            repeatTestMailResult = false;
            var responseData = transport.responseText.evalJSON(true);
            var result = responseData.result;
            var msg;
            if (result == 'STILL_RUNNING') {
                msg = '<g:message code="server.action.testMail.stillRunning"/>';
                repeatTestMailResult = true;
            } else if (result == 'NOT_RUNNING') {
                resetPage();
                msg = '<g:message code="server.action.testMail.notRunning"/>';
            } else if (result == 'SUCCESS') {
                resetPage();
                $('#requestmessages').innerHTML = '<div class="alert alert-success">' +
                        '<g:message code="server.action.testMail.success"
                            args="${[server.adminEmail]}"/></div>';
            } else if (result == 'FAILED') {
                resetPage();
                msg = responseData.errorMessage;
            } else {
                // shouldn't get here
                alert("Result = " + result);
            }
            if (msg != null) {
                $('#requestmessages').innerHTML = '<div class="alert">' +
                        msg + '</div>';
               
            }
        }
    });
    if (repeatTestMailResult) {
        window.setTimeout(fetchResult, 20000);
    }
}

function resetPage() {
    $('#cancelTestButton').prop('id', 'testButton');
    var testButton = $('#testButton');
    testButton.name = '_action_testMail';
    testButton.val("${message(code:'server.page.editMail.button.testSettings')}");
    testButton.click(testMailHandler);
    $('#spinner').hide();
}
  /* ]]> */
  </g:javascript>
</head>
<content tag="title"><g:message code="server.page.editMail.title" /></content>

<g:render template="leftNav" />

<body>
  <g:set var="testResult" value="${session['email.test.result']}"/>
  <g:if test="${testResult}">
      <g:if test="${testResult.done}">Test finished <!-- Should be handled by controller --></g:if>
      <g:elseif test="${testResult.cancelled}"><!-- Will be handled by controller, should not happen -->${message(code: 'server.action.testMail.cancelled')}</g:elseif>
      <g:else>
        <g:javascript>$(document).ready(fetchResult);</g:javascript>
      </g:else>
    </div>
  </g:if>
  
  <g:form class="form-horizontal" method="post" name="serverForm" action="updateMail">
    <fieldset>
    <g:hiddenField name="view" value="editMail"/>
    <g:hiddenField name="id" value="${config?.id}" />
    <g:hiddenField name="version" value="${config?.version}" />

    <p><g:message code="server.page.editMail.intro" args="${[server.adminEmail]}"/></p>

    <div id="enabledContainer">
      <label class="checkbox"><g:checkBox name="enabled" value="${config?.enabled}" disabled="${!config?.enabled && invalidAdminEmail}"/>
      <span class="help-inline"><g:message code="mailConfiguration.enabled.label" /></span></label>
      <g:if test="${invalidAdminEmail}">
        <div class="alert"><g:message code="mailConfiguration.enabled.invalidAdminEmail" 
                                            args="${[createLink(action: 'edit')]}"/>
      </div>
      </g:if>
      <g:javascript>
        $('#enabled').click(toggleConfigFields);
        
        function toggleConfigFields() {
            var isEnabled = $('#enabled').prop('checked');
            $('.requireEnabled').prop('disabled', !isEnabled);
        }
      </g:javascript>
    </div>

    <br/>
    <div id="mailServerDialog">
      <p><g:message code="server.page.editMail.configureSMTP"/></p>
      
      <g:propTextField bean="${config}" field="serverName" prefix="mailConfiguration" class="requireEnabled" required="true"/>
      <g:propTextField bean="${config}" field="port" prefix="mailConfiguration" class="requireEnabled" integer="true" required="true"/>
      <g:propTextField bean="${config}" field="authUser" prefix="mailConfiguration" class="requireEnabled requireAuthMethod"/>
      <g:propControlsBody bean="${config}" field="authPass" prefix="mailConfiguration" class="requireEnabled">
        <g:passwordField name="authPass" value="${fieldValue(bean: config, field: 'authPass')}" class="requireEnabled"/>
      </g:propControlsBody>
      <g:propControlsBody bean="${config}" field="securityMethod" prefix="mailConfiguration" class="requireEnabled">
        <g:select name="securityMethod" from="${com.collabnet.svnedge.domain.MailSecurityMethod?.values()}" value="${config?.securityMethod}" class="requireEnabled"/>
      </g:propControlsBody>
      <g:propTextField bean="${config}" field="fromAddress" prefix="mailConfiguration" class="requireEnabled"/>
      <g:if test="${isReplica}">
        <g:propTextField bean="${config}" field="repoSyncToAddress" prefix="mailConfiguration" class="requireEnabled"/>
      </g:if>
      </fieldset>
      <div class="form-actions">
        <g:set var="saveDisabled" value=""/>
        <g:if test="${testResult}">
          <g:set var="saveDisabled" value="disabled='disabled'"/>
        </g:if>
        <g:actionSubmit action="updateMail" value="${message(code:'server.page.edit.button.save')}" 
                    class="btn btn-primary" id="saveButton" ${saveDisabled} />
        <button type="reset" class="btn" ${saveDisabled}><g:message code="default.button.cancel.label" /></button>
        <g:if test="${testResult}">
          <g:actionSubmit action="cancelTestMail" value="${message(code:'server.page.editMail.button.cancelTest')}" 
              class="btn" id="cancelTestButton"/>
        </g:if>
        <g:else>
          <g:actionSubmit action="testMail" value="${message(code:'server.page.editMail.button.testSettings')}" 
              class="btn requireEnabled" id="testButton"/>        
        </g:else>
        <img id="spinner" class="spinner" src="/csvn/images/spinner-gray-bg.gif" alt=""/>
        <g:if test="${!testResult}"><g:javascript>$('#spinner').hide();</g:javascript></g:if>
      </div>
    </div>
  </g:form>
  <g:javascript>toggleConfigFields();</g:javascript>
</body>
</html>
