package org.ncbo.stanford.extractor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ncbo.stanford.bean.concept.ClassBean;
import org.ncbo.stanford.util.BioportalConcept;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class OntologyExtractor { 
	private static transient Logger log = Logger.getLogger(OntologyExtractor.class);
	
	private Map<String, NcboAnnotationProperty> name2prop = new HashMap<String, NcboAnnotationProperty>();
		    
	private File outputFile;
	
    String ontologyServiceDescriptor;
    OWLOntologyManager manager;
    
    private URI ontologyName;
    private OWLOntology ontology;
    private Set<OWLClass> traversed = new HashSet<OWLClass>();
       
    private static int classesImported = 0;
    
    int logCount; //default 100
    int saveCount; //default 100
    
	public OntologyExtractor(OWLOntologyManager manager,
	                         String ontologyServiceDescriptor, File output) {
	    this.manager = manager;
	    this.ontologyServiceDescriptor = ontologyServiceDescriptor;
	    this.outputFile = output;
	    this.logCount = NcboProperties.getLogCount(100);
	    this.saveCount = NcboProperties.getSaveCount(100);
	}
	
	public synchronized OWLOntology extract(OWLOntology ontology, 
	                                        String termId) throws OWLOntologyCreationException, URISyntaxException, MalformedURLException, OWLOntologyChangeException, UnknownOWLOntologyException, OWLOntologyStorageException {
	    this.ontologyName = ontology.getOntologyID().getOntologyIRI().toURI();
	    this.ontology = ontology;
	    manager.setOntologyFormat(ontology, new RDFXMLOntologyFormat());
	    traversed.clear();
	   	 	    
	    try {
	    	addConcept(new NcboConceptImpl(termId));	
		} catch (Throwable t) {
			log.error("Error at adding concept: " + termId, t);
		}	    
	    
	    OWLOntology ret = ontology;
	    cleanUp();
	    return ret;
	}
	
	private void addConcept(NcboConcept c) throws MalformedURLException, URISyntaxException, OWLOntologyChangeException {
	    if (traversed.contains(c.getOwlClass())) {
	        return;
	    }
	    traversed.add(c.getOwlClass());
	    
	    try {
	    	declare(c);
		    declareAndAttachAnnotations(c);
		    addChildren(c);	
		} catch (Throwable t) {
			log.error("Error at adding concept: " + c.getName(), t);
		}
	    
	    classesImported ++;
	    
	    if (logCount > 0 && classesImported % logCount == 0) {
	    	log.info("Imported " + classesImported + " classes.\t Last imported class: " + 
	    			c.getName() + " \t on " + new Date());
	    }
	    
	    if (saveCount > 0 && classesImported % saveCount == 0) {
	    	long t0 = System.currentTimeMillis();
	    	log.info("Saving ontology (" + classesImported + " classes imported) ... ");
	        try {
				manager.saveOntology(ontology, outputFile.toURI());
			} catch (UnknownOWLOntologyException e) {
				log.error(e.getMessage(), e);
			} catch (OWLOntologyStorageException e) {
				log.error(e.getMessage(), e);
			}
	        log.info("\tin " + (System.currentTimeMillis() - t0)/1000 + " seconds");
	    }
	}
	
	private void declare(NcboConcept c) throws OWLOntologyChangeException {
	    OWLAxiom decl = manager.getOWLDataFactory().getOWLDeclarationAxiom(c.getOwlClass());
	    manager.addAxiom(ontology, decl);
	}
	
	@SuppressWarnings("unchecked")
    private void declareAndAttachAnnotations(NcboConcept c) throws OWLOntologyChangeException, URISyntaxException {
	    attachLabel(c);

	    ClassBean conceptBean = c.getBean();
	    
	    Set<Object> rels = conceptBean.getRelations().keySet();
	    
	    for (Iterator iterator = rels.iterator(); iterator.hasNext();) {
			Object relObj = (Object) iterator.next();
			if (relObj instanceof String) {
				String annotationName = (String) relObj;
				if (!NcboProperties.getFilteredOutProperties().contains(annotationName)) {
					NcboAnnotationProperty prop = getAnnotationProperty(annotationName);				
					prop.transferFromBioportal(c);
				}
			}
		}    
	}
	
	
	private NcboAnnotationProperty getAnnotationProperty(String name) throws OWLOntologyChangeException, URISyntaxException {
		NcboAnnotationProperty prop = name2prop.get(name);
		if (prop == null) {
			prop = new NcboAnnotationProperty(manager, ontology, IRI.create(getUri(getIRIFriendlyName(name))), name, name);
			name2prop.put(name, prop);
		}
		return prop;
	}
	
	private String getIRIFriendlyName(String name) {
		StringBuffer buffer = new StringBuffer();		
		for (int i=0; i< name.length(); i++) {
			char ch = name.charAt(i);
			buffer.append(Character.isJavaIdentifierPart(ch) ? ch : "_"); //TODO: too much, but safe
		}		
		return buffer.toString();
	}
	
	
	private void attachLabel(NcboConcept c) throws OWLOntologyChangeException {
	    OWLDataFactory factory  = manager.getOWLDataFactory();
	    String label = c.getBean().getLabel();
	    OWLAnnotationProperty labelProperty = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());
	    
        OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(labelProperty,
                                                                c.getOwlClass().getIRI(), 
                                                                factory.getOWLStringLiteral(label));
        manager.addAxiom(ontology, axiom);
	}
	

	
	@SuppressWarnings("unchecked")
    private void addChildren(NcboConcept c) throws MalformedURLException, URISyntaxException, OWLOntologyChangeException {
	    OWLDataFactory factory = manager.getOWLDataFactory();
        ClassBean b = c.getBean();
        Object o = b.getRelations().get(ClassBean.SUB_CLASS_PROPERTY);
        if (o == null || !(o instanceof List)) {
            return;
        }
        for (Object o1 : (List) o) {
            if (o1 instanceof ClassBean) {
                NcboConceptImpl subConcept = new NcboConceptImpl(((ClassBean) o1).getId());
                manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(subConcept.getOwlClass(), c.getOwlClass()));
                addConcept(subConcept);
            }
        }
	}
	
    private OWLClass makeClass(String fragment) throws URISyntaxException {
        URI u = getUri(NcboProperties.getClassPrefix() + fragment);
        return manager.getOWLDataFactory().getOWLClass(u);
    }
	
	private URI getUri(String fragment) throws URISyntaxException {
	    return new URI(ontologyName.getScheme(),
	                   ontologyName.getSchemeSpecificPart(),
	                   fragment);
	}
	
	private void cleanUp() {
	    ontologyName = null;
	    ontology  = null;
	    traversed.clear();
	}
	
	class NcboConceptImpl implements NcboConcept {
	    private String name;
	    private ClassBean bean;
	    private OWLClass owlClass;
	    
	    public NcboConceptImpl(String name) throws MalformedURLException, URISyntaxException {
	        this.name    = name;
	        bean         = getClassBeanFromBP();
	        delayForBioportal(NcboProperties.getBioportalDelay());
	        owlClass     = makeClass(name);
	    }

	    public String getName() {
	        return name;
	    }

	    public ClassBean getBean() {
	        return bean;
	    }

	    public OWLClass getOwlClass() {
	        return owlClass;
	    }
	    
	    
	    private ClassBean getClassBeanFromBP() throws MalformedURLException {	    	 		    	
	    	for (int i = 0; i < 10; i++) {		
	    		ClassBean cb = new BioportalConcept().getConceptProperties(new URL(ontologyServiceDescriptor + "/" + name));
	    		if (cb == null) {
	    			log.warn("Failed attempt #" + i + " to retrieve concept " + name + ". Retry.");
	    			delayForBioportal(10000);
	    		} else {
	    			return cb;
	    		}
	    	}	    	
	    	return null;
	    }    
	    
	    
	    private void delayForBioportal(long delay) {
	        if (delay != 0) {
	            try {
                    Thread.sleep(NcboProperties.getBioportalDelay());
                } catch (InterruptedException e) {
                    log.error("Stop prodding me! I need my sleep!", e);                   
                }
	        }
	    }
	}
}
