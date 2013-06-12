<g:applyLayout name="repoDetail">
 <content tag="headSnippet">
   <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
 </content>

  <g:set var="listViewButtons">
  <g:listViewActionButton action="downloadDumpFile" minSelected="1" maxSelected="1">
    <g:message code="repository.page.fileList.button.download.label"/>
  </g:listViewActionButton>
  <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO">
    <g:listViewActionButton action="deleteDumpFiles" minSelected="1" maxSelected="1"
        confirmMessage="${message(code:'repository.page.fileList.delete.confirmation')}">
      <g:message code="default.button.delete.label"/>
    </g:listViewActionButton>
  </g:ifAnyGranted>
 </g:set>

 <content tag="tabContent">
   <g:render template="/common/fileList" 
     model="${[fileList: dumpFileList, buttons: listViewButtons, linkAction: 'downloadDumpFile',
               noFilesMessage: message(code: 'repository.page.dumpFileList.noFiles'),
               radioStyle: true]}" />
 </content>
</g:applyLayout>
