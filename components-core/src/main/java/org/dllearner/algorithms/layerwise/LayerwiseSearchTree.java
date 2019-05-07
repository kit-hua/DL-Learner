package org.dllearner.algorithms.layerwise;

import java.util.Comparator;
import java.util.TreeSet;


public class LayerwiseSearchTree <T extends LayerwiseAbstractSearchTreeNode> extends LayerwiseAbstractSearchTree<T> {

	public LayerwiseSearchTree(Comparator<T> comparator) {
		super(comparator);
//		nodes = new TreeSet<>(sortOrderComp);

	}
	
}