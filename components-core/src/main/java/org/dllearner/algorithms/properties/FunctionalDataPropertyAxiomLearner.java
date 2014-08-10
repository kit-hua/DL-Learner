/**
 * Copyright (C) 2007-2011, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dllearner.algorithms.properties;

import java.util.List;

import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.EvaluatedAxiom;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.learningproblems.AxiomScore;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.OWL;

@ComponentAnn(name="functional dataproperty axiom learner", shortName="dplfunc", version=0.1)
public class FunctionalDataPropertyAxiomLearner extends AbstractAxiomLearningAlgorithm<OWLFunctionalDataPropertyAxiom, OWLIndividual> {
	
	private static final Logger logger = LoggerFactory.getLogger(FunctionalDataPropertyAxiomLearner.class);
	
	private OWLDataProperty propertyToDescribe;
	
	private boolean declaredAsFunctional;

	public FunctionalDataPropertyAxiomLearner(SparqlEndpointKS ks){
		this.ks = ks;
		
		posExamplesQueryTemplate = new ParameterizedSparqlString("SELECT ?s WHERE {?s ?p ?o1. FILTER NOT EXISTS {?s ?p ?o2. FILTER(?o1 != ?o2)} }");
		negExamplesQueryTemplate = new ParameterizedSparqlString("SELECT ?s WHERE {?s ?p ?o1. ?s ?p ?o2. FILTER(?o1 != ?o2)}");
	}
	
	public OWLDataProperty getPropertyToDescribe() {
		return propertyToDescribe;
	}

	public void setPropertyToDescribe(OWLDataProperty propertyToDescribe) {
		this.propertyToDescribe = propertyToDescribe;
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.AbstractAxiomLearningAlgorithm#getExistingAxioms()
	 */
	@Override
	protected void getExistingAxioms() {
		String query = String.format("ASK {<%s> a <%s>}", propertyToDescribe, OWL.FunctionalProperty.getURI());
		declaredAsFunctional = executeAskQuery(query);
		if(declaredAsFunctional) {
			existingAxioms.add(df.getOWLFunctionalDataPropertyAxiom(propertyToDescribe));
			logger.warn("Data property " + propertyToDescribe + " is already declared as functional in knowledge base.");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.AbstractAxiomLearningAlgorithm#learnAxioms()
	 */
	@Override
	protected void learnAxioms() {
		if(!forceSPARQL_1_0_Mode && ks.supportsSPARQL_1_1()){
			runSPARQL1_1_Mode();
		} else {
			runSPARQL1_0_Mode();
		}
	}
	
	private void runSPARQL1_0_Mode() {
		workingModel = ModelFactory.createDefaultModel();
		int limit = 1000;
		int offset = 0;
		String baseQuery  = "CONSTRUCT {?s <%s> ?o.} WHERE {?s <%s> ?o} LIMIT %d OFFSET %d";
		String query = String.format(baseQuery, propertyToDescribe.toStringID(), propertyToDescribe.toStringID(), limit, offset);
		Model newModel = executeConstructQuery(query);
		while(!terminationCriteriaSatisfied() && newModel.size() != 0){
			workingModel.add(newModel);
			// get number of instances of s with <s p o>
			query = String.format(
					"SELECT (COUNT(DISTINCT ?s) AS ?all) WHERE {?s <%s> ?o.}",
					propertyToDescribe.toStringID());
			ResultSet rs = executeSelectQuery(query, workingModel);
			QuerySolution qs;
			int all = 1;
			while (rs.hasNext()) {
				qs = rs.next();
				all = qs.getLiteral("all").getInt();
			}
			// get number of instances of s with <s p o> <s p o1> where o != o1
			query = "SELECT (COUNT(DISTINCT ?s) AS ?functional) WHERE {?s <%s> ?o1. FILTER NOT EXISTS {?s <%s> ?o2. FILTER(?o1 != ?o2)} }";
			query = query.replace("%s", propertyToDescribe.toStringID().toString());
			rs = executeSelectQuery(query, workingModel);
			int functional = 1;
			while (rs.hasNext()) {
				qs = rs.next();
				functional = qs.getLiteral("functional").getInt();
			}
			if (all > 0) {
				currentlyBestAxioms.clear();
				currentlyBestAxioms.add(new EvaluatedAxiom<OWLFunctionalDataPropertyAxiom>(
						df.getOWLFunctionalDataPropertyAxiom(propertyToDescribe),
						computeScore(all, functional),
						declaredAsFunctional));
			}
			
			offset += limit;
			query = String.format(baseQuery, propertyToDescribe.toStringID(), propertyToDescribe.toStringID(), limit, offset);
			newModel = executeConstructQuery(query);
		}
	}
	
	private void runSPARQL1_1_Mode() {
		int numberOfSubjects = reasoner.getSubjectCountForProperty(propertyToDescribe);//TODO, getRemainingRuntimeInMilliSeconds());
		if(numberOfSubjects == -1){
			logger.warn("Early termination: Got timeout while counting number of distinct subjects for given property.");
			return;
		}
		
		if (numberOfSubjects > 0) {
			int functional = 0;
			String query = "SELECT (COUNT(DISTINCT ?s) AS ?functional) WHERE {?s <%s> ?o1. FILTER NOT EXISTS {?s <%s> ?o2. FILTER(?o1 != ?o2)} }";
			query = query.replace("%s", propertyToDescribe.toStringID().toString());
			ResultSet rs = executeSelectQuery(query);
			if (rs.hasNext()) {
				functional = rs.next().getLiteral("functional").getInt();
			}
			
			currentlyBestAxioms.add(new EvaluatedAxiom<OWLFunctionalDataPropertyAxiom>(
					df.getOWLFunctionalDataPropertyAxiom(propertyToDescribe),
					computeScore(numberOfSubjects, functional),
					declaredAsFunctional));
		}
	}
	
	public static void main(String[] args) throws Exception {
		FunctionalDataPropertyAxiomLearner l = new FunctionalDataPropertyAxiomLearner(new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpediaLiveAKSW()));
		l.setPropertyToDescribe(new OWLDataFactoryImpl().getOWLDataProperty(IRI.create("http://dbpedia.org/ontology/birthDate")));
		l.setMaxExecutionTimeInSeconds(10);
		l.setForceSPARQL_1_0_Mode(true);
		l.init();
		l.start();
		List<EvaluatedAxiom<OWLFunctionalDataPropertyAxiom>> axioms = l.getCurrentlyBestEvaluatedAxioms(5);
		System.out.println(axioms);
		
		for(EvaluatedAxiom<OWLFunctionalDataPropertyAxiom> axiom : axioms){
			printSubset(l.getPositiveExamples(axiom), 10);
			printSubset(l.getNegativeExamples(axiom), 10);
			l.explainScore(axiom);
			System.out.println(((AxiomScore)axiom.getScore()).getTotalNrOfExamples());
			System.out.println(((AxiomScore)axiom.getScore()).getNrOfPositiveExamples());
		}
	}
}
