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

<%@ page import="org.springframework.scheduling.quartz.QuartzJobBean" %>

<h3>${heading}</h3>
<table class="table table-striped table-bordered table-condensed tablesorter">
  <thead>
  <tr>
    <th width="5%">#</th>
    <th width="15%" nowrap="nowrap">${message(code: 'job.page.list.column.id')}</th>
    <th width="50%" nowrap="nowrap">${message(code: 'job.page.list.column.description')}</th>
    <th width="10%" nowrap="nowrap">${message(code: 'job.page.list.column.scheduled')}</th>
    <th width="10%" nowrap="nowrap">${message(code: 'job.page.list.column.started_at')}</th>
    <th width="10%" nowrap="nowrap">${message(code: 'job.page.list.column.finished_at')}</th>
  </tr>
  </thead>
  <tbody id="${tableName}">
  <g:each in="${itemList}" var="jobCtx" status="i">

    <tr id="run_${jobCtx.jobDetail}">

      <td>${i + 1}</td>
      <td>
        ${jobCtx.mergedJobDataMap.id}
      </td>
      <td>
        ${jobCtx.mergedJobDataMap.description}

        <g:if test="${view == 'scheduled' && jobCtx.mergedJobDataMap.urlConfigure}">
          <a href="${jobCtx.mergedJobDataMap.urlConfigure}">${message(code: 'job.page.list.jobConfigure')}</a>
        </g:if>
        <g:elseif test="${view == 'running' && jobCtx.mergedJobDataMap.urlProgress}">
          <a target="${jobCtx.mergedJobDataMap.id}" href="${jobCtx.mergedJobDataMap.urlProgress}">${message(code: 'job.page.list.jobProgress')}</a>
        </g:elseif>
        <g:elseif test="${view == 'finished' && jobCtx.mergedJobDataMap.urlResult}">
          <a href="${jobCtx.mergedJobDataMap.urlResult}">${message(code: 'job.page.list.jobResult')}</a>
        </g:elseif>

      </td>
      <td>
        <g:set var="scheduledTime" value="${view == 'scheduled' ? jobCtx.nextFireTime : jobCtx.scheduledFireTime}"/>
        <g:if test="${scheduledTime}">
          <g:formatDate format="${logDateFormat}" date="${scheduledTime}"/>
        </g:if>
        <g:else>
            -
        </g:else>
      </td>
      <td>
        <g:if test="${jobCtx.fireTime}">
          <g:formatDate format="${logDateFormat}" date="${jobCtx.fireTime}"/>
        </g:if>
        <g:else>
          -
        </g:else>
      </td>
      <td>
        <g:if test="${jobCtx.jobRunTime > -1}">
          <g:formatDate format="${logDateFormat}" date="${new Date(jobCtx.fireTime.time + jobCtx.jobRunTime)}"/>
        </g:if>
        <g:else>
          -
        </g:else>
      </td>
    </tr>
  </g:each>
  <g:if test="${!itemList}">
    <tr>
      <td>1</td>
      <td colspan="6" align="center"><b>${message(code: 'job.page.list.row.job_idle')}</b></td>
    </tr>
  </g:if>
  </tbody>
</table>
