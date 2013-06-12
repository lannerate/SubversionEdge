<html>
<head>
  <meta name="layout" content="main"/>
</head>

<content tag="title"><g:message code="server.page.advanced.title" /></content>

<g:render template="/server/leftNav" />

<body>
  <div class="alert alert-info"><g:message code="server.page.advanced.info"/></div>
  <g:form class="form-horizontal">
    <g:propCheckBox bean="${config}" field="autoVersioning" prefix="advancedConfiguration"/>
    <g:propTextField bean="${config}" field="compressionLevel" prefix="advancedConfiguration" sizeClass="small"/>
    <g:propControlsBody bean="${config}" field="allowBulkUpdates" prefix="advancedConfiguration">
      <g:checkBox name="allowBulkUpdates" value="${config.allowBulkUpdates}"/><label class="checkbox inline withFor" for="allowBulkUpdates"><g:message code="advancedConfiguration.allowBulkUpdates.option"/></label><br/>
      <g:checkBox name="preferBulkUpdates" value="${config.preferBulkUpdates}"/><label class="checkbox inline withFor" for="preferBulkUpdates"><g:message code="advancedConfiguration.preferBulkUpdates.option"/></label>   
    </g:propControlsBody>
    <g:propCheckBox bean="${config}" field="useUtf8" prefix="advancedConfiguration"/>
    <g:propTextField bean="${config}" field="hooksEnv" sizeClass="span6" prefix="advancedConfiguration"/>
    
  <g:if test="${!isManagedMode}">
    <g:propCheckBox bean="${config}" field="listParentPath" prefix="advancedConfiguration"/>
    <g:propControlsBody bean="${config}" field="pathAuthz" prefix="advancedConfiguration">
      <g:checkBox name="pathAuthz" value="${config.pathAuthz}"/><label class="checkbox inline withFor" for="pathAuthz"><g:message code="advancedConfiguration.pathAuthz.option"/></label><br/>
      <g:checkBox name="strictAuthz" value="${config.strictAuthz}"/><label class="checkbox inline withFor" for="strictAuthz"><g:message code="advancedConfiguration.strictAuthz.option"/></label>   
    </g:propControlsBody>
    <g:propTextField bean="${config}" field="svnRealm" prefix="advancedConfiguration" sizeClass="span6"/>
    <g:propTextField bean="${server}" field="svnBasePath" prefix="advancedConfiguration" sizeClass="small"/>
  </g:if>

   <fieldset>
    <legend><small><g:message code="server.page.advanced.sectionHeader.cache" /></small></legend>
    <g:propTextField bean="${config}" field="inMemoryCacheSize" prefix="advancedConfiguration" sizeClass="mini"/>
    <g:propCheckBox bean="${config}" field="cacheFullTexts" prefix="advancedConfiguration"/>
    <g:propCheckBox bean="${config}" field="cacheTextDeltas" prefix="advancedConfiguration"/>
    <g:propCheckBox bean="${config}" field="cacheRevProps" prefix="advancedConfiguration"/>    
   </fieldset>
   <fieldset>
    <legend><small><g:message code="server.page.advanced.sectionHeader.logging" /></small></legend>
    <g:propTextField bean="${config}" field="accessLogFormat" prefix="advancedConfiguration" sizeClass="span6"/>
    <g:propTextField bean="${config}" field="svnLogFormat" prefix="advancedConfiguration" sizeClass="span6"/>
   </fieldset>
   <g:if test="${!isManagedMode && server.ldapEnabled}">
     <fieldset>
       <legend><small><g:message code="server.page.advanced.sectionHeader.ldap" /></small></legend>
       <g:propTextField bean="${config}" field="ldapConnectionPoolTtl" prefix="advancedConfiguration" sizeClass="mini"/>
       <g:propTextField bean="${config}" field="ldapTimeout" prefix="advancedConfiguration" sizeClass="mini"/>
     </fieldset>
   </g:if>
    <div class="form-actions">
      <g:actionSubmit action="updateAdvanced" value="${message(code:'server.page.edit.button.save')}" class="btn btn-primary"/>
      <button type="reset" class="btn"><g:message code="default.button.cancel.label" /></button>
      <g:actionSubmit action="advancedRestoreDefaults" value="${message(code:'server.page.advanced.button.restoreDefaults')}" class="btn"/>
    </div>
  </g:form>
<g:javascript>
function allowBulkUpdatesHandler() {
  if (!$('#allowBulkUpdates').prop('checked')) {
    $('#preferBulkUpdates').prop('checked', false);
  }
}
$('#allowBulkUpdates').change(allowBulkUpdatesHandler);
allowBulkUpdatesHandler();

function preferBulkUpdatesHandler() {
  if ($('#preferBulkUpdates').prop('checked')) {
    $('#allowBulkUpdates').prop('checked', true);
  } 
}
$('#preferBulkUpdates').change(preferBulkUpdatesHandler);
preferBulkUpdatesHandler();

function pathAuthzHandler() {
  if ($('#pathAuthz').prop('checked')) {
    $('#strictAuthz').prop('disabled', false);
  } else {
    $('#strictAuthz').prop('checked', false);
    $('#strictAuthz').prop('disabled', true);
  }
}
$('#pathAuthz').change(pathAuthzHandler);
pathAuthzHandler();

function inMemoryCacheSizeHandler() {
  var size = $('#inMemoryCacheSize').val();
  if (!size || /^\s*0*\s*$/.test(size)) {
    $('#cacheFullTexts').prop('checked', false);
    $('#cacheFullTexts').prop('disabled', true);
    $('#cacheTextDeltas').prop('checked', false);
    $('#cacheTextDeltas').prop('disabled', true);
    $('#cacheRevProps').prop('checked', false);
    $('#cacheRevProps').prop('disabled', true);
  } else {
    $('#cacheFullTexts').prop('disabled', false);
    $('#cacheTextDeltas').prop('disabled', false);
    $('#cacheRevProps').prop('disabled', false);
  }
}
$('#inMemoryCacheSize').keyup(inMemoryCacheSizeHandler);
inMemoryCacheSizeHandler();
</g:javascript>  
</body>
</html>
