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
    <h3><g:message code="repository.page.hookCreate.heading"/></h3>
    <g:uploadForm class="form-horizontal" action="uploadHook">
      <fieldset>
      <g:hiddenField name="id" value="${params.id}"/>      
      <div class="control-group required-field">
      <label class="control-label"
          for="fileUpload"><g:message code="repository.page.hookCreate.upload.label"/></label>
      <div class="controls">
        <input type="file" name="fileUpload" id="fileUpload"/>
        <div class="help-block"><g:message code="repository.page.hookCreate.upload.label.tip" /></div>
      </div>
      </fieldset>
      <div class="form-actions">    
        <g:actionSubmit action="uploadHook" class="btn btn-primary" value="${message(code: 'default.button.create.label')}" />
      </div>
    </g:uploadForm>
  </content>
</g:applyLayout>
