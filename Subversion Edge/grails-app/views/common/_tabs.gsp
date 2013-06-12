<ul class="nav nav-${pills ? 'pills' : 'tabs' }">
  <g:each var="tab" in="${tabs}">
    <g:set var="tabLink">
      <g:if test="${tab.href}">
        ${tab.href}
      </g:if>
      <g:elseif test="${tab.action && !tab.controller}">
        ${createLink(action: tab.action)}
      </g:elseif>
      <g:elseif test="${tab.action && tab.controller}">
        ${createLink(action: tab.action, controller: tab.controller)}
      </g:elseif> 
    </g:set>  
    %{-- is this tab currently active? --}%
    <g:set var="active" value="${tab.action == actionName || tab.active}"/>
    <li class="${active ? 'active' : ''}"><a href="${(active || !tabLink) ? '#' : tabLink}"><%= tab.label.replaceAll(" ", "&nbsp;") %></a></li>
  </g:each>
</ul>
