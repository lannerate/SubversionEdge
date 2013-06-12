<g:applyLayout name="repoDetail">
 <content tag="headSnippet">
   <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
 </content>

 <g:set var="listViewButtons">
    <g:listViewActionButton action="createHook" minSelected="0" maxSelected="0">
      <g:message code="default.button.create.label"/>
    </g:listViewActionButton>
    <g:listViewActionButton action="editHook" minSelected="1" maxSelected="1">
      <g:message code="default.button.edit.label"/>
    </g:listViewActionButton>
    <g:listViewActionButton action="copyHook" minSelected="1" maxSelected="1"
        textInput="true"
        confirmMessage="${message(code:'repository.page.fileList.copy.confirmation')}">
      <g:message code="repository.page.fileList.button.copy.label"/>
    </g:listViewActionButton>
    <g:listViewActionButton action="renameHook" minSelected="1" maxSelected="1"
        textInput="true"
        confirmMessage="${message(code:'repository.page.fileList.rename.confirmation')}">
      <g:message code="repository.page.fileList.button.rename.label"/>
    </g:listViewActionButton>
    <g:listViewActionButton action="downloadHook" minSelected="1" maxSelected="1">
      <g:message code="repository.page.fileList.button.download.label"/>
    </g:listViewActionButton>
    <g:listViewActionButton action="deleteHook" minSelected="1" maxSelected="1"
        confirmMessage="${message(code:'repository.page.fileList.delete.confirmation')}">
      <g:message code="default.button.delete.label"/>
    </g:listViewActionButton>
 </g:set>

 <content tag="tabContent">
   <g:render template="/common/fileList" 
     model="${[fileList: hooksList, buttons: listViewButtons, linkAction: 'downloadHook',
               noFilesMessage: message(code: 'repository.page.hooksFileList.noFiles'),
               radioStyle: true]}" />
 </content>
 
</g:applyLayout>
