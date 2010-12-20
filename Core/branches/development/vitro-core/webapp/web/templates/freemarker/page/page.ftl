<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#include "doctype.html">

<#include "head.ftl">

<body>
    <div id="wrap" class="container">
        <div id="header">
        
            <#include "identity.ftl">
            
            <#-- Note to UI team: do not change this div without also making the corresponding change in menu.jsp -->
            <div id="navAndSearch" class="block">
                <#include "menu.ftl">  
                <#include "search.ftl">                
            </div> <!-- navAndSearch --> 
            
            <#include "breadcrumbs.ftl">         
        </div> <!-- header --> 

        <hr class="hidden" />

        <div id="contentwrap"> 
            <#if flash?has_content>
                <div id="flash-message">
                    ${flash}
                </div>
            </#if>
            
            <div id="content">                      
                ${body}
            </div> <!-- content -->
        </div> <!-- contentwrap -->
    
        <#include "footer.ftl">
                                      
    </div> <!-- wrap --> 
    
    <#include "scripts.ftl"> 
</body>
</html>

<#-- 
Three ways to add a stylesheet:

A. In theme directory:
${stylesheets.addFromTheme("/css/sample.css")}
${stylesheets.add(themeDir + "/css/sample.css")}

B. Any location
${stylesheets.add("/edit/forms/css/sample.css)"}

To add a script: 

A. In theme directory:
${scripts.addFromTheme("/css/sample.js")}

B. Any location
${scripts("/edit/forms/js/sample.js)"}
-->