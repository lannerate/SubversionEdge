%{--
  - CollabNet Subversion Edge
  - Copyright (C) 2012, CollabNet Inc. All rights reserved.
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

<%@ page import="org.springframework.web.util.JavaScriptUtils; org.springframework.scheduling.quartz.QuartzJobBean" %>

<h3>${heading}</h3>
<table class="table table-striped table-bordered table-condensed tablesorter" id="${tableName}"></table>

  <script type="text/javascript">
  /* Data set */
  var aDataSet = [
  <g:each in="${itemList}" var="jobCtx" status="i">
    <g:set var="jobDesc" value="${jobCtx.mergedJobDataMap.description}"></g:set>

    <g:set var="scheduledTime" value="${view == 'scheduled' ? jobCtx.nextFireTime : jobCtx.scheduledFireTime}"/>
    ['${i + 1}',
      '${JavaScriptUtils.javaScriptEscape(jobCtx.mergedJobDataMap.id)}',
      '${JavaScriptUtils.javaScriptEscape(jobDesc)}',
      '${(scheduledTime) ? formatDate(format: logDateFormat, date: scheduledTime) : "-"}'
    <g:if test="${view != 'scheduled'}">
      ,'${(jobCtx.fireTime) ? formatDate(format: logDateFormat, date: jobCtx.fireTime) : "-"}',
      '${(jobCtx.jobRunTime > -1) ? formatDate(format: logDateFormat, date: new Date(jobCtx.fireTime.time + jobCtx.jobRunTime)) : "-"}'
    </g:if>
    ] <g:if test="${i < (itemList.size() - 1)}">,</g:if>
  </g:each>
  ];
</script>
  
<g:javascript library="jquery.dataTables.min"/>
<g:javascript library="DT_bootstrap"/>
<g:javascript>
    <g:if test="${view == 'scheduled'}"><g:set var="idWidth" value="40%"/></g:if>
    <g:else><g:set var="idWidth" value="30%"/></g:else>  
  /* Table initialisation */
  $(document).ready(function() {
    $('#${tableName}').dataTable( {
      "aaData": aDataSet,
      "aoColumns": [
        {"sTitle": "#", "sWidth": "5%"},
        {"sTitle": "${message(code: 'job.page.list.column.id')}", "sWidth": "${idWidth}"},
        {"sTitle": "${message(code: 'job.page.list.column.description')}" },
        {"sTitle": "${message(code: 'job.page.list.column.scheduled')}" }
      <g:if test="${view != 'scheduled'}">  
        ,{"sTitle": "${message(code: 'job.page.list.column.started_at')}" },
        {"sTitle": "${message(code: 'job.page.list.column.finished_at')}" }
      </g:if>
		  ],
      "sDom": "<'row-fluid'<'span4'l><'pull-right'f>r>t<'row-fluid'<'span4'i><'pull-right'p>><'spacer'>",
      "sPaginationType": "bootstrap",
      "bStateSave": true,
      "fnStateSave": tableState.save('${tableName}'),
      "fnStateLoad": tableState.load('${tableName}'),
      "oLanguage": {
        "sLengthMenu": "${message(code:'datatable.rowsPerPage')}",
        "oPaginate": {
            "sNext": "${message(code:'default.paginate.next')}",
            "sPrevious": "${message(code:'default.paginate.prev')}"
        },
        "sSearch": "${message(code:'default.filter.label')}",
        "sZeroRecords": "${message(code:'default.search.noResults.message')}",
        "sEmptyTable": "${message(code: 'job.page.list.row.job_idle')}",
        "sInfo": "${message(code:'datatable.showing')}",
        "sInfoEmpty": "${message(code:'datatable.showing.empty')}",
        "sInfoFiltered": " ${message(code:'datatable.filtered')}"
        },
      "aaSorting": [[ 0, "asc" ]]
    } );
  } );
</g:javascript>
