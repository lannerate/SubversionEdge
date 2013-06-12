/*
 * CollabNet Subversion Edge
 * Copyright (C) 2010, CollabNet Inc. All rights reserved.
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

import java.text.DecimalFormatSymbols
import org.springframework.web.servlet.support.RequestContextUtils
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.integration.command.AbstractCommand

class CsvnTagLib {

    def securityService
    def authenticateService
    def userAccountService

    /**
     * this custom tag will format the "size" input (bytes from file.length) into
     * a human-readable string (eg, "17.5 KB")
     */
    def formatFileSize = { attrs ->

        def labels = [' bytes', 'KB', 'MB', 'GB', 'TB']
        def size = attrs.size
        def label = labels.find {
            if (size < 1024) {
                true
            }
            else {
                size /= 1024
                false
            }
        }
        out << "${new java.text.DecimalFormat(attrs.format ?: '0.##').format(size)} $label"
    }

    def sslRedirect = { attrs ->

        // redirect to https if configured to do so
        if (request.scheme == 'http' && Server.getServer().useSslConsole) {
            def port = System.getProperty("jetty.ssl.port", "4434")
            def sslUrl = "https://${request.serverName}${port != "443" ? ":" + port : ""}${request.forwardURI}"
            out << "<meta http-equiv=\"refresh\" content=\"0;url=${sslUrl}\"/>"
        }
    }

    /**
     * this custom tag will create a password field like the standard input tag,
     * but the "value" will be replaced with a same-length placeholder. A hidden
     * field by the same name (+ '_changed') will indicate a user edit
     */
    def passwordFieldWithChangeNotification = { attrs ->

        String fieldName = attrs.name ?: ""
        String fieldValue = attrs.value ?: ""
        String size = attrs.size ?: ""
        def fieldValueLength = fieldValue?.length()
        String pwdToken = (fieldValueLength) ? securityService.generateAlphaNumericPassword(fieldValueLength) : ""

        out << """
        <input type="hidden" name="${fieldName}_changed" id="${fieldName}_changed" value="false"/>
        <input type="password" name="${fieldName}" id="${fieldName}" value="${pwdToken}" size="${size}"/>
        <script type="text/javascript">
        \$('#${fieldName}').change(function(event){
            \$('#${fieldName}_changed').val('true');
        })
       </script>
       """
    }

    /**
     * @return the code name of the command.
     */
    def makeCommandCodeName = { attrs ->
        def className = attrs.className ?: ""
        out << className ? AbstractCommand.makeCodeName(className).trim() : className.trim()
    }

    /**
     * @return the description of a command with a link to the repo name.
     */
    def replicaCommandDescription = { attrs ->
        def repoName = attrs.repoName ?: ""
        def masterUrl = attrs.masterUrl ?: ""
        def commandDescription = attrs.commandDescription ?: ""
        def repoNameLink = "<a target=\"${repoName}\" href=\"${masterUrl}/${repoName}\">${repoName}</a>"
        out << commandDescription.replace(repoName, repoNameLink)
    }

    /**
     * this tag will output the check box for a list view "select all" checkbox
     * @return the select all checkbox
     */
    def listViewSelectAll = { attrs ->
        def name = 'listViewSelectAll'
        if (attrs.name) {
            name = attrs.name
        }
        out << "<input type='checkbox' id='${name}' name='${name}' class='listViewSelectAll'/>"
    }

    /**
     * this tag will output the check box for a list view item row
     * @attr item REQUIRED the item whose "id" attribute will be used for naming this field
     * @attr property OPTIONAL the item field to use in the element name (default: item.id)
     * @attr hidden OPTIONAL if true, this will be a hidden field instead of checkbox
     * @attr disabled OPTIONAL if true and not hidden, this checkbox will be disabled
     * @attr selected OPTIONAL if true and not hidden, this checkbox will be preselected
     * @attr radioStyle OPTIONAL if true, selecting this checkbox will clear others. Unlike radio
     * it can also be unselected
     * @return the item row checkbox
     */
    def listViewSelectItem = { attrs ->
        def prop = attrs.property ?: "id"
        def name = "listViewItem_${attrs.item[prop]}"
        def styleClasses = (attrs.radioStyle) ? "listViewSelectItem radio" : "listViewSelectItem"
        if (attrs.hidden) {
            out << "<input type=\"hidden\" class=\"${styleClasses}\" id=\"${name}\" name=\"${name}\" value=\"on\""
        }
        else {
            out << "<input type=\"checkbox\" class=\"${styleClasses}\" id=\"${name}\" name=\"${name}\""
            if (attrs.disabled) {
                out << " disabled"
            }
            if (attrs.selected) {
                out << " checked"
            }
        }
        if (attrs.onClick) {
            out << " onClick=\"${attrs.onClick}\""
        }
        out << "/>"
    }

    /**
     * This tag will create a "multi-select" action button for use
     * in list views
     * @attr action REQUIRED the controller action to execute
     * @attr primary boolean indicating whether this should be styled as the primary button
     * @attr minSelected the min number of items selected to enable the button
     * @attr maxSelected the max number of items selected to enable the button
     * @attr confirmMessage the confirmation message to display before allowing the action
     * @attr confirmTypeThis if set, the confirmation will require that the user type in this exact text
     * @body the value to display as the button text
     * @return the button html
     */
    def listViewActionButton = { attrs, body ->
        
        def styleClasses = (attrs.primary) ? "btn btn-primary" : "btn"
        def buttonId = "listViewAction_${attrs.action}"
        def modalId = "listViewAction_${attrs.action}_confirm"
        def confirmDialogTextId = "_confirmDialogText_${attrs.action}"
        def label = (attrs.dialog ? (attrs.label ?: "Missing label attribute!!!") : body()).trim()
        def dialogTitle = attrs.dialogTitle ?: "${message(code: 'default.confirmation.title')}"
        
        // html button and modal dialog (if needed) 
        out << "<input id='${buttonId}' type='submit' class='${styleClasses} listViewAction' "
        out << " name='_action_${attrs.action}' "
        out << " value='${label}'"
        if (attrs.confirmMessage || attrs.dialog) {
            out << " data-toggle='modal' data-target='#${modalId}'"
        }
        out << "/>"
        if (attrs.confirmMessage || attrs.dialog) {
            out << """
            <div id="${modalId}" class="modal hide fade" style="display: none">
            <div class="modal-header">
              <a class="close" data-dismiss="modal">&times;</a>
              <h3>${dialogTitle}</h3>
            </div>
            <div class="modal-body">
            """
           
            if (attrs.dialog) {
                out << body()
            }
            else if (attrs.confirmMessage) {
                out << "<p>${attrs.confirmMessage}</p>\n"

                if (attrs.confirmByTypingThis) {
                    out << """
                <p>${message(code: 'default.confirmation.typeThis.prompt')} ${attrs.confirmByTypingThis}</p>
                """
                }

                if (attrs.confirmByTypingThis || attrs.textInput) {
                    out << """
                <p><input id="${confirmDialogTextId}" name="${confirmDialogTextId}"
                type="text" size="20"/></p>
                """
                }
            }
            out << """
            </div>
            <div class="modal-footer">
              <a href="#" class="btn btn-primary ok">${message(code: 'default.confirmation.ok')}</a>
              <a href="#" class="btn cancel" data-dismiss="modal">${message(code: 'default.confirmation.cancel')}</a>
            </div>
            </div>
            """
        }

        // script element for button activations (min/max selection)
        out << '\n<script type="text/javascript">\n'
        out << "  var button = \$('#${buttonId}');\n"
        out << "  button.data('minSelected', ${attrs.minSelected ?: 1});\n"
        out << "  button.data('maxSelected', ${attrs.maxSelected ?: 10000000});\n"
        
        // script for displaying a confirmation dialog 
        if (attrs.confirmMessage || attrs.dialog) {
            out << """
            var submitAction = function() { 
                var button = \$("#${buttonId}");
                var form = button.closest("form");
                form.attr("action", "/csvn/${controllerName}/${attrs.action}");
                form.submit();
            }
            var confirmOk = submitAction;
            """
            
            if (attrs.confirmByTypingThis) {
                out << """
                confirmOk = function() {
                    if (\$("#${modalId}").find("#${confirmDialogTextId}").val() ==
                           "${attrs.confirmByTypingThis}") {
                        submitAction();
                    }   
                    else {
                        // no action
                    } 
                }
                """
            }
            
            if (attrs.textInput) {
                out << """
                prefillSelection = function() {
                    var selection = \$('input.listViewSelectItem:checked');
                    if (selection.size() > 0) {
                        \$("#${confirmDialogTextId}").attr('value',  
                            selection.attr('name').substring('listViewItem_'.length));
                    }
                }
                \$("#${buttonId}").on('click', prefillSelection);
            	"""
            }
            
            out << """\$("#${modalId}").find("a.ok").on("click", confirmOk);"""
        }
        out << "</script>"
    }

    /**
     * This tag replicates the Grails "paginate" control from RenderTagLib,
     * but outputs Bootstrap-compatible html
     * @attr REQUIRED total the count of items to paginate
     */
    def pagination = { attrs ->
        
        if (attrs.total == null) {
            throwTagError("Tag [pagination] is missing required attribute [total]")
        }

        def total = attrs.int('total') ?: 0
        def action = (attrs.action ? attrs.action : (params.action ? params.action : "list"))
        def offset = params.int('offset') ?: 0
        def max = params.int('max')
        def maxsteps = (attrs.int('maxsteps') ?: 10)

        if (!offset) offset = (attrs.int('offset') ?: 0)
        if (!max) max = (attrs.int('max') ?: 10)

        def linkParams = [:]
        if (attrs.params) linkParams.putAll(attrs.params)
        linkParams.offset = offset - max
        linkParams.max = max
        if (params.sort) linkParams.sort = params.sort
        if (params.order) linkParams.order = params.order

        def linkTagAttrs = [action:action]
        if (attrs.controller) {
            linkTagAttrs.controller = attrs.controller
        }
        if (attrs.id!=null) {
            linkTagAttrs.id = attrs.id
        }
        linkTagAttrs.params = linkParams
        
        // determine paging variables
        def steps = maxsteps > 0
        int currentstep = (offset / max) + 1
        int firststep = 1
        int laststep = Math.round(Math.ceil(total / max))
        
        // no pager is needed if these conditions are true, return now
        if (total == 0 || laststep == firststep) {
            return
        }

        // write the html
        out << '<div class="pagination">'
        out << '<ul>'
       
        // previous link
        if (currentstep > firststep) {
            linkParams.offset = offset - max
            out << '<li>'
            out << link(linkTagAttrs.clone()) {
                (attrs.prev ?: '&lt;&lt;')
            }
            out << '</li>'
        }
        else {
            out << '<li class="disabled"><a href="#">' + (attrs.prev ?: '&lt;&lt;') + '</a></li>'
        }
       
        
        // display steps when steps are enabled and laststep is not firststep
        if (steps && laststep > firststep) {

            // determine begin and endstep paging variables
            int beginstep = currentstep - Math.round(maxsteps / 2) + (maxsteps % 2)
            int endstep = currentstep + Math.round(maxsteps / 2) - 1

            if (beginstep < firststep) {
                beginstep = firststep
                endstep = maxsteps
            }
            if (endstep > laststep) {
                beginstep = laststep - maxsteps + 1
                if (beginstep < firststep) {
                    beginstep = firststep
                }
                endstep = laststep
            }

            // display firststep link when beginstep is not firststep
            if (beginstep > firststep) {
                linkParams.offset = 0
                out << "<li>"
                out << link(linkTagAttrs.clone()) {String.valueOf(firststep)}
                out << "</li>"
                out << '<li class="disabled"><a href="#">..</a></li>'
            }

            // display paginate steps
            (beginstep..endstep).each { i ->
                if (currentstep == i) {
                    out << '<li class="active"><a href="#">' + String.valueOf(i) + '</a></li>'
                }
                else {
                    linkParams.offset = (i - 1) * max
                    out << "<li>"
                    out << link(linkTagAttrs.clone()) {String.valueOf(i)}
                    out << "</li>"
                }
            }

            // display laststep link when endstep is not laststep
            if (endstep < laststep) {
                out << '<li class="disabled"><a href="#">..</a></li>'
                linkParams.offset = (laststep -1) * max
                out << "<li>"
                out << link(linkTagAttrs.clone()) {String.valueOf(laststep)}
                out << "</li>"
            }
        }
     
        // next link
        if (currentstep < laststep) {
            out << '<li>'
            linkParams.offset = offset + max
            out << link(linkTagAttrs.clone()) {
                (attrs.next ?: '&gt;&gt;')
            }
            out << '</li>'
        }
        else {
            out << '<li class="disabled"><a href="#">' + (attrs.next ?: '&gt;&gt;') + '</a></li>'
        }
        out <<  '</ul>'
        out <<  '</div>'
    }

    /**
     * Prints the html for a bootstrap control-group holding a text field.
     * Attributes include: bean, field
     *   sizeClass: mini, small, medium, large, xlarge (default), and xxlarge 
     *   required: true or false (default)
     *   integer:  true or false (default)
     *   prefix: Used to find message resources in the event the more specific
     *           attribute is not used. name = <prefix>.<field>.label(.tip) 
     *   label or labelCode: an already l10n value or the resource name
     *   tip or tipCode: an already l10n value or the resource name
     *   maxlength
     */
    def propTextField = { attrs ->
        out << propControlsBody(attrs) {
            def obj = attrs['bean']
            def fieldName = attrs['field']
            attrs.id = attrs.id ?: fieldName
            def sizeClass = attrs['sizeClass'] ?: 'xlarge'
            if (!sizeClass.startsWith('span')) {
                sizeClass = 'input-' + sizeClass
            }
            out << '    <input name="'
            out << fieldName << '" value="'
            def isInt = attrs['integer'] && attrs['integer'] != 'false'
            def value = fieldValue(bean: obj, field: fieldName).encodeAsHTML()
            if (isInt) {
                def locale = RequestContextUtils.getLocale(request)
                def symbols = new DecimalFormatSymbols(locale)
                value = params[fieldName] ?: value
                value = value.replace("${symbols.groupingSeparator}", '')
            }
            out << value << '" type="text"'
            def className = attrs['class'] ? 
                    attrs['class'] + ' ' + sizeClass : sizeClass
            out << ' class="' << className
            if (attrs['maxlength']) {
                out << '" maxlength="' << attrs['maxlength']
            }
            out << '" id="' << attrs.id << '"/>\n'
        }
    }

    /**
     * Prints the html for a bootstrap control-group with the contents of the
     * body inserted into the controls div.
     * Attributes include: bean, field
     *   required: true or false (default)
     *   prefix: Used to find message resources in the event the more specific
     *           attribute is not used. name = <prefix>.<field>.label(.tip) 
     *   label or labelCode: an already l10n value or the resource name
     *   tip or tipCode: an already l10n value or the resource name
     */
    def propControlsBody = { attrs, body ->
        def obj = attrs['bean']
        def fieldName = attrs['field']
        attrs.id = attrs.id ?: fieldName
        def groupId = attrs['groupId']
        
        out << '\n<div'
        if (groupId) {
            out << ' id="' << groupId << '"'
        }
        out << ' class="control-group'
        def isRequired = attrs['required']
        if (isRequired && isRequired != 'false') {
            out << ' required-field'
        }
        
        out << hasErrors(bean: obj, field: fieldName, ' error')
        out << '">\n'

        out << '  <label class="control-label" for="' << fieldName << '">'
        def labelText = label(attrs, fieldName, 'label', 'label', fieldName)
        out << labelText << '</label>\n'
        out << '  <div class="controls">\n'
        out << body()
        
        def tip = label(attrs, fieldName, 'tip', 'label.tip', '')
        if (tip) {
            out << '    <div class="help-block">' << '<span class="help-marker"></span>' << tip
        }
        def skipHtmlEncoding = attrs['skipHtmlEncoding']
        out << hasErrors(bean: obj, field: fieldName) {
            if (!tip) {
                out << '    <div class="help-block">'
            }
            out << '\n      <ul>\n'
            out << eachError(bean: obj, field: fieldName) {
                out << '        <li>'
                if (skipHtmlEncoding) {
                    out << message(error: it)
                } else {
                    out << message(error: it, encodeAs: "HTML")
                }
                out << '</li>\n'
            }
            out << '      </ul>\n'
            if (!tip) {
                out << '    </div>\n'
            }
        }
        if (tip) {
            out << '    </div>\n'
        }
        out << '  </div>\n</div>\n'
    }

    
    private def label(attrs, fieldName, attrName = 'label',  
            suffix = 'label', defaultValue = null) {
        def codePrefix = attrs['prefix']
        def label = attrs[attrName]
        if (!label) {
            def labelCode = attrs[attrName + 'Code']
            if (labelCode) {
                label = message(code: labelCode)
            } else if (codePrefix) {
                label = message(code: 
                        codePrefix + '.' + fieldName + '.' + suffix,
                        'default': defaultValue ?: '')
            } else {
                label = defaultValue
            }
        }
        return label
    }

    /**
     * Prints the html for a bootstrap control-group containing a checkbox
     * Attributes include: bean, field
     *   required: true or false (default)
     *   prefix: Used to find message resources in the event the more specific
     *           attribute is not used. name = <prefix>.<field>.label(.tip) 
     *   label or labelCode: an already l10n value or the resource name
     *   tip or tipCode: an already l10n value or the resource name
     */
    def propCheckBox = { attrs ->
        def obj = attrs['bean']
        def fieldName = attrs['field']
        attrs.id = attrs.id ?: fieldName
        def groupId = attrs['groupId']
        
        out << '\n<div'
        if (groupId) {
            out << ' id="' << groupId << '"'
        }
        out << ' class="control-group'
        def isRequired = attrs['required']
        if (isRequired && isRequired != 'false') {
            out << ' required-field'
        }
        
        out << hasErrors(bean: obj, field: fieldName, ' error')
        out << '">\n'

        out << '  <label class="control-label" for="' << fieldName << '">'
        def codePrefix = attrs['prefix']
        def labelText = label(attrs, fieldName, 'label', 'label', fieldName)
        out << labelText << '</label>\n'
        out << '  <div class="controls">\n'
        
        out << checkBox(name: fieldName, value: obj[fieldName])
        def tip = label(attrs, fieldName, 'tip', 'label.tip') 
        if (tip) {
            out << '    <label class="checkbox inline withFor" for="' 
            out << fieldName << '">' << tip << '</label>\n'
        }
        out << hasErrors(bean: obj, field: fieldName) {
            out << '    <div class="help-block">\n'
            out << '      <ul>\n'
            out << eachError(bean: obj, field: fieldName) {
                out << '        <li>'
                out << message(error: it, encodeAs: "HTML")
                out << '</li>\n'
            }
            out << '      </ul>\n'
            out << '    </div>\n'
        }
        out << '  </div>\n</div>\n'
    }
    
    
    /**
     * This tag determines the help tip to display
     */
    def tipSelector = { attrs, body ->
        try {
            Server server = Server.getServer()
            if (authenticateService.isLoggedIn() &&
                    server.mode == ServerMode.STANDALONE) {
                def idString = loggedInUserInfo(field:'id')
                if (idString) {
                    User user = User.get(idString as int)
                    String code = userAccountService.tipMessageCode(
                            user, params.controller, params.action)
                    out << body(tip: message(code: code))
                }
            }
        } catch (Exception e) {
            log.warn("Exception prevented showing tip content: " + e.message)
            log.debug("Exception prevented showing tip content", e)
        }
    }
}
