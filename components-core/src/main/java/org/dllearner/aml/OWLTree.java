/**
 * 
 */
package org.dllearner.aml;

import java.util.HashSet;
import java.util.Set;

import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.HasFiller;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class OWLTree {
	
	private SimpleTreeNode<NodeType> root;
	
	public static final String HAS_IE = "hasIE";
	public static final String HAS_EI = "hasEI";
	
	public OWLTree() {
		root = new SimpleTreeNode<NodeType>();
	}
	
	public OWLTree(SimpleTreeNode<NodeType> root) {
		this.root = root;
	}
	
	public OWLTree(OWLClassExpression rootExpr) {		
		this();		
		root.data = toNodeType(rootExpr);	 
	}
	
	public OWLTree(OWLTree other) {
		this.root = other.root;
		copyChildrenRec(root, other.root);
	}
	
	// expand all nodes in the tree
	// due to disjunctions, this can generate more trees
	public Set<OWLTree> expandAll () {
		Set<OWLTree> trees = new HashSet<OWLTree>();
		
		return trees;
	}
	
	// expand the top-level disjunction in the given node
	// this will generate more trees 
	private Set<OWLTree> expandDisjunction (SimpleTreeNode<NodeType> node){
		Set<OWLTree> trees = null;
		
		if(node.data instanceof DataNode) {
			DataNode dn = (DataNode) node.data;
			OWLClassExpression expr = dn.getData();
			
			// in case it really has a top-level disjunction
			if(expr instanceof OWLObjectUnionOf) {
				trees = new HashSet<OWLTree>();
				
				Set<OWLClassExpression> operands = expr.asDisjunctSet();
				for(OWLClassExpression operand : operands) {
					// for each operand, clone a new tree
					OWLTree tree = new OWLTree(this);
					// for each operand, make a new node
					SimpleTreeNode<NodeType> operandNode = new SimpleTreeNode<NodeType>();
					operandNode.data = new DataNode(operand);
					// find the given node in the new tree
					for(SimpleTreeNode<NodeType> cloned : tree.root.getDescendantOrSelf()) {
						// compare by ID: should find it since the copy constructor copies the id
						if(cloned.equals(node)) {
							// get the parent
							SimpleTreeNode<NodeType> parent = cloned.getParent();
							// remove old child
							parent.removeChild(cloned);
							// add new child
							parent.addChild(operandNode);
							// stop searching
							break;
						}
					}
					
					trees.add(tree);
				}
			}			
		}
		
		return trees;
	}
	
	// expand the top-level conjunction in the role filler of the given node	
	// this will generate more nodes to the tree
	// the given node will be removed from the child list of its parent
	// and a new logical node will be added - since it is expanded
	private void expandConjunctionsInRoleFiller(SimpleTreeNode<NodeType> node) {
		if(node.data instanceof DataNode) {
			DataNode dn = (DataNode) node.data;
			OWLClassExpression expr = dn.getData();
			
			SimpleTreeNode<NodeType> newNode = toLogicNode(node);
			if(newNode != null) {
				OWLClassExpression filler = getRoleFiller(expr);
				if(filler instanceof OWLObjectIntersectionOf) {					
					
					Set<OWLClassExpression> operands = filler.asConjunctSet();					
					for(OWLClassExpression operand : operands) {
						SimpleTreeNode<NodeType> child = new SimpleTreeNode<NodeType>();
						child.data = new DataNode(operand);
						newNode.addChild(child);
					}
				}
				
				SimpleTreeNode<NodeType> parent = node.getParent();
				
				if(parent == null) {
					// if the node has no parent, then it must be root of the tree
					// in this case, we replace the root node of the tree directly
					this.root = newNode;
				}else {
					// remove the current node from its parent
					parent.removeChild(node);
					// add the new node to the parent
					parent.addChild(newNode);
				}
			}
			else {
				// if it is not a role restriction axiom on top level, then newNode=null
				// do nothing
			}
		}
	}
	
	private SimpleTreeNode<NodeType> toLogicNode (SimpleTreeNode<NodeType> node){
		if(node.data instanceof DataNode) {
			DataNode dn = (DataNode) node.data;
			OWLClassExpression expr = dn.getData();
							
			SimpleTreeNode<NodeType> logicNode = new SimpleTreeNode<NodeType>();
			logicNode.data = toNodeType(expr);
			return logicNode;
		}
		return null;		
	}
	
	private void copyChildrenRec(SimpleTreeNode<NodeType> node, SimpleTreeNode<NodeType> source) {
		// for each child of the source node, make a copy, and add the copy as a child to the current node
		// do it recursively for the children of the child of the source node
		for(SimpleTreeNode<NodeType> sourceChild : source.getChildren()) {
			SimpleTreeNode<NodeType> childNodeCopy = new SimpleTreeNode<NodeType>(sourceChild);
			copyChildrenRec(childNodeCopy,  sourceChild);
			node.addChild(childNodeCopy);
		}		
	}
	
	private NodeType toNodeType (OWLClassExpression expr) {
		NodeType nt = null;
		
		if(expr instanceof OWLObjectAllValuesFrom) {
			OWLObjectAllValuesFrom avf = (OWLObjectAllValuesFrom) expr;
			OWLPropertyExpression role = avf.getProperty();
			if(role.toString().equals(HAS_IE))
				nt = new LogicNode(LogicNodeType.ALL_IE);
			if(role.toString().equals(HAS_EI))
				nt = new LogicNode(LogicNodeType.ALL_EI);
		}
		else if(expr instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom asf = (OWLObjectSomeValuesFrom) expr;
			OWLPropertyExpression role = asf.getProperty();
			if(role.toString().equals(HAS_IE))
				nt = new LogicNode(LogicNodeType.SOME_IE);
			if(role.toString().equals(HAS_EI))
				nt = new LogicNode(LogicNodeType.SOME_EI);
		}
		else if(expr instanceof OWLObjectIntersectionOf) {				
			nt = new LogicNode(LogicNodeType.CONJUNCTION);
		}
		else
			// handle disjunction later
			// OWLObjectHasValueOf is ignored first
			nt = new DataNode(expr);
		
//		if(OWLClassExpressionUtils.getChildren(expr).isEmpty()) {
//			nt = new DataNode(expr);
//		}
//		else {
//			if(expr.asConjunctSet().size() > 1) {				
//				nt = new LogicNode(LogicNodeType.CONJUNCTION);
//			}
//			if(expr instanceof OWLObjectAllValuesFrom) {
//				OWLObjectAllValuesFrom avf = (OWLObjectAllValuesFrom) expr;
//				OWLPropertyExpression role = avf.getProperty();
//				if(role.toString().equals(HAS_IE))
//					nt = new LogicNode(LogicNodeType.ALL_IE);
//				if(role.toString().equals(HAS_EI))
//					nt = new LogicNode(LogicNodeType.ALL_EI);
//			}
//			if(expr instanceof OWLObjectSomeValuesFrom) {
//				OWLObjectSomeValuesFrom asf = (OWLObjectSomeValuesFrom) expr;
//				OWLPropertyExpression role = asf.getProperty();
//				if(role.toString().equals(HAS_IE))
//					nt = new LogicNode(LogicNodeType.SOME_IE);
//				if(role.toString().equals(HAS_EI))
//					nt = new LogicNode(LogicNodeType.SOME_EI);
//			}
//		}		
		
		return nt;
	}
	
	private OWLClassExpression getRoleFiller(OWLClassExpression expr) {
		
		OWLClassExpression filler = null;
		
		if(expr instanceof OWLQuantifiedObjectRestriction) {
			OWLQuantifiedObjectRestriction res = (OWLQuantifiedObjectRestriction) expr;
			filler = res.getFiller();
		}
		
		return filler;
	}
	
	private OWLPropertyExpression getRole(OWLClassExpression expr) {
		
		OWLPropertyExpression property = null;
		
		if(expr instanceof OWLQuantifiedObjectRestriction) {
			OWLQuantifiedObjectRestriction res = (OWLQuantifiedObjectRestriction) expr;
			property = res.getProperty();
		}
		
		return property;
	}

}
