%{--
  - CollabNet Subversion Edge
  - Copyright (C) 2011, CollabNet Inc. All rights reserved.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%@page import="com.collabnet.svnedge.integration.command.ShortRunningCommand"%>
<%@page import="com.collabnet.svnedge.integration.command.AbstractCommand"%>
<%@page import="com.collabnet.svnedge.integration.command.CommandState"%>

<h3><small>
  <g:if test="${shortRun}">
    <g:message code="job.page.list.short_running.header"/>
  </g:if>
  <g:else>
    <g:message code="job.page.list.long_running.header"/>
  </g:else>
</small></h3>
<table class="table table-striped table-bordered table-condensed">
  <thead>
  <tr>
    <tr>
       <th width="18">#</th>
       <th width="15%">${message(code: 'job.page.list.column.id')}</th>
       <th>${message(code: 'job.page.list.column.code')}</th>
       <th width="20%">${message(code: 'job.page.list.column.started_at')}</th>
    </tr>
  </thead>
  <tbody id="${tableName}">
   <g:each in="${(0..maxNumber-1)}" var="i">
    <g:if test="${i < runningCommands.size()}">
     <g:set var="command" value="${runningCommands.get(i)}" />

    <g:if test="${command.state == CommandState.RUNNING}">
      <tr id="run_${command.id}">
    </g:if>
    <g:elseif test="${(command.state == CommandState.TERMINATED || command.state == CommandState.REPORTED) && command.succeeded}">
      <tr id="run_${command.id}" style="background-color : #99D6AD;">
    </g:elseif>
    <g:elseif test="${(command.state == CommandState.TERMINATED || command.state == CommandState.REPORTED) && !command.succeeded}">
      <tr id="run_${command.id}" style="background-color : #FFB2B2;">
    </g:elseif>

       <td>${i+1}</td>
       <g:set var="commandCode" value="${AbstractCommand.makeCodeName(command)}" />
       <g:set var="commandDesc" value="job.page.list.${commandCode}" />
         <g:if test="${command?.params?.repoName}">
           <g:set var="repoName" value="${command.params.repoName.substring(command.params.repoName.lastIndexOf("/") + 1)}" />
         </g:if>
       <td>
         <g:if test="${commandCode.contains('replica') || command.state == CommandState.REPORTED}">
            ${command.id}
         </g:if>
         <g:elseif test="${command.state == CommandState.RUNNING || command.state == CommandState.TERMINATED}">
            <a target="${command.id}" href="/csvn/log/show?fileName=/temp/${command.id}.log&view=tail">${command.id}</a>
         </g:elseif>
       </td>

       <td>
         <img border="0" src="/csvn/images/replica/${commandCode}.png"> 
         <g:if test="${command?.params?.repoName}">
           <g:set var="repoName" value="${command.params.repoName.substring(command.params.repoName.lastIndexOf('/') + 1, command.params.repoName.length())}" />
           ${message(code: commandDesc, args:[repoName])}
         </g:if>
         <g:else>
           ${message(code: commandDesc)}
         </g:else>
         <g:if test="${!command?.params?.repoName}">
           ${command.params}
         </g:if>
       </td>
       <td>
        <g:formatDate format="${logDateFormat}"
             date="${new Date(command.getCurrentStateTransitionTime())}"/>
       </td>
      </tr> 
    </g:if>
    <g:else>
      <tr>
       <td>${i+1}</td>
       <td colspan="3" align="center"><strong>${message(code: 'job.page.list.row.job_idle')}</strong></td>
      </tr>
    </g:else>
   </g:each>
  </tbody>
 </table>
