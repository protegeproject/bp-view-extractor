package org.ncbo.stanford.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;

public class NcboProperties {
	private static transient Logger log = Logger.getLogger(NcboProperties.class);
	
    public static final String DELAY_PROPERTY               = "bioportal.delay.ms";
    public static final String CALLS_BETWEEN_DELAY_PROPERTY = "bioportal.calls.between.delays";
    public static final String ONTOLOGY_ID_PROPERTY         = "bioportal.ontology.ref";
    public static final String TOP_CONCEPT_ID_PROPERTY      = "bioportal.top.concept.id";
    public static final String FILTERED_OUT_PROPERTIES      = "bioportal.filter.relations";

    public static final String ONTOLOGY_FILE_PROPERTY       = "target.ontology.file";
    public static final String ONTOLOGY_NAME_PROPERTY       = "target.ontology.name";
    public static final String APPEND_PROPERTY              = "target.append.existing.ontology";
    public static final String CLASS_PREFIX 				= "target.class.prefix";
    
    public static final String LOG_COUNT_PROPERTY  = "log.count";
    public static final String SAVE_COUNT_PROPERTY  = "save.count";
    
    //TODO: should not be static
    private static Collection<String> filteredProps;
    
    private static Properties p = new Properties();
    static {
        try {
            p.load(new FileInputStream(new File("local.properties")));
        }
        catch (IOException ioe) {
            log.error("Could not load properties file", ioe);            
        }
    }
    
    public static long getBioportalDelay() {
        String delay = p.getProperty(DELAY_PROPERTY);
        return Long.parseLong(delay);
    }

    public static String getBioportalOntologyId() {
        return p.getProperty(ONTOLOGY_ID_PROPERTY);
    }

    public static String getBioportalTopConceptId() {
        return p.getProperty(TOP_CONCEPT_ID_PROPERTY);
    }

    public static String getOntologyFileLocation() {
        return p.getProperty(ONTOLOGY_FILE_PROPERTY);
    }

    public static String getOwlOntologyName() {
        return p.getProperty(ONTOLOGY_NAME_PROPERTY);
    }

    public static boolean getAppendOntologyFile() {
        String appendPropertyValue = p.getProperty(APPEND_PROPERTY);
        return !(appendPropertyValue == null || !appendPropertyValue.toLowerCase().equals("true"));
    }
    
    public static Collection<String> getFilteredOutProperties() {
    	if (filteredProps == null) {
    		String allProps = p.getProperty(FILTERED_OUT_PROPERTIES);
    		if (allProps == null) { return null;}
    		String[] allPropsArray = allProps.split(",");
    		filteredProps = Arrays.asList(allPropsArray);
    	} 
    	return filteredProps;
    }
    
    public static int getLogCount(int defaultValue) {
    	String c = p.getProperty(LOG_COUNT_PROPERTY);
    	if (c == null) { return defaultValue;}
    	int count = 0;
    	try {
			count = Integer.parseInt(c);
		} catch (Throwable e) { }
		return count;
    }
    
    public static int getSaveCount(int defaultValue) {
    	String c = p.getProperty(SAVE_COUNT_PROPERTY);
    	if (c == null) { return defaultValue;}
    	int count = 0;
    	try {
			count = Integer.parseInt(c);
		} catch (Throwable e) { }
		return count;
    }
    
    public static String getClassPrefix() {
    	String c = p.getProperty(CLASS_PREFIX);
    	return c == null ? "" : c;
    }
}
