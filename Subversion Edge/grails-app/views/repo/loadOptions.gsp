<html>
    <head>
        <meta name="layout" content="main" />
        
        <g:javascript>

// instantiate the polling task on load
$(document).ready(function() {
    $('#loadButton').click(function() {
        $('#uploadProgress').show();
        // re-animate IE
        setTimeout(function() { 
            $('#uploadSpinner').prop('src', '/csvn/images/spinner.gif');
        }, 100);
        return true;
    });
    
    $('#loadFileUpload').submit(function() {
        $('#loadButton').prop('disabled', true);
        setTimeout(fetchUploadProgress, 1000);
        setInterval(fetchUploadProgress, 5000);
        return true;
    });
});

/** function to fetch replication info and update ui */
function fetchUploadProgress() {
    $.getJSON('/csvn/repo/uploadProgress',
            {uploadProgressKey:'${uploadProgressKey}', avoidCache: new Date().getTime()},
            function(data, textStatus, jqXHR) {
                var percentComplete = data.uploadStats.percentComplete;
                $('#percentComplete').html(percentComplete + '%');
            });
}
        </g:javascript>
    </head>

<g:render template="leftNav" />

<content tag="title">
     <g:message code="repository.page.load.title" />
</content>

<body>
  <table>
    <tbody>
      <tr>
        <td style="padding-right: 20px"><g:message code="repository.page.dump.repo.name" /></td>
        <td>${repositoryInstance.name}</td>
      </tr>
      <tr>
        <td><g:message code="repository.page.show.status" /></td>
        <td><g:if test="${repositoryInstance.permissionsOk}">
               <g:if test="${repositoryInstance.verifyOk}">
                 <span style="color:green"><g:message code="repository.page.list.instance.permission.ok" /></span>
               </g:if>
               <g:else>
                 <span style="color:red"><g:message code="repository.page.list.instance.verify.failed" /></span>
               </g:else>
          </g:if> <g:else>
            <span style="color: red"><g:message code="repository.page.list.instance.permission.needFix" /></span>
          </g:else>
        </td>
      </tr>
      <tr>
        <td><g:message code="repository.page.dump.headRevision" /></td>
        <td>${headRev}</td>
      </tr>
      <tr>
        <td><g:message code="repository.page.show.uuid" /></td>
        <td>${repoUUID}</td>
      </tr>
    </tbody>
  </table>
  <br />
  <g:if test="${headRev > 0}">
    <div class="alert alert-info"><g:message code="repository.page.load.not.empty.message"/></div>
  </g:if>
  <h2><small><g:message code="repository.page.load.subtitle" /></small></h2>
  <g:uploadForm class="form-horizontal" action="loadFileUpload" name="loadFileUpload" params="${[uploadProgressKey: uploadProgressKey]}">
    <input type="hidden" name="id" value="${repositoryInstance?.id}" />    
    <div class="control-group required-field">
      <label class="control-label" 
          for="dumpFile"><g:message code="repository.page.load.fileupload.label"/></label>
      <div class="controls">
        <input type="file" name="dumpFile" id="dumpFile"/>
        <div class="help-block">
          <span id="uploadProgress" style="display: none;">
            <img id="uploadSpinner" class="spinner" align="middle" src="/csvn/images/spinner.gif" /><g:message 
                code="repository.page.load.uploading.ellipses"/>&nbsp;<span id="percentComplete"></span></span>
        </div>
      </div>
    </div>
    
    <g:if test="${headRev == 0}">
      <div class="control-group">
        <label class="control-label" 
            for="ignoreUuid"><g:message code="repository.page.load.ignoreUuid.label"/></label>
        <div class="controls">
          <g:checkBox name="ignoreUuid" id="ignoreUuid" value="${params.ignoreUuid}"/>
          <label class="checkbox inline withFor" for="ignoreUuid"><g:message code="repository.page.load.ignoreUuid.tip" /></label>
        </div>
      </div>
    </g:if>

    <div class="form-actions">    
      <g:submitButton id="loadButton" name="loadButton" value="${message(code:'repository.page.load.button.label')}" class="btn btn-primary"/>
      <g:submitButton name="cancelButton" value="${message(code:'default.confirmation.cancel')}" class="btn"/>
    </div>
  </g:uploadForm>
</body>
</html>
