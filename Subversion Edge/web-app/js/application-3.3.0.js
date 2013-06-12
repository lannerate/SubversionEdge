/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// global ajax event listener
$('#spinner').bind("ajaxSend", function(){
    $(this).show();
}).bind("ajaxComplete", function(){
    $(this).hide();
});

/**
 * A class for streaming a log file into a div or other element
 * @param logFileName the logfile to stream or tail
 * @param initialOffset the offset at which to begin streaming (eg, length of already displayed content)
 * @param elementToUpdate the container for the log content
 * @param divElementToScroll the div element to scroll with the updates (could be same as element to update)
 * @param errorMsg text to display if the incremental update reports an error)
 */
function LogStreamer(logFileName, initialOffset, elementToUpdate, divElementToScroll, errorMsg) {

    this.logData = { "log" : {"fileName": logFileName, "startIndex": 0, "endIndex": initialOffset}};
    this.contentElement = elementToUpdate;
    this.scrollingElement = divElementToScroll;
    this.errorMsg = errorMsg;
    this.fetchUpdates = function() {
        var self = this
        $.ajax({url: '/csvn/log/tail', 
                data: {fileName: self.logData.log.fileName, startIndex: self.logData.log.endIndex },
                context: self,
                success: function(data, status, xhr) {
                        logStreamer = this
                        logStreamer.logData = data    
                        appendText = ""
                        if (logStreamer.logData.log.error) {
                            appendText = (logStreamer.errorMsg) ? logStreamer.errorMsg : "\n\n** " + logStreamer.logData.log.error + " **"
                            logStreamer.stop()
                        }
                        else {
                            appendText = logStreamer.logData.log.content
                        }
                        if (appendText) {
                            if ( $.browser.msie ) {
                                var newContent = logStreamer.contentElement.text() + appendText
                                logStreamer.contentElement.html(newContent);
                            }
                            else {
                                var newContent = logStreamer.contentElement.text() + appendText
                                logStreamer.contentElement.text(newContent);
                            }
                        }
                        logStreamer.scrollingElement.prop({ scrollTop: logStreamer.scrollingElement.prop("scrollHeight") });
                    }
                })
    }
    this.periodicUpdater = null;
    this.start = function() {
        var self = this
        this.periodicUpdater = setInterval(function() { self.fetchUpdates() }, 1000);
    }
    this.stop = function() {
        if (this.periodicUpdater) {
            clearInterval(this.periodicUpdater);
        }
    }
}

/**
 * A class for validating that a given token (username, domain name) is available in CollabNet cloud services via ajax
 * @param inputElement the input field to validate (element)
 * @param messageElement the message field in which to indicate result (element)
 * @param ajaxUrl the ajax endpoint for validating availability
 * @param checkingString message when talking to the server
 * @param promptString message for initial state
 */
function CloudTokenAvailabilityChecker(inputElement, messageElement, ajaxUrl, checkingString, promptString) {
    this.inputElement = inputElement
    this.messageElement = messageElement
    this.delayCheckTimer = null
    this.onSuccess = null
    this.onFailure = null
    this.tokenAvailable = false
    this.ajaxUrl = ajaxUrl
    this.checkingString = checkingString
    this.messageElement.html(promptString)
    this.doAjaxRequest = function() {
        var checker = this
        checker.messageElement.html('<img src="/csvn/images/spinner-green.gif" alt="spinner" align="top"/> ' + checker.checkingString)
        checker.ajaxInstance = $.ajax({
                    url: checker.ajaxUrl, 
                    data: {token: checker.inputElement.val() },
                    context: checker,
                    success: function(data, status, xhr) {
                        var checker = this
                        var responseJson = data
                        if (responseJson.result.tokenStatus == 'ok') {
                            checker.messageElement.html('<img src="/csvn/images/ok.png" alt="ok icon" align="top"/> ' + responseJson.result.message)
                            checker.tokenAvailable = true
                            if (checker.onSuccess != null) {
                                checker.onSuccess()
                            }
                        }
                        else {
                            checker.messageElement.html('<img src="/csvn/images/attention.png" alt="problem icon" align="top"/> ' + responseJson.result.message)
                            checker.tokenAvailable = false
                            if (checker.onFailure != null) {
                                checker.onFailure()
                            }
                        }
                    }
                })
    }
    this.keypressHandler = function() {
        clearTimeout(this.delayCheckTimer)
        var self = this
        var checkAvailability = function() { self.doAjaxRequest() }
        this.delayCheckTimer = setTimeout(checkAvailability, 1000)
    }

    
}

/**
 * helper to submit a form, optionally updating the action first
 * @param form
 * @param actionAttr OPTIONAL set the action before submitting
 */
function formSubmit(form, action) {
    if (action) {
        form.attr("action", action)
    }
    form.submit()
} 

function elementExists(jQueryObject) {
	return jQueryObject.length > 0;
}

var tableState = {
    save: function(tableId) { 
    	var key = this.key(tableId);
    	return function(oSettings, oData) {
    		localStorage.setItem(key, JSON.stringify(oData));
    	};
    },

    load: function(tableId) { 
    	var key = this.key(tableId);
    	return function(oSettings) {
    		return JSON.parse(localStorage.getItem(key));
    	};
    },
    
    key: function(tableId) {
    	return 'DataTables_' + tableId + '_' + window.location.pathname;
    }
}

function ajaxIgnoreResponse(getUrl) {
    $.ajax({
      url: getUrl,
      type: "GET",
      success: function(data, textStatus, jqXHR) {
      },
      error: function(jqXHR, textStatus, errorThrown) {
        alert(textStatus + '\n\n' + errorThrown);
      }
    });
}

function stopEvent(e) {
    if (!e) {
    	e = window.event;
    }
    e.cancelBubble = true;
    if (e.stopPropagation) {
    	e.stopPropagation();
    }
}

function selectUsernameNavigationStyle() {
	var mainNavbar = $('#main-navbar');
	if (mainNavbar.length > 0) {
      $('.full-user-menu').show();
      $('.short-user-menu').hide();		
      var x = mainNavbar.width() - $('.brand-img').width() - 
              $('#main-nav').width() - $('#user-nav').width();
      //alert(mainNavbar.width() + ' ' + $('.brand-img').width() + ' ' + $('#main-nav').width() + ' ' + $('#user-nav').width() + ' ' + x);
      if (x < 80) {
          $('.full-user-menu').hide();
          $('.short-user-menu').show();
      } else {
        $('.full-user-menu').show();
        $('.short-user-menu').hide();		
      }
    }
}
$(document).ready(selectUsernameNavigationStyle);
$(window).resize(selectUsernameNavigationStyle);
