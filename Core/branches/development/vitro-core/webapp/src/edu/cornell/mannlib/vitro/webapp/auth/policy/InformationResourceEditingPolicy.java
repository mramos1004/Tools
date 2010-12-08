/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.auth.policy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.Lock;

import edu.cornell.mannlib.vitro.webapp.auth.identifier.IdentifierBundle;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.PolicyDecision;
import edu.cornell.mannlib.vitro.webapp.auth.policy.ifaces.PolicyIface;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.ifaces.RequestedAction;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.propstmt.AbstractDataPropertyAction;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.propstmt.AbstractObjectPropertyAction;
import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;

/**
 * Allows a self-editor to edit properties on any InformationResource of which
 * he is an author or an editor.
 */
public class InformationResourceEditingPolicy extends BaseSelfEditingPolicy
		implements PolicyIface {
	private static final Log log = LogFactory
			.getLog(InformationResourceEditingPolicy.class);

	private static final String NS_CORE = "http://vivoweb.org/ontology/core#";
	private static final String URI_INFORMATION_RESOURCE_TYPE = NS_CORE
			+ "InformationResource";
	private static final String URI_EDITOR_PROPERTY = "http://purl.org/ontology/bibo/editor";
	private static final String URI_IN_AUTHORSHIP_PROPERTY = NS_CORE
			+ "informationResourceInAuthorship";
	private static final String URI_LINKED_AUTHOR_PROPERTY = NS_CORE
			+ "linkedAuthor";

	private final OntModel model;
	private final AdministrativeUriRestrictor restrictor;

	public InformationResourceEditingPolicy(OntModel model,
			AdministrativeUriRestrictor restrictor) {
		this.model = model;
		this.restrictor = restrictor;
	}

	@Override
	public PolicyDecision isAuthorized(IdentifierBundle whoToAuth,
			RequestedAction whatToAuth) {
		if (whoToAuth == null) {
			return inconclusiveDecision("whoToAuth was null");
		}
		if (whatToAuth == null) {
			return inconclusiveDecision("whatToAuth was null");
		}

		List<String> userUris = getUrisOfSelfEditor(whoToAuth);

		if (userUris.isEmpty()) {
			return inconclusiveDecision("Not self-editing.");
		}

		if (whatToAuth instanceof AbstractObjectPropertyAction) {
			return isAuthorizedForObjectPropertyAction(userUris,
					(AbstractObjectPropertyAction) whatToAuth);
		}

		if (whatToAuth instanceof AbstractDataPropertyAction) {
			return isAuthorizedForDataPropertyAction(userUris,
					(AbstractDataPropertyAction) whatToAuth);
		}

		return inconclusiveDecision("Does not authorize "
				+ whatToAuth.getClass().getSimpleName() + " actions");
	}

	/**
	 * The user can edit a data property if it is not restricted and if it is
	 * about an information resource which he authored or edited.
	 */
	private PolicyDecision isAuthorizedForDataPropertyAction(
			List<String> userUris, AbstractDataPropertyAction action) {
		String subject = action.getSubjectUri();
		String predicate = action.getPredicateUri();

		if (!restrictor.canModifyResource(subject)) {
			return cantModifyResource(subject);
		}
		if (!restrictor.canModifyPredicate(predicate)) {
			return cantModifyPredicate(predicate);
		}

		if (isInformationResource(subject)) {
			if (anyUrisInCommon(userUris, getUrisOfEditors(subject))) {
				return authorizedSubjectEditor();
			}
			if (anyUrisInCommon(userUris, getUrisOfAuthors(subject))) {
				return authorizedSubjectAuthor();
			}
		}

		return userNotAuthorizedToStatement();
	}

	/**
	 * The user can edit an object property if it is not restricted and if it is
	 * about an information resource which he authored or edited.
	 */
	private PolicyDecision isAuthorizedForObjectPropertyAction(
			List<String> userUris, AbstractObjectPropertyAction action) {
		String subject = action.getUriOfSubject();
		String predicate = action.getUriOfPredicate();
		String object = action.getUriOfObject();

		if (!restrictor.canModifyResource(subject)) {
			return cantModifyResource(subject);
		}
		if (!restrictor.canModifyPredicate(predicate)) {
			return cantModifyPredicate(predicate);
		}
		if (!restrictor.canModifyResource(object)) {
			return cantModifyResource(object);
		}

		if (isInformationResource(subject)) {
			if (anyUrisInCommon(userUris, getUrisOfEditors(subject))) {
				return authorizedSubjectEditor();
			}
			if (anyUrisInCommon(userUris, getUrisOfAuthors(subject))) {
				return authorizedSubjectAuthor();
			}
		}

		if (isInformationResource(object)) {
			if (anyUrisInCommon(userUris, getUrisOfEditors(object))) {
				return authorizedObjectEditor();
			}
			if (anyUrisInCommon(userUris, getUrisOfAuthors(object))) {
				return authorizedObjectAuthor();
			}
		}

		return userNotAuthorizedToStatement();
	}

	private boolean isInformationResource(String uri) {
		Selector selector = createSelector(uri, VitroVocabulary.RDF_TYPE,
				URI_INFORMATION_RESOURCE_TYPE);

		StmtIterator stmts = null;
		model.enterCriticalSection(Lock.READ);
		try {
			stmts = model.listStatements(selector);
			if (stmts.hasNext()) {
				return true;
			} else {
				return false;
			}
		} finally {
			if (stmts != null) {
				stmts.close();
			}
			model.leaveCriticalSection();
		}
	}

	private List<String> getUrisOfEditors(String infoResourceUri) {
		List<String> list = new ArrayList<String>();

		Selector selector = createSelector(infoResourceUri,
				URI_EDITOR_PROPERTY, null);

		StmtIterator stmts = null;
		model.enterCriticalSection(Lock.READ);
		try {
			stmts = model.listStatements(selector);
			while (stmts.hasNext()) {
				list.add(stmts.next().getObject().toString());
			}
			return list;
		} finally {
			if (stmts != null) {
				stmts.close();
			}
			model.leaveCriticalSection();
		}
	}

	private List<String> getUrisOfAuthors(String infoResourceUri) {
		List<String> list = new ArrayList<String>();

		Selector selector = createSelector(infoResourceUri,
				URI_IN_AUTHORSHIP_PROPERTY, null);

		StmtIterator stmts = null;

		model.enterCriticalSection(Lock.READ);
		try {
			stmts = model.listStatements(selector);
			while (stmts.hasNext()) {
				RDFNode objectNode = stmts.next().getObject();
				if (objectNode.isResource()) {
					log.debug("found authorship for '" + infoResourceUri
							+ "': " + objectNode);
					list.addAll(getUrisOfAuthors(objectNode.asResource()));
				}
			}
			log.debug("uris of authors for '" + infoResourceUri + "': " + list);
			return list;
		} finally {
			if (stmts != null) {
				stmts.close();
			}
			model.leaveCriticalSection();
		}
	}

	/** Note that we must already be in a critical section! */
	private List<String> getUrisOfAuthors(Resource authorship) {
		List<String> list = new ArrayList<String>();

		Selector selector = createSelector(authorship,
				URI_LINKED_AUTHOR_PROPERTY, null);

		StmtIterator stmts = null;

		try {
			stmts = model.listStatements(selector);
			while (stmts.hasNext()) {
				list.add(stmts.next().getObject().toString());
			}
			return list;
		} finally {
			if (stmts != null) {
				stmts.close();
			}
		}
	}

	private Selector createSelector(String subjectUri, String predicateUri,
			String objectUri) {
		Resource subject = (subjectUri == null) ? null : model
				.getResource(subjectUri);
		return createSelector(subject, predicateUri, objectUri);
	}

	private Selector createSelector(Resource subject, String predicateUri,
			String objectUri) {
		Property predicate = (predicateUri == null) ? null : model
				.getProperty(predicateUri);
		RDFNode object = (objectUri == null) ? null : model
				.getResource(objectUri);
		return new SimpleSelector(subject, predicate, object);
	}

	private boolean anyUrisInCommon(List<String> userUris,
			List<String> editorsOrAuthors) {
		for (String userUri : userUris) {
			if (editorsOrAuthors.contains(userUri)) {
				return true;
			}
		}
		return false;
	}

	private PolicyDecision authorizedSubjectEditor() {
		return authorizedDecision("User is editor of the subject of the statement");
	}

	private PolicyDecision authorizedObjectEditor() {
		return authorizedDecision("User is editor of the object of the statement");
	}

	private PolicyDecision authorizedSubjectAuthor() {
		return authorizedDecision("User is author of the subject of the statement");
	}

	private PolicyDecision authorizedObjectAuthor() {
		return authorizedDecision("User is author of the object of the statement");
	}

	/**
	 * TODO
	 * 
	 * <pre>
	 * We don't need to do resource operations.
	 * 
	 * We can do data or object property operations 
	 *    if not restricted
	 *    if the subject or object is an information resource
	 *    if that information resource has an author or editor who is an active self-editor.
	 * </pre>
	 */

	/**
	 * TODO
	 * 
	 * <pre>
	 * If the request is an object property operation
	 * 
	 * Check restrictions. If restricted, we are done.
	 * Get the URIs of self-editors identifiers. If none, we are done.
	 * Get the list of editors and authors for this document. Is 
	 * Get the list of information resources that these self-editors author or edit.
	 * If subject or object is in that set, approve.
	 * 
	 * If the request is a data property operations, same except there is no object.
	 * </pre>
	 */

}
