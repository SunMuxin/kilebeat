package com.neusoft.aclome.kilebeat.util.solr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.neusoft.aclome.kilebeat.util.Util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class SolrPasteTool {
	
	private static Logger logger = (Logger) LoggerFactory.getLogger(SolrPasteTool.class);
	static {
		logger.setLevel(Level.WARN);
	}
	
	private static final String COMMIT_XML = "<commit/>";
	private static final String Content_Type = "Content-Type";
	private static final String Json_Type = "application/json";
	private static final String XML_Type = "application/xml";
	private static final String Charset = "charset";
	private static final String Defualt_Charset = "UTF-8";
	private static final String Update = "update";
	private static final char net_splite = '/';
	
	private CloseableHttpClient SOLR_CLIEND = null;
	private String SOLR_URL = null;
	
	/**
	 * Constructs an instance for posting data to the specified Solr URL (ie:
	 * "http://localhost:8983/solr/update")
	 */
	public SolrPasteTool(String SOLR_URL, String username, String password) {
		if (SOLR_URL.endsWith(String.valueOf(net_splite))) 
			this.SOLR_URL = SOLR_URL+Update;
		else 
			this.SOLR_URL = SOLR_URL + net_splite + Update;
		this.SOLR_CLIEND = Util.getHttpClient(username, password);
	}
	
	public SolrPasteTool(String SOLR_URL) {
		this(SOLR_URL, null, null);
	}

	/**
	 * Does a simple commit operation TODO,json
	 * 
	 * @throws Exception
	 */
	public void commit() throws Exception {
		postDocs(COMMIT_XML, XML_Type);
	}

	private String postDocs(String docs, String dataType) throws Exception {
		InputStream is = null;
		try {
			byte[] data = docs.getBytes(Defualt_Charset);
			is = new ByteArrayInputStream(data);
			return postData(is, data.length, dataType);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Reads data from the data stream and posts it to solr, writes to the
	 * response to output
	 * 
	 * @throws Exception
	 */
	public String postData(InputStream data, Integer length, String dataType)
			throws Exception {

		HttpURLConnection urlc = null;
		try {
			try {
				urlc = (HttpURLConnection) new URL(SOLR_URL).openConnection();
				try {
					urlc.setRequestMethod("POST");
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				urlc.setDoOutput(true);
				urlc.setDoInput(true);
				urlc.setUseCaches(false);
				urlc.setAllowUserInteraction(false);
				urlc.setRequestProperty("Content-Type", dataType);
				urlc.setRequestProperty("charset", "UTF-8");

				if (null != length)
					urlc.setFixedLengthStreamingMode(length);

			} catch (IOException e) {
				throw new Exception("Connection error (is Solr running at "
						+ SOLR_URL + " ?)", e);

			}

			OutputStream out = null;
			try {
				out = urlc.getOutputStream();
				pipe(data, out);
			} catch (IOException e) {
				throw new Exception("IOException while posting data", e);
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException x) { /* NOOP */
				}
			}

			InputStream in = null;
			try {
				if (HttpURLConnection.HTTP_OK != urlc.getResponseCode()) {
					throw new Exception("Solr returned an error #"
							+ urlc.getResponseCode() + " "
							+ urlc.getResponseMessage());
				}
				// to do ,response
				in = urlc.getInputStream();
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				pipe(in, output);
				return output.toString("UTF-8");
			} catch (IOException e) {
				throw new Exception("IOException while reading response: ", e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException x) { /* NOOP */
				}
			}

		} finally {
			 if (urlc != null)
			 urlc.disconnect();
		}
	}

	/**
	 * Reads data from the data stream and posts it to solr, writes to the
	 * response to output
	 * 
	 * @throws Exception
	 */
	public void postJson(JsonElement json) throws Exception {
				
		try {
	        HttpPost httpPost = new HttpPost(SOLR_URL);
	        
	        httpPost.addHeader(Charset, Defualt_Charset);
	        httpPost.addHeader(Content_Type, Json_Type);
	        
	        StringEntity entity = new StringEntity(json.toString(), Defualt_Charset);
	        entity.setContentEncoding(Defualt_Charset);
	        entity.setContentType(Json_Type);
	        httpPost.setEntity(entity);
	        
	        HttpResponse response = this.SOLR_CLIEND.execute(httpPost);
	        
	        int statusCode = response.getStatusLine().getStatusCode();
	        if(statusCode != HttpStatus.SC_OK){
	        	Util.error("SolrPasteTool", String.valueOf(statusCode));
	        }
	        
	        httpPost.releaseConnection();

		} catch (IOException e) {
			throw new Exception("Connection error (is Solr running at "
					+ this.SOLR_URL + " ?)", e);

		}
	}
	/**
	 * Pipes everything from the source to the dest. If dest is null, then
	 * everything is read fro msource and thrown away.
	 */
	private static void pipe(InputStream source, OutputStream dest)
			throws IOException {
		byte[] buf = new byte[1024];
		int read = 0;
		while ((read = source.read(buf)) >= 0) {
			if (null != dest)
				dest.write(buf, 0, read);
		}
		if (null != dest)
			dest.flush();
	}
	
	public void close() throws IOException {
		this.SOLR_CLIEND.close();
	}
	
}
