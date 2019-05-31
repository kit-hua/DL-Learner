/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
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
package org.dllearner.algorithms.celoe;

import java.text.DecimalFormat;
import java.util.Map;

import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.AbstractSearchTreeNode;
import org.dllearner.utilities.datastructures.SearchTreeNode;
import org.dllearner.utilities.owl.OWLAPIRenderers;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * A node in the search tree of the ontology engineering algorithm.
 * 
 * Differences to the node structures in other algorithms (this may change):
 * - covered examples are not stored in node (i.e. coverage needs to be recomputed
 * for child nodes, which costs time but saves memory)
 * - only evaluated nodes are stored
 * - too weak nodes are not stored
 * - redundant nodes are not stored (?)
 * - only accuracy is stored to make the node structure reusable for different
 *   learning problems and -algorithms
 * 
 * @author Jens Lehmann
 *
 */
public class OENode extends AbstractSearchTreeNode<OENode> implements SearchTreeNode {

	protected OWLClassExpression description;
	
	protected double accuracy;
	
	protected int horizontalExpansion;
		
	// the refinement count corresponds to the number of refinements of the
	// OWLClassExpression in this node - it is a better heuristic indicator than child count
	// (and avoids the problem that adding children changes the heuristic value)
	private int refinementCount = 0;
	
	protected static DecimalFormat dfPercent = new DecimalFormat("0.00%");
	
	protected double score = Double.NaN;
	
//	protected int expansionCounter;
//	protected int accumulatedExpansionCounter;
	// wehther or not this node is recently expanded and new children are added
//	protected int recentlyExpanded = 0;
//	protected OENode bestChild;
	
	public OENode(OWLClassExpression description, double accuracy) {
		this.description = description;
		this.accuracy = accuracy;
		this.horizontalExpansion = OWLClassExpressionUtils.getLength(description) - 1;
//		this.expansionCounter = 0;
//		this.accumulatedExpansionCounter = 0;
	}
	
	public OENode(OWLClassExpression description, double accuracy, boolean modifyHe) {
		this.description = description;
		this.accuracy = accuracy;
		if(modifyHe)
			this.horizontalExpansion = OWLClassExpressionUtils.getLength(description) - 1;
		else
			this.horizontalExpansion = OWLClassExpressionUtils.getLength(description);
		
//		this.expansionCounter = 0;
//		this.accumulatedExpansionCounter = 0;
	}	
	
//	public OENode(OENode parentNode, OWLClassExpression description, double accuracy) {
//		this(description, accuracy);
//		this.setParent(parentNode);
//	}


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
		ret += "c:" + children.size() + ", ";
		ret += "ref:" + refinementCount;
		
		if(score != Double.NaN) {
			ret +=  ", score:" + score + "]";
		}else {
			ret += "]";
		}
//		ret += "cnt:" + expansionCounter + ", ";
//		ret += "acnt:" + accumulatedExpansionCounter + "]";
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