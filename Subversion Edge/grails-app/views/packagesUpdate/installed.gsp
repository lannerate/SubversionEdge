<html>
  <head>
      <title>
        CollabNet Subversion Edge <g:message code="packagesUpdate.page.installed.title" />
      </title>
      <meta name="layout" content="main" />
      <g:javascript library="prototype" />

   <g:if test="${anyConnectionProblem || !packagesInfo || packagesInfo.size() == 0}">
      <script type="text/javascript" src="/csvn/plugins/cometd-0.1.5/dojo/dojo.js"
        djconfig="parseOnLoad: true, isDebug: false"></script>
      <script type="text/javascript">
        dojo.addOnLoad(function() {
           document.getElementById("reloadUpdates").disabled = true;
           document.getElementById("reloadAddOns").disabled = true;
        });
      </script>
   </g:if>

  </head>
  <content tag="title">
    <g:message code="packagesUpdate.page.leftNav.header" />
  </content>

  <g:render template="/server/leftNav" />

  <body>

    <g:render template="/packagesUpdate/packagesInfoTable" />

    <g:form method="post">

              <div class="pull-right">
                  <g:actionSubmit id="reloadUpdates" action="reloadUpdates" 
                                  value="${message(code:'packagesUpdate.page.installed.button.reloadUpdates')}" 
                                  class="btn btn-primary"/>
                  <g:actionSubmit id="reloadAddOns" action="reloadAddOns" 
                                  value="${message(code:'packagesUpdate.page.installed.button.reloadAddOns')}" 
                                  class="btn"/>
              </div>

    </g:form>

  </body>
</html>
