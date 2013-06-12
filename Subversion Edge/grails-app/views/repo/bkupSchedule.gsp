<g:applyLayout name="repoDetail">
 <content tag="headSnippet">
   <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
 </content>

 <content tag="tabContent">
    <g:render template="backupScheduleForm"/>
 </content>
 </g:applyLayout>
 <content tag="bottomOfBody">
    <g:javascript library="listView-3.0.0"/>
    <g:javascript library="jquery.dataTables.min"/>
    <g:javascript library="DT_bootstrap"/>
 </content>
    
 
