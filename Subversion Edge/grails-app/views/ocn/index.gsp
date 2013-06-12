<html>
  <head>
      <meta name="layout" content="main" />
  </head>
  <body>

  <g:if test="${ocnContent}">
    <!-- Banner content from OCN begin -->
    <%= ocnContent %>
    <!-- Banner content from OCN end -->  
  </g:if>
  <g:else>
    <!-- Banner content begin -->
    <g:if test="${cloudEnabled}">
      <g:ifAnyGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_SYSTEM,ROLE_ADMIN_USERS">
        <g:render template="bannerAdmin"/>
      </g:ifAnyGranted>
      <g:ifNotGranted role="ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_SYSTEM,ROLE_ADMIN_USERS">
        <g:render template="bannerUser"/>
      </g:ifNotGranted>
    </g:if>
    <g:else>
      <g:render template="bannerAdmin"/>
    </g:else>
    <!-- Banner content end -->
  </g:else>

    <div class="row-fluid">
      <div class="span4">
        <h2><g:message code="ocn.page.subversion.clients" /></h2>
        <p><g:message code="ocn.page.subversion.clients.text" /></p>
        <ul class="nav nav-list">
          <li class="nav-header"><g:message code="ocn.page.ide.heading" /></li>
          <li><a target="_blank"
            href="http://www.open.collab.net/products/desktops/getit.html"><g:message code="ocn.page.eclipse" /></a>
          </li>
          <li><a target="_blank"
            href="http://www.open.collab.net/products/desktops/getit.html"><g:message code="ocn.page.vstudio" /></a></li>
          <li class="nav-header"><g:message code="ocn.page.cli.heading" /></li>
          <li><a target="_blank"
            href="http://www.open.collab.net/downloads/subversion/"><g:message code="ocn.page.windows" /></a>
          </li>
          <li><a target="_blank"
            href="http://www.open.collab.net/downloads/subversion/linux.html"><g:message code="ocn.page.linux" /></a>
          </li>
          <li><a target="_blank"
            href="http://www.open.collab.net/downloads/community/"><g:message code="ocn.page.mac" /></a>
          </li>
          <li><a target="_blank"
            href="http://www.open.collab.net/downloads/subversion/solaris.html"><g:message code="ocn.page.solaris" /></a>
          </li>
        </ul>
      </div>
      <div class="span4">
        <h2><g:message code="ocn.page.subversion.support" /></h2>
        <p><g:message code="ocn.page.subversion.support.text" />
        </p>
        <p>
          <a class="btn" target="_blank"
            href="http://visit.collab.net/SVNsupport.html"><g:message code="ocn.page.subversion.support.action" /> &raquo;</a>
        </p>
        <h2><g:message code="ocn.page.subversion.training" /></h2>
        <p><g:message code="ocn.page.subversion.training.text" />
        </p>
        <p>
          <a class="btn" target="_blank"
            href="http://www.open.collab.net/training/subversion/"><g:message code="ocn.page.subversion.training.action" /> &raquo;</a>
        </p>
        <h2><g:message code="ocn.page.commercial" /></h2>
        <p><g:message code="ocn.page.commercial.text" />
        </p>
        <p>
          <a class="btn" target="_blank"
            href="http://www.open.collab.net/servlets/ContactCollabNet"><g:message code="ocn.page.commercial.action" /> &raquo;</a>
        </p>
      </div>
      <div class="span4">
        <h2><g:message code="ocn.page.community" /></h2>
        <p><g:message code="ocn.page.community.text" /></p>
        <ul class="nav nav-list">
          <li class="nav-header"><g:message code="ocn.page.forum.heading" /></li>
          <li><a target="_blank"
            href="http://www.collab.net/ask/SVNserver"><g:message code="ocn.page.forum.server" /></a></li>
          <li><a target="_blank"
            href="http://www.collab.net/ask/SVNclient"><g:message code="ocn.page.forum.client" /></a></li>
          <li class="nav-header"><g:message code="ocn.page.project.heading" /></li>
          <li><a target="_blank"
            href="http://blogs.collab.net/subversion/"><g:message code="ocn.page.project.blog" /></a></li>
          <li><a target="_blank"
            href="https://ctf.open.collab.net/sf/wiki/do/viewPage/projects.svnedge/wiki/HomePage"><g:message code="ocn.page.project.wiki" /></a>
          </li>
          <li><a target="_blank"
            href="https://ctf.open.collab.net/sf/tracker/do/listTrackers/projects.svnedge/tracker"><g:message code="ocn.page.project.tracker" /></a></li>
          <li><a target="_blank"
            href="https://ctf.open.collab.net/integration/viewvc/viewvc.cgi/?system=exsy1005&root=svnedge"><g:message code="ocn.page.project.code" /></a></li>
        </ul>
      </div>
    </div>

  </body>

  <content tag="bottomOfBody">
    <script>
        $('#myCarousel').carousel()
    </script>
  </content>

</html>
