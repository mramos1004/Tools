/*******************************************************************************
 * Copyright (c) 2010 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which
 * accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html Contributors:
 * Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams - initial API and implementation
 ******************************************************************************/
package org.vivoweb.ingest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vivoweb.ingest.util.args.ArgDef;
import org.vivoweb.ingest.util.args.ArgList;
import org.vivoweb.ingest.util.args.ArgParser;
import org.vivoweb.ingest.util.repo.JenaConnect;
import org.vivoweb.ingest.util.repo.Record;
import org.vivoweb.ingest.util.repo.RecordHandler;
import org.xml.sax.SAXException;

/**
 * Merge multiple rdf files into one
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class Merge {
	/**
	 * Log4J Logger
	 */
	protected static Log log = LogFactory.getLog(Merge.class);
	/**
	 * Input RecordHandler
	 */
	private RecordHandler input;
	/**
	 * Output RecordHandler
	 */
	private RecordHandler output;
	/**
	 * Regex for finding primary records (with a grouping for the subsection to use to find sub-records)
	 */
	private Pattern regex;
	
	/**
	 * Constructor
	 * @param input input recordhandler
	 * @param output output recordhandler
	 * @param regex record regex
	 */
	public Merge(RecordHandler input, RecordHandler output, String regex) {
		this.input = input;
		this.output = output;
		this.regex = Pattern.compile(regex);
	}
	
	/**
	 * Constructor
	 * @param argList arguments
	 */
	public Merge(ArgList argList) {
		try {
			this.input = RecordHandler.parseConfig(argList.get("i"), argList.getProperties("I"));
			this.output = RecordHandler.parseConfig(argList.get("o"), argList.getProperties("O"));
			this.regex = Pattern.compile(argList.get("b"));
		} catch(ParserConfigurationException e) {
			log.error(e.getMessage(), e);
		} catch(SAXException e) {
			log.error(e.getMessage(), e);
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Runs the merge
	 */
	private void execute() {
		try {
			for(Record r : this.input) {
				JenaConnect jc = new JenaConnect();
				Matcher m = this.regex.matcher(r.getID());
				if(m.matches()) {
					for(String id : this.input.find(m.group(1))) {
						jc.loadRDF(new ByteArrayInputStream(this.input.getRecord(id).getData().getBytes()), null);
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					jc.exportRDF(baos);
					baos.flush();
					this.output.addRecord(m.group(1), baos.toString("UTF-8"), this.getClass());
				}
			}
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage(), e);
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("Transfer");
		// Inputs
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("input").withParameter(true, "CONFIG_FILE").setDescription("config file for input jena model").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("inputOverride").withParameterProperties("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of input jena model config using VALUE").setRequired(false));		
		// Outputs
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "CONFIG_FILE").setDescription("config file for output jena model").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('O').setLongOpt("outputOverride").withParameterProperties("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of output jena model config using VALUE").setRequired(false));
		// Params
		parser.addArgument(new ArgDef().setShortOption('b').setLongOpt("baseRegex").withParameter(true, "REGEX").setDescription("match records using REGEX and use the first Group to find sub-records").setRequired(true));
		return parser;
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		log.info(getParser().getAppName()+": Start");
		try {
			new Merge(new ArgList(getParser(), args)).execute();
		} catch(IllegalArgumentException e) {
			log.fatal(e.getMessage());
			System.out.println(getParser().getUsage());
		} catch(IOException e) {
			log.fatal(e.getMessage(), e);
			// System.out.println(getParser().getUsage());
		} catch(Exception e) {
			log.fatal(e.getMessage(), e);
		}
		log.info(getParser().getAppName()+": End");
	}
}