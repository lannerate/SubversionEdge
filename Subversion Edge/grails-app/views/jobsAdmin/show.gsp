<html>
  <head>
    <meta name="layout" content="main" />
  </head>
  <body>
    <content tag="title">
      Scheduled Jobs Administration
    </content>
    
    <g:render template="/server/leftNav"/>

      <div class="well">
        The list of schedule jobs can be seen in the form.
        Here's the list of operations that can be done.
        <ul>
          <li><strong>Pause All:</strong> stops all jobs from running</li>
          <li><strong>Resume All:</strong> restart all paused jobs</li>
        </ul>
        <strong>Current Summary from Quartz:</strong>
        <p>
          <%=summary %>
        </p>
      </div>

    Update Jobs Scheduler:
                      <g:if test="${anyJobsRunning}">
                        <g:form method="post" style="display: inline;">
                          <input type="hidden" name="operation"
                                 value="pauseAll"/>
                          <g:actionSubmit class="save" value="Pause All"
                                          action="updateJobs" />
                        </g:form>
                      </g:if>
                      <g:else>
                        <input type="button" value="Pause All"
                               disabled="value"/>
                      </g:else>
                      <g:if test="${anyJobsPaused}">
                        <g:form method="post" style="display: inline;">
                          <input type="hidden" name="operation"
                                 value="resumeAll"/>
                          <g:actionSubmit class="save" value="Resume All"
                                          action="updateJobs" />
                        </g:form>
                      </g:if>
                      <g:else>
                        <input type="button" value="Resume All"
                               disabled="value"/>
                      </g:else>

<br/>
<hr/>
<br/>

      <g:render template="/jobsAdmin/currentJobsTableSummary" 
                model="['groupTriggers':groupTriggers]" />

  </body>
</html>
