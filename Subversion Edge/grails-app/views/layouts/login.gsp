<!DOCTYPE html>
<html lang="en">
  <head>
    <meta http-equiv="X-UA-Compatible" content="IE=Edge"/>
    <g:sslRedirect/>
    <title><g:layoutTitle default="CollabNet Subversion Edge Console" /></title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Subversion Edge"/>
    <meta name="author" content="CollabNet"/>

    <g:set var="bootstrapVersion"><g:meta name="vendor.twitter-bootstrap.version"/></g:set>
    <g:set var="svnedgeCssVersion"><g:meta name="app.svnedgeCss.version"/></g:set>
    <g:set var="applicationJsVersion"><g:meta name="app.applicationJs.version"/></g:set>
    <g:set var="jqueryVersion"><g:meta name="vendor.jquery.version"/></g:set>

    <link href="${resource(dir:'css',file:'bootstrap-' + bootstrapVersion + '.css')}" rel="stylesheet"/>
    <link href="${resource(dir:'css',file:'svnedge-' + svnedgeCssVersion + '.css')}" rel="stylesheet"/>                                                                                                                                                                  
    <link href="${resource(dir:'css',file:'bootstrap-responsive-' + bootstrapVersion + '.css')}" rel="stylesheet"/>

    <style type="text/css">
      .container {
      width: 300px;
      }
      
      /* Override some defaults */
      html, body {
        background-color: #eee;
      }

      /* The white background content wrapper */
      .container > .content {
        background-color: #fff;
        padding: 20px;
        margin: 0 -20px;
        -webkit-border-radius: 10px 10px 10px 10px;
           -moz-border-radius: 10px 10px 10px 10px;
                border-radius: 10px 10px 10px 10px;
        -webkit-box-shadow: 0 1px 2px rgba(0,0,0,.15);
           -moz-box-shadow: 0 1px 2px rgba(0,0,0,.15);
                box-shadow: 0 1px 2px rgba(0,0,0,.15);
      }

      .login-form {
        margin-left: 65px;
      }

      legend {
        margin-right: -50px;
        font-weight: bold;
        color: #404040;
      }
    </style>    

    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <link rel="shortcut icon" href="${resource(dir:'images/icons',file:'favicon.ico')}" />
    <g:javascript library="jquery-${jqueryVersion}.min"/>
    <g:javascript library="application-${applicationJsVersion}" />
    <g:layoutHead />

    
  </head>

  <body ${pageProperty(name: 'body.onload', writeEntireProperty: true)}${pageProperty(name: 'body.onunload', writeEntireProperty: true)}>

    <div class="navbar navbar-fixed-top navbar-inverse">
      <div class="navbar-inner">
        <div class="container-fluid">
          <g:link controller="status" class="brand"><img
               class="hidden-phone"
               src="${resource(dir:'images/masthead',file:'logo.png')}"
               alt="${message(code:'layout.page.home') }"/><img
               class="visible-phone"
               src="${resource(dir:'images/masthead',file:'small-logo.png')}"
               alt="${message(code:'layout.page.home') }"/></g:link>
            <ul class="nav pull-right">
              <g:render template="/layouts/helpLink"/>
            </ul>
        </div>
      </div>
    </div>
    <g:render template="/layouts/aboutModal"/>              

    <!-- main content section begin -->
    <div class="container-fluid">    
      <div class="sessionmessages">
            <g:if test="${flash.error}">
                <div class="alert alert-error">${flash.error}</div>
            </g:if>
      </div>
    </div><!-- /container-fluid -->    

      <div id="main" class="container">
        <g:layoutBody />    
      </div>
      <!-- main content section end -->
    <!-- Placed at the end of the document so the pages load faster -->
    <g:javascript library="bootstrap-${bootstrapVersion}"/>
  </body>
</html>
