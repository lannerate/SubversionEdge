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

/*
 * This file provides js functions for "list view" item selection and actions
 */

String.prototype.trim = function () {
    return this.replace(/^\s*/, "").replace(/\s*$/, "");
}

$(function() {
    // add observer to checkboxes for enabling / disabling command buttons
    applyCheckboxObserver();

    // "select all" handler
    $('input.listViewSelectAll').click( function(event) {
        	var table = $(this).parents('table').first();
        	var cboxes = table.find('input.listViewSelectItem');
            // set all the item checkboxes to state of the "select all" checkbox
            var checkedState = $(this).is(':checked');
            cboxes.each(function() {
                if (!$(this).attr('disabled')) {
                    $(this).attr('checked', checkedState)
                }
            })
            updateActionButtons()
        });

    // enable/disable action buttons based on initial page state
    updateActionButtons();
});

/**
 * any buttons created with <g:listViewActionButton> will be enabled/disabled
 * according to their minSelected and maxSelected attributes
 */
function updateActionButtons()  {
    numberItemsSelected = $('input.listViewSelectItem:checked').size()
    $('input.listViewAction').each(function() {
        if ((parseInt($(this).data('minSelected')) > numberItemsSelected) ||
                (parseInt($(this).data('maxSelected')) < numberItemsSelected)) {
            $(this).attr('disabled', true)
        }                
        else {
            $(this).removeAttr('disabled')
        }
    })
}

/**
 * adds click handler to all of the list view select checkboxes on the page
 */
function applyCheckboxObserver() {
    var allItemSelectCheckboxes = $('input.listViewSelectItem');
    allItemSelectCheckboxes.removeAttr("checked");
    applyCheckboxObserverTo(allItemSelectCheckboxes);
}

function applyCheckboxObserverTo(jQueryObject) {
    $(jQueryObject).unbind("click", listViewCheckboxClickHandler);
    $(jQueryObject).click(listViewCheckboxClickHandler);
}

function listViewCheckboxClickHandler() {
    if ($(this).hasClass("radio")) {
        currentId = $(this).attr("id");
        $('input.listViewSelectItem:checked[id!="' + currentId + '"]').removeAttr("checked");
    }
    updateActionButtons();
}

/**
 * Gives a text element focus with the cursor placed after the default value.
 */
function focusWithCursorAtEnd(textElementId) {
    var el = $(textElementId);
    el.focus();
    var value = el.value;
    el.value = '';
    el.value = value;	
}