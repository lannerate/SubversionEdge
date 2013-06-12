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

<g:applyLayout name="repoDetail" params="[suppressTabs: true]">
  <content tag="tabContent">
  <g:render template="/common/fileEditor"
      model="[fileName: fileName,
          fileId: fileId,
          fileContent: fileContent,
          saveAction: 'saveHook',
          cancelAction: 'cancelHookEdit',
          ajaxCancelUrl: '/csvn/repo/cancelHookEdit?fileId=' + fileId,
          heading: message(code: 'repository.page.hookEdit.heading', args: [fileName])]" />
  </content>
</g:applyLayout>

