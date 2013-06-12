<div class="control-group required-field${hasErrors(bean:con,field:'serverKey',' error')}">
      <label class="control-label"
          for="serverKey"><g:message code="ctfConversionBean.serverKey.label"/></label>
      <div class="controls">
          <input class="input-xlarge" type="text" id="serverKey" name="serverKey" 
              value="${fieldValue(bean: con, field: 'serverKey')}"/>
        <div class="help-block">
          <g:hasErrors bean="${con}" field="serverKey">
              <g:eachError bean="${con}" field="serverKey">
                  <li><g:message error="${it}" encodeAs="HTML"/></li>
              </g:eachError>
          </g:hasErrors>
          <div class="alert alert-error"><g:message code="setupTeamForge.page.error.additional" /></div>
          <g:message code="ctfConversionBean.serverKey.error.missing" />
        </div>
      </div>
</div>