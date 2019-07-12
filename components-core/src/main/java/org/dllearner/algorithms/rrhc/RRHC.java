/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dllearner.algorithms.rrhc;

import org.dllearner.core.*;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.utilities.*;
import org.dllearner.utilities.owl.*;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * The RRHC learning algorithm for learning description logic concepts
 * It uses the basic CELOE heuristic (OEHeuristicRuntime)
 * and a rapid restart hill climbing search for next node selection 
 * 
 * @author Yingbing Hua
 *
 */
@SuppressWarnings("CloneDoesntCallSuperClone")
@ComponentAnn(name="RRHC", shortName="rrhc", version=1.0, description="Rapid Restart Hill Climbing for Learning Description Logic Concepts")
public class RRHC extends CELOEBase implements Cloneable{

	private static final Logger logger = LoggerFactory.getLogger(RRHC.class);

	@ConfigOption(defaultValue="celoe_heuristic")
	protected LayerwiseAbstractHeuristic heuristic;
	protected LayerwiseSearchTree<LayerwiseSearchTreeNode> searchTree;
	
	
	/**
	 * ------------------------------------------------------------------------------------------------------------------------------
	 * Layer-wise tree traverse parameters 
	 */	
	@ConfigOption(defaultValue="0", description="each refinement must introduce at least so many new nodes into the tree")
	protected int newNodesLowerbound = 0;
	
	@ConfigOption(defaultValue="0", description="break the traverse if the score increase smaller than this threshold")
	protected double traverseBreakingThreshold = 0;
	
	List<LayerwiseSearchTreeNode> selectedNodes = new ArrayList<LayerwiseSearchTreeNode>();
	LayerwiseSearchTreeNode currentNode = null;
	
	/**
	 * --------------------------------------------------------------------------------------------------------------------------------
	 */
	
	private boolean aborted = false;

	public RRHC() {}

	public RRHC(RRHC rrhc){
		super(rrhc);
		setHeuristic(rrhc.heuristic);
	}

	public RRHC(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
	}
	
	@Override
	public void init() throws ComponentInitException {
		
		
		// if no one injected a heuristic, we use a default one
		if(heuristic == null) {
			heuristic = new LayerwiseOEHeuristicRuntime();
			heuristic.init();
		}
		
		super.init();
		initialized = true;
	}
	
	@Override
	public void start() {
		stop = false;
		isRunning = true;
		reset();
		nanoStartTime = System.nanoTime();
		
		currentHighestAccuracy = 0.0;
		LayerwiseSearchTreeNode nextNode;

		String s = "\nCurrent config: \n";
		s += " - logging to: " + logFile + "\n";
		s += " - heStep: " + heStep + "\n";
		s += " - heCorr: " + heCorrection + "\n";
		s += this.heuristic.toString();
		s += " - acc: " + this.learningProblem.getAccuracyMethod().getClass().getSimpleName() + "\n";
		s += " - breaking: " + this.traverseBreakingThreshold + "\n";
		s += " - start: " + startClass + "\n";
		try {
			saveLog(s, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		LayerwiseSearchTreeNode root = addNode(startClass, null);
 		
 		if(root == null) {
 			logger.error("The start class specified by the user is invalid: it does not cover all positive examples");
 			this.stop();
 			aborted = true;
 		}
 		
 		if (!aborted && writeSearchTree) {
			writeSearchTree(root);
		}
 		currentNode = root;
		
 		long runtimeLastRec = getCurrentRuntimeInMilliSeconds();
 		
		while (!terminationCriteriaSatisfied()) {
		
	 		countIterations++;
			if(getCurrentRuntimeInMilliSeconds() - runtimeLastRec > 60000) {
				runtimeLastRec = getCurrentRuntimeInMilliSeconds();
				System.out.println(runtimeLastRec/60000 + " minutes passed...");
			}
			showIfBetterSolutionsFound();

			// chose best node according to heuristics
			long selectStart = System.nanoTime();
			nextNode = getNextNodeToExpand();
			selectedNodes.add(nextNode);

			long selectEnd = System.nanoTime();
			selectTime += (selectEnd - selectStart);
			
			int horizExp = nextNode.getHorizontalExpansion();
			
			// apply refinement operator
			int newNodes = 0;
			double newBestScore = Double.MIN_VALUE;
			boolean expandAgain = true;
			while(expandAgain) {				
				 // if newNodesLowerbound = 0: we want to do one refinement only
				if(this.newNodesLowerbound == 0) {
					expandAgain = false;
				}
				
				TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
//				System.out.println("refining node: " + nextNode.toString());
				while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
					// pick element from set
					OWLClassExpression refinement = refinements.pollFirst();					

					// get length of class expression
					int length = OWLClassExpressionUtils.getLength(refinement);
					
					// we ignore all refinements with lower length and too high depth
					// (this also avoids duplicate node children)
					if(heCorrection && length > horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
						// add node to search tree
						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
						if(newNode != null) {
//							newNodes.add(newNode);
							newNodes++;
							if(newBestScore < newNode.getCeloeScore())
								newBestScore = newNode.getCeloeScore();
						}
					}
					if(!heCorrection && length >= horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
						if(newNode != null) {
//							newNodes.add(newNode);
							newNodes++;
							if(newBestScore < newNode.getCeloeScore())
								newBestScore = newNode.getCeloeScore();
						}
					}
				}
				// if newNodesLowerbound > 0: we want to do so many refinements until we have so enough new nodes
				if(expandAgain && newNodes >= this.newNodesLowerbound)
					expandAgain = false;
			}
			

			if (writeSearchTree) {
				writeSearchTree(nextNode);
			}
			
			showIfBetterSolutionsFound();						
		}
		
		if(!aborted && singleSuggestionMode) {
			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
		}
						
		if(!aborted) {
			// print some stats
			printAlgorithmRunStats(searchTree.size());
			
			logger.info("writing tree strcuture and selected nodes to log file.");
			
			// print tree structure
			printTreeStructure();
			
			// print node ids:
			printSelectedNodes(false);
			
			// print retrieval details:
			printRetrievalDetails();
			
			try {
				logWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}					
		}
		
		isRunning = false;
		
	}
	
	protected void printSelectedNodes(boolean verbose) {
		try {
			saveLog("\nselected nodes in each iteration:", false);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(LayerwiseSearchTreeNode node : selectedNodes) {
			String s = "";
			if(verbose)
				s = node.getId() + " : " + node.toString();
			else
				s = node.getId();
			
			try {
				saveLog(s, false);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}			
	}
	
	private void printTreeStructure() {		
		try {
			saveLog("tree structure: " + searchTree.getDepth(), false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<LayerwiseSearchTreeNode> nodes = new ArrayList<LayerwiseSearchTreeNode>();
		nodes.add(searchTree.getRoot());
		printTreeStructure(nodes);
	}
	
	private void printTreeStructure(List<LayerwiseSearchTreeNode> nodesOnLevel) {
		if(nodesOnLevel.isEmpty())
			return;
		try {
			saveLog(String.valueOf(nodesOnLevel.size()), false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<LayerwiseSearchTreeNode> nextLevel = new ArrayList<LayerwiseSearchTreeNode>();
		for(LayerwiseSearchTreeNode node : nodesOnLevel) {
			nextLevel.addAll(node.getChildren());			
		}
		printTreeStructure(nextLevel);
	}
	
	
	private double getCeloeScore (LayerwiseSearchTreeNode node) {
		return heuristic.getNodeScore(node);
	}
	
	/**
	 * compute the decidability of celoe based on the score of the 
	 * @param iterator
	 * @param sorted
	 */
	protected void getCELOEDecidability (Iterator<LayerwiseSearchTreeNode> iterator, boolean sorted) {

		if(!sorted)
			return;
		
		double highest_score = Double.MIN_VALUE;
		if(iterator.hasNext()) {
			highest_score = getCeloeScore(iterator.next());
		}
			
		
		int nrBestNodes = 1;
		while(iterator.hasNext()) {
			// Only look at the nodes with the highest CELOE score
			double score = getCeloeScore(iterator.next());			
			if (score < highest_score) {
				break;
			}
			nrBestNodes++;			
		}

		if (nrBestNodes == 1) {
			timesCeloeDecided++;
		} 
		else {
			timesCeloeUndecided++;
		}
	}
	
	private LayerwiseSearchTreeNode getNextNodeToExpand() {
	
		 /* Traverse the tree from root according to UCT */            
        while (!currentNode.getChildren().isEmpty()) {
        	
        		LayerwiseSearchTreeNode child = currentNode.descendingIterator().next();

        		// if the current node is better than all its children
        		// then select the current node               	
        		double currentScore = getCeloeScore(currentNode);
        		double childScore = getCeloeScore(child);
        		
        		if (childScore - currentScore <=  traverseBreakingThreshold) {
        			break;
        		}            		
        		
        		// otherwise, go to the next level 
        		currentNode = child;
        }
        return currentNode;
	}
	
	// expand node horizontically
	protected TreeSet<OWLClassExpression> refineNode(LayerwiseSearchTreeNode node) {
		
		long refineStart = System.nanoTime();
		
		// in case of normal celoe, whose score depends on the horizontal expansion
		// we have to remove and add the node since its heuristic evaluation changes through the expansion
		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
		// otherwise you may see rarely occurring but critical false ordering in the nodes set)

		// remove the node from the tree
		searchTree.updatePrepare(node);
		
		int horizExp = node.getHorizontalExpansion();
				
		TreeSet<OWLClassExpression> refinements = new TreeSet<OWLClassExpression>();
		if(heCorrection)
			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1+heStep);
		else
			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+heStep);
		
		// increase the he for next loop
		// if we had a heStep>0, we need to increase the he more than once
		for(int i = 0; i <= heStep; i++) {
			node.incHorizontalExpansion();
		}		
		
		node.setRefinementCount(refinements.size());
		
		// re add the node to the tree
		searchTree.updateDone(node);
		
		long refineEnd = System.nanoTime();
		refineTime += (refineEnd - refineStart);
		
		getCeloeScore(node);
		
		// if node is not root, then take its parent as a start node for next iteration
		// otherwise, use the root node again
		if(!node.isRoot())
			currentNode = node.getParent();
		return refinements;
	}
	
	
	/**
	 * Add node to search tree if it is not too weak.
	 * @return TRUE if node was added and FALSE otherwise
	 */
	protected LayerwiseSearchTreeNode addNode(OWLClassExpression description, LayerwiseSearchTreeNode parentNode) {
		
//		System.out.println("adding node: first checking allowed");
		// redundancy check (return if redundant)
		boolean nonRedundant = descriptions.add(description);
		if(!nonRedundant) {
			return null;
		}
		
		// check whether the class expression is allowed
		if(parentNode == null) {
			if(!isDescriptionAllowed(description, null)) {				
				return null;
			}					
		}
		else {
			if(!isDescriptionAllowed(description, parentNode.getExpression())) {
				return null;
			}
		}
		
//		if(learningProblem instanceof PosNegLP) {
//			OWLClassExpressionDisjunctionCounter counter = new OWLClassExpressionDisjunctionCounter();
//			int numPos = ((PosNegLP) learningProblem).getPositiveExamples().size();
//			int numDisj = counter.getNumDisjunctions(description);
//			
//			System.out.println(numPos + ", " + numDisj);
//		}
		
		// Disallow descriptions with unnecessary disjunctions
		if(learningProblem instanceof PosNegLP) {
			
			OWLClassExpressionDisjunctionCounter counter = new OWLClassExpressionDisjunctionCounter();
			int numPos = ((PosNegLP) learningProblem).getPositiveExamples().size();
			int numDisj = counter.getNumDisjunctions(description);
			
//			System.out.println("\ntesting " + description + ": " + numDisj + ", " + numPos);
			
			// if the description has more disjunctions than necessary, i.e. number of positive examples
			if(numDisj > numPos-1) {
//				System.out.println("[" + description + "] disallowed");
				return null;
			}
			
			if(parentNode != null) {
				double parentAccuracy = learningProblem.getAccuracyOrTooWeak(parentNode.getExpression(), noise);
//				System.out.println("parent " + parentNode.getExpression() + ": " + parentAccuracy);
				// if the parent is already correct (1.0 accuracy), then the new description shall not use more disjunctions
				if(parentAccuracy == 1.0) {
					int numDisjParent = counter.getNumDisjunctions(parentNode.getExpression());
					if(numDisj > numDisjParent) {
//						System.out.println("[" + description + "] disallowed");
						return null;
					}					
				}	
			}			
		}		
		
		// quality of class expression (return if too weak)
		double accuracy = learningProblem.getAccuracyOrTooWeak(description, noise);
		
		// issue a warning if accuracy is not between 0 and 1 or -1 (too weak)
		if(accuracy > 1.0 || (accuracy < 0.0 && accuracy != -1)) {
			throw new RuntimeException("Invalid accuracy value " + accuracy + " for class expression " + description +
					". This could be caused by a bug in the heuristic measure and should be reported to the DL-Learner bug tracker.");
		}
		
		expressionTests++;
		
		// return FALSE if 'too weak'
		if(accuracy == -1) {
			return null;
		}
		
		long treeStart = System.nanoTime();
		LayerwiseSearchTreeNode node = new LayerwiseSearchTreeNode(description, accuracy, heCorrection, heuristic);
		searchTree.addNode(parentNode, node);
//		getCeloeScore(node);
			
		long treeEnd = System.nanoTime();
		treeTime += (treeEnd - treeStart);
		
		// in some cases (e.g. mutation) fully evaluating even a single class expression is too expensive
		// due to the high number of examples -- so we just stick to the approximate accuracy
		if(singleSuggestionMode) {
			if(accuracy > bestAccuracy) {
				bestAccuracy = accuracy;
				bestDescription = description;
				logger.info("more accurate (" + dfPercent.format(bestAccuracy) + ") class expression found: " + descriptionToString(bestDescription)); // + getTemporaryString(bestDescription));
			}
			return node;
		}
		
		// maybe add to best descriptions (method keeps set size fixed);
		// we need to make sure that this does not get called more often than
		// necessary since rewriting is expensive
		boolean isCandidate = !bestEvaluatedDescriptions.isFull();
		if(!isCandidate) {
			EvaluatedDescription<? extends Score> worst = bestEvaluatedDescriptions.getWorst();
			double accThreshold = worst.getAccuracy();
			isCandidate =
				(accuracy > accThreshold ||
				(accuracy >= accThreshold && OWLClassExpressionUtils.getLength(description) < worst.getDescriptionLength()));
		}
		
		long rewriteStart = System.nanoTime();
//		if(isCandidate) {
//			//TODO: rewrite forte/uncle_small will produce "male and married only nothing" from "male and married max 0 thing"
//			//      and the reasoner will have different instance retrieval results based on this
//			System.out.println("node: " + node);
//			OWLClassExpression niceDescription = rewrite(node.getExpression());
////			rewriteCount++;
////			System.out.println("nice: " + niceDescription);
//
//			if(niceDescription.equals(classToDescribe)) {
//				return null;
//			}
//			
//			if(!isDescriptionAllowed(niceDescription, node.getExpression())) {
//				return null;
//			}
//			
//			// another test: none of the other suggested descriptions should be
//			// a subdescription of this one unless accuracy is different
//			// => comment: on the one hand, this appears to be too strict, because once A is a solution then everything containing
//			// A is not a candidate; on the other hand this suppresses many meaningless extensions of A
//			boolean shorterDescriptionExists = false;
//			if(forceMutualDifference) {
//				for(EvaluatedDescription<? extends Score> ed : bestEvaluatedDescriptions.getSet()) {
//					if(Math.abs(ed.getAccuracy()-accuracy) <= 0.00001 && ConceptTransformation.isSubdescription(niceDescription, ed.getDescription())) {
//						shorterDescriptionExists = true;
//						break;
//					}
//				}
//			}
//			
//			if(!shorterDescriptionExists) {
//				if(!filterFollowsFromKB || !((ClassLearningProblem)learningProblem).followsFromKB(niceDescription)) {
//					bestEvaluatedDescriptions.add(niceDescription, accuracy, learningProblem);
//				}
//			}
//		}
		long rewriteEnd = System.nanoTime();		
		rewriteTime += (rewriteEnd - rewriteStart);
		
		bestEvaluatedDescriptions.add(node.getExpression(), accuracy, learningProblem);
		return node;
	}
	
	protected void reset() {
		// set all values back to their default values (used for running
		// the algorithm more than once)
		searchTree = new LayerwiseSearchTree<>(heuristic);
		super.reset();
	}
	
	
	private StringBuilder nodeToString (LayerwiseSearchTreeNode node) {
		StringBuilder treeString = new StringBuilder();
		String nodeStr = node.toString();
		nodeStr = nodeStr.substring(0, nodeStr.length()-1);
		treeString.append(nodeStr);
//		treeString.append(", celoe:");
//		treeString.append(getCeloeScore(node));
		treeString.append("]\n");
		
		return treeString;
	}
	
	private StringBuilder toTreeString(LayerwiseSearchTreeNode node, int depth) {
		StringBuilder treeString = new StringBuilder();
		for (int i = 0; i < depth - 1; i++) {
			treeString.append("  ");
		}
		if (depth != 0) {
			treeString.append("|--> ");
		}
		
		treeString.append(nodeToString(node));

		for (LayerwiseSearchTreeNode child : node.getChildren()) {
			treeString.append(toTreeString(child, depth+1));
		}
		return treeString;
	}
	
	protected void writeSearchTree() {
		StringBuilder treeString = toTreeString(searchTree.getRoot(), 0);
		treeString.append("\n");

		// replace or append
		if (isReplaceSearchTree()) {
			Files.createFile(new File(getSearchTreeFile()), treeString.toString());
		} else {
			Files.appendToFile(new File(getSearchTreeFile()), treeString.toString());
		}
	}
	
	protected void writeSearchTree(LayerwiseSearchTreeNode selected) {
		long logStart = System.nanoTime();
		StringBuilder treeString = new StringBuilder("------------- Iteration " + countIterations + " -------------\n");
		treeString.append("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
		treeString.append("selected node: " + nodeToString(selected));
		treeString.append(toTreeString(searchTree.getRoot(), 0));
		treeString.append("\n");

		// replace or append
		if (isReplaceSearchTree()) {
			Files.createFile(new File(getSearchTreeFile()), treeString.toString());
		} else {
			Files.appendToFile(new File(getSearchTreeFile()), treeString.toString());
		}
		
		long logEnd = System.nanoTime();
		logTime += (logEnd - logStart);
	}

	public LayerwiseAbstractHeuristic getHeuristic() {
		return heuristic;
	}

	@Autowired(required=false)
	public void setHeuristic(LayerwiseAbstractHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	/**
	 * @return the newNodesLowerbound
	 */
	public int getNewNodesLowerbound() {
		return newNodesLowerbound;
	}

	/**
	 * @param newNodesLowerbound the newNodesLowerbound to set
	 */
	public void setNewNodesLowerbound(int newNodesLowerbound) {
		this.newNodesLowerbound = newNodesLowerbound;
	}

	/**
	 * @return the traverseBreakingThreshold
	 */
	public double getTraverseBreakingThreshold() {
		return traverseBreakingThreshold;
	}

	/**
	 * @param traverseBreakingThreshold the traverseBreakingThreshold to set
	 */
	public void setTraverseBreakingThreshold(double traverseBreakingThreshold) {
		this.traverseBreakingThreshold = traverseBreakingThreshold;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new RRHC(this);
	}	
}