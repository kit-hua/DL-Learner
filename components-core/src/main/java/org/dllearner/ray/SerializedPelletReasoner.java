package org.dllearner.ray;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerConfiguration;

public class SerializedPelletReasoner extends PelletReasoner implements java.io.Serializable{

	public SerializedPelletReasoner(OWLOntology ont, PelletReasonerConfiguration config)
			throws IllegalConfigurationException {
		super(ont, config);
		// TODO Auto-generated constructor stub
	}

}
