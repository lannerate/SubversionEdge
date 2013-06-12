<html>
  <head>
    <title><g:message code="packagesUpdate.page.installUpdatesStatus.title" /></title>

      <meta name="layout" content="main" />
      <script type="text/javascript" src="/csvn/plugins/cometd-0.1.5/dojo/dojo.js"
                djconfig="parseOnLoad: false, isDebug: false"></script>

    <g:set var="restartServer" value="${message(code:'packagesUpdate.page.installUpdatesStatus.restartServer')}" />
    <g:set var="installFinished" value="${message(code:'packagesUpdate.page.installUpdatesStatus.finished')}" />
    <g:set var="serverRestarting" value="${message(code:'packagesUpdate.page.installUpdatesStatus.serverIsRestarting')}" />
    <g:set var="serverRestartingTip" value="${message(code:'packagesUpdate.page.installUpdatesStatus.serverIsRestarting.tip')}" />
    <g:set var="serverRestartingTip2" value="${message(code:'packagesUpdate.page.installUpdatesStatus.serverIsRestarting.tip2')}" />

    <script type="text/javascript">
        /**
         * Author: Marcello de Sales (mdesales@collab.net)
         * Date: May 12, 2010
         */
        dojo.require('dojox.cometd');
        dojo.require("dijit.Dialog");

        /** The timer instance */
        var t;

        /** Defines if timer, used in background requests, has been loaded */
        var timerIsOn = false;

        /** Defines if the process has been finished */
        var hasFinished = false;

        /**
         * Makes an asynchronous non-blocking HTTP POST request to 
         * "packagesUpdate/confirmStart" to confirm the start of the upgrade 
         * process.
         */
        function startUpgrade() {
            var xhrArgs = {
                url: "confirmStart",
                handleAs: "text",
                preventCache: true,
                handle: function(error, ioargs) {
                }
            }
            //Call the asynchronous xhrGet
            var deferred = dojo.xhrPost(xhrArgs);
        }

        /**
         * Function instance that is used to initialize upgrade process. Namely,
         * the dojox.cometd component, as well as the major UI objects for the 
         * initial status.
         */
        var init = function() {
            dojox.cometd.init('/csvn/plugins/cometd-0.1.5/cometd');
            dojox.cometd.subscribe('/csvn-updates/percentages', onMessage);
            dojox.cometd.subscribe('/csvn-updates/status', onMessage);

            dojo.byId('restartButton').disabled = true;
            startUpgrade();
        };
        dojo.addOnLoad(init);

        /**
         * Function instance that is used to finish the upgrade process. It
         * finishes the dojox.cometd components.
         */
        var destroy = function() {
            dojox.cometd.unsubscribe('/csvn-updates/percentages');
            dojox.cometd.unsubscribe('/csvn-updates/status');
            dojox.cometd.disconnect();
        };
        dojo.addOnUnload(destroy);

        /**
         * Callback function for the cometd client that receives the messages
         * from the subscribed channels.
         * @param m is the message object proxied by the cometd server from 
         * one of the subscribed channels.
         * @see init()
         */
        function onMessage(m) {
            if (!hasFinished) {
                var c = m.channel;
                var o = eval('('+m.data+')')
                if (c == "/csvn-updates/percentages") {
                    dojo.byId("progressBar").style.width = "" + o.overallPercentage + "%";
                    if (o.overallPercentage == 100) {
                        hasFinished = true
                        dojo.byId('restartButton').disabled = false;
                        dojo.byId('roller').style.display = 'none';
                        dojo.byId('progressStatus_phase').innerHTML = "${installFinished}"
                        dojo.byId('progressStatus_statusMessage').innerHTML = "";
                        dojo.removeClass("progressBar", "active");
                        destroy()
                    }

                } else {

                    if (o.phase != "") {
                        dojo.byId('progressStatus_phase').innerHTML = o.phase;
                    }
                    if (o.statusMessage != "") {
                        dojo.byId('progressStatus_statusMessage').innerHTML = o.statusMessage;
                        dojo.byId('progressStatus_statusMessage').scrollTop = dojo.byId('progressStatus_statusMessage').scrollHeight;
                    }
                }
            }
        }

        /**
         * Makes an asynchronous non-blocking HTTP GET request to "/csvn" to 
         * confirm the verify if the server is reachable while the server is 
         * restarting. When the server is back, the user is redirected to the
         * login page.
         */
        function serverRestartListener() {
            //The parameters to pass to xhrGet, the url, how to handle it, and the callbacks.
            var xhrArgs = {
                url: "/csvn",
                handleAs: "text",
                preventCache: true,
                handle: function(error, ioargs) {
                    switch (ioargs.xhr.status) {
                    case 200:
                    case 301:
                    case 302:
                        window.location = "/csvn"
                        break;
                    }
                }
            }
            //Call the asynchronous xhrGet
            var deferred = dojo.xhrGet(xhrArgs);
        }

        /**
         * Utility method that waits for the server to restart at every
         * 5 seconds.
         */
        function waitForCsvnServer() {
            serverRestartListener();
            t = setTimeout("waitForCsvnServer();", 5000);
        }

        /**
         * Makes an asynchronous non-blocking HTTP GET request to 
         * "packagesUpdate/restartServer" to request the server to restart.
         */
        function requestServerRestart() {
            var xhrArgs = {
                url: "restartServer",
                handleAs: "text",
                preventCache: true,
                handle: function(error, ioargs) {
                }
            }
            //Call the asynchronous xhrGet
            var deferred = dojo.xhrPost(xhrArgs);
        }

        /**
         * Starts the background listener process, changing UI objects for the
         * transition from when the process finishes to the server restart.
         */
        function startBackgroundListener() {
            dojo.byId('roller').style.display = '';
            dojo.byId('progressStatus_phase').innerHTML = "${serverRestarting}&#133;";
            dojo.byId('progressStatus_statusMessage').innerHTML = "${serverRestartingTip} ${serverRestartingTip2}";
            dojo.byId('progressBar').style.display = 'none';
            dojo.byId('restartButton').disabled = true;
            requestServerRestart();
            if (!timerIsOn) {
                timerIsOn = true;
                waitForCsvnServer();
            }
        }

    </script>

  </head>

    <g:if test="${session.install.equals('addOns')}">
        <g:set var="processType" value="${message(code:'packagesUpdate.page.installUpdatesStatus.installing')}" />
    </g:if>
    <g:else>
        <g:set var="processType" value="${message(code:'packagesUpdate.page.installUpdatesStatus.upgrading')}" />
    </g:else>

  <content tag="title">
    <%= processType %>
  </content>

  <body>

  <div id="progressModal" class="modal">
      <div class="modal-header">
        <h3>${message(code:'packagesUpdate.page.installUpdatesStatus.header')}</h3>
      </div>
      <div class="modal-body">
        <p><img src="/csvn/images/pkgupdates/roller.gif" id="roller" align="middle"/>
          <strong><span id="progressStatus_phase">
            <g:message code="packagesUpdate.page.installUpdatesStatus.initialPhase" />&#133;</span>
          </strong>
        </p>
        <div class="well" id="progressStatus_statusMessage" name="progressStatus_statusMessage"></div>
        <div class="progress progress-info progress-striped active">
          <div id="progressBar" class="bar" style="width: 0%;"></div>
        </div>
      </div>
      <div class="modal-footer">
        <input id="restartButton" type="button" value="${message(code:'packagesUpdate.page.installUpdatesStatus.button.restart')}"
               class="btn btn-primary" onClick="startBackgroundListener();" />
      </div>
    </div>
    <div class="modal-backdrop fade in"></div>
  </body>
</html>
