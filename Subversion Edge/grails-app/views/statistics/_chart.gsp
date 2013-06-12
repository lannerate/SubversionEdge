<ofchart:resources/>
<g:javascript library="prototype"/>  
<script type="text/javascript">
   var currentGraph = null;

   function setCurrentGraph(graph) {
       this.currentGraph = graph;
   }

   function getCurrentGraph() {
       return this.currentGraph;
   }

   function updateGraph() {
       var ts = document.getElementById("ts").value
       new Ajax.Request(getCurrentGraph(),
                        {asynchronous:true,evalScripts:true,
                         parameters: {timespan: ts, repoId : '${repositoryInstance?.id}' },
                         onSuccess:function(e){uploadChart('chart',
                                               e.responseText)}});
   }

   setCurrentGraph("${createLink(controller: "statistics", action: initialGraph)}");
</script>

<div class="body">
 <div class="dialog">
   <table align="center" width="99%">
     <tbody>
       <tr>
         <td>
           <ofchart:chart name="chart" 
            url="${createLink(action:initialGraph, controller:'statistics', 
            params : [repoId: repositoryInstance?.id, timespan: 1]).encodeAsHTML()}" width="100%"
            height="400"/>
          <br />
          <g:select id="ts" from="${timespanSelect}" 
            optionValue="value" optionKey="key" value="1" 
            onchange="updateGraph();"/>
          <br />
         </td>
       </tr>
     </tbody>
   </table>
 </div>
</div>
