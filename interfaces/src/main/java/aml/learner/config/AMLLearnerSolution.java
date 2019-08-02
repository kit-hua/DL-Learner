package aml.learner.config;

import java.util.NavigableSet;

import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.semanticweb.owlapi.model.OWLClassExpression;

public class AMLLearnerSolution {

	private OWLClassExpression concept;
	private String xquery;
	private String queryResult;
	
	public AMLLearnerSolution() {
		
	}

	/**
	 * @return the concept
	 */
	public OWLClassExpression getConcept() {
		return concept;
	}

	/**
	 * @param concept the concept to set
	 */
	public void setConcept(OWLClassExpression concept) {
		this.concept = concept;
	}

	/**
	 * @return the xquery
	 */
	public String getXquery() {
		return xquery;
	}

	/**
	 * @param xquery the xquery to set
	 */
	public void setXquery(String xquery) {
		this.xquery = xquery;
	}

	/**
	 * @return the queryResult
	 */
	public String getQueryResult() {
		return queryResult;
	}

	/**
	 * @param queryResult the queryResult to set
	 */
	public void setQueryResult(String queryResult) {
		this.queryResult = queryResult;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = "";
		if(concept != null)
			s += " - concept: \n\t" + concept.toString() + "\n";
		
		if(xquery != null && xquery != "") {
			xquery = xquery.replaceAll("\n", "\n\t");
			s += "\n - xquery: \n\t" + xquery + "\n";
		}
			
		
		if(queryResult != null && queryResult != "") {
			queryResult = queryResult.replaceAll("\n", "\n\t");
			s += "\n - result: \n\t" + queryResult + "\n";
		}
			
		
		return s;
	}	
	
}
