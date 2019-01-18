///**
// * 
// */
//package org.dllearner.aml;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.dllearner.utilities.owl.OWLClassExpressionUtils;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
//import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
//
///**
// * @author Yingbing Hua, yingbing.hua@kit.edu
// *
// */
//public class OWLTreePattern {
//	
//	private OWLTreePatternNode root;
//	
//	private List<OWLTreePatternNode> expandables;
//	
//	public OWLTreePattern() {
//		expandables = new ArrayList<OWLTreePatternNode>();
//	}
//	
//	public OWLTreePattern(OWLTreePatternNode root) {
//		this();
//		this.root = root;
//		
//		for(SimpleTreeNode<OWLClassExpression> node : this.root.getDescendantOrSelf()) {
//			OWLTreePatternNode owlnode = (OWLTreePatternNode) node;
//			if(owlnode.isExpandable())
//				expandables.add(owlnode);
//		}
//	}
//	
//	// copy constructor
//	public OWLTreePattern(OWLTreePattern other) {
//		
//		this.root = new OWLTreePatternNode(other.root);		
//		// copy the children and references
//		copyChildrenRec(root, other.root);
//	}
//	
//	// expand the current tree
//	public Set<OWLTreePattern> expandAll() {
//		OWLObjectAllValuesFrom avf;
//		// first expand all role restrictions
//		for(OWLTreePatternNode expandable : expandables) {
//			expandRoleFillers(expandable);
//		}
//	}
//	
//	private void copyChildrenRec(OWLTreePatternNode node, OWLTreePatternNode source) {
//		// for each child of the source node, make a copy, and add the copy as a child to the current node
//		// do it recursively for the children of the child of the source node
//		for(SimpleTreeNode<OWLClassExpression> sourceChild : source.getChildren()) {
//			OWLTreePatternNode childNodeCopy = new OWLTreePatternNode((OWLTreePatternNode) sourceChild);
//			copyChildrenRec(childNodeCopy, (OWLTreePatternNode) sourceChild);
//			node.addChild(childNodeCopy);
//		}		
//	}
//	
//	// expand the given node if it has a role restriction axiom
//	private void expandRoleFillers(OWLTreePatternNode node) {
//		
//		OWLTreePatternNode parent = (OWLTreePatternNode) node.parent;
//		OWLClassExpression filler;
//		
//		if(node.data instanceof OWLObjectSomeValuesFrom) {
//			OWLObjectSomeValuesFrom svf = (OWLObjectSomeValuesFrom) node.data;
//			filler = svf.getFiller();
//		}
//		
//		if(node.data instanceof OWLObjectAllValuesFrom) {
//			OWLObjectAllValuesFrom avf = (OWLObjectAllValuesFrom) node.data;
//			filler = avf.getFiller();
//		}
//		
//		if(filler != null) {
//			Set<OWLClassExpression> children = OWLClassExpressionUtils.getChildren(filler);
//			if(!children.isEmpty()) {
//				OWLTreePatternNode expanded 
//			}
//		}
//			
//		
//		if(node.isRoleReistriction()) {
//			
//			Set<OWLClassExpression> fillers = OWLClassExpressionUtils.getChildren(node.data);
//						
//			if(fillers.size() > 1) {
//				OWLTreePatternNode parent = (OWLTreePatternNode) node.parent;
//				for(OWLClassExpression filler : fillers) {
//					OWLTreePatternNode child = new OWLTreePatternNode(filler);
//					parent.addChild(child);
//				}
//				parent.removeChild(node);
//			}
//		}
//	}
//	
//	// expand the given node if it has disjunctions
//	private Set<OWLTreePatternNode> expandDisjunction(OWLTreePatternNode node) {
//		
//		Set<OWLTreePatternNode> expandedNodes = new HashSet<OWLTreePatternNode>();
//		
//		Set<OWLClassExpression> disjunctions = node.data.asDisjunctSet();
//		if(disjunctions.size() > 1) {
//			for(OWLClassExpression disjunction : disjunctions) {
//				OWLTreePatternNode expandedNode = new OWLTreePatternNode(disjunction);
//				expandedNodes.add(expandedNode);
//			}
//		}
//		
//		return expandedNodes;
//	}
//	
//
//}
