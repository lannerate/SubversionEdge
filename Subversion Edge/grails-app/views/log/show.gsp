<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<html>
<head>
  <meta name="layout" content="main"/>
  <g:javascript>
    $(function() {
        $('#tailButton').on('click', toggleLogStreaming)
        $('#tailSpinner').hide()

        // allow initial state of tailing
        if ('${params.view}' == 'tail') {
            toggleLogStreaming()
        }
    })

    var logStreamer;
    var tailLog = false
    function toggleLogStreaming() {
      tailLog = !tailLog;
      if (tailLog) {
          logStreamer  = new LogStreamer('${params.fileName}', '${fileSizeBytes}', $('#fileContent'), $('#fileContentDiv'), "\n\n** <g:message code="logs.page.show.tail.error"/> **\n")
          logStreamer.start();
          $('#tailButton').text("<g:message code="logs.page.show.button.tailStop"/>");
          $('#tailSpinner').show()
      }
      else {
          logStreamer.stop();
          $('#tailButton').text("<g:message code="logs.page.show.button.tail"/>");
          $('#tailSpinner').hide()
      }
    }

  </g:javascript>
</head>

<content tag="title">
  <g:message code="logs.page.show.title" args="${[params.fileName]}" encodeAs="HTML"/>
</content>

<g:render template="/server/leftNav" />

<body>
<g:if test="${flash.message}">
  <div class="message">${flash.message}</div>
</g:if>

<div id="fileName" class="row-fluid" style="margin-bottom: 5px;">
  <div class="span1"><strong><g:message code="logs.page.show.header.fileName" /></strong></div>
  <div class="span3">${params.fileName}</div>
  <div class="span1"><strong><g:message code="logs.page.show.header.size" /></strong></div>
  <div class="span2">${fileSize} &nbsp;</div>
  <div class="span2"><strong><g:message code="logs.page.show.header.lastModification" /></strong></div>
  <div class="span3">${fileModification}</div>
</div>

<g:if test="${file}">
  <div style="overflow: auto; height='300px'" id="fileContentDiv">
<!-- Leave this left-justified so that spaces are not padded in the first line of the log -->
<pre id="fileContent">
<%
  if (params.highlight) {
    file.withReader { reader ->
      String line
      boolean found = false
      while ( (line = reader.readLine() ) != null ) {
        if (!found && line.contains(params.highlight)) {
          line = "<a name='loc'> </a><BR>"  + line
          found = true
        }
        line = line.replace(params.highlight, "<span style='background-color: #FFFF00'>${params.highlight}</span>")
        out << line + "<BR/>"
      }
    }
  } else {
    file.withReader { reader ->
      String line
      while ( (line = reader.readLine() ) != null ) {
        out << StringEscapeUtils.escapeHtml(line) + "\n"
      }
    }
  }
%>
</pre>
  </div>

</g:if>
<g:else>
  <g:message code="logs.page.show.header.fileNotFound" args="${[params.fileName]}" encodeAs="HTML"/>

</g:else>

<div class="pull-right">
  <img id="tailSpinner" class="spinner" src="/csvn/images/spinner-gray-bg.gif" alt="Tailing.."/>
  <g:link id="tailButton" url="#" class="btn" onclick="return false"><g:message code="logs.page.show.button.tail" /></g:link>
  <g:link target="_blank" action="show" params="[fileName : file.name, view : 'raw']" class="btn"><g:message code="logs.page.show.button.viewRaw" /> &#133;</g:link>
  <g:link action="list" class="btn"><g:message code="logs.page.show.button.return" /></g:link>
</div>

<g:javascript>
  function resizeFileViewer() {
    $("#fileContentDiv").css('height', (Math.round($(window).height()) - 220) + 'px');
  }

  $(document).ready(function() {
    resizeFileViewer();
    $(window).bind('resize', resizeFileViewer);
  });
</g:javascript>


</body>
</html>
