<%@page import="java.util.concurrent.TimeUnit"%>
<%@page import="com.collabnet.svnedge.integration.command.CommandState"%>
<%@page import="com.collabnet.svnedge.integration.command.CommandState"%>
<%@page import="com.collabnet.svnedge.integration.command.AbstractCommand"%>
<head>
    <meta name="layout" content="main" />
</head>

<body>

<content tag="title">
  <g:message code="job.page.header"/> - ${replicaName}
</content>

<g:render template="/server/leftNav"/>

<div class="well">
  <div class="row-fluid">
    <div class="span2"><strong>${message(code:'status.page.replica.master_hostname')}</strong></div>
    <div class="span9">${svnMasterUrl}</div>
  </div>
  <div class="row-fluid">
    <div class="span2"><strong>${message(code:'status.page.url.teamforge')}</strong></div>
    <div class="span9">${ctfUrl}</div>
  </div>
  <div class="row-fluid">
    <div class="span11">
      <div id="pollingIntervalString" style="float: left;">
        ${message(code:'job.page.list.polling_interval', args:[commandPollRate])}
      </div>
    </div>
  </div>
</div>
<br/>

<div>
  <h3><small><g:message code="job.page.list.scheduled.header"/></small></h3>
  <table class="table table-striped table-bordered table-condensed" id="scheduledCommandsTable">
    <tbody>
 <g:if test="${scheduledCommands}">
  <g:each in="${scheduledCommands}" status="i" var="schCommand">
    <tr id="sch_${schCommand.id}">
     <td>
      &nbsp; <g:set var="commandCode" value="${AbstractCommand.makeCodeName(schCommand)}" />
      <img border="0" src="/csvn/images/replica/${commandCode}.png">
      ${schCommand.id} ${schCommand.params.repoName ? "(" + schCommand.params.repoName.substring(schCommand.params.repoName.lastIndexOf("/") + 1, schCommand.params.repoName.length()) + ")" : ""}
     </td>
    </tr>
  </g:each>
 </g:if>
 <tr><td><g:message code="job.page.list.none"/></td></tr>
    </tbody>
   </table>
</div>
<g:if test="${unprocessedCommands}">
 <br />
 <div>
 <h3><small><g:message code="job.page.list.blocked.header"/></small></h3>
   <table class="table table-striped table-bordered table-condensed" id="blockedCommandsTable">
     <tbody>
  <g:each in="${unprocessedCommands}" status="i" var="cmd">
    <tr id="sch_${cmd.id}">
     <td>
      &nbsp; <g:set var="commandCode" value="${AbstractCommand.makeCodeName(cmd)}" />
      <img border="0" src="/csvn/images/replica/${commandCode}.png" alt="" />
      ${cmd.id} ${cmd.params.repoName ? "(" + cmd.params.repoName.substring(cmd.params.repoName.lastIndexOf("/") + 1, cmd.params.repoName.length()) + ")" : ""}
     </td>
    </tr>
  </g:each>
    </tbody>
   </table>
   <small><g:message code="job.page.list.blocked.note"/></small>
 </div>
</g:if>

<g:render template="/job/replicaCommands" model="['tableName': 'longRunningCommandsTable',
 'runningCommands': longRunningCommands, 'maxNumber': maxLongRunning, 'shortRun': false]" />

<g:render template="/job/replicaCommands" model="['tableName': 'shortRunningCommandsTable',
 'runningCommands': shortRunningCommands, 'maxNumber': maxShortRunning, 'shortRun': true]" />

</body>
