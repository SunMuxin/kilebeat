package com.neusoft.aclome.nilebeat.util.solr;

import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neusoft.aclome.nilebeat.util.Util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SolrWriter{
	
	private static Logger logger = (Logger) LoggerFactory.getLogger(SolrWriter.class);
	static {
		logger.setLevel(Level.WARN);
	}
	
	private final String SOLR_URL;
	private final SolrPasteTool SOLR_PASTE_TOOL;
	private final int rows;
	private boolean status = false;
	private JsonArray docs = new JsonArray();
	
	
	public String getSOLR_URL() {
		return SOLR_URL;
	}
	
	public SolrWriter(String SOLR_URL, String username, String password, Integer rows) {
		if (SOLR_URL == null) {
			throw new IllegalArgumentException(
					"Parameter fileName can not be null.");
		}

		this.rows = rows;
		this.SOLR_URL = SOLR_URL;
		this.SOLR_PASTE_TOOL = new SolrPasteTool(SOLR_URL, username, password);
		this.status = true;
	}
	
	public SolrWriter(String SOLR_URL, String username, String password) {
		this(SOLR_URL, username, password, 1000);
	}
	
	public SolrWriter(String SOLR_URL) {
		this(SOLR_URL, null, null, 1000);
	}
	
	public void write(JsonObject doc) {
		docs.add(Util.copy(doc));
		if (docs.size() > this.rows) {
			try {
				flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Util.info("solr write", e.getMessage());
			}
		}
	}

	public synchronized void flush() throws Exception {
		if ( docs.size()>0 && this.status ) {
			this.SOLR_PASTE_TOOL.postJson(docs);
			this.SOLR_PASTE_TOOL.commit();
			docs = new JsonArray();
		}
	}

	public synchronized void close() throws Exception {
		// TODO Auto-generated method stub
		this.flush();
		this.SOLR_PASTE_TOOL.close();
		this.status = false;
	}
	
}
