package org.dllearner.algorithms.rrch;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import org.dllearner.utilities.datastructures.SearchTreeNode;
import org.dllearner.utilities.owl.OWLAPIRenderers;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * This is a copy of org.dllearner.utilities.datastructures.AbstractSearchTree for the sake of layerwise traversing of the search treee
 * It replaces the global priority queue with individual queues of each node
 * It also stores the score with the node
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
public class LayerwiseSearchTreeNode extends LayerwiseAbstractSearchTreeNode<LayerwiseSearchTreeNode>{

	protected OWLClassExpression description;
	
	protected double accuracy;
	
	protected int horizontalExpansion;
		
	// the refinement count corresponds to the number of refinements of the
	// OWLClassExpression in this node - it is a better heuristic indicator than child count
	// (and avoids the problem that adding children changes the heuristic value)
	private int refinementCount = 0;
	
	protected static DecimalFormat dfPercent = new DecimalFormat("0.00%");
	
	// use Double object instead of double value to allow comparison between Double.NaN 
	protected Double celoeScore = new Double(Double.NaN);
	
	public LayerwiseSearchTreeNode(OWLClassExpression description, double accuracy, Comparator<LayerwiseSearchTreeNode> comparator) {
		super(comparator);
		this.description = description;
		this.accuracy = accuracy;
		this.horizontalExpansion = OWLClassExpressionUtils.getLength(description) - 1;
				
		this.children = new TreeSet<>(comparator);		
	}
	
	public LayerwiseSearchTreeNode(OWLClassExpression description, double accuracy, boolean modifyHe, Comparator<LayerwiseSearchTreeNode> comparator) {
		super(comparator);
		this.description = description;
		this.accuracy = accuracy;
		if(modifyHe)
			this.horizontalExpansion = OWLClassExpressionUtils.getLength(description) - 1;
		else
			this.horizontalExpansion = OWLClassExpressionUtils.getLength(description);
				
		this.children = new TreeSet<>(comparator);
	}	
	
	public void incHorizontalExpansion() {
		horizontalExpansion++;
		this.celoeScore = Double.NaN;
	}
	
	public boolean isRoot() {
		return (parent == null);
	}
	
	/**
	 * @return the description
	 */
	public OWLClassExpression getDescription() {
		return description;
	}

	@Override
	public OWLClassExpression getExpression() {
		return getDescription();
	}
	
	/**
	 * @return the accuracy
	 */
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * @return the horizontalExpansion
	 */
	public int getHorizontalExpansion() {
		return horizontalExpansion;
	}
	
	public String getShortDescription(String baseURI) {
		return getShortDescription(baseURI, null);
	}
	
	public String getShortDescription(String baseURI, Map<String, String> prefixes) {
		String ret = OWLAPIRenderers.toDLSyntax(description) + " [";
		ret += "acc:" + dfPercent.format(accuracy) + ", ";
		ret += "he:" + horizontalExpansion + ", ";				
		// always append celoe score at the end, even if it is NaN
		ret +=  "celoe:" + celoeScore + "]";
		
		return ret;
	}
	
	@Override
	public String toString() {
		return getShortDescription(null);
	}

	/**
	 * @return the refinementCount
	 */
	public int getRefinementCount() {
		return refinementCount;
	}

	/**
	 * @param refinementCount the refinementCount to set
	 */
	public void setRefinementCount(int refinementCount) {
		this.refinementCount = refinementCount;
	}


	/**
	 * @return the score
	 */
	public Double getCeloeScore() {
		return celoeScore;
	}

	/**
	 * @param score the score to set
	 */
	public void setCeloeScore(Double score) {
		this.celoeScore = score;
	}
}