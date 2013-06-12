<head>
    <meta name="layout" content="main" />
    <title><g:message code="job.page.header"/></title>
  <link href="${resource(dir:'css',file:'DT_bootstrap.css')}" rel="stylesheet"/>
</head>


<content tag="title">
  <g:message code="job.page.header"/>
</content>

<g:render template="/server/leftNav"/>

<body>

<g:render template="/job/backgroundJobs" model="['view': 'running','tableName': 'backgroundJobsRunningTable',
 'heading': message(code:'job.page.list.backgroundActive.header'), 'itemList': backgroundJobsRunning, 'maxNumber': 1]" />

<br/><br/>

<g:render template="/job/backgroundJobs" model="['view': 'finished','tableName': 'backgroundJobsFinishedTable',
 'heading': message(code:'job.page.list.backgroundFinished.header'),'itemList': backgroundJobsFinished, 'maxNumber': 1]" />

<br/><br/>

<g:render template="/job/backgroundJobsDatatable" model="['view': 'scheduled', 'tableName': 'backgroundJobsScheduledTable',
 'heading': message(code:'job.page.list.backgroundScheduled.header'), 'itemList': backgroundJobsScheduled,
 'maxNumber': 1]" />

</content>
</body>
