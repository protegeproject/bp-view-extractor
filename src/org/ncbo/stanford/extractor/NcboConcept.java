package org.ncbo.stanford.extractor;

import org.ncbo.stanford.bean.concept.ClassBean;
import org.semanticweb.owlapi.model.OWLClass;

public interface NcboConcept {
    String getName();
    ClassBean getBean();
    OWLClass getOwlClass();
}
