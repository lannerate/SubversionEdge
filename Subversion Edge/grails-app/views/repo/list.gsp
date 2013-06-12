<%@ page import="org.springframework.web.util.JavaScriptUtils" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <title>CollabNet Subversion Edge <g:message code=repository.page.list.header.title /></title>
        <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
        <g:set var="adminView" value="${false}"/>
        <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_HOOKS">
          <g:set var="adminView" value="${true}"/>
        </g:ifAnyGranted>  
    </head>

<g:render template="leftNav" />

<content tag="title">
   <g:message code="repository.page.leftnav.title" />
</content>

<body>

<g:form>
    <table id="datatable" class="table table-striped table-bordered table-condensed tablesorter"></table>
    <script type="text/javascript">
    /* Data set */
    var aDataSet = [
    <g:each in="${repositoryInstanceList}" status="i" var="repositoryInstance">
    <g:set var="repoName" value="${JavaScriptUtils.javaScriptEscape(repositoryInstance.name)}"/>
    <g:set var="statusMsg" value="${repositoryInstance.permissionsOk ? (repositoryInstance.verifyOk ? message(code: 'repository.page.list.instance.permission.ok') : message(code: 'repository.page.list.instance.verify.failed')) : message(code: 'repository.page.list.instance.permission.needFix')}"/>
      <g:if test="${adminView}">
        ['${repositoryInstance.id}',
          '${repoName}',
          '${repoName}',
          '${repositoryInstance.id}|${statusMsg}'
        ]<g:if test="${i < (repositoryInstanceList.size() - 1)}">,</g:if>
      </g:if> 
      <g:else>
        ['${repoName}', '${repoName}']<g:if test="${i < (repositoryInstanceList.size() - 1)}">,</g:if>
       </g:else> 
    </g:each>
    ];
    </script>

<g:if test="${adminView}">
  <div class="pull-right">
  <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO">
    <g:if test="${!isManagedMode}">
      <g:listViewActionButton action="create" minSelected="0" maxSelected="0"><g:message code="default.button.create.label" /></g:listViewActionButton>
    </g:if>
    <g:if test="${!isReplicaServer}">
      <g:listViewActionButton action="discover" minSelected="0" maxSelected="0"><g:message code="repository.page.list.button.discover.label" /></g:listViewActionButton>
    </g:if>
  </g:ifAnyGranted>
   <g:listViewActionButton action="show" minSelected="1" maxSelected="1"><g:message code="default.button.show.label" /></g:listViewActionButton>
  <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO">  
   <g:listViewActionButton action="dumpOptions" minSelected="1" maxSelected="1">
     <g:message code="repository.page.list.button.dump.label"/>
   </g:listViewActionButton>
   <g:listViewActionButton action="verify" minSelected="1" maxSelected="1"><g:message code="default.button.verify.label" /></g:listViewActionButton>
   <g:if test="${!isReplicaServer}">
     <g:listViewActionButton action="loadOptions" minSelected="1" maxSelected="1">
       <g:message code="repository.page.list.button.load.label"/>
     </g:listViewActionButton>
   </g:if>
   <g:if test="${!isManagedMode}">
     <g:listViewActionButton action="deleteMultiple" minSelected="1" maxSelected="1"
                             confirmMessage="${message(code:'repository.page.list.delete.confirmation')}"
                             confirmByTypingThis="${message(code:'default.confirmation.typeThis')}">
       <g:message code="default.button.delete.label"/>
     </g:listViewActionButton>
   </g:if>
   <g:if test="${isReplicaServer}">
     <g:listViewActionButton action="replicaSyncRepo" minSelected="1">
       <g:message code="repository.page.list.button.replicaSyncRepo.label"/>
     </g:listViewActionButton>
     <g:listViewActionButton action="replicaSyncRevprops" minSelected="1" maxSelected="1"
         dialog="true" dialogTitle="${message(code: 'repository.page.list.replicaSyncRevprops.confirmation')}"
         label="${message(code: 'repository.page.list.button.replicaSyncRevprops.label')}">
       <div class="control-group" id="revision">
         <div class="controls">
           <label class="radio">
             <g:radio class="inline" id="specifiedRevision" name="revisionType" value="specified" checked="checked"/>
                 <g:message code="repository.page.list.replicaSyncRevprops.revision"/></label>
                 <input name="revision" id="revision" onclick="$('#specifiedRevision').prop('checked', true);"/>
           <label class="radio">
             <g:radio id="allRevision" name="revisionType" value="all"/>
               <g:message code="repository.page.list.replicaSyncRevprops.revision.all"/>
           </label>
         </div>
       </div>
     </g:listViewActionButton>
   </g:if>
   </g:ifAnyGranted>
  </div>
</g:if>
</g:form>

<g:javascript library="jquery.dataTables.min"/>
<g:javascript library="DT_bootstrap"/>
<g:javascript>
  /* Table initialisation */
  $(document).ready(function() {
    var dt = $('#datatable').dataTable( {
      "aaData": aDataSet,
      "sDom": "<'row-fluid'<'span4'l><'pull-right'f>r>t<'row-fluid'<'span4'i><'pull-right'p>><'spacer'>",
      "sPaginationType": "bootstrap",
      "bStateSave": true,
      "fnStateSave": tableState.save('datatable'),
      "fnStateLoad": tableState.load('datatable'),
      "oLanguage": {
        "sLengthMenu": "${message(code:'datatable.rowsPerPage')}",
        "oPaginate": {
            "sNext": "${message(code:'default.paginate.next')}",
            "sPrevious": "${message(code:'default.paginate.prev')}"
        },
        "sSearch": "${message(code:'default.filter.label')}",
        "sZeroRecords": "${message(code:'default.search.noResults.message')}",
        "sEmptyTable": "${(isReplicaServer) ? message(code:'repository.page.list.replica.noRepos') : message(code:'repository.page.list.noRepos')}",
        "sInfo": "${message(code:'datatable.showing')}",
        "sInfoEmpty": "${message(code:'datatable.showing.empty')}",
        "sInfoFiltered": " ${message(code:'datatable.filtered')}"
      },
      "fnCreatedRow": function(nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
          applyCheckboxObserverTo($('input.listViewSelectItem', nRow));
      },
      "fnDrawCallback": updateActionButtons, 
      <g:if test="${adminView}">  
      "aaSorting": [[ 1, "asc" ]],
      "aoColumns": [
        {"sTitle": "<g:if test="${isReplicaServer}"><g:listViewSelectAll name="datatableSelectAll"/></g:if>", 
         "bSortable": false,
         "fnRender": function (oObj, sVal) {
           var template = '<input type="checkbox" class="listViewSelectItem<g:if test="${!isReplicaServer}"> radio</g:if>" id="listViewItem_ID" name="listViewItem_ID"/>'
           return template.replace(/ID/g, sVal);
         }
        },
        {"sTitle": "${message(code:'repository.page.list.name')}",
         "fnRender": function ( oObj, sVal ) {
           var template = '<a href="${server.viewvcURL("REPO")}" target="_blank">REPO</a>';
           return template.replace(/REPO/g, sVal);
         }
        },
        {"sTitle": "${message(code:'repository.page.list.checkout_command')}",
         "fnRender": function (oObj, sVal) {
           var template = 'svn co ${server.svnURL()}REPO REPO --username=<g:loggedInUsername/>';
           var repoName = sVal;
           if (navigator.appVersion.indexOf("Win") == -1) {
             repoName = repoName.replace("'", "\\'");
           }
           return template.replace(/REPO/g, repoName);
         }
        },
        {"sTitle": "${message(code:'repository.page.list.status')}",
         "bSortable": false ,
         "fnRender": function(oObj, sVal) {
           var template = '<g:link action="show" id="REPOID">MSG</g:link>';
           return template.replace("REPOID", sVal.split("|")[0]).replace("MSG", sVal.split("|")[1]);
         }
        }
      ]
      </g:if>
      <g:else>  
      "aaSorting": [[ 0, "asc" ]],
      "aoColumns": [
        {"sTitle": "${message(code:'repository.page.list.name')}",
         "fnRender": function ( oObj, sVal ) {
           var template = '<a href="${server.viewvcURL("REPO")}" target="_blank">REPO</a>';
           return template.replace(/REPO/g, sVal);
         }
        },
        {"sTitle": "${message(code:'repository.page.list.checkout_command')}",
         "fnRender": function (oObj, sVal) {
           var template = 'svn co ${server.svnURL()}REPO REPO --username=<g:loggedInUsername/>';
           var repoName = $(oObj.aData[0]).text();
           if (navigator.appVersion.indexOf("Win") == -1) {
             repoName = repoName.replace("'", "\\'");
           }
           return template.replace(/REPO/g, repoName);
         }
        }
      ]
      </g:else>
    } );
    
    // limit filter to column 1 only (the repo name)
    filterElement= $('#datatable_filter').find("input")
  	filterElement.keyup( function () {
        dt.fnFilter(filterElement.val(), 1);
    } );
  } );
</g:javascript>
<g:javascript library="listView-3.0.0"/>

</body>
</html>
