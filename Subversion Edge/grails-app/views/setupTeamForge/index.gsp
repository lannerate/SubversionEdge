<html>
  <head>
      <meta name="layout" content="main" />
  </head>

  <g:render template="/server/leftNav" />

  <body>
  <div class="marketing">
    <h1><g:message code="setupTeamForge.page.index.almTitle" /></h1>
    <p class="marketing-byline"><g:message code="setupTeamForge.page.index.almByline" /></p>
    
    <div class="row-fluid">
      <div class="span4 pagination-centered" id="gallery" data-toggle="modal-gallery" data-target="#modal-gallery">
        <a href="${resource(dir:'images/teamforge',file:'teamforge-diagram.png')}" title="<g:message code="setupTeamForge.page.index.title" />" rel="gallery">
        <img src="${resource(dir:'images/teamforge',file:'teamforge-diagram-small.png')}"/>
        <p><g:message code="setupTeamForge.page.index.enlarge" /></p></a>
      </div>
      <div class="span8">
      <g:form method="post">
        <h3><img src="${resource(dir:'images/teamforge',file:'one.png')}" /> <g:message code="setupTeamForge.page.index.action.head.1" /></h3>
        <span class="pull-right"><a class="btn" target="_blank"
                href="http://www.collab.net/SVNE2TeamForge"><g:message code="setupTeamForge.page.index.button.discover" /> &raquo;</a>
        </span>
        <p style="margin-left:35px"><g:message code="setupTeamForge.page.index.action.body.1" /></p>
        <h3><img src="${resource(dir:'images/teamforge',file:'two.png')}" /> <g:message code="setupTeamForge.page.index.action.head.2" /></h3>
        <span class="pull-right"><g:actionSubmit id="btnCtfMode" value="${message(code:'setupTeamForge.page.index.button.connect')} &raquo;" 
            controller="setupTeamForge" action="ctfInfo" class="btn btn-primary"
            /></span>
        <p style="margin-left:35px"><g:message code="setupTeamForge.page.index.action.body.2" /></p>
        <h3><img src="${resource(dir:'images/teamforge',file:'three.png')}" /> <g:message code="setupTeamForge.page.index.action.head.3" /></h3>
        <span class="pull-right"><input type="button" id="btnReplicaMode" value="${message(code:'setupTeamForge.page.index.button.connect')} &raquo;"
             onclick="document.location.href='${createLink(controller: 'setupReplica', action:'ctfInfo')}'; return false"
             class="btn btn-primary"/>
        </span>
        <p style="margin-left:35px"><g:message code="setupTeamForge.page.index.action.body.3" /></p>
      </g:form>
      </div>
    </div>

    <!-- modal-gallery is the modal dialog used for the image gallery -->
    <div id="modal-gallery" class="modal modal-gallery hide fade">
        <div class="modal-header">
            <a class="close" data-dismiss="modal">&times;</a>
            <h3 class="modal-title"><g:message code="setupTeamForge.page.index.title" /></h3>
        </div>
        <div class="modal-body"><div class="modal-image"></div></div>
    </div>

    <hr/>

    <div class="row-fluid">
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_042_group.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.projects" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.projects" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_203_lock.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.rbac" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.rbac" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_079_podium.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.planning" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.planning" /></p>
        </div>
      </div>

      <div class="row-fluid">
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_214_resize_small.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.collaboration" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.collaboration" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_082_roundabout.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.ci" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.ci" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_266_book_open.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.docs" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.docs" /></p>
        </div>
      </div>

      <div class="row-fluid">
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_023_cogwheels.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.workflow" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.workflow" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_041_charts.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.reports" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.reports" /></p>
        </div>
        <div class="span4">
          <img class="bs-icon" src="${resource(dir:'images/glyphicons',file:'glyphicons_232_cloud.png')}">
          <h2><g:message code="setupTeamForge.page.index.point.head.cloud" /></h2>
          <p><g:message code="setupTeamForge.page.index.point.body.cloud" /></p>
        </div>
      </div>

      <a target="_blank" href="http://www.open.collab.net/products/ctf/capabilities.html"><g:message code="setupTeamForge.page.index.button.more" /></a>

  </div>

  </body>
</html>
