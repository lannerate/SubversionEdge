<head>
  <meta name='layout' content='login' />
  <title>CollabNet Subversion Edge <g:message code="login.page.auth.title" /></title>
</head>
  <content tag="title">
    <g:message code="login.page.auth.title" />
  </content>
<body>

  <div class="content">
    <div class="row">
      <div class="login-form">
        <form action='${postUrl}' method='post' id='loginForm'>
          <fieldset>
            <div class="clearfix">
              <label class="control-label"><g:message code="login.page.auth.username.label"/><br/>
              <input type="text" name="j_username" id="j_username"/></label>
              <g:if test="${isDefaultPassword}">
                <g:javascript>
                $(document).ready(function() {
                  var options = { trigger: 'manual', html: true };
                  options.title = '<g:message code="login.page.auth.initialCredentials"/>';
                  options.content = "<g:message code="login.page.auth.username.label"/>: admin<br/><g:message code="login.page.auth.password.label"/>: admin";
                  $('#j_username').popover(options);
                  $('#j_username').popover('show');
                });
                </g:javascript>
              </g:if>
            </div>
            <div class="clearfix">
              <label class="control-label"><g:message code="login.page.auth.password.label"/><br/>
              <input type="password" name="j_password" id="j_password"/></label>
            </div>
            <input class="btn btn-primary" value="<g:message code="login.page.auth.button.submit" />" type="submit"></input>
          </fieldset>
        </form>
      </div>
    </div>
  </div>
  <script type="text/javascript">
     <!--
         (function(){
         document.forms['loginForm'].elements['j_username'].focus();
         })();
     // -->
   </script>
 </body>
