package org.ncbo.stanford.extractor;

import java.util.List;

import org.ncbo.stanford.bean.concept.ClassBean;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLStringLiteral;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class NcboAnnotationProperty {
    private OWLOntologyManager manager;
    private OWLOntology  ontology;
    private String lookupString;
    
    private OWLAnnotationProperty annotationProperty;
    
    public NcboAnnotationProperty(OWLOntologyManager manager, OWLOntology ontology,
                                  IRI id,  String label, String lookupString) throws OWLOntologyChangeException {
        this.manager = manager;
        this.ontology = ontology;
        this.lookupString   = lookupString;
        annotationProperty   = manager.getOWLDataFactory().getOWLAnnotationProperty(id);
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLAxiom decl = factory.getOWLDeclarationAxiom(annotationProperty);
        manager.addAxiom(ontology, decl);
        OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
                                                                id, factory.getOWLStringLiteral(label));
        manager.addAxiom(ontology, axiom);
    }
    
    @SuppressWarnings("unchecked")
    public void transferFromBioportal(NcboConcept c) throws OWLOntologyChangeException {
        OWLDataFactory factory  = manager.getOWLDataFactory();
        Object annotationValues = c.getBean().getRelations().get(lookupString);
        if (!(annotationValues instanceof List)) {
            return;
        }
        for (Object o2 : (List) annotationValues) {
            if (o2 instanceof String) {
                OWLStringLiteral  annotationValue = factory.getOWLStringLiteral((String) o2);
                OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(annotationProperty,
                                                                        c.getOwlClass().getIRI(), 
                                                                        annotationValue);
                manager.addAxiom(ontology, axiom);
            } else if (o2 instanceof ClassBean) {
            	String id = ((ClassBean)o2).getId();
            	String label = ((ClassBean)o2).getLabel();
            	OWLStringLiteral annotationValue = factory.getOWLStringLiteral(label + " (Id: " + id + ")");
                OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(annotationProperty,
                                                                        c.getOwlClass().getIRI(), 
                                                                        annotationValue);
                manager.addAxiom(ontology, axiom);
            }
        }
    }
}
