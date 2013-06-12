<%@ page import="com.collabnet.svnedge.domain.ServerMode" %>
<html>
  <head>
      <meta name="layout" content="main" />

    <g:set var="editAuthConfirmMessage" value="${message(code:'server.page.edit.authentication.confirm')}" />

    <g:javascript>
    /* <![CDATA[ */
    var addrInterfaceMap = []
    <g:each in="${addrInterfaceMap}">
        addrInterfaceMap["${it.key}"] = [
        <g:each var="iface" in="${it.value}">
            "${iface}", 
	    </g:each>
	    ]
    </g:each>
        
        function updateInterface(addrSelect) {
            var val = addrSelect.value
            var options = addrInterfaceMap[val]
            var selectElement = document.getElementById("netInterface")
            removeAllOptions(selectElement)
            for (var i = 0; i < options.length; i++) {
                addOption(selectElement, options[i], options[i])
            }
        }
    
        // update select boxes with new options for change in NetworkInterface
        function updateNetInt(e) {
           var result = eval("(" + e.responseText + ")")
           var ipAddresses = result.ipAddresses
           updateIpAddresses(ipAddresses)
        }

        // update the select for ipaddresses
        function updateIpAddresses(ipAddresses) {
            var ipSelect = document.getElementById("ipaddress")
            updateSelect(ipSelect, ipAddresses)
        }

        // updates a select for the given values.  Assumes that the options
        // should have both text and value set the same.
        function updateSelect(selectElem, values) {
            removeAllOptions(selectElem)
            for (var i = 0; i < values.length; i++) {
                addOption(selectElem, values[i], values[i])
            }
        }

        // add an option with the given text/value to the select element.
        function addOption(selectElem, text, value) {
            var opt = document.createElement('option');
	        opt.text = text
	        opt.value = value
            try {
                selectElem.add(opt, null) // standards compliant
            }
            catch(ex) {
                selectElem.add(opt) // IE only
            }
        }

        // remove all current options from the select element.
        function removeAllOptions(selectElem) {
            for (var i = selectElem.length - 1; i >= 0; i--) {
                selectElem.remove(i)
            }
        }

        function toggleNetworkFields() {
            var isEnabled = $('#networkEnabled').prop('checked');
            $('.requireNetworkEnabled').prop('disabled', !isEnabled);
        }
        
        function toggleRepoDiskFields() {
            var isEnabled = $('#repoDiskEnabled').prop('checked');
            $('.requireRepoDiskEnabled').prop('disabled', !isEnabled);
        }
        
        function checkDailyFrequency() {
            $('#frequency_daily').prop('checked', true);
        }
        
        function checkHourlyFrequency() {
            $('#frequency_1hour').prop('checked', true);
        }
    </g:javascript>
    
  </head>
  <content tag="title"><g:message code="admin.page.leftNav.editMonitoring" /></content>

  <g:render template="leftNav" />

  <body>
    <div class="message">${result}</div>

  <g:form class="form-horizontal" method="post" name="serverForm" id="serverForm">
      <g:hiddenField name="view" value="editMonitoring"/>
      <g:hiddenField name="id" value="${config.id}" />

      <fieldset>            
        <legend><small>Networking</small></legend>
        <g:propCheckBox bean="${config}" field="networkEnabled" prefix="server"/>

        <g:propControlsBody bean="${config}" field="ipAddress" prefix="server">
            <select class="select-medium requireNetworkEnabled" name="ipAddress" id="ipAddress" onchange="updateInterface(this)">
              <optgroup label="IPv4">
                <g:each var="addr" in="${ipv4Addresses.collect{ it.getHostAddress() }}">
                  <g:set var="isSelected" value=""/>
                  <g:if test="${config.ipAddress == addr}">
                    <g:set var="isSelected"> selected="selected"</g:set>
                  </g:if>
                  <option value="${addr}"${isSelected}>${addr}</option>
                </g:each>
              </optgroup>
              <optgroup label="IPv6">
                <g:each var="addr" in="${ipv6Addresses.collect{ it.getHostAddress() }}">
                  <g:set var="isSelected" value=""/>
                  <g:if test="${config.ipAddress == addr}">
                    <g:set var="isSelected"> selected="selected"</g:set>
                  </g:if>
                  <option value="${addr}"${isSelected}>${addr}</option>
                </g:each>
              </optgroup>
            </select>
        </g:propControlsBody>

        <g:propControlsBody bean="${config}" field="netInterface" prefix="server">
            <g:select name="netInterface" id="netInterface" from="${networkInterfaces}" 
                  value="${config.netInterface}" class="requireNetworkEnabled"/>
            <script type="text/javascript">updateInterface(document.getElementById('ipAddress'))</script>
        </g:propControlsBody>
      </fieldset>
      <fieldset>
        <legend><small>Disk Usage</small></legend>
        <g:propCheckBox bean="${config}" field="repoDiskEnabled" prefix="server"/>
        
        <div class="control-group">
          <label class="control-label"
              for="type"><g:message code="server.page.editMonitoring.frequency"/></label>
          <div class="controls">
            <label class="radio">
              <g:radio id="frequency_30min" name="frequency" value="HALF_HOUR"
                  checked="${config.frequency.toString() == 'HALF_HOUR'}"
                  class="requireRepoDiskEnabled"/>
              <g:message code="server.page.editMonitoring.frequency.halfHour"/>
            </label>
            <div style="margin-bottom: 10px">
            <label class="radio inline">
              <g:radio id="frequency_1hour" name="frequency" value="ONE_HOUR"
                  checked="${config.frequency.toString() == 'ONE_HOUR'}"
                  class="requireRepoDiskEnabled"/>
              <g:message code="server.page.editMonitoring.frequency.hour.every"/>
            </label>
            <g:set var="hours"
                value="${(1..12).collect {formatNumber(number: it, minIntegerDigits: 1)}}"/>
              &nbsp;<g:select id="frequencyHour" name="repoDiskFrequencyHours" from="${hours}"
                  value="${formatNumber(number: config.repoDiskFrequencyHours, minIntegerDigits: 1)}"
                  class="requireRepoDiskEnabled autoWidth"
                  onchange="checkHourlyFrequency();"/>&nbsp;
              <label class="radio inline withFor" for="frequency_1hour"><g:message code="server.page.editMonitoring.frequency.hour"/></label>
            </div>
            <label class="radio inline">
              <g:radio id="frequency_daily" name="frequency" value="DAILY"
                  checked="${config.frequency.toString() == 'DAILY'}"
                  class="requireRepoDiskEnabled"/>
              <g:message code="server.page.editMonitoring.frequency.daily"/>
            </label>
            <g:set var="hours"
              value="${(0..23).collect {formatNumber(number: it, minIntegerDigits: 2)}}"/>
            <g:set var="minutes"
              value="${(0..59).collect {formatNumber(number: it, minIntegerDigits: 2)}}"/>
            <span id="time" style="margin-left: 1em">
              <g:select id="startHour" name="repoDiskHourOfDay" from="${hours}"
                  value="${formatNumber(number: config.repoDiskHourOfDay, minIntegerDigits: 2)}"
                  class="requireRepoDiskEnabled autoWidth"
                  onchange="checkDailyFrequency();"/>&nbsp;:&nbsp;<g:select id="startMinute"
                  name="repoDiskMinuteOfDay" from="${minutes}" value="${formatNumber(number: config.repoDiskMinuteOfDay, minIntegerDigits: 2)}"
                  class="requireRepoDiskEnabled autoWidth"
                  onchange="checkDailyFrequency();"/>&nbsp;:&nbsp;00
            </span>
          </div>
        </div>
      </fieldset>
      <div class="form-actions">
        <g:actionSubmit action="updateMonitoring" value="${message(code:'server.page.edit.button.save')}" class="btn btn-primary"/>
        <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
      </div>
    </g:form>
    <g:javascript>
      toggleNetworkFields();
      $('#networkEnabled').click(toggleNetworkFields);
      toggleRepoDiskFields();
      $('#repoDiskEnabled').click(toggleRepoDiskFields);
    </g:javascript>             
  </body>
</html>
