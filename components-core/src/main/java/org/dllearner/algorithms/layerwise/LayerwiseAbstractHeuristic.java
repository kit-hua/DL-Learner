package org.dllearner.algorithms.layerwise;

import java.util.Comparator;

import org.dllearner.core.AbstractComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.Heuristic;

import com.google.common.collect.ComparisonChain;

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
				.compare(node1.getDescription(), node2.getDescription())
				.result();
	}

	public abstract double getNodeScore(LayerwiseSearchTreeNode node);

}