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
<html>
<head>
  <title>CollabNet Subversion Edge <g:message code="admin.page.leftNav.cloudServices"/></title>
  <meta name="layout" content="main"/>
</head>

<g:render template="/server/leftNav"/>

<body>
  <div class="marketing">
    <div class="row-fluid">
    <span class="pull-right"><a target="_blank" href="http://www.open.collab.net/cloud/"><img src="${resource(dir:'images/cloud',file:'subversion-edge-to-the-cloud.png')}"/></a></span>
    <h1 style="text-align:left"><g:message code="setupCloudServices.page.index.title"/></h1>
    <p class="marketing-byline" style="text-align:left"><g:message code="setupCloudServices.page.index.byline"/></p>
    </div>

    <div class="row-fluid">&nbsp;</div>

    <div class="row-fluid">
      <div class="span4">
        <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_079_podium.png')}">
        <h2><g:message code="setupCloudServices.page.index.point.1"/></h2>
        <p><g:message code="setupCloudServices.page.index.body.1"/></p>
      </div>
      <div class="span4">
        <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_203_lock.png')}">
        <h2><g:message code="setupCloudServices.page.index.point.2"/></h2>
        <p><g:message code="setupCloudServices.page.index.body.2"/></p>
      </div>
      <div class="span4">
        <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_082_roundabout.png')}">
        <h2><g:message code="setupCloudServices.page.index.point.3"/></h2>
        <p><g:message code="setupCloudServices.page.index.body.3"/></p>
      </div>
    </div>

    <hr/>

    <div class="row-fluid">
    
      <div class="span6">
        <p><img src="${resource(dir:'images/cloud',file:'cloud-backup-logo.png')}" /></p>
        <p><g:message code="setupCloudServices.page.index.service.backup.detail"/></p>
        <p><span class="pull-right"><g:link controller="setupCloudServices" action="getStarted"
           class="btn btn-primary"><g:message code="setupCloudServices.page.index.button.continue"/></g:link></span></p>
      </div>
    
      <div class="span6">
        <p><img src="${resource(dir:'images/cloud',file:'cloudforge-logo.png')}" /></p>
        <p><g:message code="setupCloudServices.page.index.service.migrate.detail"/></p>
        <p><span class="pull-right"><a target="_blank"
            href="https://app.cloudforge.com/trial_signup/new?source=svnedge" class="btn btn-primary"><g:message code="setupCloudServices.page.index.button.moveToCloud"/></a></span>
        </p>
      </div>
    
    </div>

  </div>

</body>
</html>
