<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta name="layout" content="main" />
    <script type="text/javascript">

    <g:if test="${isReplicaMode}">

        /** Handle for the polling function */
        var periodicUpdater

        // instantiate the polling task on load
        $(function() {
            periodicUpdater = new PeriodicalExecuter(fetchReplicationData, 1)
        })

        /** function to fetch replication info and update ui */
        function fetchReplicationData() {
          $.ajax({
            url: '/csvn/status/replicationInfo',
            type: "GET",
            success: function(data, textStatus, jqXHR) {
              numberOfCommands = data.relicaServerInfo.runningCmdsSize
              updateUiCommandsRunning(numberOfCommands)
            }
          });                    
        }

        /**
         * If there are commands running, then print the number and 
         */
        function updateUiCommandsRunning(numberOfCommands) {
            if (numberOfCommands > 0) {
                $('#spinner').prop('src', '/csvn/images/replica/commands_updating_spinner.gif');
                $('#commandsCount').html('<g:message code="status.page.status.replication.commands_running"/> ' + numberOfCommands);
            } else {
                $('#spinner').prop('src','/csvn/images/fping_up.gif');
                $('#commandsCount').html('<g:message code="status.page.status.replication.no_commands"/>'); 
            }
        }
    </g:if>
    </script>

  </head>
  <body>

    <content tag="title">
      <g:message code="status.page.header.title" />
    </content>

    <g:render template="/common/restartSupport"/>
    
    <!-- Following content goes in the left nav area -->
    <g:render template="/server/leftNav" />
    
  <g:form method="post">
  <div class="row-fluid">
    <div class="span7 well">
    <g:if test="${isStarted}">
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.subversion" /> </strong></div>
        <div class="span8"><img src="${resource(dir:'images', file:'fping_up.gif')}" width="16" height="16"
                         hspace="4" alt="<g:message code='status.page.subversion.on' />"/><g:message code="status.page.subversion.on" /></div>
      </div>
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.hostname" /> </strong></div><div class="span8"> ${server.hostname}</div>
      </div>
     <g:if test="${!ctfUrl && server.viewvcURL()}">
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.url.repository" /></strong></div>
        <div class="span8">
          <g:if test="${server.advancedConfig().listParentPath}"><a href="${server.svnURL()}" target="_blank">${server.svnURL()}</a></g:if>
          <g:else>${server.svnURL()}</g:else>
        </div>
      </div>
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.url.repository.browse" /></strong></div>
        <div class="span8">
          <g:if test="${server.advancedConfig().listParentPath}"><a href="${server.viewvcURL()}" target="_blank">${server.viewvcURL()}</a></g:if>
          <g:else>${server.viewvcURL()}</g:else>
        </div>
      </div>
     </g:if>
   </g:if>
   <g:else>
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.subversion" /> </strong></div>
        <div class="span8"><img src="${resource(dir:'images', file:'fping_down.gif')}" width="16" height="16"
                         hspace="4" alt="<g:message code='status.page.subversion.off' />"/><g:message code="status.page.subversion.off" /></div>
      </div>
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.hostname" /> </strong></div><div class="span8"> ${server.hostname}</div>
      </div>
   </g:else>
    <g:if test="${ctfUrl}">
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.url.teamforge" /></strong></div><div class="span8"> <a href="${ctfUrl}" target="_blank">${ctfUrl}</a></div>
      </div>
    </g:if>
    <g:if test="${isReplicaMode}">
      <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.replica.name" /></strong></div><div class="span8"> ${currentReplica.name}</div>
      </div>
      <g:if test="${currentReplica.svnMasterUrl}">
         <div class="row-fluid">
           <div class="span4"><strong><g:message code="status.page.replica.location" /></strong></div><div class="span8"> ${currentReplica.svnMasterUrl}</div>
         </div>
      </g:if>
    </g:if>
    <g:if test="${isStarted && isReplicaMode}">
        <div class="row-fluid">
        <div class="span4"><strong><g:message code="status.page.status.replication.activity" /></strong></div>
        <div class="span8">
        <g:set var="replicationStatusIcon" value="fping_up.gif" />
        <g:if test="${replicaCommandsSize > 0}">
            <g:set var="replicationStatusIcon" value="replica/commands_updating_spinner.gif" />
        </g:if>
        <img src="/csvn/images/${replicationStatusIcon}" id="spinner">
             <div id="commandsCount">
               <g:if test="${replicaCommandsSize == 0}">
                 <g:message code="status.page.status.replication.no_commands"/>
               </g:if>
               <g:else>
                 <g:message code="status.page.status.replication.commands_running"/> ${replicaCommandsSize}
               </g:else>
             </div>
         </div>
        </div>
    </g:if>
        
    </div>
    <div class="span1">
        <g:if test="${isStarted}">
          <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM">
            <g:actionSubmit class="btn btn-danger" value="${message(code:'status.page.subversion.stop')}" action="stop"/>
          </g:ifAnyGranted>
        </g:if>
        <g:else>
          <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM">
            <g:actionSubmit class="btn btn-success" value="${message(code:'status.page.subversion.start')}" action="start"/>
          </g:ifAnyGranted>
        </g:else>       
    </div>
    </div>
    </g:form>


      <h2><small style="color: #333;"><g:message code="status.page.header.information"/></small></h2>
      <table class="table table-striped table-bordered table-condensed">
        <tbody>
        <g:if test="${softwareVersion}">
          <tr>
            <td><strong><g:message code="status.page.status.version.software"/></strong></td>
            <td>${softwareVersion}</td>
          </tr>
        </g:if>
        <g:if test="${svnVersion}">
          <tr>
            <td><strong><g:message code="status.page.status.version.subversion"/></strong></td>
            <td>${svnVersion}</td>
          </tr>
        </g:if>
        <g:each status="i" var="stat" in="${perfStats}">
          <tr>
            <td><strong>${stat.label}</strong></td>
            <td>${stat.value ?: message(code: 'status.page.status.noData')}</td>
          </tr>
        </g:each>
        </tbody>
      </table>
  
  </body>
</html>
