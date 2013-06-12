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

<%@ page import="org.springframework.web.util.JavaScriptUtils; com.collabnet.svnedge.console.SchedulerBean" %>
<g:form class="form-horizontal" method="post" name="bkupForm">
  <input type="hidden" name="id" value="${repositoryInstance?.id}"/>

<div class="row-fluid">
  <div  class="span9" id="scheduler">
    <div class="control-group required-field">
      <label class="control-label"
          for="type"><g:message code="repository.page.bkupSchedule.type"/></label>
      <div class="controls">
              <g:set var="isCloud" value="${params.type == 'cloud' || dump.cloud}"/>
              <g:set var="isHotcopy" value="${params.type == 'hotcopy' || dump.hotcopy}"/>
              <g:set var="isVerify" value="${params.type == 'verify'}"/>
              <select id="type" name="type" class="scheduleElement">
                <g:if test="${cloudEnabled}">
                  <option value="cloud" <g:if test="${isCloud}">selected="selected"</g:if>><g:message
                          code="repository.page.bkupSchedule.type.cloud"/></option>
                </g:if>
                <option value="dump" <g:if
                        test="${!isCloud && !isHotcopy}">selected="selected"</g:if>><g:message
                        code="repository.page.bkupSchedule.type.fullDump"/></option>
                <option value="hotcopy" <g:if test="${isHotcopy}">selected="selected"</g:if>><g:message
                        code="repository.page.bkupSchedule.type.hotcopy"/></option>
                <g:if test="${verifyEnabled}">
                <option value="verify" <g:if test="${isVerify}">selected="selected"</g:if>><g:message
                        code="repository.page.bkupSchedule.type.verify"/></option>
                </g:if>
              </select>
              <g:if test="${cloudRegistrationRequired}">
                <span id="cloudRegister" class="help-inline" style="display: none;">
                  <img width="15" height="15" alt="Warning" align="bottom"
                       src="${resource(dir: 'images/icons', file: 'icon_warning_sml.gif')}" border="0"/>
                  <g:message code="repository.page.bkupSchedule.cloud.not.configured"
                             args="${[createLink(controller: 'setupCloudServices', action: 'index')]}"/>
                </span>
              </g:if>
      </div>
    </div>

    <div class="control-group required-field" id="whenRow">
      <label class="control-label"
          for="frequency"><g:message code="repository.page.bkupSchedule.period"/></label>
      <div class="controls">
                    <div style="display: inline-block;">
                      <label class="radio">
                        <g:radio id="frequency_h" name="schedule.frequency" value="HOURLY"
                                 checked="${dump.schedule.frequency == SchedulerBean.Frequency.HOURLY}"
                                 class="scheduleElement"/>
                        <g:message code="repository.page.bkupSchedule.period.hourly"/>
                      </label>
                      <label class="radio">
                        <g:radio id="frequency_d" name="schedule.frequency" value="DAILY"
                                 checked="${dump.schedule.frequency == SchedulerBean.Frequency.DAILY}"
                                 class="scheduleElement"/>
                        <g:message code="repository.page.bkupSchedule.period.daily"/>
                      </label>
                      <label class="radio">
                        <g:radio id="frequency_w" name="schedule.frequency" value="WEEKLY"
                                 checked="${dump.schedule.frequency != SchedulerBean.Frequency.HOURLY && dump.schedule.frequency != SchedulerBean.Frequency.DAILY}"
                                 class="scheduleElement"/>
                        <g:message code="repository.page.bkupSchedule.period.weekly"/>
                      </label>
                    </div>
                    <table style="display: inline-block; margin-left: 20px; vertical-align: top;">
                      <tr>
                        <td>
                          <g:message code="repository.page.bkupSchedule.startTime"/>&nbsp;&nbsp;
                        </td>
                        <td>
                          <g:set var="hours"
                                 value="${(0..23).collect {formatNumber(number: it, minIntegerDigits: 2)}}"/>
                          <g:set var="minutes"
                                 value="${(0..59).collect {formatNumber(number: it, minIntegerDigits: 2)}}"/>
                          <span id="time">
                            <g:select id="startHour" name="schedule.hour" from="${hours}"
                                      value="${formatNumber(number: dump.schedule.hour, minIntegerDigits: 2)}"
                                      class="scheduleElement autoWidth"/>&nbsp;:&nbsp;<g:select id="startMinute"
                                                                                      name="schedule.minute"
                                                                                      from="${minutes}"
                                                                                      value="${formatNumber(number: dump.schedule.minute, minIntegerDigits: 2)}"
                                                                                      class="scheduleElement autoWidth"/>&nbsp;:&nbsp;00
                          </span>
                        </td>
                      </tr>
                      <tr id="dayOfWeekRow">
                        <g:set var="daysOfWeek"
                               value="${[message(code: 'default.dayOfWeek.sunday'), message(code: 'default.dayOfWeek.monday'), message(code: 'default.dayOfWeek.tuesday'), message(code: 'default.dayOfWeek.wednesday'), message(code: 'default.dayOfWeek.thursday'), message(code: 'default.dayOfWeek.friday'), message(code: 'default.dayOfWeek.saturday')]}"/>
                        <td><g:message code="repository.page.bkupSchedule.dayOfWeek"/></td>
                        <td>
                          <select id="dayOfWeek" name="schedule.dayOfWeek" class="scheduleElement autoWidth">
                            <g:each status="i" var="day" in="${daysOfWeek}">
                              <option value="${i + 1}" <g:if
                                      test="${dump.schedule.dayOfWeek == i + 1}">selected="selected"</g:if>>${day}</option>
                            </g:each>
                          </select>
                        </td>
                      </tr>
                    </table>
      </div>
    </div>
              
    <div class="control-group required-field" id="keepRow">
      <label class="control-label"
          for="keep"><g:message code="repository.page.bkupSchedule.numberToKeep"/></label>
      <div class="controls">
        <input type="text" name="numberToKeep" id="numberToKeep" value="${dump.numberToKeep}"
             class="scheduleFormElement input-mini"/>
        <div class="help-block"><g:message code="repository.page.bkupSchedule.numberToKeep.all"/></div>
      </div>
    </div>
    <g:propCheckBox bean="${dump}" field="deltas" prefix="repository.page.bkupSchedule" groupId="deltasRow"/>

    <g:if test="${repositoryInstance && (flash['nameAdjustmentRequired' + repositoryInstance.id] || repositoryInstance.cloudName && repositoryInstance.cloudName != repositoryInstance.name)}">
    <div class="control-group required-field" id="cloudNameRow">
      <label class="control-label"
          for="${'cloudName' + repositoryInstance.id}"><g:message code="repository.page.bkupSchedule.cloudName"/></label>
      <div class="controls">
                <g:if test="${flash['nameAdjustmentRequired' + repositoryInstance.id]}">
                  <input type="text" name="${'cloudName' + repositoryInstance.id}"
                         value="${params['cloudName' + repositoryInstance.id] ?: repositoryInstance.cloudName ?: repositoryInstance.name.replace('.', '_')}"/>
                </g:if>
                <g:elseif
                        test="${repositoryInstance.cloudName && repositoryInstance.cloudName != repositoryInstance.name}">${repositoryInstance.cloudName}</g:elseif>
      </div>
    </div>
    </g:if>
    
  </div>
            <g:if test="${cloudEnabled}">
              <div class="span3" style="vertical-align: top; text-align: center" id="cloudInfo">
                <a target="_blank" href="${helpBaseUrl}/index.jsp?topic=/csvn/action/movetocncloud.html"><img 
                        width="200" height="152" alt="" src="${resource(dir:'images/cloud',file:'subversion-edge-to-the-cloud.png')}" border="0"/><br/>
                <g:message code="repository.page.bkupSchedule.help.link.label"/></a>
              </div>
            </g:if>
</div>

          
          <br/>
        <g:if test="${!repositoryInstance}">
          <div class="tabbable">
            <ul class="nav nav-tabs">
              <li<g:if test="${flash['tabPane'] != 'newJobs'}"> class="active"</g:if>><a href="#existingJobs" data-toggle="tab" id="existingJobsLink"><g:message code="repository.page.bkupSchedule.job.existingJobs"/></a></li>
              <li<g:if test="${flash['tabPane'] == 'newJobs'}"> class="active"</g:if>><a href="#newJobs" data-toggle="tab" id="newJobsLink"><g:message code="repository.page.bkupSchedule.job.newJobs"/></a></li>
           </ul>
           <div class="tab-content">
             <div class="tab-pane<g:if test="${flash['tabPane'] != 'newJobs'}"> active</g:if>" id="existingJobs">
        </g:if>

          <table id="jobDataTable" class="table table-striped table-bordered table-condensed"></table>
          <script type="text/javascript">
            var repoJobMap = {};
            <g:each in="${repoList}" status="j" var="repoMap">
              <g:set var="cloudActivationRequired" value="${false}"/>
              jobList = [];
              repoJobMap['id${repoMap.repoId}'] = jobList;
              <g:each in="${repoBackupJobMap[repoMap.repoId]}" status="i" var="job">
               <g:if test="${cloudRegistrationRequired && job.typeCode == 'cloud'}">
                 <g:set var="cloudActivationRequired" value="${true}"/>
               </g:if>
               <g:if test="${!repositoryInstance && flash['nameAdjustmentRequired' + job.repoId]}">
                 <g:set var="cloudNameChangeRequired" value="${true}"/>
               </g:if>
               var cloudNameParam = "${params['cloudName' + job.repoId] == null ? '' : params['cloudName' + job.repoId]}";
               jobList.push(['${job.repoId}__${JavaScriptUtils.javaScriptEscape(job.id)}|${job.typeCode}|${job.keepNumber}|${job.schedule?.frequency}|${job.schedule?.hour}|${job.schedule?.minute}|${job.schedule?.dayOfWeek}',
               <g:if test="${!repositoryInstance}">
                 '${job.repoId}|<%=JavaScriptUtils.javaScriptEscape(job.repoName)%>|${JavaScriptUtils.javaScriptEscape(job.cloudName)}|${(cloudNameChangeRequired) ? "nc" : ""}|${JavaScriptUtils.javaScriptEscape(cloudNameParam)}',
               </g:if>
               '${job.type}|${(cloudActivationRequired) ? "ca" : ""}',
               '${job.scheduleFormatted}',
               '${job.keepNumber == 0 ? "ALL" : job.keepNumber}'
             ]);
           </g:each>
          </g:each>
          
            /* Data set */
            var jobDataSet = [];
            for (repoId in repoJobMap) {
              var jobList = repoJobMap[repoId];
              for (var i = 0; i < jobList.length; i++) {
                jobDataSet.push(jobList[i]);
              }
            }
          </script>

          <div class="pull-right">
          <g:if test="${repositoryInstance}">
            <g:listViewActionButton action="addBkupSchedule" minSelected="0" maxSelected="0">
              <g:message code="repository.page.bkupSchedule.job.new"/>
            </g:listViewActionButton>
            <g:listViewActionButton action="updateBkupSchedule" minSelected="1" maxSelected="1">
              <g:message code="repository.page.bkupSchedule.job.replace"/>
            </g:listViewActionButton>
          </g:if>
          <g:else>
            <g:listViewActionButton action="updateBkupSchedule" minSelected="1">
              <g:message code="repository.page.bkupSchedule.job.replace"/>
            </g:listViewActionButton>
          </g:else>
            <g:listViewActionButton action="deleteBkupSchedule" minSelected="1">
              <g:message code="repository.page.bkupSchedule.job.delete"/>
            </g:listViewActionButton>
          </div>

        <g:if test="${!repositoryInstance}">
          </div>
          <div class="tab-pane<g:if test="${flash['tabPane'] == 'newJobs'}"> active</g:if>" id="newJobs">
          
          <table id="newJobDataTable" class="table table-striped table-bordered table-condensed"></table>
          <script type="text/javascript">
            /* Data set */
            var newJobDataSet = [
              <g:each in="${repoList}" status="i" var="repoMap">
                <g:set var="cloudNameChangeRequired" value="${false}"/>
                <g:if test="${!repositoryInstance && flash['nameAdjustmentRequired' + repoMap.repoId]}">
                  <g:set var="cloudNameChangeRequired" value="${true}"/>
                </g:if>
                <g:if test="${i > 0}">,</g:if>
                ['${repoMap.repoId}',
                 '${repoMap.repoId}|<%=JavaScriptUtils.javaScriptEscape(repoMap.repoName)%>|${JavaScriptUtils.javaScriptEscape(repoMap.cloudName)}|${(cloudNameChangeRequired) ? "nc" : ""}|${JavaScriptUtils.javaScriptEscape(params['cloudName' + repoMap.repoId])}',
                 '${repoMap.jobCount ? repoMap.repoId : 0}']
              </g:each>
            ];
          </script>
          <div class="pull-right">
            <g:listViewActionButton action="addBkupSchedule" minSelected="1">
              <g:message code="repository.page.bkupSchedule.job.new"/>
            </g:listViewActionButton>
          </div>
          
          </div>
        </div>
       </div>
        </g:if>
  
</g:form>
<g:javascript>
  function typeHandler() {
    var typeSelect = $('#type');
    var typeSelectValue = typeSelect.val();
    // hide all controls, then reveal those needed for each job type
    $('#whenRow').hide();
    $('#keepRow').hide();
    $('#deltasRow').hide();
    $('#cloudRegister').hide();
    $('#cloudNameRow').hide();
    if (typeSelectValue == 'dump' ||
        typeSelectValue == 'hotcopy') {
      $('#whenRow').show();
      $('#keepRow').show();
      if (typeSelectValue == 'dump') {
        $('#deltasRow').show();
      }
    } else if (typeSelectValue == 'cloud') {
      $('#whenRow').show();
      $('#cloudRegister').show();
      $('#cloudNameRow').show();
    } else if (typeSelectValue == 'verify') {
      $('#whenRow').show();
    }
  }
  typeHandler();
  $('#type').change(typeHandler);

  var hourSelect = $('#startHour');
  var hourOptions = [];
  var hourSelectOptions = $('#startHour option');
  for (var i = 0; i < hourSelectOptions.length; i++) {
    hourOptions[i] = hourSelectOptions[i];
  }
  function displayTimeWidget() {
    if ($('#frequency_h').prop('checked')) {
      hourSelect.empty();
      //hourSelect.append('<option value="' + hourOptions[0] + '"/>')
      hourSelect.append(hourOptions[0]);
      hourSelect.prop('disabled', true);
    } else {
      var hours = $('#startHour option');
      if (hours.length == 1) {
        hours.prop('selected', true);
        hourSelect.empty()
        for (var i = 1; i < hourOptions.length; i++) {
          hourSelect.append(hourOptions[i]);
        }
        hourSelect.prepend(hourOptions[0]);
      }
      hourSelect.prop('disabled', false);
    }
  }
  function displayDayOfWeekWidget() {
    if ($('#frequency_w').prop('checked')) {
      $('#dayOfWeekRow').show();
    } else {
      $('#dayOfWeekRow').hide();
    }
  }
  function frequencyHandler() {
    displayTimeWidget();
    displayDayOfWeekWidget();
  }
  frequencyHandler();
  $('#frequency_h').change(frequencyHandler);
  $('#frequency_d').change(frequencyHandler);
  $('#frequency_w').change(frequencyHandler);

  var allowScheduleFormStateClobber = true;
  function setFormState(state) {

    // only change form if we are allowing changes and values are provided
    if (!allowScheduleFormStateClobber || state.type == '') {
      return;
    }

    // after this, disallow picking up changes from the existing jobs
    allowScheduleFormStateClobber = false

    var type = state.type;
    if (type == 'dump_delta') {
        type = 'dump';
        $('#deltas').prop('checked', true);
    } else {
        $('#deltas').prop('checked', false);
    }
    $("#type").val(type);

    var hour = state.scheduleHour.length == 1 ? '0' + state.scheduleHour : '' + state.scheduleHour;
    $('#startHour').val(hour);

    var minute = state.scheduleMinute.length == 1 ? '0' + state.scheduleMinute : '' + state.scheduleMinute;
    $('#startMinute').val(minute);

    $('#dayOfWeek').val(state.scheduleDayOfWeek);
    $('#numberToKeep').val(state.numberToKeep);
    if (state.scheduleFrequency == "HOURLY") {
      $('#frequency_h').prop('checked', true);
    }
    else if (state.scheduleFrequency == "DAILY") {
      $('#frequency_d').prop('checked', true);
    }
    else if (state.scheduleFrequency == "WEEKLY") {
        $('#frequency_w').prop('checked', true);
      }
    typeHandler()
    frequencyHandler()
  }

  // add observer to disable state changes after the form is manipulated
  $(document).ready(function() {
    $(".scheduleElement").click(function() {
        allowScheduleFormStateClobber = false;
    });
  });
  
  var cloudNameRepoIds = [];
  var isCheckCloudNameRepos = false;

  function renderRepoName(oObj, sVal) {
          var repoId = sVal.split("|")[0]
          var repoName = sVal.split("|")[1]
          var cloudName = sVal.split("|")[2] 
          var cloudNameRequired = sVal.split("|")[3] == 'nc'
          var cloudNameParam = sVal.split("|")[4]
           
          var out = repoName
          // if "cloud name change required", indicated in third token, show form element
          if (cloudNameRequired) {
            // if no param from previous form submit, prefill form element with cloud or repo name
            if (cloudNameParam.length == 0 ) {
                cloudNameParam = ((cloudName.length > 0) ? cloudName : repoName).replace('.', '_');
            }
            out += 
            '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' +
            '<label><g:message code="repository.page.bkupSchedule.cloudName"/> ' +
              '<input type="text" name="cloudName' + repoId + '"' +
                     ' value="' + cloudNameParam + '" size="30"/>' +
            '</label>';
            if (isCheckCloudNameRepos) {
              cloudNameRepoIds.push(repoId);
            }
          }
          else if (cloudName.length > 0 && cloudName != repoName) {
            out += " (" + cloudName + ")"
          }
          return out
  }
  
  var i18nMessages = {
        "sLengthMenu": "${message(code:'datatable.rowsPerPage')}",
        "oPaginate": {
            "sNext": "${message(code:'default.paginate.next')}",
            "sPrevious": "${message(code:'default.paginate.prev')}"
        },
        "sSearch": "${message(code:'default.filter.label')}",
        "sZeroRecords": "${message(code:'default.search.noResults.message')}",
        "sEmptyTable": "${message(code:'repository.page.bkupSchedule.noJobs')}",
        "sInfo": "${message(code:'datatable.showing')}",
        "sInfoEmpty": "${message(code:'datatable.showing.empty')}",
        "sInfoFiltered": " ${message(code:'datatable.filtered')}"
  };
      
  // data table configuration for repo listing
 $(document).ready(function() {
    var jobdt = $('#jobDataTable').dataTable( {
      "aaData": jobDataSet,
      "sDom": "<'row-fluid'<'span4'l><'pull-right'f>r>t<'row-fluid'<'span4'i><'pull-right'p>><'spacer'>",
      "sPaginationType": "bootstrap",
      "bStateSave": true,
      "fnStateSave": tableState.save('jobDataTable'),
      "fnStateLoad": tableState.load('jobDataTable'),
      "oLanguage": i18nMessages,
      "aLengthMenu": [25, 50, 100, 1000],
      "fnCreatedRow": function(nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
          applyCheckboxObserverTo($('input.listViewSelectItem', nRow));
      },
      "fnDrawCallback": updateActionButtons, 
    "aaSorting": [[ 1, "asc" ]],
    "aoColumns": [
      {"sTitle": "<g:listViewSelectAll/>", 
       "bSortable": false,
       "fnRender": function (oObj, sVal) {
           var template = '<input type="checkbox" class="listViewSelectItem" id="listViewItem_{0}" name="listViewItem_{0}" onClick="setFormState({\'type\': \'{1}\', \'numberToKeep\': \'{2}\', \'scheduleFrequency\': \'{3}\', \'scheduleHour\': \'{4}\', \'scheduleMinute\': \'{5}\', \'scheduleDayOfWeek\': \'{6}\'})"/>'
           var expanded = new String(template);
           var vals = sVal.split("|");
           for (i = 0; i < vals.length; i++) {
              var token = "\\{" + i + "\\}"
              expanded = expanded.replace(new RegExp(token,'g'), vals[i]);
           }
           return expanded;
        }
      },
    <g:if test="${!repositoryInstance}">
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.repoName')}",
       "fnRender": renderRepoName
      },
    </g:if>
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.type')}",
       "fnRender": function(oObj, sVal) {
          out = sVal.split("|")[0]
          // if "cloud activation required", indicated in second token, show message
          if (sVal.split("|")[1] == 'ca') {
            out += 
            ' <span class="TextRequired">' +
                '<g:message code="repository.page.bkupSchedule.cloud.activation.required" args="${[createLink(controller: 'setupCloudServices', action: 'index')]}"/>' +
            '</span>'
          }
          return out
       }
      },
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.scheduledFor')}"},
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.keepNumber')}"}
    ] 
   })
   
 <g:if test="${!repositoryInstance}">
  isCheckCloudNameRepos = true;
  var newJobdt = $('#newJobDataTable').dataTable( {
      "aaData": newJobDataSet,
      "sDom": "<'row-fluid'<'span4'l><'pull-right'f>r>t<'row-fluid'<'span4'i><'pull-right'p>><'spacer'>",
      "sPaginationType": "bootstrap",
      "bStateSave": true,
      "fnStateSave": tableState.save('newJobDataTable'),
      "fnStateLoad": tableState.load('newJobDataTable'),
      "oLanguage": i18nMessages,
      "aLengthMenu": [25, 50, 100, 1000],
      "fnCreatedRow": function(nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
          applyCheckboxObserverTo($('input.listViewSelectItem', nRow));
      },
      "fnDrawCallback": updateActionButtons, 
    "aaSorting": [[ 1, "asc" ]],
    "aoColumns": [
      {"sTitle": "<g:listViewSelectAll name="newJobSelectAll"/>", 
       "bSortable": false,
       "fnRender": function (oObj, sVal) {
           return '<input type="checkbox" class="listViewSelectItem" id="listViewItem_' + 
                sVal + '" name="listViewItem_' + sVal + '"/>';
        }
      },
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.repoName')}",
       "fnRender": renderRepoName
      },
      {"sTitle": "${message(code:'repository.page.bkupSchedule.job.existingJobs')}",
       "fnRender": function (oObj, sVal) {
         if (sVal == '0') {
           return '<g:message code="repository.page.bkupSchedule.job.none"/>';
         } else {
           var jobList = repoJobMap['id' + sVal];
           var jobMap = {};
           for (var i = 0; i < jobList.length; i++) {
             var type = jobList[i][2].split("|")[0];
             var count = jobMap[type];
             if (!count) {
               jobMap[type] = 1;
             } else {
               jobMap[type] = count + 1;
             }
           }
           var typeList = [];
           for (type in jobMap) {
             typeList.push((jobMap[type] > 1) ? type + ': ' + jobMap[type] : type); 
           }
           typeList.sort();
           return typeList.join(', ');
         }
       }
      }
    ]
  });
   
  $(document).ready(function() {
    var tabs = ['#existingJobsLink', '#newJobsLink'];
    for (var i = 0; i < tabs.length; i++) {
      $(tabs[i]).click(function() {
        $('input.listViewSelectAll').prop('checked', false);
        $('input.listViewSelectItem').prop('checked', false);
      });
    }
    for (var i = 0; i < cloudNameRepoIds.length; i++) {
        $("#listViewItem_" + cloudNameRepoIds[i]).prop('checked', true);
        $("#listViewItem_" + cloudNameRepoIds[i]).click();
        $("#listViewItem_" + cloudNameRepoIds[i]).prop('checked', true);
    }
  });
 </g:if>
    
  });
</g:javascript>
