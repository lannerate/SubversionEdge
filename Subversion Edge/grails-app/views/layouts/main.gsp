<!DOCTYPE html>
<html lang="en">
  <head>
    <meta http-equiv="X-UA-Compatible" content="IE=Edge"/>
    <g:sslRedirect/>

    <g:if test="${pageProperty(name:'page.title')}">
        <g:set var="pageHeader"><g:pageProperty name="page.title" /></g:set>
    </g:if>    
    <title><g:layoutTitle default="CollabNet Subversion Edge ${pageHeader ?: 'Console'}" /></title>
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
 
    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
      
    <link rel="shortcut icon" href="${resource(dir:'images/icons',file:'favicon.ico')}" />
          
    <!-- jquery lib is often needed before page html is rendered -->
    <g:javascript library="jquery-${jqueryVersion}.min"/>
    <g:javascript library="application-${applicationJsVersion}" />
    <g:layoutHead />

  </head>
  <body ${pageProperty(name: 'body.onload', writeEntireProperty: true)}${pageProperty(name: 'body.onunload', writeEntireProperty: true)}>
    
    <div id="main-navbar" class="navbar navbar-fixed-top navbar-inverse">
      <div class="navbar-inner">
        <div class="container-fluid">
          <g:link controller="status" class="brand"><img
               class="brand-img hidden-phone" width="217" height="40"
               src="${resource(dir:'images/masthead',file:'logo.png')}"
               alt="${message(code:'layout.page.home') }"/><img
               class="brand-img visible-phone" width="160" height="29"
               src="${resource(dir:'images/masthead',file:'small-logo.png')}"
               alt="${message(code:'layout.page.home') }"/></g:link>
            
                    <%--
            Algorithm:
            set buttonMap (controllerName: buttonName)
            set iconMap (controllerName: iconUrl)
            set ordered buttonList (controllerName)
            for button in buttonList:
                set buttonClass = "Button"
                if ${controllerName} == button:
                    add SelectedLeft
                    set buttonClass = "Button Selected"
                add button (taking name from buttonMap, 
                         icon url from iconMap,
                         use buttonClass)
                if ${controllerName} == button:
                    add SelectedRight


             Note that "featureList" is a set of tokens
                in the page model identifying which buttons to show.
                See ApplicationFilters.groovy for the filter
                that creates the active feature list

        --%>

        <g:set var="controllerButtonMap"
               value="${[status: 'admin',
                      repo: 'repo',
                      repoTemplate: 'repo',
                      user: 'user',
                      role: 'user',
                      job: 'admin',
                      userCache: 'userCache',
                      statistics: 'admin',
                      admin: 'admin',
                      server: 'admin',
                      log: 'admin',
                      packagesUpdate: 'admin',
                      setupTeamForge: 'admin',
                      setupReplica: 'admin',
                      setupCloudServices: 'admin',
                      ocn: 'ocn'
                      ]}" />

        <g:set var="buttonNameMap"
               value="${[status: message(code:'status.main.icon'),
                      repo: message(code:'repository.main.icon'),
                      job: message(code:'job.main.icon'),
                      user: message(code:'user.main.icon'),
                      userCache: message(code:'userCache.main.icon'),
                      statistics: message(code:'statistics.main.icon'),
                      admin: message(code:'server.main.icon'),
                      ocn: message(code:'ocn.main.icon')
                      ]}" />

        <%-- activate buttons in this order: 'activeButton' property in model, controllerName, or default (status) --%>
        <g:set var="selectedButton">admin</g:set>
        <g:if test="${controllerButtonMap[activeButton]}">
            <g:set var="selectedButton"
                value="${controllerButtonMap[activeButton]}" />
        </g:if>
        <g:elseif test="${controllerButtonMap[controllerName]}">
            <g:set var="selectedButton" 
                value="${controllerButtonMap[controllerName]}" />
        </g:elseif>
        
        <g:if test="${!hideButtons}">
            <!-- buttons -->
            <ul id="main-nav" class="nav">
        <g:each in="${featureList}">
          <g:set var="isButtonSelected" 
              value="${(controllerButtonMap[controllerName] == controllerButtonMap[it]) || 
              (it == 'status' && selectedButton == 'status')}" />
          <li<g:if test="${isButtonSelected}"> class="active"</g:if>><a href="${createLink(controller: it )}" 
              target="_top">${buttonNameMap[controllerButtonMap[it]]}</a></li>
        </g:each>
        <g:set var="isButtonSelected" 
              value="${(controllerButtonMap[controllerName] == controllerButtonMap['ocn'])}" />
        <li class="hidden-phone<g:if test="${isButtonSelected}"> active</g:if>"><a href="${createLink(controller: 'ocn' )}" 
              target="_top">${buttonNameMap[controllerButtonMap['ocn']]}</a></li>
            </ul>
            <!-- buttons end -->
        </g:if>
        
            <ul id="user-nav" class="nav pull-right">
                <g:isNotLoggedIn>
                  <li><g:link controller="login"><g:message code="layout.page.login" /></g:link>
                </g:isNotLoggedIn>
                <g:isLoggedIn>
                  <li id="loggedInUser" class="full-user-menu">
                    <g:link controller="user" action="showSelf">
                    <g:loggedInUserInfo field="realUserName"/>&nbsp;(<g:loggedInUsername/>)
                    </g:link>
                  </li>
                  <li class="divider-vertical full-user-menu"></li>
                  <li class="full-user-menu"><g:link controller="logout"><g:message code="layout.page.logout"/></g:link></li>
                  <li class="dropdown short-user-menu">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><g:loggedInUsername/>  
                    <b class="caret"></b></a>
                    <ul class="dropdown-menu">
                      <li id="userProfileMenuItem"><g:link controller="user" action="showSelf"><g:message code="layout.page.userProfile"/></g:link></li>
                      <li><g:link controller="logout"><g:message code="layout.page.logout"/></g:link></li>
                    </ul>
                  </li>
                </g:isLoggedIn>
              <li class="divider-vertical visible-desktop"></li>
              <g:render template="/layouts/helpLink"/>
            </ul>
        </div>
      </div>
    </div>
    <g:render template="/layouts/aboutModal"/>
    
    <!-- main content section begin -->
    <div class="container-fluid">    
      <div class="sessionmessages" id="sessionmessages">
        <!-- 
          <div class="alert alert-block">
          <h4 class="alert-heading">Updates Available</h4>
          There are new updates available for <a href="updates.html">download</a>.
          </div>
        -->            
            <g:if test="${flash.wizard_message}">
                <div class="alert alert-success">${flash.wizard_message}</div>
            </g:if>

            <g:if test="${flash.message}">
                <div class="alert alert-success">${flash.message}</div>
            </g:if>
            <g:elseif test="${flash.unfiltered_message}">
                <div class="alert alert-success"><%=flash.unfiltered_message%></div>
            </g:elseif>
            <g:if test="${flash['info']}">
                <div class="alert alert-info">${flash['info']}</div>
            </g:if>
            <g:elseif test="${request['unfiltered_info']}">
                <div class="alert alert-info"><%=flash['unfiltered_info']%></div>
            </g:elseif>
            <g:if test="${flash.warn}">
                <div class="alert">${flash.warn}</div>
            </g:if>
            <g:elseif test="${flash.unfiltered_warn}">
                <div class="alert"><%=flash.unfiltered_warn%></div>
            </g:elseif>
            <g:if test="${flash.error}">
                <div class="alert alert-error">${flash.error}</div>
            </g:if>
            <g:elseif test="${flash.unfiltered_error}">
                <div class="alert alert-error"><%=flash.unfiltered_error%></div>
            </g:elseif>
      </div>
      <div class="requestmessages" id="requestmessages"> 
            <g:if test="${request['message']}">
                <div class="alert alert-success">${request['message']}</div>
            </g:if>
            <g:elseif test="${request['unfiltered_message']}">
                <div class="alert alert-success"><%=request['unfiltered_message']%></div>
            </g:elseif>
            <g:if test="${request['info']}">
                <div class="alert alert-info">${request['info']}</div>
            </g:if>
            <g:elseif test="${request['unfiltered_info']}">
                <div class="alert alert-info"><%=request['unfiltered_info']%></div>
            </g:elseif>
            <g:if test="${request['warn']}">
                <div class="alert">${request['warn']}</div>
            </g:if>
            <g:elseif test="${request['unfiltered_warn']}">
                <div class="alert"><%=request['unfiltered_warn']%></div>
            </g:elseif>
            <g:if test="${request['error']}">
                <div class="alert alert-error">${request['error']}</div>
            </g:if>
            <g:elseif test="${request['unfiltered_error']}">
                <div class="alert alert-error"><%=request['unfiltered_error']%></div>
            </g:elseif>
      </div>
    </div> <!-- /container-fluid -->
 
    <div id="main" class="container-fluid">
      <div class="row-fluid">

        <g:set var="blocks" value="12"/>
        <g:set var="wizard" value="${activeWizard}"/>
        <g:if test="${pageProperty(name:'page.leftMenu')}">
          <g:set var="blocks" value="9"/>
          <!-- *************  LEFT NAV STUFF GOES HERE *********** -->
          <div class="span3">
            <g:if test="${wizard?.active && !wizard.done}">
            <g:ifAnyGranted role="${wizard.rolesAsString}">
              <button id="wizard${wizard.label}CollapseButton" type="button" class="btn btn-inverse" 
                      data-toggle="collapse" data-target="#wizard${wizard.label}">
                 <g:message code="wizard.${wizard.label}.title"/>
                 <span style="font-size: 20px; font-weight: bold; line-height: 18px;">&nbsp;&times;</span>
              </button>

              <div class="wizard collapse in" id="wizard${wizard.label}" style="width: 100%">
                <div class="wizard-inner">
                    <ul class="nav nav-list wizard-sidenav" id="wizardsteps">
                      <g:each status="i" var="step" in="${wizard.steps}">
                      <g:set var="stepLabel"><g:if test="${wizard.ordered}">${i + 1}. </g:if><g:message code="wizard.${wizard.label}.${step.label}.title"/></g:set>
                      <g:set var="isStepActive" value="${step.id == wizard.currentStep.id}"/>
                      <g:if test="${step.done}">
                        <li class="disabled"><a onclick="return false;"><i class="icon-ok"></i>${stepLabel}</a></li>
                      </g:if>
                      <g:else>
                        <li<g:if test="${isStepActive}"> class="active"</g:if>>
                            <g:link controller="${wizard.controller}" action="gotoStep" params="${[label: step.label]}">
                            <g:if test="${isStepActive}">
                              <i class="icon-chevron-down"></i>
                            </g:if>
                            <g:else> 
                              <i class="icon-chevron-right"></i>
                            </g:else>
                            ${stepLabel}</g:link>
                            <g:if test="${isStepActive}">
                              <div class="wizard-content"><g:render template="${step.helper().getContentTemplate(controllerName, actionName)}" /></div>
                            </g:if>
                        </li>
                      </g:else>
                      </g:each>
                    </ul>
                </div>
              </div>
              <div id="closeWizardModal${wizard.label}" class="modal hide fade" style="display: none;">
                <div class="modal-header">
                  <a class="close" data-dismiss="modal">&times;</a>
                  <h3><g:message code="wizard.exitTheWizard"/></h3>
                </div>
                <div class="modal-body">
                  <g:set var="wizardTitle"><g:message code="wizard.${wizard.label}.title"/></g:set>
                  <g:message code="wizard.suspend.modal.body" args="${[wizardTitle]}"/>
                </div>
                <div class="modal-footer">
                  <a href="#" class="btn btn-primary" data-dismiss="modal"><g:message code="default.button.close.label"/></a>
                </div>
              </div>
              
              <g:javascript>
                $('#wizard${wizard.label}').on('hidden', function () {
                  $('#closeWizardModal${wizard.label}').modal('show');                  
                  $('#wizard${wizard.label}CollapseButton').hide();
                  ajaxIgnoreResponse('/csvn/${wizard.controller}/suspendWizard');
                  $('#wizardHelpMenu${wizard.label}').show();
                  $('#wizardHelpMenuDivider').show();
                });
              </g:javascript>
            </g:ifAnyGranted>
            </g:if>
            
            <div class="well sidebar-nav">
              <ul class="nav nav-list">
                <g:pageProperty name="page.leftMenu" />
              </ul>
            </div> <!--/.well -->

            <g:tipSelector>
            <div id="tip" class="well hidden-phone">
              <span class="label label-info">Tip:</span>
              <%=tip%>
            </div> <!--/.well -->
            </g:tipSelector>

          </div> <!--/span3-->
        </g:if>
        <div class="span${blocks}">
          <g:if test="${pageHeader}">
            <div class="page-header"><h1>${pageHeader}</h1></div>
          </g:if>

          <div id="pageContent">
            <g:layoutBody />
          </div>
        </div><!-- /spanX -->
      </div><!-- /row-fluid -->
    </div><!-- /container-fluid #main-->
    <!-- main content section end -->
    <!-- ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <g:javascript library="jquery-tablesorter"/>
    <g:javascript library="bootstrap-${bootstrapVersion}"/>
    <g:javascript library="load-image.min"/>
    <g:javascript library="bootstrap-image-gallery.min"/>
    <g:pageProperty name="page.bottomOfBody" />
  </body>
</html>
