<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Template for displaying paged search results -->

<h2>
    Search results for '${querytext}'
    <#if classGroupName?has_content>limited to type '${classGroupName}'</#if>
    <#if typeName?has_content>limited to type '${typeName}'</#if>
</h2>

<div class="contentsBrowseGroup">

    <#-- Refinement links -->
    <#if classGroupLinks?has_content>
        <div class="searchTOC">
            <span class="jumpText">Show only results of this <b>type</b>:</span>           
            <#list classGroupLinks as link>
                <a href="${link.url}">${link.text}</a>
            </#list>
        </div>
    </#if>

    <#if classLinks?has_content>
        <div class="searchTOC">
            <span class="jumpText">Show only results of this <b>subtype</b>:</span>           
            <#list classLinks as link>
                <a href="${link.url}">${link.text}</a>
            </#list>
        </div>
    </#if>

    <#-- Search results -->
    <ul class="searchhits">
        <#list individuals as individual>
            <li>                        
                <#include "${individual.searchView}">
            </li>
        </#list>
    </ul>
    
    <#-- Paging controls -->
    <#if (pagingLinks?size > 0)>
        <div class="searchpages">
            Pages: 
            <#if prevPage??><a class="prev" href="${prevPage}">Previous</a></#if>
            <#list pagingLinks as link>
                <#if link.url??>
                    <a href="${link.url}">${link.text}</a>
                <#else>
                    <span>${link.text}</span> <#-- no link if current page -->
                </#if>
            </#list>
            <#if nextPage??><a class="next" href="${nextPage}">Next</a></#if>
        </div>
    </#if>

</div> <!-- end contentsBrowseGroup -->

${stylesheets.addFromTheme("/css/search.css")}