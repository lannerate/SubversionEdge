    <div id="aboutModal" class="modal hide fade" style="display: none;">
      <div class="modal-header">
        <a class="close" data-dismiss="modal">&times;</a>
        <h3><g:message code="layout.page.help.about" /></h3>
      </div>
      <div class="modal-body">
        <p>
          <g:set var="version"><g:meta name="app.version"/></g:set>
          <strong><g:message code="layout.page.help.about.release" 
              args="${[version]}"/></strong>
        </p>
        <p>&copy; 2012 <g:message code="layout.page.trademark" /></p>
        <p><g:message code="layout.page.help.about.glyphicons" /></p>
        <p><g:link url="https://ctf.open.collab.net/sf/wiki/do/viewPage/projects.svnedge/wiki/OpenSourceComponents"
              target="_blank"><g:message code="layout.page.help.about.componentList" /></g:link>
        </p>
              
      </div>
      <div class="modal-footer">
        <a href="#" class="btn btn-primary" data-dismiss="modal">Close</a>
      </div>
    </div>
