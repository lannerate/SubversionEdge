<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main" />
    <title>CollabNet Subversion Edge</title>
  </head>
  <body>

    <content tag="title">
      <g:message code="status.page.certDetails.label" />
    </content>

  <g:form method="post">
      <table class="table table-striped table-bordered table-condensed">
        <tbody>
        <tr>
          <td><strong><g:message code="status.page.hostname.label" /></strong></td>
          <td>${certHostname}</td>
        </tr>
        <tr>
          <td><strong><g:message code="status.page.certValidity.label" /></strong></td>
          <td>${certValidity}</td>
        </tr>
        <tr>
          <td><strong><g:message code="status.page.certIssuer.label" /></strong></td>
          <td>${certIssuer}</td>
        </tr>
        <tr>
          <td><strong><g:message code="status.page.fingerPrint.label" /></strong></td>
          <td>${certFingerPrint}</td>
        </tr>
        </tbody>
      </table>
      <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_SYSTEM">
        <div class="pull-right">
          <g:actionSubmit action="acceptCertificate" value="${message(code:'status.page.validate.button')}" class="btn btn-primary"/>
        </div>
      </g:ifAnyGranted>
      <input type="hidden" name="currentlyAcceptedFingerPrint" value="${certFingerPrint}">
  </g:form>
  </body>
</html>
