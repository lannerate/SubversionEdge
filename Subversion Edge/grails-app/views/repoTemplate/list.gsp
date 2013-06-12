<html>
    <head>
        <meta name="layout" content="main" />
        <g:javascript library="listView-3.0.0"/>
        <g:javascript library="jquery-ui-1.8.18.custom.min"/>
    </head>

<g:render template="/repo/leftNav" />

<content tag="title">
   <g:message code="repoTemplate.page.list.header.title" />
</content>


<body>
<!-- 
  <div class="nav">
    <span class="menuButton"><g:link class="create" action="create"><g:message code="repoTemplate.page.create.header.title" /></g:link></span>
  </div>
-->
  <p>
    <g:message code="repoTemplate.page.list.sort.instructions"/>
  </p>

    
      <table id="reposTable" class="table table-striped table-bordered table-condensed tablesorter">
        <thead>
        <tr>
          <th><g:message code="repoTemplate.page.list.name.label"/></th>
          <th><g:message code="repoTemplate.active.label"/></th>
        </tr>
      </thead>
      <tbody id="templates">
        <g:each in="${repoTemplateInstanceList}" status="i" var="repoTemplateInstance">
          <tr id="repoTemplate_${repoTemplateInstance.id}" style="cursor: move">
            <!-- <td><g:listViewSelectItem item="${repoTemplateInstance}"/></td> -->
            <td><g:link action="edit" id="${repoTemplateInstance.id}">${repoTemplateInstance.name}</g:link></td>
            <td><g:formatBoolean boolean="${repoTemplateInstance.active}" /></td>
          </tr>
        </g:each>
        <g:if test="${repoTemplateInstanceList.size() == 0}">
          <tr>
            <td colspan="2">
              <p><g:message code="repoTemplate.page.list.empty"/></p>
            </td>
          </tr>
        </g:if>
      </tbody>
      </table>
      
      <g:form>
      <p class="pull-right">
        <g:listViewActionButton action="create" minSelected="0" maxSelected="0" primary="true"><g:message code="default.button.create.label" /></g:listViewActionButton>
      </p>
      </g:form>
      
      <g:javascript>
        $('#templates').sortable({
            helper: function(event, elem)
            {
              var originals = elem.children();
              var helper = elem.clone();
              helper.children().each(function(index)
              {
                // Set helper cell sizes to match the original sizes
                $(this).width(originals.eq(index).width())
              });
              return helper;
            },
            stop: function(event, ui) {
              itemList = new Array();
              $('#templates tr').each ( function() {
                var id = $(this).prop('id').replace(/[^\d]+/g, '');
                itemList[itemList.length] = id;
              })
              $.post('/csvn/repoTemplate/updateListOrder', {
                'templates[]': itemList.join(",")
              });
          }
        });
      </g:javascript>
</body>
</html>
