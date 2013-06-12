        <table class="table table-striped table-bordered">
          <thead>
            <tr>
              <td>Group/Job name</td>
              <td>Trigger name</td>
              <td>Last trigger</td>
              <td>Next trigger</td>
            </tr>
          </thead>
            <tbody>

      <g:each status="i" var="groupName" in="${groupTriggers.keySet()}">
              <tr>
                <th colspan="4">
                    <div style="float: right">
                      <g:if test="${groupTriggers[groupName]['triggerState'] == 'Paused'}">
                        <g:set var="stateClass" value="triggerPaused"/>
                        <g:set var="clock" value="clock_pause.png"/>
                        <g:set var="operation" value="resumeAll"/>
                        <g:set var="alt" value="Paused trigger"/>
                        <g:set var="jobAction" value="Resume"/>
                      </g:if>
                      <g:else>
                        <g:set var="stateClass" value="triggerRunning"/>
                        <g:set var="clock" value="clock_play.png"/>
                        <g:set var="operation" value="pauseAll"/>
                        <g:set var="alt" value="Active trigger"/>
                        <g:set var="jobAction" value="Pause"/>
                      </g:else>
                      <img src="${request.contextPath}/images/${clock}"
                           alt="${alt}" align="left" style="float: none"/>
                      <span class="${stateClass}">${groupTriggers[groupName]["triggerState"]}</span>
                    <g:form method="post" style="display: inline">
                        <input type="hidden" name="operation" value="${operation}"/>
                        <input type="hidden" name="chosenJob" value="${groupName}"/>
                      <g:actionSubmit class="btn save" value="${jobAction}"
                                      action="updateJobs" />
                    </g:form>
                    </div>
                  <div>${groupName}</div>
                </th>
              </tr>

              <g:each status="j" var="jobName" in="${groupTriggers[groupName].keySet()}">
                <g:if test="${jobName != 'triggerState'}">
                <tr>
                  <td rowspan="${groupTriggers[groupName][jobName].size()}">${jobName}</td>
              <g:each status="k" var="trigger" in="${groupTriggers[groupName][jobName]}">
                  <g:if test="${k != 0}">
                    </tr>
                    <tr>
                  </g:if>
                    <td>${trigger.name}</td>
<!--
                    <g:if test="${trigger.description}">
                      <li><strong>Description:</strong> ${trigger.description}</li>
                    </g:if>
-->
                    <td>${trigger.previousFireTime}</td>
                    <td>${trigger.nextFireTime}</td>
              </g:each>
                </tr> 
              </g:if>
              </g:each>
      </g:each>
      
    </tbody>
  </table>
</div>
