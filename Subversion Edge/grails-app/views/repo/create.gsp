<html>
<head>
  <meta name="layout" content="main"/>
  <script type="text/javascript" src="/csvn/js/simpletreemenu.js">
    /***********************************************
     * Simple Tree Menu- © Dynamic Drive DHTML code library (www.dynamicdrive.com)
     * This notice MUST stay intact for legal use
     * Visit Dynamic Drive at http://www.dynamicdrive.com/ for full source code
     ***********************************************/
  </script>
  <link rel="stylesheet" type="text/css" href="/csvn/css/simpletree.css"/>
  <script type="text/javascript">
  /* <![CDATA[ */
    $(document).ready(function() {
      $('#name').focus();
      $('input.repoInitOptions').click(showSelectedOptionDetail);

      setInitialOptionState()
      showSelectedOptionDetail()

      // load all repo dump files into the backup chooser div
      $.get('/csvn/repo/dumpFileListAll', prepareBackupsTree)
      // this only retrieves the cloud backups, if the option is selected
      getCloudBackups();
    });

    var initOptionParam = "${params.initOption ?: ''}";
    var initOptionSelectedParam = "${params.initOptionSelected ?: ''}";

    function showSelectedOptionDetail() {
      hideAllOptionDetails()
      $('input.repoInitOptions').each(function(index, item) {
        if (item.checked) {
          var detailsClass = ".initOptionDetail." + item.id;
          $(detailsClass).show();
        }
      })
    }

    function hideAllOptionDetails() {
      $('.initOptionDetail').hide();
    }

    function setInitialOptionState() {
      if (initOptionParam.length > 0) {
        $('input.repoInitOptions').each(function(index, item) {
          if (item.value == initOptionParam) {
            item.checked = true;
          }
        })
      }
    }

    function prepareBackupsTree(responseJson, textStatus, jqXHR) {
      if (responseJson.result != null) {
        backupsHtml = "<p><g:message code='repository.page.create.useBackup.instructions'/></p>";
        backupsHtml += '<ul id="backupsTree" class="treeview">';
        for (var key in responseJson.result.repoDumps) {
          // add repo name to list
          backupsHtml += "<li class='repo'>" + key
          // pre-open if item was selected in previous submit
          var initialStateOpen = initOptionSelectedParam.indexOf(key) > -1

          // add sublist with backup files for the repo
          var backups = responseJson.result.repoDumps[key]
          if (backups.length > 0) {
            if (initialStateOpen) {
              backupsHtml += "<ul class='backupList' rel='open'>"
            }
            else {
              backupsHtml += "<ul class='backupList'>"
            }
            for (i = 0; i < backups.length; i++) {
              // highlight backup file if item was selected in previous submit
              if (initOptionSelectedParam.indexOf(backups[i]) > -1) {
                backupsHtml += "<li class='backup selected'>"
              }
              else {
                backupsHtml += "<li class='backup'>"
              }
              backupsHtml += backups[i] + "</li>"
            }
            backupsHtml += "</ul>"
          }

          backupsHtml += "</li>"
        }
        backupsHtml += "</ul>"

        $('#backupChooser').html(backupsHtml);

        // use simple tree menu to style list as tree
        ddtreemenu.createTree("backupsTree", false)

        // add "clickability" to the backup file names
        $('ul.backupList li').click(function() {
            // unselect all items
            $('ul.backupList li').removeClass('selected');
            // select this item
            $(this).addClass('selected');

            // store the selection in a hidden field for submit
            var selectedItem = this.innerHTML;
            var repo = $(this.parentNode.parentNode).clone()
                .children().remove().end().text();
            $('#initOptionSelected').val(repo + "/" + selectedItem);
        });
      }
    }

    var isCloudReady = false;
    var initCloudBackupParam = "${params.cloudBackup ?: ''}";
    
    function getCloudBackups() {
        if ($('#useCloud').prop('checked') && !isCloudReady) {
            $.get('/csvn/repo/cloudBackupList', prepareCloudBackups);
        }
    }

    function prepareCloudBackups(responseJson, textStatus, jqXHR) {
        if (responseJson.result != null && responseJson.result.projects) {
            var html = "<p><g:message code='repository.page.create.useCloud.instructions'/></p>";
            var projects = responseJson.result.projects;
            for (var i = 0; i < projects.length; i++ ) {
                var project = projects[i];
                // add project name to list
                html += '<label><input name="cloudBackup" value=' + project.projectId + 
                    ' type="radio"';
                if (project.projectId == initCloudBackupParam) {
                    html += ' checked="checked"';
                }
                html += '/> ' + project.name + '</label>';
            }
            isCloudReady = true;
        } else {
            html += '<p><g:message code="repository.page.create.useCloud.emptyProjects"/></p>';
        }
        $('#cloudChooser').html(html);
    }   
  /* ]]> */
  </script>
  <style>
  div.initOptionDetail {
    border: 1px;
    border-color: #CCCCCC;
    border-style: solid;
    background-color: #EEEEEE;
    margin: 3px 0 6px 0;
    padding: 5px;
    overflow: auto;
  }

  .backup.selected {
    background-color: #0066CC;
    color: white;
  }

  </style>
</head>

<content tag="title"><g:message code="repository.page.create.title"/></content>

<g:render template="leftNav"/>

<body>
<g:form class="form-horizontal" action="save" method="post">
  <g:propTextField bean="${repo}" field="name" required="true" prefix="repository.page.create"/>
    
  <div class="control-group ${hasErrors(bean: repo, field: 'useBackup', 'error')}">
    <div class="control-label"><g:message code="repository.page.create.initOptions"/></div>
    <div class="controls">
      <label class="radio"><g:radio name="initOption" value="useTemplate" id="useTemplate" class="repoInitOptions" checked="checked"/>
            <g:message code="repository.page.create.useTemplate"/></label>
      <div id="templateChooser" class="initOptionDetail useTemplate" style="display:none">
        <g:each in="${templateList}" status="i" var="template">
            <label class="radio"><g:radio name="templateId" value="${template.id}" 
              checked="${(params.templateId == template.id as String) || (i == 0 && !params.templateId)}"/><span class="help-inline">${template.name}</span></label>
        </g:each>
      </div>    
      <label class="radio"><g:radio name="initOption" value="useBackup" id="useBackup" class="repoInitOptions"/>
        <g:message code="repository.page.create.useBackup"/></label>
      <g:hiddenField name="initOptionSelected" value="${params.initOptionSelected}"/>
      <div id="backupChooser" class="initOptionDetail useBackup" style="display:none">
        <g:message code="repository.page.create.useBackup.loading"/>
      </div>
      <g:if test="${showCloudBackups}">
        <label class="radio"><g:radio name="initOption" value="useCloud" id="useCloud" class="repoInitOptions" onclick="getCloudBackups();"/>
            <g:message code="repository.page.create.useCloud"/></label>
        <div id="cloudChooser" class="initOptionDetail useCloud" style="display:none">
          <img id="spinner" class="spinner" src="/csvn/images/spinner-gray-bg.gif" alt=""/>
          <g:message code="repository.page.create.useCloud.loading"/>
        </div>
      </g:if>
    </div>
  </div>
  <div class="form-actions">    
    <input class="btn btn-primary" type="submit" value="${message(code: 'repository.page.create.button.create')}"/>
   </div>
</g:form>
</table>
</body>
</html>
