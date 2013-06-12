<%@ page import=" org.springframework.web.util.JavaScriptUtils" %>
<head>
    <meta name="layout" content="main" />
  <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>

</head>


<content tag="title">
  <g:message code="user.page.header"/>
</content>

<g:render template="leftNav" />

<body>
  <table class="table table-striped table-bordered table-condensed tablesorter" id="datatable"></table>
  <script type="text/javascript">
  /* Data set */
  var aDataSet = [
  <g:each in="${userInstanceList}" status="i" var="person">
      ['${person.id}|${person.username}',
        '<%= JavaScriptUtils.javaScriptEscape(person.realUserName) %>',
        '<%= JavaScriptUtils.javaScriptEscape(person.description) %>']<g:if test="${i < (userInstanceList.size() - 1)}">,</g:if>
  </g:each>
  ];
  </script>
<g:form>
  <p class="pull-right">
    <g:listViewActionButton action="create" minSelected="0" maxSelected="0" primary="true"><g:message code="default.button.create.label" /></g:listViewActionButton>
  </p>
</g:form>

<g:javascript library="jquery.dataTables.min"/>
<g:javascript library="DT_bootstrap"/>
<g:javascript>
  /* Table initialisation */
  $(document).ready(function() {
    $('#datatable').dataTable( {
      "aaData": aDataSet,
	  "aoColumns": [
	    {"sTitle": "${message(code: 'user.page.list.column.username')}",
	     "fnRender": function (oObj, sVal) {
          var template = '<g:link action="edit" id="ID">USERNAME</g:link>'
          return template.replace("ID", sVal.split("|")[0]).replace("USERNAME", sVal.split("|")[1]);;
       }
	    },
		  {"sTitle": "${message(code: 'user.page.list.column.realUserName')}" },
		  {"sTitle": "${message(code: 'user.page.list.column.description')}" }
	  ],
      "sDom": "<'row-fluid'<'span4'l><'pull-right'f>r>t<'row-fluid'<'span4'i><'pull-right'p>><'spacer'>",
      "sPaginationType": "bootstrap",
      "bStateSave": true,
      "fnStateSave": tableState.save('datatable'),
      "fnStateLoad": tableState.load('datatable'),
      "oLanguage": {
        "sLengthMenu": "${message(code:'datatable.rowsPerPage')}",
        "oPaginate": {
            "sNext": "${message(code:'default.paginate.next')}",
            "sPrevious": "${message(code:'default.paginate.prev')}"
        },
        "sSearch": "${message(code:'default.filter.label')}",
        "sZeroRecords": "${message(code:'default.search.noResults.message')}",
        "sEmptyTable": "${message(code:'default.search.noResults.message')}",
        "sInfo": "${message(code:'datatable.showing')}",
        "sInfoEmpty": "${message(code:'datatable.showing.empty')}",
        "sInfoFiltered": " ${message(code:'datatable.filtered')}"
        },
      "aaSorting": [[ 0, "asc" ]]
    } );
  } );
</g:javascript>

</body>
