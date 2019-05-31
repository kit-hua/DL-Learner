package org.dllearner.algorithms.rrch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dllearner.utilities.datastructures.SearchTreeNode;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * This is a copy of org.dllearner.core.AbstractSearchTreeNode for the sake of layerwise traversing of the search treee
 * It replaces the global priority queue with individual queues of each node
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
public abstract class LayerwiseAbstractSearchTreeNode <T extends LayerwiseAbstractSearchTreeNode> implements SearchTreeNode {

	protected Set< LayerwiseAbstractSearchTree<T> > trees = new HashSet<>();
	
	protected T parent;
	protected Comparator<T> sortOrderComp;
	protected NavigableSet<T> children;
	private int depth = 0; //depth of the node in a tree
	
	protected String id = "";
	
	public LayerwiseAbstractSearchTreeNode(Comparator<T> comparator) {
		// TODO Auto-generated constructor stub
		sortOrderComp = comparator;
	}


	@Override
	public abstract OWLClassExpression getExpression();

	/**
	 * add a child node to this node
	 * @param node the child node
	 */
	public void addChild(T node) {
		node.setParent(this);				
		node.setId(this.getId() + "-" + String.valueOf(children.size()+1));
		children.add(node);		
		node.setDepth(this.depth+1);
		node.notifyTrees(this.trees);
	}
	
	/**
	 * set the parent of this node
	 * @param node parent node
	 */
	protected void setParent(T node) {
		if (this.parent == node) {
			return;
		} else if (this.parent != null) {
			throw new Error("Parent already set on node");
		}
		this.parent = node;
	}

	/**
	 * internally used by the tree<->node contract to add this node to a set of trees
	 * @param trees the set of owning trees
	 */
	public void notifyTrees( Collection<? extends LayerwiseAbstractSearchTree<T>> trees ) {
		updatePrepareTree();
		this.trees.addAll(trees);
		notifyTree();
	}

	public void notifyTree( LayerwiseAbstractSearchTree<T> tree ) {
		updatePrepareTree();
		this.trees.add(tree);
		notifyTree();
	}
	
	private void notifyTree() {
		for(LayerwiseAbstractSearchTree<T> tree : trees) {
			tree.notifyNode((T)this);
		}
	}
	
	private void updatePrepareTree() {
		for(LayerwiseAbstractSearchTree<T> tree : trees) {
			tree.updatePrepare((T)this);
		}
	}

	/**
	 * @return the parent
	 */
	public T getParent() {
		return parent;
	}

	public Collection<T> getChildren() {
		return children;
	}
	
	
	/**
	 * @return an iterator over the elements in this search tree in descending comparison order
	 */
	public Iterator<T> descendingIterator() {
		return children.descendingIterator();
	}

	/**
	 * @return a set of the nodes in the search tree ordered in descending comparison order
	 */
	public SortedSet<T> descendingSet() {
		return children.descendingSet();
	}

	/**
	 * @return best node according to comparator
	 */
	public T best() {
		return children.last();
	}
	
	public void setDepth(int d) {
		this.depth = d;
	}


 	public int getDepth() {
		return this.depth;
	}


	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}


	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
