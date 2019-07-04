package org.dllearner.algorithms.rrhc;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;


/**
 * This is a copy of org.dllearner.utilities.datastructures.AbstractSearchTree for the sake of layerwise traversing of the search treee
 * It replaces the global priority queue with individual queues of each node
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
public class LayerwiseAbstractSearchTree  <T extends LayerwiseAbstractSearchTreeNode> {

	// all nodes in the search tree (used for selecting most promising node)
//	protected NavigableSet<T> nodes;
	
	// the sort order on the set
	protected Comparator<T> sortOrderComp;

	// root of search tree
	protected T root;
	
	// @Hua: depth of the tree
	private int depth;
	
	protected int size = 0;
	
	/**
	 * create a new search tree
	 * @param comparator the comparator to use for the nodes
	 */
	public LayerwiseAbstractSearchTree(Comparator<T> comparator) {
		sortOrderComp = comparator;
		depth = 0;
	}

	/**
	 * add node to the search tree
	 * @param parentNode the parent node or null if root
	 * @param node the node to add
	 */
	public void addNode(T parentNode, T node) {
		// link to parent (unless start node)
		if(parentNode == null) {
			this.setRoot(node);
		} else {
			parentNode.addChild(node);
		}
		
		if(this.depth < node.getDepth())
			this.depth = node.getDepth();
		
		size++;
	}
	
	/**
	 * internally used by tree<->node contract to notify a tree about an added node
	 * @param node the node
	 */
	public final void notifyNode(T node) {
		/**
		 * @Hua: to check if nodes contains the parent, it goes to use the comparator in AbstractHeuristic
		 * which is defined as OEHeuristicRuntime
		 */
//		if (node.getParent() == null || nodes.contains(node.getParent())) {
//		if (node.getParent() != null) {
//			if (allowedNode(node))
//				node.getParent().addChild(node);
//		}
	}

	/**
	 * filter certain nodes to be permitted in the node-set
	 * @param node node to test
	 * @return whether node is allowed in the node-set
	 */
	protected boolean allowedNode(T node) {
		return true;
	}

	/**
	 * set the tree root to a node
	 * @param node the node
	 */
	public void setRoot(T node) {
//		if (this.root != null || !this.nodes.isEmpty()) {
		if (this.root != null ) {
			throw new Error("Tree Root already set");
		}
		this.root = node;
		node.setId("0");
		node.notifyTree(this);
	}

	/**
	 * must be called before modifying a node, to support immutable set element pattern
	 * @param node the node
	 */
	public final void updatePrepare(T node) {
				
		if(node.parent != null) {
			// update the node in its parent's children list
			T parent = (T) node.getParent();
			parent.children.remove(node);			
		}
	}
	
	/**
	 * must be called after modifying a node, to support immutable set element pattern
	 */
	public final void updateDone(T node) {
				
		// update the node in its parent's children list
		if(node.parent != null) {
			T parent = (T) node.getParent();
			parent.children.add(node);
		}
	}

	/**
	 * @return the tree root node
	 */
	public T getRoot() {
		return root;
	}
	
	public int getDepth() {
		return this.depth;
	}
	
 	public int size() {
 		return this.size;
 	}

 	
    public void update(T e) {	
    		updatePrepare(e);
    		updateDone(e);
     }
}
