package org.ncbo.stanford.extractor;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class ExtractionUtil {
    private static transient Logger log = Logger.getLogger(ExtractionUtil.class);
    
	public static void main(String[] args) {
	    try {
	        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	        File output = new File(NcboProperties.getOntologyFileLocation());
	        OWLOntology ontology;
	        if (NcboProperties.getAppendOntologyFile() && output.exists()) {
	            log.info("Loading ontology");
	            ontology = manager.loadOntologyFromPhysicalURI(output.toURI());
	        }
	        else {
	            ontology = manager.createOntology(IRI.create(NcboProperties.getOwlOntologyName()));
	        }
	        NcboProperties.getFilteredOutProperties();
	        OntologyExtractor extractor = new OntologyExtractor(manager, NcboProperties.getBioportalOntologyId(), output);
	        log.info("Started ontology extraction on " + new Date());
	        extractor.extract(ontology, NcboProperties.getBioportalTopConceptId());
	        log.info("Finished ontology extraction on " + new Date());
	        log.info("Saving ontology");
	        manager.saveOntology(ontology, output.toURI());	        
	        log.info("Done on " + new Date());
	    }
	    catch (Throwable t) {
	        log.log(Level.ERROR, t.getMessage(),t);
	    }
	}

}
