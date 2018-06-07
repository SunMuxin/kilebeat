package com.neusoft.aclome.nilebeat.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Util {
	
	private static final String charset = "utf-8";
	private static final boolean print_screen = true;
	private static final String root = System.getProperty("user.dir");
	private static final Path log_path = Paths.get(root, "logs", "alertai.out");
	private static final SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void info(String name, String message) {
		String content = String.format("%s [info] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
		try {
			if (print_screen) {
				System.out.println(content);
			} else {
				log_path.getParent().toFile().mkdirs();
				FileWriter fw = new FileWriter(log_path.toFile(), true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(content);
				pw.flush();
				pw.close();
				fw.close();
			}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}
	
	public static void error(String name, String message) {
		// TODO Auto-generated method stub
		String content = String.format("%s [error] %s - %s", 
				date_formatter.format(new Date()), 
				name, 
				message);
		try {
			if (print_screen) {
				System.out.println(content);
			} else {
				log_path.getParent().toFile().mkdirs();
				FileWriter fw = new FileWriter(log_path.toFile(), true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(content);
				pw.flush();
				pw.close();
				fw.close();
			}
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
	}
	
	public static JsonElement copy(JsonElement json) {
		return new JsonParser().parse(json.toString());
	}
	
	public static Object copy(Serializable oldObj) {
			Object obj = null;
			try {
				// Write the object out to a byte array
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
					out.writeObject(oldObj);
					out.flush();
					out.close();
				}
	 
				// Retrieve an input stream from the byte array and read
				// a copy of the object back in.
				ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
				ObjectInputStream in = new ObjectInputStream(bis);
				obj = in.readObject();
			} catch (	IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return obj;
	}
	
	public static CloseableHttpClient getHttpClient() {
		return getHttpClient(null, null);
	}
	
	public static CloseableHttpClient getHttpClient(String username, String passwd) {
		
		CloseableHttpClient client = null;
		
		try {
			SSLContext sslcontext = createIgnoreVerifySSL();
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslcontext, new DefaultHostnameVerifier())).build();
			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			CredentialsProvider provider = new BasicCredentialsProvider();
			if (username!=null && passwd!=null) {
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, passwd);
			    provider.setCredentials(AuthScope.ANY, credentials);
				client = HttpClients.custom().setDefaultCredentialsProvider(provider).setConnectionManager(connManager).build();
			} else {
				client = HttpClients.custom().setConnectionManager(connManager).build();

			}
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return client;
	}
	
	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSLv3");

		// 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
		X509TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		sc.init(null, new TrustManager[] { trustManager }, null);
		return sc;
	}
	
	public static String HttpGET(String url) throws ClientProtocolException, IOException {
		CloseableHttpClient client =  HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
		CloseableHttpResponse resp = client.execute(get);
		
		if (resp.getStatusLine().getStatusCode() != 200)
			return null;
		
		HttpEntity entity = resp.getEntity();
		Scanner sin = new Scanner(EntityUtils.toString(entity));
		entity.getContent();
		
		StringBuffer res = new StringBuffer();
		while(sin.hasNext()) {
			res.append(sin.nextLine());
		}
		
		sin.close();
		resp.close();
		client.close();
		
		return res.toString();
	}
	
	public static String HttpPOST(String url, Map<String, Object> params) throws ClientProtocolException, IOException {
		return HttpPOST(url, params, charset);
	}
	
	public static String HttpPOST(String url, Map<String, Object> params, String charset) throws ClientProtocolException, IOException {
		CloseableHttpClient client =  HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
        StringEntity se = new StringEntity(new Gson().toJson(params), charset);
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        CloseableHttpResponse resp = client.execute(post);	
		if (resp.getStatusLine().getStatusCode() != 200)
			return null;
		
		HttpEntity entity = resp.getEntity();
		Scanner sin = new Scanner(EntityUtils.toString(entity));
		entity.getContent();
		
		StringBuffer res = new StringBuffer();
		while(sin.hasNext()) {
			res.append(sin.nextLine());
		}
		
		sin.close();
		resp.close();
		client.close();
		
		return res.toString();
	}
	
	public static List<File> scanLogger(String configName) {
		File config = new File(configName);
		Path currPath = new File(config.getParent()).toPath();
		return scanLogger(config, currPath);
	}
		
	public static List<File> scanLogger(File config, Path currPath) {

		List<File> files = new ArrayList<File>();
		try {
			for (File file : currPath.toFile().listFiles()) {
				if (file.isFile()) {
					Optional<File> temp_file = related(config, file.toPath());
					if (temp_file.isPresent()) files.add(temp_file.get());

				} else if (file.isDirectory()) {
					files.addAll(scanLogger(config, file.toPath()));
				}
			}
		} catch (NullPointerException e) {
			return files;
		}
		return files;
	}
	
	private static Optional<File> related(File config, final Path path) {
		final File initialResource = config;				
		final String currentName = path.toFile().getName();
		
		final File newSc;
		if (match(initialResource.getName(), currentName)) {
			newSc = path.toFile();
		} else {
			newSc = null;
		}
		
		return Optional.ofNullable(newSc);
	}
	
	//XXX until new idea, we only support '?' and '*' placeholders
	//see https://stackoverflow.com/questions/34514650/wildcard-search-using-replace-function
	private static boolean match(String configName, String fileName) {	
		final String regex = configName.replace("?", ".?").replace("*", ".*?");		
		final Pattern pattern = Pattern.compile(regex);
		
		return pattern.matcher(fileName).matches();
	}
}

