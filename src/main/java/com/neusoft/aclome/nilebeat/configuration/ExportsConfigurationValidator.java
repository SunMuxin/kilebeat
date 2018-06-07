package com.neusoft.aclome.nilebeat.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExportsConfigurationValidator {	
	
	/**  
	    * @Title: isValid  
	    * @Description: add SolrEndpointActor
	    * @param configuration, conf_key
	    * @return ValidationResponse  
	*/
	public ExportsValidationResponse isValidExports(Config config) {

		final Config load = config;
		if (load.isEmpty() || !load.hasPath("exports")) {
			throw new IllegalArgumentException("configuration file not valid");
		}
		
		@SuppressWarnings("unchecked")
		final List<ConfigObject> conf_value = (List<ConfigObject>) load.getObjectList("exports");
		if (conf_value.isEmpty()) {
			throw new IllegalArgumentException("configuration file not valid");
		}
		
		final ExportsValidationResponse response = new ExportsValidationResponse();
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

	public final class ExportsValidationResponse {	
		private final Map<Integer, SingleConfiguration> configs;
		private final Map<Integer, List<String>> errors;

		private ExportsValidationResponse() {
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
			
			final SingleConfiguration build = new SingleConfiguration(c.getString("path"), rules);
									
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
		
		@Getter
		private final SendRules rules;
		
		private SingleConfiguration(String path, SendRules rules) {
			this.path = path;
			this.rules = rules;
		}
		
		public File getPath() {
			return new File(path);
		}

		public SingleConfiguration makeCopy(String path) {
			final SingleConfiguration ret = new SingleConfiguration(path, rules);
			
			return ret;			
		}
	}
				
}
