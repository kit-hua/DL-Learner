package org.dllearner.ray;

import java.io.Serializable;
import java.util.Map;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;

public class SerializedOWLObjectDuplicator extends OWLObjectDuplicator implements Serializable{

	public SerializedOWLObjectDuplicator(OWLDataFactory dataFactory, Map<IRI, IRI> iriReplacementMap,
			Map<OWLLiteral, OWLLiteral> literals) {
		super(dataFactory, iriReplacementMap, literals);
		// TODO Auto-generated constructor stub
	}

	public SerializedOWLObjectDuplicator(OWLDataFactory dataFactory, Map<IRI, IRI> iriReplacementMap) {
		super(dataFactory, iriReplacementMap);
		// TODO Auto-generated constructor stub
	}

	public SerializedOWLObjectDuplicator(OWLDataFactory dataFactory) {
		super(dataFactory);
		// TODO Auto-generated constructor stub
	}

	public SerializedOWLObjectDuplicator(Map<OWLEntity, IRI> entityIRIReplacementMap, OWLDataFactory dataFactory,
			Map<OWLLiteral, OWLLiteral> literals) {
		super(entityIRIReplacementMap, dataFactory, literals);
		// TODO Auto-generated constructor stub
	}

	public SerializedOWLObjectDuplicator(Map<OWLEntity, IRI> entityIRIReplacementMap, OWLDataFactory dataFactory) {
		super(entityIRIReplacementMap, dataFactory);
		// TODO Auto-generated constructor stub
	}

}
