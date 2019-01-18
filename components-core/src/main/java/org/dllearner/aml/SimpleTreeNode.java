/**
 * 
 */
package org.dllearner.aml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 * A generic tree node class
 */
public class SimpleTreeNode<T>{

    public T data;
    public SimpleTreeNode<T> parent;
    public List<SimpleTreeNode<T>> children;
    private static AtomicInteger nextId = new AtomicInteger(0);
    protected int id;
    
    public SimpleTreeNode() {
    }
    
    // copy constructor
    public SimpleTreeNode(SimpleTreeNode<T> other) {
    		this.data = other.data;
    		this.id = other.id;
    }

    public SimpleTreeNode(T data) {
        this.data = data;
        this.children = new ArrayList<SimpleTreeNode<T>>();
        this.parent = null;
        this.id = nextId.getAndIncrement();
    }

    public void addChild(T child) {
        SimpleTreeNode<T> childNode = new SimpleTreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
    }
    
    public void addChild(SimpleTreeNode child) {
//        SimpleTreeNode<T> childNode = new SimpleTreeNode<T>(child);
        child.parent = this;
        this.children.add(child);
    }
    
    public List<SimpleTreeNode<T>> getDescendant(){
    		List<SimpleTreeNode<T>> descendants = new ArrayList<SimpleTreeNode<T>>();
    		
    		for(SimpleTreeNode child : children) {
    			// direct child
    			descendants.add(child);
    			// recursive descendant
    			descendants.addAll(child.getDescendant());
    		}    		
    		return descendants;
    } 
    
    public List<SimpleTreeNode<T>> getDescendantOrSelf(){
    		List<SimpleTreeNode<T>> descendants = this.getDescendant();
    		descendants.add(this);
    		return descendants;
    }    
 
    public SimpleTreeNode<T> getParent(){
    		return this.parent;
    }
    
    // although it is a list, it is already sorted by depth, since it does recursive calls on parent->parent->...
    public List<SimpleTreeNode<T>> getAncestor(){
    		List<SimpleTreeNode<T>> ancestors = new ArrayList<SimpleTreeNode<T>>();
    		if(this.parent != null) {
    			ancestors.add(this.parent);
    			if(this.parent.getAncestor() != null)
    				ancestors.addAll(this.parent.getAncestor());
    		}
    		return ancestors;
    }
    
    public List<SimpleTreeNode<T>> getChildren(){
    		return this.children;
    }
    
    public List<SimpleTreeNode<T>> getSibling(SimpleTreeNode<T> node) {
		List<SimpleTreeNode<T>> siblings = new ArrayList<SimpleTreeNode<T>>();
		for(SimpleTreeNode<T> sibling : this.parent.children) {
			siblings.add(sibling);
		}		
		return siblings;
	}
    
    public SimpleTreeNode<T> getLeastCommonParent(SimpleTreeNode<T> other) {
    		SimpleTreeNode<T> parent = null;
    		int d = -1;
    		List<SimpleTreeNode<T>> ancestor_this = this.getAncestor();
    		ancestor_this.add(this);
    		List<SimpleTreeNode<T>> ancestor_other = other.getAncestor();
    		ancestor_other.add(other);
    		for (int i = 0; i < ancestor_this.size(); i++) {    			
    			for (int j = 0; j < ancestor_other.size(); j++) {
    				if(ancestor_this.get(i) == ancestor_other.get(j)){
    					int depth = ancestor_this.get(i).getDepth();
    					if(depth > d) {
    						parent = ancestor_this.get(i);
        					d = depth;	
    					}    					
    				}
    			}
    		}
    		return parent;
    }
    
    public int getDepth() {
    		if(this.parent == null)
    			return 0;
    		else
    			return this.getParent().getDepth()+1;
    }
    
    public List<SimpleTreeNode<T>> getDescendantsAtDepth (int depth){
    		if(depth < this.getDepth())
    			return null;
    		
    		List<SimpleTreeNode<T>> ret = new ArrayList<SimpleTreeNode<T>>();
    		List<SimpleTreeNode<T>> descendants = getDescendant();
    		for(SimpleTreeNode<T> descendant : descendants) {
    			if(descendant.getDepth() == depth)
    				ret.add(descendant);
    		}
    		
    		return ret;    		
    }
    
    public List<SimpleTreeNode<T>> getParentToNode (SimpleTreeNode<T> node) {    	
    		List<SimpleTreeNode<T>> ret = new ArrayList<SimpleTreeNode<T>>();
    		List<SimpleTreeNode<T>> ancestors = this.getAncestor();
    		for(SimpleTreeNode<T> ancestor : ancestors) {
    			if(ancestor.getDepth() <= node.getDepth())
    				ret.add(ancestor);
    		}
    		
    		return ret;
    }
    
    public List<SimpleTreeNode<T>> getLeafNodes(){
    		List<SimpleTreeNode<T>> leaves = new ArrayList<SimpleTreeNode<T>>();
    		List<SimpleTreeNode<T>> descendants = this.getDescendantOrSelf();
    		for(SimpleTreeNode<T> descendant : descendants) {
    			if(descendant.getChildren().isEmpty())
    				leaves.add(descendant);
    		}
    		return leaves;
    }
    
    public int getID() {
    		return id;
    }
    
    public void removeChild(SimpleTreeNode<T> child) {
		if(children.contains(child))
			children.remove(child);
    }

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) return false;
	    if (!(obj instanceof SimpleTreeNode<?>))
	        return false;
	    if (obj == this)
	        return true;
	    
	    return 
//	    		this.data.equals( ((SimpleTreeNode<?>) obj).data ) &&
	    		this.id == ((SimpleTreeNode<?>) obj).id; 
	}
	

}