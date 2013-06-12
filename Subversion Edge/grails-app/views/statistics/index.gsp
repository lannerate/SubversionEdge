<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main" />
    <title>CollabNet Subversion Edge <g:message code="statistics.page.title" /></title>
    </head>
    

      <content tag="title">
        <g:message code="statistics.page.title" /> 
      </content>
    
      <!-- Following content goes in the left nav area -->
        <g:render template="/server/leftNav"/>

      <g:render template="chart" />

        <ul class="category">
        <g:each status="i" var="data" in="${statData}">
          <%-- hide empty stat group categories (eg, replica-only categories in standalone mode --%>
          <g:if test="${data.statgroups}">
          <div class="span3"><h3>${data.category}</h3>
            <g:each status="j" var="group" in="${data.statgroups}">
              <div class="group">
                ${group.statgroup}
                <ul class="graph">
                <g:each status="k" var="graph" in="${group.graphs}">
                  <li><a href="#" 
                         onclick="setCurrentGraph('${graph.graphData}');
                                  updateGraph();return false;">
                         ${graph.graphName}
                  </a></li>
                </g:each>
              </div>
            </g:each>
          </div> <!-- /span3 -->
          </g:if>
        </g:each>
        </ul>
  
</body>
</html>
