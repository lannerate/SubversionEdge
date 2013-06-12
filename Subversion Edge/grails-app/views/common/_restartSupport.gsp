%{--
  - CollabNet Subversion Edge
  - Copyright (C) 2012, CollabNet Inc. All rights reserved.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -  
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  --}%
<div id="restartModal" class="modal hide fade" style="display: none;">
  <div class="modal-header">
    <h3>${message(code: 'packagesUpdate.page.installUpdatesStatus.serverIsRestarting')}</h3>
  </div>
  <div class="modal-body">
    ${message(code: 'packagesUpdate.page.installUpdatesStatus.serverIsRestarting.tip')}
    ${message(code: 'packagesUpdate.page.installUpdatesStatus.serverIsRestarting.tip2')}
    <img src="${resource(dir:'images',file:'spinner-gray-bg.gif')}" align="baseline"/>
  </div>
  <div class="modal-footer">
  </div>
</div>
<script type="text/javascript">
  // add restart support for unapplied updates
  $(function() {
    var restartLinkElement = $('#restartLink')
    if (restartLinkElement == null) {
      return
    }
    restartLinkElement.on('click', function(event){
      // show modal
      $("#restartModal").modal("toggle");

      // execute restart
      $.ajax({
        url: '/csvn/status/restartConsole',
        type: "POST",
        success: function(data, textStatus, jqXHR){
          status = data.result.restart
          timeoutId = window.setTimeout('waitForRestart()', 5000);
        },
        error: function(jqXHR, textStatus, errorThrown) {
          $("#restartModal").find("div.modal-body").html("<p class='dialogBody'>${message(code: 'packagesUpdate.page.installUpdatesStatus.serverIsRestarting.failed')}</p>")
        }
      });
    });
  })

  var pingUrl = "/csvn/images/project/project-homeicon.gif";
  var timeoutId

  function waitForRestart() {
    var image = new Image();
    var uniqueUrl = pingUrl + "?s=" + Math.random();
    image.onload = function() {
      if (this.height > 0)  {
        window.location = "/csvn"
      }
      else {
        timeoutId = window.setTimeout('waitForRestart()', 5000);
      }
    }
    image.onerror = function() {
      timeoutId = window.setTimeout('waitForRestart()', 5000);
    }
    image.src = uniqueUrl
  }
</script>

