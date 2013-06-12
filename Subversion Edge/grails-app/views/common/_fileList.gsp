
<%@ page import="org.springframework.web.util.JavaScriptUtils" %>

<g:form>
  <input type="hidden" name="id" value="${params.id}" />
  <table class="table table-striped table-bordered table-condensed" id="datatable"></table>
  <script type="text/javascript">
    /* Data set */
    var aDataSet = [
      <g:each in="${fileList}" status="i" var="file">
      <g:set var="fileSize"><%=file.size()%></g:set>
      <g:set var="fileName">${JavaScriptUtils.javaScriptEscape(file.name)}</g:set>
      ['${fileName}', '${fileName}',
        '<g:formatDate format="${message(code: "default.dateTime.format.withZone")}" date="${file.date}"/>',
        '${file.size}|<g:formatFileSize size="${file.size}"/>']<g:if test="${i < (fileList.size() - 1)}">,</g:if>
      </g:each>
    ];
  </script>
  
  <div class="pull-right">
    <%=buttons%>
  </div>

  
  <g:javascript library="jquery.dataTables.min"/>
  <g:javascript library="DT_bootstrap"/>
  <g:javascript>
  /* Table initialisation */
  $(document).ready(function() {
    $('#datatable').dataTable( {
      "aaData": aDataSet,
      "aoColumns": [
	    {"sTitle": "",
	     "bSortable": false,
	     "fnRender": function (oObj, sVal) {
	        <g:set var="itemSelectStyles" value="${radio ? 'listViewSelectItem radio' : 'listViewSelectItem'}"/>
          var template = '<input type="checkbox" name="listViewItem_FILENAME" id="listViewItem_FILENAME" class="${itemSelectStyles}">';
          return template.replace(/FILENAME/g, sVal);
       }
	    },
	    {"sTitle": "${message(code: 'logs.page.list.column.name')}",
	     "fnRender": function (oObj, sVal) {
          var template = '<g:link action="${linkAction}" id="${params.id}" params="[filename : 'FILENAME']">FILENAME</g:link>';
          return template.replace(/FILENAME/g, sVal);
       }
	    },
		{ "sTitle": "${message(code: 'repository.page.fileList.timestamp')}" },
		{ "sTitle": "${message(code: 'repository.page.fileList.fileSize')}",
		  "fnRender": function (oObj, sVal) {
          var template = '<span title="SIZE">FORMATTED</span>';
          return template.replace("SIZE", sVal.split("|")[0]).replace("FORMATTED", sVal.split("|")[1]);
       },
		  "sType": "title-numeric" // sorts on title attribute, rather than text of the file size element
		}
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
        "sEmptyTable": "${noFilesMessage}",
        "sInfo": "${message(code:'datatable.showing')}",
        "sInfoEmpty": "${message(code:'datatable.showing.empty')}",
        "sInfoFiltered": " ${message(code:'datatable.filtered')}"
        },
      "fnCreatedRow": function(nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
          applyCheckboxObserverTo($('input.listViewSelectItem', nRow));
      },
      "fnDrawCallback": updateActionButtons, 
      "aaSorting": [[ 1, "asc" ]]
    } );
  } );
  </g:javascript>
  <g:javascript library="listView-3.0.0"/>
  
</g:form>
