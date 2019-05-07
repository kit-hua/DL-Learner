package org.dllearner.algorithms.layerwise;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import org.dllearner.utilities.datastructures.SearchTreeNode;
import org.dllearner.utilities.owl.OWLAPIRenderers;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;

public class LayerwiseSearchTreeNode extends LayerwiseAbstractSearchTreeNode<LayerwiseSearchTreeNode> implements SearchTreeNode {

	protected OWLClassExpression description;
	
	protected double accuracy;
	
	protected int horizontalExpansion;
		
	// the refinement count corresponds to the number of refinements of the
	// OWLClassExpression in this node - it is a better heuristic indicator than child count
	// (and avoids the problem that adding children changes the heuristic value)
	private int refinementCount = 0;
	
	protected static DecimalFormat dfPercent = new DecimalFormat("0.00%");
	
	protected int expansionCounter = 0;
	protected int accumulatedExpansionCounter = 0;
	// wehther or not this node is recently expanded and new children are added
	protected int recentlyExpanded = 0;
	protected LayerwiseSearchTreeNode bestChild;
	protected double score = Double.NaN;
	
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
	
//	public LayerwiseSearchTreeNode(LayerwiseSearchTreeNode parentNode, OWLClassExpression description, double accuracy) {
//		this(description, accuracy);
//		this.setParent(parentNode);
//	}
	
	/**
	 * update the counters after this node get expanded
	 */
	public void updateCounter () {
				
		if(recentlyExpanded != 0) {
			this.expansionCounter += recentlyExpanded;
			recentlyExpanded = 0;
		}
		
		this.accumulatedExpansionCounter = this.expansionCounter;
		for(LayerwiseSearchTreeNode child : this.getChildren()) {			
			this.accumulatedExpansionCounter += child.getAccumulatedExpansionCounter();
		}
		
		this.score = Double.NaN;
	}

	public void incHorizontalExpansion() {
		horizontalExpansion++;
		this.score = Double.NaN;
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
//		String ret = OWLAPIRenderers.toManchesterOWLSyntax(description) + " [";
//		ret += "score" + NLPHeuristic.getNodeScore(this) + ",";
		ret += "acc:" + dfPercent.format(accuracy) + ", ";
		ret += "he:" + horizontalExpansion + ", ";		
//		ret += "c:" + children.size() + ", ";
//		ret += "ref:" + refinementCount + "]";
		ret += "cnt:" + expansionCounter + ", ";
		ret += "acnt:" + accumulatedExpansionCounter;
		if(score != Double.NaN) {
			ret +=  ", score:" + score + "]";
		}else {
			ret += "]";
		}

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
	 * @return the expansionCounter
	 */
	public int getExpansionCounter() {
		return expansionCounter;
	}


	/**
	 * @return the accumulatedExpansionCounter
	 */
	public int getAccumulatedExpansionCounter() {
		return accumulatedExpansionCounter;
	}
	
	/**
	 * @return the newChildrenAdded
	 */
	public int getRecentlyExpanded() {
		return recentlyExpanded;
	}

	/**
	 * @param newChildrenAdded the newChildrenAdded to set
	 */
	public void setRecentlyExpanded(int recentlyExpanded) {
		this.recentlyExpanded = recentlyExpanded;
	}

	/**
	 * @return the bestChild
	 */
	public LayerwiseSearchTreeNode getBestChild() {
		return bestChild;
	}

	/**
	 * @param bestChild the bestChild to set
	 */
	public void setBestChild(LayerwiseSearchTreeNode bestChild) {
		this.bestChild = bestChild;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}
}