///**
// * 
// */
//package org.dllearner.aml;
//
//import java.util.ArrayList;
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
// * If the node does not contain any OWLClassExpression, then this nodes represents a "conjunction" of its children
// * Disjunctions will be treated as cloned trees
// */
//public class OWLTreePatternNode extends SimpleTreeNode<NodeType>{
//	
//	private boolean isExpandable;
//	
//	public static final String LOGIC_NODE = "logic";
//	public static final String DATA_NODE = "data";
//	
//	public OWLTreePatternNode(OWLTreePatternNode other) {
//		super(other);
//	}
//	
//	public OWLTreePatternNode(OWLClassExpression expr) {
//		
//		super();
//		
//		Set<OWLClassExpression> conjs = expr.asConjunctSet();
//		
//		if(conjs.isEmpty()) {
//			this.data = expr;
//			if(this.data.toString().eq)
//			isExpandable = OWLClassExpressionUtils.getChildren(this.data).isEmpty();
//		}			
//		else {											
//			for(OWLClassExpression conj : conjs) {
//				// since DL-Learner never generates nested conjunctions, we do not need to handle them recursively
//				// although this would still run into recursions, but nothing will happen on the children
//				OWLTreePatternNode child = new OWLTreePatternNode(conj);
//				this.addChild(child);
//			}
//			isExpandable = true;
//		}
//		
//	}	 
//	
//	public boolean isRoleReistriction() {
//		if(this.data == null)
//			return false;
//		
//		if(this.data instanceof OWLObjectSomeValuesFrom || this.data instanceof OWLObjectAllValuesFrom)
//			return true;
//		else
//			return false;
//	}
//	
//	public boolean isExpandable() {
////		return OWLClassExpressionUtils.getChildren(this.data).isEmpty();
//		return isExpandable;
//	}		
//
//}
