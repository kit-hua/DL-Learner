/**
 * 
 */
package org.dllearner.aml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dllearner.utilities.owl.OWLClassExpressionChildrenCollector;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.HasFiller;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.vocab.OWLFacet;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 * an OWL tree is a tree without any disjunctions
 * negations and data properties are threated as atomic concepts
 */
public class OWLTree {
//	private SimpleTreeNode<OWLClassExpression> root;
	private OWLTreeNode root;
		
	public OWLTree() {
//		 root = new SimpleTreeNode<OWLClassExpression>();
		root = new OWLTreeNode();
	}
	
//	public OWLTree(SimpleTreeNode<OWLClassExpression> root) {
	public OWLTree(OWLTreeNode root) {
		this.root = root;
	}
	
	public OWLTree(OWLClassExpression ce) {		
//		this.root = new SimpleTreeNode<OWLClassExpression> (ce);
		this.root = new OWLTreeNode(ce);
	}
	
	/**
	 * Copy constructor
	 * @param other the other tree to be copied
	 */
	public OWLTree(OWLTree other) {
//		this.root = new SimpleTreeNode<OWLClassExpression>(other.root);
		this.root = new OWLTreeNode(other.root);
		copyChildrenRec(root, other.root);
	}		
	
	/**
	 * copy the children of the source tree node to the current node
	 * @param node current node
	 * @param source source node
	 */
	private void copyChildrenRec(SimpleTreeNode<OWLClassExpression> node, SimpleTreeNode<OWLClassExpression> source) {
		// for each child of the source node, make a copy, and add the copy as a child to the current node
		// do it recursively for the children of the child of the source node
		for(SimpleTreeNode<OWLClassExpression> sourceChild : source.getChildren()) {
			SimpleTreeNode<OWLClassExpression> childNodeCopy = new SimpleTreeNode<OWLClassExpression>(sourceChild);
			copyChildrenRec(childNodeCopy,  sourceChild);
			node.addChild(childNodeCopy);
		}		
	}
	
	/**
	 * Generate tree patterns for owl class expressions: start from a tree with a single root, expand it to a set of trees
	 * - case disjunction: make duplicates of the tree for each operand 
	 * - case conjunction: expand each of the operands 
	 * - case filler: expand the filler
	 * @return
	 */
	public Set<OWLTree> expand() {
		
		Set<OWLTree> trees = new HashSet<OWLTree>();
		
		if(this.root.data == null) {
			trees.add(this);
		}
		
		if(!OWLCLassExpressionUtils2.isExpandable(this.root.data)) {
			trees.add(this);
		}
		
		if(OWLCLassExpressionUtils2.isDisjunctive(this.root.data)) {
			OWLObjectUnionOf disjunctions = (OWLObjectUnionOf) this.root.data;
			for(OWLClassExpression op : disjunctions.getOperands()) {
				OWLTree opTree = new OWLTree(op);
				Set<OWLTree> opTreeExpanded = opTree.expand();
				trees.addAll(opTreeExpanded);
			}
		}
		
		if(OWLCLassExpressionUtils2.isConjunctive(this.root.data)) {
			
			// maintain a set of trees in case the operands expand to multiple trees
			// then we need to muliplex them
			Set<OWLTree> clones = new HashSet<OWLTree>(); 			
			// we add the current tree to it
			clones.add(this);				
			
			OWLObjectIntersectionOf conjunctions = (OWLObjectIntersectionOf) this.root.data;
			for(OWLClassExpression op : conjunctions.getOperands()) {
				
				OWLTree opTree = new OWLTree(op);								
				Set<OWLTree> opTreeExpanded = opTree.expand();
				
				// no disjunction in the operand
				if(opTreeExpanded.size() == 1) {
//					for(OWLTree expanded : opTreeExpanded) {
//						opTree.root.addChild(expanded.root);
//					}
					
					// for each tree we need to add the child: we might have several already
//					for(OWLTree clone : clones) {
					for(Iterator<OWLTree> iterator = clones.iterator(); iterator.hasNext();) {
						OWLTree clone = iterator.next();
						clone.root.addChild(opTree.root);
					}
				}
				else { //>1
					
					// we have disjunctions somewhere, so that the expanded opTree has size > 1
					// clone the current tree with number of disjunctions
					// that is, size of the opTreeExpanded
//					for(OWLTree clone : clones) {
					Set<OWLTree> extendedClones = new HashSet<OWLTree>();
					for(Iterator<OWLTree> iterator = clones.iterator(); iterator.hasNext();) {
						OWLTree clone = iterator.next();
					
						Set<OWLTree> newClones = new HashSet<OWLTree>();
						
						// we only clone each existing tree n-1 times (MULTIPLEXING), since
						// say we have 2 disjunctions, and originally have 2 trees A and B
						// then we need 4 trees after clone
						// if we clone A twice and B twice, then we have A + A1 + A2 + B + B1 + B2: 6 trees
						for(int i = 1; i < opTreeExpanded.size(); i++) {
							OWLTree newClone = new OWLTree(clone);
							newClones.add(newClone);
						}
						
						// add the expanded opTree but not the opTree itself
						// e.g. for (A or B) and C, we want at the end two trees:
						// - (A or B) and C 
						//   - A
						//   - C
						// - (A or B) and C
						//   - B
						//   - C
						// we do not want any disjunction in the tree, e.g.
						// - (A or B) and C
						//   - A or B
						//     - A
						//   - C
						Iterator<OWLTree> iteratorExpanded = opTreeExpanded.iterator();						
						clone.root.addChild(iteratorExpanded.next().root);
						for(OWLTree newClone : newClones) {
							// we do not need to concern about the range
							// since it is synchronized implicitly with the previous addchild and "i=1"
							newClone.root.addChild(iteratorExpanded.next().root);
						}
						
						extendedClones.addAll(newClones);
					}
					clones.addAll(extendedClones);
						
				}
			}//end for each operand in conjunction
			trees.addAll(clones);
		} // end conjunctions
			
		if(OWLCLassExpressionUtils2.isFillerExpandable(this.root.data)) {
			// maintain a set of trees in case the operands expand to multiple trees
			// then we need to muliplex them
			Set<OWLTree> clones = new HashSet<OWLTree>(); 			
			// we add the current tree to it
			clones.add(this);				
			
			OWLQuantifiedObjectRestriction restriction = (OWLQuantifiedObjectRestriction) this.root.data;
			OWLClassExpression filler = restriction.getFiller();
			OWLTree opTree = new OWLTree(filler);
			
			Set<OWLTree> opTreeExpanded = opTree.expand();
			// no disjunction in the operand
			if(opTreeExpanded.size() == 1) {
//				for(OWLTree expanded : opTreeExpanded) {
//					opTree.root.addChild(expanded.root);
//				}
				
				// for each tree we need to add the child: we might have several already
//				for(OWLTree clone : clones) {
				for(Iterator<OWLTree> iterator = clones.iterator(); iterator.hasNext();) {
					OWLTree clone = iterator.next();
					clone.root.addChild(opTree.root);
				}
			}
			else { //>1
				
				// we have disjunctions somewhere, so that the expanded opTree has size > 1
				// clone the current tree with number of disjunctions
				// that is, size of the opTreeExpanded
//				for(OWLTree clone : clones) {			
				Set<OWLTree> extendedClones = new HashSet<OWLTree>();
				for(Iterator<OWLTree> iterator = clones.iterator(); iterator.hasNext();) {
					OWLTree clone = iterator.next();
					Set<OWLTree> newClones = new HashSet<OWLTree>();
					
					// we only clone each existing tree n-1 times (MULTIPLEXING), since
					// say we have 2 disjunctions, and originally have 2 trees A and B
					// then we need 4 trees after clone
					// if we clone A twice and B twice, then we have A + A1 + A2 + B + B1 + B2: 6 trees
					for(int i = 1; i < opTreeExpanded.size(); i++) {
						OWLTree newClone = new OWLTree(clone);
						newClones.add(newClone);
					}
					
					// add the expanded opTree but not the opTree itself
					// e.g. for (A or B) and C, we want at the end two trees:
					// - (A or B) and C 
					//   - A
					//   - C
					// - (A or B) and C
					//   - B
					//   - C
					// we do not want any disjunction in the tree, e.g.
					// - (A or B) and C
					//   - A or B
					//     - A
					//   - C
					Iterator<OWLTree> iteratorExpanded = opTreeExpanded.iterator();						
					clone.root.addChild(iteratorExpanded.next().root);
					for(OWLTree newClone : newClones) {
						// we do not need to concern about the range
						// since it is synchronized implicitly with the previous addchild and "i=1"
						newClone.root.addChild(iteratorExpanded.next().root);
					}
					extendedClones.addAll(newClones);					
				}
				clones.addAll(extendedClones);
			}
			trees.addAll(clones);
		}
		
		return trees;
	}
	

	
	public static void main(String[] args) {
		OWLDataFactory dataFactory = new OWLDataFactoryImpl();
		OWLClass A = dataFactory.getOWLClass(IRI.create("A"));
		OWLClass B = dataFactory.getOWLClass(IRI.create("B"));
		OWLClass C = dataFactory.getOWLClass(IRI.create("C"));
		OWLClass D = dataFactory.getOWLClass(IRI.create("D"));
		OWLClass E = dataFactory.getOWLClass(IRI.create("E"));
		OWLClass Robot = dataFactory.getOWLClass(IRI.create("Robot"));
		OWLClass Controller = dataFactory.getOWLClass(IRI.create("Controller"));
		OWLClass Joint = dataFactory.getOWLClass(IRI.create("Joint"));
		OWLClass Motor = dataFactory.getOWLClass(IRI.create("Motor"));
		
		OWLObjectProperty r = dataFactory.getOWLObjectProperty(IRI.create("r"));
		OWLDataProperty price = dataFactory.getOWLDataProperty(IRI.create("hasPrice"));
		OWLObjectProperty ie = dataFactory.getOWLObjectProperty(IRI.create("hasIE"));
		OWLDataProperty weight = dataFactory.getOWLDataProperty(IRI.create("hasWeight"));
		OWLDataProperty axis = dataFactory.getOWLDataProperty(IRI.create("hasAxis"));
		OWLDataProperty vel = dataFactory.getOWLDataProperty(IRI.create("hasVel"));
		
		OWLClassExpression rE = dataFactory.getOWLObjectSomeValuesFrom(r, E);
//		OWLClassExpression arE = dataFactory.getOWLObjectIntersectionOf(dataFactory.getOWLClass(IRI.create("A")), rE);
		OWLClassExpression arE = dataFactory.getOWLObjectUnionOf(dataFactory.getOWLClass(IRI.create("A")), rE);
		
		OWLDataRange doubleLe30 = dataFactory.getOWLDatatypeRestriction(dataFactory.getDoubleOWLDatatype(), OWLFacet.MAX_INCLUSIVE, dataFactory.getOWLLiteral(30));
		OWLClassExpression hasPrice = dataFactory.getOWLDataSomeValuesFrom(price, doubleLe30);
		
		OWLDataRange doubleLe10 = dataFactory.getOWLDatatypeRestriction(dataFactory.getDoubleOWLDatatype(), OWLFacet.MAX_INCLUSIVE, dataFactory.getOWLLiteral(10));
		OWLClassExpression hasWeight = dataFactory.getOWLDataSomeValuesFrom(weight, doubleLe10);
		OWLClassExpression hasController = dataFactory.getOWLObjectSomeValuesFrom(ie, dataFactory.getOWLObjectIntersectionOf(Controller, hasWeight));
		
		OWLClassExpression hasAxis = dataFactory.getOWLDataHasValue(axis, dataFactory.getOWLLiteral(6));
		
		OWLDataRange doubleGr10 = dataFactory.getOWLDatatypeRestriction(dataFactory.getDoubleOWLDatatype(), OWLFacet.MIN_INCLUSIVE, dataFactory.getOWLLiteral(10));
		OWLClassExpression hasVel = dataFactory.getOWLDataSomeValuesFrom(vel, doubleGr10);
	
		OWLClassExpression hasMotor = dataFactory.getOWLObjectSomeValuesFrom(ie, dataFactory.getOWLObjectIntersectionOf(Motor, hasVel));
		
		OWLClassExpression hasJoint = dataFactory.getOWLObjectIntersectionOf(Joint, hasMotor);
		
		OWLClassExpression hasRobot = dataFactory.getOWLObjectIntersectionOf(Robot, hasAxis, hasJoint);
		
//		OWLClassExpression b = dataFactory.getOWLObjectIntersectionOf(hasPrice, hasController);
		OWLClassExpression b = dataFactory.getOWLObjectUnionOf(hasPrice, hasController, hasRobot);
		
//		OWLClassExpression a = dataFactory.getOWLObjectUnionOf(arE, b);
		OWLClassExpression a = dataFactory.getOWLObjectIntersectionOf(arE, b);
		
		OWLClassExpression abcd = dataFactory.getOWLObjectIntersectionOf(dataFactory.getOWLObjectUnionOf(A,B), dataFactory.getOWLObjectUnionOf(C,D));				
		
		OWLTree tree = new OWLTree(a);
		
		Set<OWLTree> trees = tree.expand();				
		int i = 1;
		for(OWLTree tr : trees) {
			System.out.println("\ntree " + (i++) + ": \n" + tr.root.toString());
		}
	}
	
}

















