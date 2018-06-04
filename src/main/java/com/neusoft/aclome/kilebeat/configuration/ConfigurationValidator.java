package com.neusoft.aclome.kilebeat.configuration;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

import org.apache.commons.beanutils.BeanUtils;

import com.neusoft.aclome.kilebeat.service.Endpoint;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValue;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ConfigurationValidator {	
	
	public ValidationResponse isValidExports(File configuration) {
		return isValid(configuration, "exports");
	}
	
	public ValidationResponse isValidScans(File configuration) {
		return isValid(configuration, "scans");
	}
	
	/**  
	    * @Title: isValid  
	    * @Description: add SolrEndpointActor
	    * @param configuration, conf_key
	    * @return ValidationResponse  
	*/
	public ValidationResponse isValid(File configuration, String conf_key) {
		
		if (!configuration.exists()) {
			throw new IllegalArgumentException("configuration file absent");
		}

		final Config load = ConfigFactory.parseFile(configuration);
		if (load.isEmpty() || !load.hasPath(conf_key)) {
			throw new IllegalArgumentException("configuration file not valid");
		}
		
		@SuppressWarnings("unchecked")
		final List<ConfigObject> conf_value = (List<ConfigObject>) load.getObjectList(conf_key);
		if (conf_value.isEmpty()) {
			throw new IllegalArgumentException("configuration file not valid");
		}
		
		final ValidationResponse response = new ValidationResponse();
		IntStream.range(0, conf_value.size())
			.forEachOrdered(i -> {
				
				final Config eConfig = conf_value.get(i).toConfig();
				LOGGER.debug("{}° => {}", i, eConfig);
				
				/*
				 * validation starts
				 */
				
				boolean hasPath = eConfig.hasPath("path");
				
				if (!hasPath) {
					response.addError(i, String.format("%d element does not contains %s or %s", i, "path", "scan"));
				}
				
				boolean hasHttp = eConfig.hasPath("http");
				boolean hasKafka = eConfig.hasPath("kafka");
				boolean hasSolr = eConfig.hasPath("solr");
				
				if (!hasHttp && !hasKafka && !hasSolr) {
					response.addError(i, String.format("%d element does not contains %s, %s and %s ... one is mandatory", i, "http", "kafka", "solr"));
				} else {
					if (hasHttp) {
						if (!eConfig.hasPath("http.url")) {
							response.addError(i, String.format("%d element does not contains %s", i, "http.url"));
						}
					}

					if (hasKafka) {
						if (!eConfig.hasPath("kafka.queue")) {
							response.addError(i, String.format("%d element does not contains %s", i, "kafka.queue"));
						}					
					}
					
					if (hasSolr) {
						if (!eConfig.hasPath("solr.core")) {
							response.addError(i, String.format("%d element does not contains %s", i, "solr.core"));
						}
						if (!eConfig.hasPath("solr.url")) {
							response.addError(i, String.format("%d element does not contains %s", i, "solr.url"));
						}
					}
				}
				
				boolean hasSendIfMatch = eConfig.hasPath("send-if-match");
				boolean hasSendIfNotMatch = eConfig.hasPath("send-if-not-match");
				
				if (hasSendIfMatch && hasSendIfNotMatch) {
					response.addError(i, String.format("%d element contains both %s and %s ... only one expected", i, "send-if-match", "send-if-not-match"));					
				} else {
					if (hasSendIfMatch) {
						if (! isPattern(eConfig.getString("send-if-match"))) {
							response.addError(i, String.format("%d element on 'send-if-match' contains not valid pattern", i));							
						}
					}
					
					if (hasSendIfNotMatch) {
						if (! isPattern(eConfig.getString("send-if-not-match"))) {
							response.addError(i, String.format("%d element on 'send-if-not-match' contains not valid pattern", i));
						}						
					}					
				}
				
				boolean hasBulk = eConfig.hasPath("bulk");
				
				if (hasBulk) {
					if (!eConfig.hasPath("bulk.size")) {
						response.addError(i, String.format("%d element does not contain mandatory 'bulk.size' property", i));
					}
				}
				
				/*
				 * finally add Configuration 
				 */
				
				if (!response.containsError(i)) {
					response.addConfiguration(i, eConfig);
				}
				
			});
		
		return response;
	}
	
	private boolean isPattern(String regex) {
		try {
			Pattern.compile(regex);
			
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		}		
	}
	
//	private boolean isValid(SingleConfiguration conf) {		
//		final File path = conf.getPath();
//		
//		if (! path.isAbsolute()) {
//			return false;
//		}
//		
//		final String sPath = path.getPath();	
//		final int lastSlash = StringUtils.lastIndexOf(sPath, "/");				
//		
//		final int lastQ = StringUtils.lastIndexOf(sPath, "?");
//		final String[] arrayQ = StringUtils.split(sPath, "?");
//		if (lastQ != -1 && arrayQ.length > 1) {
//			return false;
//		}
//		if (lastQ != -1 && lastQ < lastSlash) {
//			return false;
//		}
//		
//		final int lastA = StringUtils.lastIndexOf(sPath, "*");
//		final String[] arrayA = StringUtils.split(sPath, "*");
//		if (lastA != -1 && arrayA.length > 1) {
//			return false;
//		}		
//		if (lastA != -1 && lastA < lastSlash) {
//			return false;
//		}				
//		
//		return true;
//	}	
	
	// -----------------------------------------------------------------------

	public final class ValidationResponse {	
		private final Map<Integer, SingleConfiguration> configs;
		private final Map<Integer, List<String>> errors;

		private ValidationResponse() {
			this.configs = new HashMap<>();
			this.errors = new HashMap<>();
		}

		private void addError(int i, String e) {
			if (! errors.containsKey(i)) {
				errors.put(i, new ArrayList<>());
			}
			
			errors.get(i).add(e);
		}

		private void addConfiguration(int i, Config c) {
			final SendRules rules = new SendRules();
			
						
			if (c.hasPath("send-if-match")) {
				rules.addMatch(Pattern.compile(c.getString("send-if-match")));
			}
			
			if (c.hasPath("send-if-not-match")) {
				rules.addNotMatch(Pattern.compile(c.getString("send-if-not-match")));
			}			
			
			final Bulk bulk = new Bulk(
				c.hasPath("bulk.size") ? c.getInt("bulk.size") : null,
				c.hasPath("bulk.timeout") ? c.getInt("bulk.timeout") : null
			);
			
			final SingleConfiguration build = new SingleConfiguration(c.getString("path"), rules, bulk);
			
			Arrays.stream(Endpoint.values()).forEach(e -> {		

				final Config config = c.hasPath(e.getConfKey()) ? c.getObject(e.getConfKey()).toConfig() : null;

				//XXX potrebbe diventare if (config != null) {
				//visto che la validazione è fatta a priori!!!
				
				if (config != null && !config.isEmpty()) {
					build.addEndpoint(e.buildEndpoint(config));
				}
			});
									
			configs.put(i, build);
		}	

		public boolean isValid() {
			return errors.isEmpty();
		}	
		
		public ExportsConfiguration getConfig() {
			return new ExportsConfiguration(
				new TreeMap<Integer, SingleConfiguration>(configs)
			);
		}

		private boolean containsError(int i) {
			return errors.containsKey(i);
		}
	}	
	
	// -----------------------------------------------------------------------	
	
	public final class ExportsConfiguration {
		private final SortedMap<Integer, SingleConfiguration> exports;
		
		private ExportsConfiguration(SortedMap<Integer, SingleConfiguration> e) {
			this.exports = e;
		}	
		
		//return sorted by key !!?
		public Collection<SingleConfiguration> getExports() { 			
			return exports.values();
		}
	}	
	
	// -----------------------------------------------------------------------
	
	@ToString
	public final class Bulk {		
		@Getter
		private final Integer size;
		
		@Getter
		private final Integer timeout;
		
		public Bulk(Integer size, Integer timeout) {
			this.size = size;
			this.timeout = timeout;
		}
		
		public boolean isAvailable() {
			return size != null; //timeout is OPTIONAL
		}		
	}
	
	// -----------------------------------------------------------------------
	
	@ToString
	public final class SendRules {		
		private Pattern match;
		private Pattern notMatch;
		
		public void addMatch(Pattern compile) {
			this.match = compile;
		}

		public void addNotMatch(Pattern compile) {
			this.notMatch = compile;
		}
		
		public boolean mustBeSent(String line) {
			if (match != null) {
				return match.matcher(line).find();
			} else if (notMatch != null) {
				return !notMatch.matcher(line).find();
			} else {
				return true;
			}
		}
	}
	
	// -----------------------------------------------------------------------
	
	@ToString
	public final class SingleConfiguration {		
		private final String path;
		private final List<ConfigurationEndpoint> endpoints;
		
		@Getter
		private final SendRules rules;
		
		@Getter
		private Bulk bulk;
		
		private SingleConfiguration(String path, SendRules rules, Bulk bulk) {
			this.path = path;
			this.rules = rules;
			this.bulk = bulk;
			this.endpoints = new ArrayList<>();
		}
		
		private void addEndpoint(ConfigurationEndpoint endpoint) {
			endpoints.add(endpoint);
		}
		
		public List<ConfigurationEndpoint> getEndpoints() {
			return Collections.unmodifiableList(endpoints);
		}
		
		public File getPath() {
			return new File(path);
		}

		public SingleConfiguration makeCopy(String path) {
			final SingleConfiguration ret = new SingleConfiguration(path, rules, bulk);
			
			endpoints.forEach(ep -> {				
				final ConfigurationEndpoint dest = buildFakeEndpoint(ep);
				
				try {
					BeanUtils.copyProperties(dest, ep);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
				
				ret.addEndpoint(dest);
			});
			
			return ret;			
		}
		
		//XXX bad code ... please find out another way to do the same work!!?
		private ConfigurationEndpoint buildFakeEndpoint(ConfigurationEndpoint ep) {
			return Endpoint.valueOf(ep).buildEndpoint(new EmptyConfig());
		}		
	}	
	
	// -----------------------------------------------------------------------
	
	private class EmptyConfig implements Config {
		@Override
		public ConfigObject root() {
			return null;
		}

		@Override
		public ConfigOrigin origin() {
			return null;
		}

		@Override
		public Config withFallback(ConfigMergeable other) {
			return null;
		}

		@Override
		public Config resolve() {
			return null;
		}

		@Override
		public Config resolve(ConfigResolveOptions options) {
			return null;
		}

		@Override
		public boolean isResolved() {
			return false;
		}

		@Override
		public Config resolveWith(Config source) {
			return null;
		}

		@Override
		public Config resolveWith(Config source, ConfigResolveOptions options) {
			return null;
		}

		@Override
		public void checkValid(Config reference, String... restrictToPaths) {
		}

		@Override
		public boolean hasPath(String path) {
			return false;
		}

		@Override
		public boolean hasPathOrNull(String path) {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public Set<Entry<String, ConfigValue>> entrySet() {
			return null;
		}

		@Override
		public boolean getIsNull(String path) {
			return false;
		}

		@Override
		public boolean getBoolean(String path) {
			return false;
		}

		@Override
		public Number getNumber(String path) {
			return null;
		}

		@Override
		public int getInt(String path) {
			return 0;
		}

		@Override
		public long getLong(String path) {
			return 0;
		}

		@Override
		public double getDouble(String path) {
			return 0;
		}

		@Override
		public String getString(String path) {
			return null;
		}

		@Override
		public <T extends Enum<T>> T getEnum(Class<T> enumClass, String path) {
			return null;
		}

		@Override
		public ConfigObject getObject(String path) {
			return null;
		}

		@Override
		public Config getConfig(String path) {
			return null;
		}

		@Override
		public Object getAnyRef(String path) {
			return null;
		}

		@Override
		public ConfigValue getValue(String path) {
			return null;
		}

		@Override
		public Long getBytes(String path) {
			return null;
		}

		@Override
		public ConfigMemorySize getMemorySize(String path) {
			return null;
		}

		@Override
		public Long getMilliseconds(String path) {
			return null;
		}

		@Override
		public Long getNanoseconds(String path) {
			return null;
		}

		@Override
		public long getDuration(String path, TimeUnit unit) {
			return 0;
		}

		@Override
		public Duration getDuration(String path) {
			return null;
		}

		@Override
		public ConfigList getList(String path) {
			return null;
		}

		@Override
		public List<Boolean> getBooleanList(String path) {
			return null;
		}

		@Override
		public List<Number> getNumberList(String path) {
			return null;
		}

		@Override
		public List<Integer> getIntList(String path) {
			return null;
		}

		@Override
		public List<Long> getLongList(String path) {
			return null;
		}

		@Override
		public List<Double> getDoubleList(String path) {
			return null;
		}

		@Override
		public List<String> getStringList(String path) {
			return null;
		}

		@Override
		public <T extends Enum<T>> List<T> getEnumList(Class<T> enumClass, String path) {
			return null;
		}

		@Override
		public List<? extends ConfigObject> getObjectList(String path) {
			return null;
		}

		@Override
		public List<? extends Config> getConfigList(String path) {
			return null;
		}

		@Override
		public List<? extends Object> getAnyRefList(String path) {
			return null;
		}

		@Override
		public List<Long> getBytesList(String path) {
			return null;
		}

		@Override
		public List<ConfigMemorySize> getMemorySizeList(String path) {
			return null;
		}

		@Override
		public List<Long> getMillisecondsList(String path) {
			return null;
		}

		@Override
		public List<Long> getNanosecondsList(String path) {
			return null;
		}

		@Override
		public List<Long> getDurationList(String path, TimeUnit unit) {
			return null;
		}

		@Override
		public List<Duration> getDurationList(String path) {
			return null;
		}

		@Override
		public Config withOnlyPath(String path) {
			return null;
		}

		@Override
		public Config withoutPath(String path) {
			return null;
		}

		@Override
		public Config atPath(String path) {
			return null;
		}

		@Override
		public Config atKey(String key) {
			return null;
		}

		@Override
		public Config withValue(String path, ConfigValue value) {
			return null;
		}		
	}
	
				
}
