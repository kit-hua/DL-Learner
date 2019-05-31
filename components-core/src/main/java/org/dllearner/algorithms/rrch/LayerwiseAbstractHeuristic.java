package org.dllearner.algorithms.rrch;

import java.util.Comparator;

import org.dllearner.core.AbstractComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.Heuristic;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;

import com.google.common.collect.ComparisonChain;

/**
 * This is a copy of org.dllearner.core.AbstractHeuristic for the sake of layerwise traversing of the search treee
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
public abstract class LayerwiseAbstractHeuristic extends AbstractComponent implements Heuristic, Comparator<LayerwiseSearchTreeNode>{
	
	public LayerwiseAbstractHeuristic() {}
	
	@Override
	public void init() throws ComponentInitException {

		initialized = true;
	}
	
	@Override
	public int compare(LayerwiseSearchTreeNode node1, LayerwiseSearchTreeNode node2) {
		return ComparisonChain.start()
				.compare(getNodeScore(node1), getNodeScore(node2))
//				.compare(OWLClassExpressionUtils.getLength(node1.getExpression()), OWLClassExpressionUtils.getLength(node2.getExpression()))
				.compare(node1.getDescription(), node2.getDescription())
				.result();
	}

	public abstract double getNodeScore(LayerwiseSearchTreeNode node);

}