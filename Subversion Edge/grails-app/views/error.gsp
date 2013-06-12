<!DOCTYPE html>
<html lang="en">
  <head>
    <g:sslRedirect/>
    <title>CollabNet Subversion Edge Error"</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Subversion Edge"/>
    <meta name="author" content="CollabNet"/>

    <link href="${resource(dir:'css',file:'bootstrap.css')}" rel="stylesheet"/>
    <style type="text/css">
      body {
        padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
        padding-bottom: 40px;
      }

      .sidebar-nav {
        padding: 9px 0;
      }
    </style>
    <link href="${resource(dir:'css',file:'bootstrap-responsive.css')}" rel="stylesheet"/>
    <link href="${resource(dir:'css',file:'svnedge-3.0.0.css')}" rel="stylesheet"/>                                                                                                                                                                  
 
    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
      
    <link rel="shortcut icon" href="${resource(dir:'images/icons',file:'favicon.ico')}" />          
    <g:javascript library="application-3.0.0" />
    
    <style type="text/css">
      .message {
        border: 1px solid black;
        padding: 5px;
        background-color:#E9E9E9;
      }
      .stack {
        border: 1px solid black;
        padding: 5px;
        overflow:auto;
        height: 300px;
      }
      .snippet {
        padding: 5px;
        background-color:white;
        border:1px solid black;
        margin:3px;
        font-family:courier;
      }
    </style>
  </head>
  <body ${pageProperty(name: 'body.onload', writeEntireProperty: true)}${pageProperty(name: 'body.onunload', writeEntireProperty: true)}>
    
    <div class="navbar navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse"> 
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <g:link controller="status" class="brand"><img
               src="${resource(dir:'images/masthead',file:'logo.png')}"
               alt="${message(code:'layout.page.home') }"/></g:link>
          <div class="nav-collapse">
            <!-- buttons -->
            <ul class="nav">
              <li><a href="${createLink(controller: 'status')}" 
                  target="_top"><g:message code="status.main.icon"/></a></li>
            </ul>
            <!-- buttons end -->
            <ul class="nav pull-right">
                <g:isNotLoggedIn>
                  <li><g:link controller="login"><g:message code="layout.page.login" /></g:link>
                </g:isNotLoggedIn>
                <g:isLoggedIn>
                    <li>
                    <g:link controller="user" action="showSelf">
                    <g:loggedInUserInfo field="realUserName"/>&nbsp;(<g:loggedInUsername/>)
                    </g:link>
                </li>
                <li class="divider-vertical"></li>
                <li><g:link controller="logout"><g:message code="layout.page.logout"/></g:link>
                </g:isLoggedIn>
              </li>
              <li class="divider-vertical"></li>
              <g:render template="/layouts/helpLink"/>
            </ul>
          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>
    <g:render template="/layouts/aboutModal"/>
    
    <!-- main content section begin -->
    <div class="container-fluid">    
      <div class="sessionmessages" id="sessionmessages">
         <div class="alert alert-block alert-error"><g:message code="error.alertMessage"/></div>
      </div>
    </div> <!-- /container-fluid -->
 
    <div id="main" class="container-fluid">
      <div class="row-fluid">

        <div class="span12">
          <div class="row-fluid">
            <div class="span2">
              <h2>Error</h2>
            </div>
          </div>

          <div class="row-fluid">
                    <p><g:message code="error.contactAdmin"/></p> 
                    <p><g:message code="error.submitErrorReport" args="${['https://ctf.open.collab.net/sf/discussion/do/listTopics/projects.svnedge/discussion.user_questions', 
                        (exception && exception.message ? exception?.message?.replace('?', 'QMark').replace('&', 'AND').encodeAsHTML().replace('&', '_') : 'null')]}"/>
                    <br />
                    <!-- \" , '${}'      -->
                    
                    <g:message code="error.showDetails" args="${['setDisplayMode(\'errorDetails\', \'block\')']}"/> 
                    <a href="#" onclick="setDisplayMode('errorDetails', 'block')">[ <g:message code="error.showDetailsLink"/> ]</a>
                    </p>
                    <div id="errorDetails" style="display: none;">
                        <a style="float: right" href="#" onclick="setDisplayMode('errorDetails', 'none')"><small>[ <g:message code="error.hideDetails"/> ]</small></a><h2>Details</h2>
    <div class="message">
      <strong>Error ${request.'javax.servlet.error.status_code'}:</strong>
      ${request.'javax.servlet.error.message'}<br/>
      <strong>Servlet:</strong>
      ${request.'javax.servlet.error.servlet_name'}<br/>
      <strong>URI:</strong> ${request.'javax.servlet.error.request_uri'}<br/>
      <g:if test="${exception}">
        <input type="hidden" name="ExceptionMessage" value=""/>
        <strong>Exception Message:</strong>
        ${exception.message ?: 'null'} <br />
        <strong>Caused by:</strong>
        ${exception.cause?.message} <br />
        <strong>Class:</strong> ${exception.className} <br />
        <strong>At Line:</strong> [${exception.lineNumber}] <br />
        <g:if test="${exception.codeSnippet}">
        <strong>Code Snippet:</strong><br />
        <div class="snippet">
          <g:each var="cs" in="${exception.codeSnippet}">
            ${cs}<br />
          </g:each>
        </div>
        </g:if>
      </g:if>
    </div>
    <g:if test="${exception}">
     <h2>Stack Trace</h2>
      <div class="stack">
      <pre>
<g:each in="${exception.stackTraceLines}">${it}</g:each>
      </pre>
      </div>
    </g:if>
                    </div>
          </div>
        </div><!-- /spanX -->
      </div><!-- /row-fluid -->
    </div><!-- /container-fluid #main-->
    <!-- main content section end -->
    <!-- ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <g:javascript library="jquery-1.7.1.min"/>
    <g:javascript library="bootstrap"/>    
    <script type="text/javascript">
      function setDisplayMode(id, mode) {
        document.getElementById(id).style.display = mode; 
        return false;
      }
    </script>
  </body>
</html>
