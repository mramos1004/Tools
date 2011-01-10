/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.visualization.freemarker.entitygrantcount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import com.google.gson.Gson;
import com.hp.hpl.jena.query.DataSource;

import edu.cornell.mannlib.vitro.webapp.beans.Portal;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.visualization.freemarker.VisualizationFrameworkConstants;
import edu.cornell.mannlib.vitro.webapp.controller.visualization.freemarker.DataVisualizationController;
import edu.cornell.mannlib.vitro.webapp.visualization.constants.VOConstants;
import edu.cornell.mannlib.vitro.webapp.visualization.exceptions.MalformedQueryParametersException;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.Entity;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.JsonObject;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.valueobjects.SubEntity;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.QueryRunner;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.entitycomparison.EntitySubOrganizationTypesQueryRunner;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.UtilityFunctions;
import edu.cornell.mannlib.vitro.webapp.visualization.freemarker.visutils.VisualizationRequestHandler;


public class EntityGrantCountRequestHandler implements
		VisualizationRequestHandler {

	public static String ENTITY_VIS_MODE;
	public static String SUB_ENTITY_VIS_MODE;	

	
	@Override
	public ResponseValues generateStandardVisualization(
			VitroRequest vitroRequest, Log log, DataSource dataSource)
			throws MalformedQueryParametersException {
		
		String entityURI = vitroRequest
				.getParameter(VisualizationFrameworkConstants.INDIVIDUAL_URI_KEY);

		
		QueryRunner<Entity> queryManager = new EntityGrantCountQueryRunner(
				entityURI, dataSource, log);	
		Entity entity = queryManager.getQueryResult();	
		
		ENTITY_VIS_MODE = vitroRequest
		.getParameter(VisualizationFrameworkConstants.VIS_MODE_KEY);
		setVisModes();
		
//		for(SubEntity se : entity.getSubEntities()){
//			System.out.println("se key -->" + se.getIndividualLabel());
//		}

		QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
				entityURI, dataSource, log);
		
		Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes.getQueryResult();
		
//		for(Map.Entry soTR : subOrganizationTypesResult.entrySet()){
//			System.out.println("soTR key -->" + soTR.getKey());
//		}
//		
//		System.out.println();
		
		return prepareStandaloneResponse(vitroRequest,
				entity,entityURI, subOrganizationTypesResult);
		
	}
	
	@Override
	public Map<String, String> generateDataVisualization(
			VitroRequest vitroRequest, Log log, DataSource dataSource)
			throws MalformedQueryParametersException {

		String entityURI = vitroRequest
				.getParameter(VisualizationFrameworkConstants.INDIVIDUAL_URI_KEY);
		
		ENTITY_VIS_MODE = vitroRequest
		.getParameter(VisualizationFrameworkConstants.VIS_MODE_KEY);
		setVisModes();
		
		QueryRunner<Entity> queryManager = new EntityGrantCountQueryRunner(
				entityURI, dataSource, log);	
		
		Entity entity = queryManager.getQueryResult();
		
		
		QueryRunner<Map<String, Set<String>>> queryManagerForsubOrganisationTypes = new EntitySubOrganizationTypesQueryRunner(
				entityURI, dataSource, log);
		
		Map<String, Set<String>> subOrganizationTypesResult = queryManagerForsubOrganisationTypes.getQueryResult();

		return prepareDataResponse(entity, entity.getSubEntities(),subOrganizationTypesResult);

	}
	
	@Override
	public Object generateAjaxVisualization(VitroRequest vitroRequest, Log log,
			DataSource dataSource) throws MalformedQueryParametersException {
		throw new UnsupportedOperationException("Entity Grant Count does not provide Ajax Response.");
	}
	
	/**
	 * Provides response when json file containing the grant count over the
	 * years is requested.
	 * 
	 * @param entity
	 * @param subentities
	 * @param subOrganizationTypesResult
	 */
	private Map<String, String> prepareDataResponse(Entity entity, Set<SubEntity> subentities,
			Map<String, Set<String>> subOrganizationTypesResult) {

		String entityLabel = entity.getEntityLabel();

		/*
		* To make sure that null/empty records for entity names do not cause any mischief.
		* */
		if (StringUtils.isBlank(entityLabel)) {
			entityLabel = "no-organization";
		}
		
		String outputFileName = UtilityFunctions.slugify(entityLabel)
				+ "_grants-per-year" + ".csv";
		
		
		Map<String, String> fileData = new HashMap<String, String>();
		
		fileData.put(DataVisualizationController.FILE_NAME_KEY, 
					 outputFileName);
		fileData.put(DataVisualizationController.FILE_CONTENT_TYPE_KEY, 
					 "application/octet-stream");
		fileData.put(DataVisualizationController.FILE_CONTENT_KEY, 
				getEntityGrantsPerYearCSVContent(subentities, subOrganizationTypesResult));
		return fileData;
	}
	
	/**
	 * 
	 * @param vreq
	 * @param valueObjectContainer
	 * @return
	 */
	private TemplateResponseValues prepareStandaloneResponse(VitroRequest vreq,
			Entity entity, String entityURI, Map<String, Set<String>> subOrganizationTypesResult) {

        Portal portal = vreq.getPortal();
        String standaloneTemplate = "entityComparisonStandaloneActivator.ftl";
		
        String jsonContent = "";
		jsonContent = writeGrantsOverTimeJSON(entity.getSubEntities(), subOrganizationTypesResult);

		

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("portalBean", portal);
        body.put("title", "Temporal Graph Visualization");
        body.put("organizationURI", entityURI);
        body.put("organizationLabel", entity.getEntityLabel());
        body.put("jsonContent", jsonContent);
        
        return new TemplateResponseValues(standaloneTemplate, body);
        
	}
	
	private void setVisModes() {
		
		if (ENTITY_VIS_MODE.equalsIgnoreCase("DEPARTMENT")) {
			SUB_ENTITY_VIS_MODE = "PERSON";
		}else if (ENTITY_VIS_MODE.equalsIgnoreCase("SCHOOL")) {
			SUB_ENTITY_VIS_MODE = "DEPARTMENT";
		}else {
			SUB_ENTITY_VIS_MODE = "SCHOOL";
		}		
	}
	
	
	/**
	 * function to generate a json file for year <-> grant count mapping
	 * @param subentities
	 * @param subOrganizationTypesResult  
	 */
	private String writeGrantsOverTimeJSON(Set<SubEntity> subentities, Map<String, Set<String>> subOrganizationTypesResult) {

		Gson json = new Gson();
		Set<JsonObject> subEntitiesJson = new HashSet<JsonObject>();

		for (SubEntity subentity : subentities) {
			JsonObject entityJson = new JsonObject(
					subentity.getIndividualLabel());

			List<List<Integer>> yearGrantCount = new ArrayList<List<Integer>>();

			for (Map.Entry<String, Integer> grantEntry : UtilityFunctions
					.getYearToGrantCount(subentity.getGrants())
					.entrySet()) {

				List<Integer> currentGrantYear = new ArrayList<Integer>();
				if (grantEntry.getKey().equals(
						VOConstants.DEFAULT_GRANT_YEAR))
					currentGrantYear.add(-1);
				else
					currentGrantYear.add(Integer.parseInt(grantEntry.getKey()));
				currentGrantYear.add(grantEntry.getValue());
				yearGrantCount.add(currentGrantYear);
			}

			entityJson.setYearToActivityCount(yearGrantCount);
			entityJson.getOrganizationType().addAll(subOrganizationTypesResult.get(entityJson.getLabel()));

			entityJson.setEntityURI(subentity.getIndividualURI());
			setEntityVisMode(entityJson);
			subEntitiesJson.add(entityJson);
		}
		
	//	System.out.println("\nStopWords are "+ EntitySubOrganizationTypesQueryRunner.stopWords.toString() + "\n");
		return json.toJson(subEntitiesJson);

	}
	
	private void setEntityVisMode(JsonObject entityJson) {
		
		if(entityJson.getOrganizationType().contains("Department")){
			entityJson.setVisMode("DEPARTMENT");
		}else if(entityJson.getOrganizationType().contains("School")){
			entityJson.setVisMode("SCHOOL");
		}else{
			entityJson.setVisMode(SUB_ENTITY_VIS_MODE);
		}
		
	}
	
	private String getEntityGrantsPerYearCSVContent(Set<SubEntity> subentities, Map<String, Set<String>> subOrganizationTypesResult) {

		StringBuilder csvFileContent = new StringBuilder();
		
		csvFileContent.append("Entity Name, Grant Count, Entity Type\n");
		
		for(SubEntity subEntity : subentities){
			
			csvFileContent.append(StringEscapeUtils.escapeCsv(subEntity.getIndividualLabel()));
			csvFileContent.append(", ");
			csvFileContent.append(subEntity.getGrants().size());
			csvFileContent.append(", ");
			
			StringBuilder joinedTypes = new StringBuilder();
			
			for(String subOrganizationType : subOrganizationTypesResult.get(subEntity.getIndividualLabel())){
				joinedTypes.append(subOrganizationType + "; ");
			}
			
			csvFileContent.append(StringEscapeUtils.escapeCsv(joinedTypes.toString()));
			csvFileContent.append("\n");
		}	

		return csvFileContent.toString();
	}	
	
	
}