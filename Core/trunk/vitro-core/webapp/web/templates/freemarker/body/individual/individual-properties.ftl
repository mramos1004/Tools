<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Template for property listing on individual profile page -->

<#-- RY Just a temporary fix to prevent classgroup heading from being pushed to the right edge of the page. 
Feel free to redo/remove. -->
<div style="clear: both;" />

<#list propertyGroups as group>

    <#-- Get the group name -->
    <#if group.name??>        
        <#if group.name?has_content>
            <#assign groupName = group.name>
        <#else>
            <#-- This is the group for properties not assigned to any group. It has an empty name. -->
            <#assign groupName = "other">
        </#if>
    <#else>
        <#-- If there are no groups, a dummy group has been created with a null name. -->
        <#assign groupName = "">
    </#if> 
    
    <div class="property-group" id="group-${groupName}">
   
        <#-- Display the group heading --> 
        <#if groupName?has_content>
            <h3>${groupName}</h3>
        </#if>
        
        <#-- Now list the properties in the group -->
        <div class="properties">
            <#list group.properties as property>
                <div class="property" id="prop-${property.name}">
                    <#-- Property display name -->
                    <h4>${property.name}</h4>

                    <#-- List the statements for each property -->                    
                    <#if property.type == "data"> <#-- data property -->
                        <#list property.statements as statement>
                            <div class="data-prop-stmt-value">
                                ${statement.value}
                            </div> <!-- end data-prop-stmt-value -->
                        </#list>
                        
                    <#else> <#-- object property -->      
                        <#if property.collatedBySubclass>                             
                            <#include "objectPropertyList-collated.ftl">
                        <#else>
                            <#include "${property.template}">
                        </#if>
                    </#if>                   
                </div> <!-- end property -->               
            </#list>            
        </div> <!-- end properties -->        
    </div> <!-- end property-group -->   
</#list> 