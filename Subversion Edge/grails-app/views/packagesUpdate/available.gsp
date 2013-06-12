<html>
  <head>
  <title>
    CollabNet Subversion Edge <g:message code="packagesUpdate.page.available.title" />
  </title>
      <meta name="layout" content="main" />

      <script type="text/javascript" src="/csvn/plugins/cometd-0.1.5/dojo/dojo.js"
        djconfig="parseOnLoad: true, isDebug: false"></script>

      <script type="text/javascript">
        dojo.addOnLoad(function() {

           <g:if test="${anyConnectionProblem || !packagesInfo || packagesInfo.size() == 0}">
           document.getElementById("installButton").disabled = true;
           </g:if>

           <g:if test="${!anyConnectionProblem && packagesInfo && packagesInfo.size() > 0}">
           document.getElementById("reloadButton").disabled = true;
           </g:if>
        });
      </script>

  </head>
  <content tag="title">
    <g:message code="packagesUpdate.page.leftNav.header" />
  </content>

  <g:render template="/server/leftNav" />

  <body>

    <g:render template="/common/restartSupport"/>
    <g:render template="/packagesUpdate/packagesInfoTable" />

    <g:form method="post" id="updatesForm">

              <div class="pull-right">
                  <g:actionSubmit id="reloadButton" action="reloadUpdates" 
                                  value="${message(code:'packagesUpdate.page.available.button.reload')}" 
                                  class="btn"/>
                  <g:set var="confirmMsg" value="${message(code:'packagesUpdate.available.install.confirmation')}" />
                  <g:actionSubmit id="installButton" action="installUpdates" 
                                  value="${message(code:'packagesUpdate.page.available.button.install')}" 
                                  class="btn btn-primary"
                                  data-toggle='modal' data-target='#confirmInstall'
                               />
              </div>

      <div id="confirmInstall" class="modal hide fade" style="display: none">
        <div class="modal-header">
          <a class="close" data-dismiss="modal">&times;</a>
          <h3>${message(code: 'default.confirmation.title')}</h3>
        </div>
        <div class="modal-body">
          <p>${confirmMsg}</p>
        </div>
        <div class="modal-footer">
          <a href="#" class="btn btn-primary ok" 
             onclick="formSubmit($('#installButton').closest('form'), '/csvn/packagesUpdate/installUpdates')">${message(code: 'default.confirmation.ok')}</a>
          <a href="#" class="btn cancel" data-dismiss="modal">${message(code: 'default.confirmation.cancel')}</a>
        </div>
      </div>

    </g:form>

  </body>
</html>
