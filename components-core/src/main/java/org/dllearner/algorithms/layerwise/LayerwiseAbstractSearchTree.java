package org.dllearner.algorithms.layerwise;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;


public class LayerwiseAbstractSearchTree  <T extends LayerwiseAbstractSearchTreeNode> {

	// all nodes in the search tree (used for selecting most promising node)
//	protected NavigableSet<T> nodes;
//	protected HashSet<T> nodes;
	
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
		node.notifyTree(this);
	}

	/**
	 * must be called before modifying a node, to support immutable set element pattern
	 * @param node the node
	 */
	public final void updatePrepare(T node) {
		
//		Collection<T> children = (Collection<T>)node.getChildren();
		// update the node in the global list
//		for (T child : (Collection<T>)node.getChildren()) {
//		for (T child : children) {
//			System.out.println(child.toString());
//			if (allowedNode(child))
//				updatePrepare(child);
//		}
//		nodes.remove(node);
		
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
		
		// update the node in the global list
//		if (allowedNode(node)) {
//			nodes.add(node);
//			for (T child : (Collection<T>)node.getChildren()) {
//				updateDone(child);
//			}
//		}
		
		// update the node in its parent's children list
		if(node.parent != null) {
			T parent = (T) node.getParent();
			parent.children.add(node);
		}
	}

	/**
	 * @return an iterator over the elements in this search tree in descending comparison order
	 */
//	public Iterator<T> descendingIterator() {
//		return nodes.descendingIterator();
////		return null;
//	}
//
//	/**
//	 * @return a set of the nodes in the search tree ordered in descending comparison order
//	 */
//	public SortedSet<T> descendingSet() {
//		return nodes.descendingSet();
////		return null;
//	}
//
//	/**
//	 * @return best node according to comparator
//	 */
//	public T best() {
//		return nodes.last();
////		return null;
//	}
//
//	/**
//	 * @return the underlying set of all tree nodes
//	 */
//	public Set<T> getNodeSet() {
//		return nodes;
//	}

	/**
	 * @return the tree size
	 */
//	public int size() {
//		return nodes.size();
//	}

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

 	
//    public boolean update(T e) {
//        if (nodes.remove(e)) {
//        		if(nodes.add(e))
//        			return true;
//    			else
//    				return false;
////            return nodes.add(e);
//        } else { 
//            return false;
//        }
//     }
}
