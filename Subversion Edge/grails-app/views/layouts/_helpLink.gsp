
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                  <g:message code="layout.page.help" /> <b class="caret"></b></a>
                <ul class="dropdown-menu pull-right">
                  <li><a href="${helpUrl}" target="_blank"><g:message code="layout.page.help.current" /></a></li>
                  <li><a href="${helpBaseUrl}/topic/csvn/faq/whatiscollabnetsubversion.html"
                      target="_blank"><g:message code="layout.page.help.contents" /></a></li>
                  <li><a href="${helpBaseUrl}/topic/csvn/releasenotes/csvnedge.html"
                      target="_blank"><g:message code="layout.page.help.releaseNotes" /></a></li>
                  <li class="visible-phone"><a href="${createLink(controller: 'ocn' )}" 
                      target="_top"><g:message code="ocn.main.icon"/></a></li>
                  <li class="divider"></li>
                  <g:set var="isShowWizards" value="${false}"/>
                  <g:each var="wizard" in="${allWizards}">
                      <g:if test="${!wizard.active && !wizard.done}">
                      <g:ifAnyGranted role="${wizard.rolesAsString}">
                        <g:set var="isShowWizards" value="${true}"/>
                      </g:ifAnyGranted>
                      </g:if>
                  </g:each>
                    <g:each var="wizard" in="${allWizards}">
                      <g:if test="${!wizard.done}">
                      <g:ifAnyGranted role="${wizard.rolesAsString}">
                        <li id="wizardHelpMenu${wizard.label}"<g:if test="${wizard.active}"> style="display: none;"</g:if>><g:link controller="${wizard.controller}" action="startWizard">
                        <g:message code="wizard.${wizard.label}.inactiveTitle"/></g:link></li>
                      </g:ifAnyGranted>
                      </g:if>
                    </g:each>                  
                    <li id="wizardHelpMenuDivider" class="divider"<g:if test="${!isShowWizards}"> style="display: none;"</g:if>></li>
                  <li><a data-toggle="modal" href="#aboutModal"><g:message code="layout.page.help.about" /></a></li>
                </ul>
              </li>
