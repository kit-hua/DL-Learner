package org.dllearner.algorithms.rrch;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * This is a copy of org.dllearner.utilities.datastructures.SearchTree for the sake of layerwise traversing of the search treee
 * It replaces the global priority queue with individual queues of each node
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
public class LayerwiseSearchTree <T extends LayerwiseAbstractSearchTreeNode> extends LayerwiseAbstractSearchTree<T> {

	public LayerwiseSearchTree(Comparator<T> comparator) {
		super(comparator);
	}
	
}