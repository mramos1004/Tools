/******************************************************************************************************************************
 * Copyright (c) 2011 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams, James Pence, Michael Barbieri.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this
 * distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 * Contributors:
 * Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams, James Pence, Michael Barbieri
 * - initial API and implementation
 *****************************************************************************************************************************/
package org.vivoweb.harvester.translate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;
import com.hp.gloze.Gloze;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Gloze Tranlator This class translates XML into its own natural RDF ontology using the gloze library. Translation into
 * the VIVO ontology is completed using the RDF Translator. TODO Stephen: Identify additional parameters required for
 * translation TODO Stephen: Identify methods to invoke in the gloze library
 * @author Stephen V. Williams swilliams@ctrip.ufl.edu
 */
public class GlozeTranslator {
	/**
	 * the log property for logging errors, information, debugging
	 */
	private static Logger log = LoggerFactory.getLogger(GlozeTranslator.class);
	/**
	 * the file to be translated FIXME Stephen: remove this and use the incoming stream
	 */
	protected File incomingXMLFile;
	/**
	 * The incoming schema to help gloze translate the xml file
	 */
	protected File incomingSchema;
	/**
	 * the uri base for relative nodes in the xml file
	 */
	protected URI uriBase;
	/**
	 * in stream is the stream containing the file (xml) that we are going to translate
	 */
	private InputStream inStream;
	/**
	 * out stream is the stream that the controller will be handling and were we will dump the translation
	 */
	private OutputStream outStream;
	/**
	 * record handler for incoming records
	 */
	protected RecordHandler inStore;
	/**
	 * record handler for storing records
	 */
	protected RecordHandler outStore;
	
	/**
	 * Default Constructor
	 */
	public GlozeTranslator() {
		setURIBase("http://vivoweb.org/glozeTranslation/noURI/");
	}
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error parsing options
	 */
	public GlozeTranslator(String... args) throws IOException {
		this(new ArgList(getParser(), args));
	}
	
	/**
	 * @param argumentList <ul>
	 *        <li><em>inRecordHandler</em> the incoming record handler when record handlers are due</li>
	 *        <li><em>schema</em> the incoming schema for gloze translation</li>
	 *        <li><em>outRecordHandler</em> the out record handler</li>
	 *        <li><em>uriBase</em> required for gloze translation the unset URIBASE used is
	 *        http://vivoweb.org/glozeTranslation/noURI/</li>
	 *        </ul>
	 * @throws IOException error connecting to record handlers
	 */
	public GlozeTranslator(ArgList argumentList) throws IOException {
		// the uri base if not set is http://vivoweb.org/glozeTranslation/noURI/"
		if(argumentList.has("uriBase")) {
			setURIBase(argumentList.get("uriBase"));
		}
		// pull in the translation xsl
		if(argumentList.has("xmlSchema")) {
			setIncomingSchema(new File(argumentList.get("xmlSchema")));
		}
		
		// create record handlers
		this.inStore = RecordHandler.parseConfig(argumentList.get("input"));
		this.outStore = RecordHandler.parseConfig(argumentList.get("output"));
	}
	
	/**
	 * Setter for xmlFile
	 * @param xmlFile the file to translate
	 */
	public void setIncomingXMLFile(File xmlFile) {
		this.incomingXMLFile = xmlFile;
	}
	
	/**
	 * Setter for schema
	 * @param schema the schema that gloze can use, but doesn't need to translate the xml
	 */
	public void setIncomingSchema(File schema) {
		this.incomingSchema = schema;
	}
	
	/**
	 * Setter for uriBase
	 * @param base the base uri to apply to all relative entities
	 */
	public void setURIBase(String base) {
		try {
			this.uriBase = new URI(base);
		} catch(URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	/**
	 * The main translation method for the gloze translation class setups up the necessary conditions for using the
	 * gloze library then executes its translation class
	 */
	public void translateFile() {
		Gloze gl = new Gloze();
		
		Model outputModel = ModelFactory.createDefaultModel();
		
		try {
			// Create a temporary file to use for translation
			File tempFile = new File("temp");
			FileOutputStream tempWrite = new FileOutputStream(tempFile);
			while(true) {
				int bytedata = this.inStream.read();
				if(bytedata == -1) {
					break;
				}
				tempWrite.write(bytedata);
			}
			this.inStream.close();
			tempWrite.close();
			
			gl.xml_to_rdf(tempFile, new File("test"), this.uriBase, outputModel);
			tempFile.delete();
		} catch(Exception e) {
			log.error("", e);
		}
		
		outputModel.write(this.outStream);
	}
	
	/***
	 * 
	 */
	public void execute() {
		if((this.uriBase != null) && (this.inStream != null)) {
			try {
				// create a output stream for writing to the out store
				ByteArrayOutputStream buff = new ByteArrayOutputStream();
				// get from the in record and translate
				for(Record r : this.inStore) {
					if(r.needsProcessed(this.getClass())) {
						this.inStream = new ByteArrayInputStream(r.getData().getBytes());
						this.outStream = buff;
						translateFile();
						buff.flush();
						this.outStore.addRecord(r.getID(), buff.toString(), this.getClass());
						r.setProcessed(this.getClass());
						buff.reset();
					}
				}
				buff.close();
			} catch(Exception e) {
				log.error("", e);
			}
		} else {
			log.error("Invalid Arguments: Gloze Translation requires a URIBase and XMLFile");
			throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	protected static ArgParser getParser() {
		ArgParser parser = new ArgParser("GlozeTranslator");
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("input").withParameter(true, "CONFIG_FILE").setDescription("config file for input record handler").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "CONFIG_FILE").setDescription("config file for output record handler").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('z').setLongOpt("xmlSchema").withParameter(false, "XML_SCHEMA").setDescription("xsl file").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('u').setLongOpt("uriBase").withParameter(false, "URI_BASE").setDescription("uri base").setRequired(true));
		return parser;
	}
	
	/**
	 * Main Method
	 * @param args list of arguments required to execute glozetranslate
	 */
	public static void main(String[] args) {
		try {
			InitLog.initLogger(args, getParser());
			log.info(getParser().getAppName() + ": Start");
			new GlozeTranslator(args).execute();
		} catch(IllegalArgumentException e) {
			System.out.println(getParser().getUsage());
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(getParser().getAppName() + ": End");
		}
	}
	
}
