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

<content tag="title"><g:message code="repository.page.leftnav.title" /></content>

<body>
<g:if test="${isStarted}">

<form class="form-inline" action="${server.viewvcURL()}" target="_blank">
<div class="control-group required-field">
  <div class="controls">
    <input name="root" placeholder="<g:message code="repository.page.index.repositoryPlaceholder" />" value="" type="text" class="input-large search-query" id="root" autocomplete="off"/> <input type="submit" value="<g:message code="repository.page.index.browseButton" />" class="btn"/>
    <div class="help-block"><span class="help-marker"></span><g:message code="repository.page.index.searchRepositoryHelp" /></div>
  </div>
</div>
</form>
  
</g:if>
<g:elseif test="${isSystemAdmin}">

<p><g:message code="repository.page.index.systemAdminServerStopped" args="${[createLink(controller: 'status', action: 'index')]}"/></p>

</g:elseif>
<g:else>

<p class="alert"><g:message code="repository.page.index.serverStopped"/></p>

<address>
<strong><g:message code="repository.page.index.serverAdmin" /></strong><br />
${server.adminName}<br />
<a href="mailto:${server.adminEmail}">${server.adminEmail}</a><br />
<g:if test="${server.adminAltContact}">
${server.adminAltContact}<br />
</g:if>

</g:else>

<g:if test="${!isAdmin}">
<br />
<h4><g:message code="repository.page.index.otherActions" /></h4>
<p><g:message code="repository.page.index.nonAdmin" /></p>
<ul>
  <g:if test="${isStarted && server.advancedConfig().listParentPath}">
    <li>
      <g:message code="repository.page.index.listRepositories" args="${[server.viewvcURL()]}"/>
    </li>      
  </g:if>

<li><g:message code="repository.page.index.extensions" args="${[createLink(controller: 'ocn', action: 'index')]}"/></li>
<li><g:message code="repository.page.index.selfProfile" args="${[createLink(controller: 'user', action: 'showSelf')]}"/></li>
</ul>
</p>
</g:if>

<g:javascript>
var prevQuery = '';
var typeAheadSource = [];
var isIgnoreKeyUp = false;
$(document).ready(function() {
typeAhead = $('#root').typeahead({
source: typeAheadSource, 	
items: 20,
minLength: 3,
});
});

$('#root').keyup( function() {
 var el = $('#root')
 if (!isIgnoreKeyUp) {
  var query = el.val();
  if (query.length >= 3) {
    if (query.contains(prevQuery) && typeAheadSource.length > 0) {
      return typeAheadSource;
    } else {
      isIgnoreKeyUp = true;
      prevQuery = query;
      typeAheadSource = [];
      $.ajax({
        url: '/csvn/repo/listMatching', 
        data: { q: query },
        success: function(data, status, xhr) {
            var responseJson = data;
            typeAheadSource = responseJson.result.repositories;
            el.data('typeahead').source = typeAheadSource;
      		el.keyup();
      		setTimeout('isIgnoreKeyUp = false;', 50);
        },
        error: function(xhr, status, error) {
          isIgnoreKeyUp = false;
        }
      });
    }
  } else {
    typeAheadSource = [];
  }
 }
});
</g:javascript>
</body>
</html>