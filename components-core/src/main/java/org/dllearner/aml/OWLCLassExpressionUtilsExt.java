///**
// * 
// */
//package org.dllearner.aml;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import org.dllearner.utilities.owl.OWLClassExpressionUtils;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLDataFactory;
//import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
//
//import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
//
///**
// * @author Yingbing Hua, yingbing.hua@kit.edu
// *
// */
//public class OWLCLassExpressionUtilsExt extends org.dllearner.utilities.owl.OWLClassExpressionUtils {
//	
//	private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();
//	
//	public static Set<SimpleTreeNode<OWLClassExpression>> toTree(OWLClassExpression expr){
//		
//		OWLTreePatternNode root = new OWLTreePatternNode(expr);
//		
//		SimpleTreeNode<OWLClassExpression> root = new SimpleTreeNode<OWLClassExpression>();
//		
//		if(getChildren(expr).isEmpty())
//			root.data = expr;		
//		else {
//				
//			// if the expression has disjunctions
//			if(expr.asDisjunctSet().size() > 1) {
//				Set<OWLClassExpression> disjunctions = expr.asDisjunctSet();
//				
//			}
//			
//			// if the expression has conjunctions
//			if(expr.asConjunctSet().size()>1) {
//				
//				OWLDataSomeValuesFrom svf = dataFactory.getOWLDataSomeValuesFrom(null, null);
//			}
//		}
//		
//		Set<SimpleTreeNode<OWLClassExpression>> trees = new HashSet<SimpleTreeNode<OWLClassExpression>>();
//		trees.add(root);
//		return trees;
//	}
//
//
//	   public static void tokenize(OWLClassExpression expr) {
//			Set<OWLClassExpression> disjunctions = expr.asDisjunctSet();
//			for(OWLClassExpression disjunction : disjunctions) {
//				Set<OWLClassExpression> conjunctions = disjunction.asConjunctSet();
//				for(OWLClassExpression conjunction : conjunctions) {
//					Set<OWLClassExpression> terms = conjunction.getNestedClassExpressions();
//					for(OWLClassExpression term : terms) {
//						System.out.println(term);
//					}
//				}
//			}	
//	    }
//	    
//	    public static void tokenizeRec(OWLClassExpression expr) {
//	    		Set<OWLClassExpression> set = OWLClassExpressionUtils.getChildren(expr);
//	    		for(OWLClassExpression item : set) {
//	    			if(!getChildren(item).isEmpty()) {
//	    				tokenizeRec(item);
//	    			}
//	    		}
//	    }
//}
