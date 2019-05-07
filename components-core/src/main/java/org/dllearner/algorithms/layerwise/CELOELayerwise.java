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
package org.dllearner.algorithms.layerwise;

import com.google.common.collect.Sets;

import org.dllearner.accuracymethods.AccMethodPredAcc;
import org.dllearner.accuracymethods.AccMethodPredAccNegOnly;
import org.dllearner.algorithms.celoe.OEHeuristicRuntime;
import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.*;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.owl.ClassHierarchy;
import org.dllearner.core.owl.DatatypePropertyHierarchy;
import org.dllearner.core.owl.ObjectPropertyHierarchy;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.learningproblems.ClassAsInstanceLearningProblem;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.dllearner.reasoning.ReasonerImplementation;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.refinementoperators.*;
import org.dllearner.utilities.*;
import org.dllearner.utilities.datastructures.SearchTree;
import org.dllearner.utilities.owl.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The CELOE (Class Expression Learner for Ontology Engineering) algorithm.
 * It adapts and extends the standard supervised learning algorithm for the
 * ontology engineering use case.
 * 
 * @author Jens Lehmann
 *
 */
@SuppressWarnings("CloneDoesntCallSuperClone")
@ComponentAnn(name="CELOELayerwise", shortName="celoe_lw", version=1.0, description="CELOELayerwise is an adapted and extended version of the CELOE algorithm")
public class CELOELayerwise extends CELOEBase implements Cloneable{

	private static final Logger logger = LoggerFactory.getLogger(CELOELayerwise.class);

	@ConfigOption(defaultValue="celoe_heuristic")
	protected LayerwiseAbstractHeuristic heuristic;
//	protected AbstractHeuristic heuristic;
	
//	protected SearchTree<OENode> searchTree;
	protected LayerwiseSearchTree<LayerwiseSearchTreeNode> searchTree;
	
	
	/**
	 * ------------------------------------------------------------------------------------------------------------------------------
	 * Layer-wise tree traverse parameters 
	 */	
	@ConfigOption(defaultValue="false", description="each iteration will traverse the complete tree to find the next node instead of picking the global best one")
	protected boolean traverseWholeTree = false;
	
	@ConfigOption(defaultValue="0", description="each refinement must introduce at least so many new nodes into the tree")
	protected int newNodesLowerbound = 0;
	
	@ConfigOption(defaultValue="0.01", description="break the traverse if the score increase smaller than this threshold")
	protected double traverseBreakingThreshold = 0.01;
	
	/**
	 * --------------------------------------------------------------------------------------------------------------------------------
	 */


	public CELOELayerwise() {}

	public CELOELayerwise(CELOELayerwise celoe){
		super(celoe);
		setHeuristic(celoe.heuristic);
	}

	public CELOELayerwise(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
	}
	
	@Override
	public void init() throws ComponentInitException {
		
		/*
		 * Batch config settings
		 * original celoe: heStep = 0, heCorr = true
		 */
		heStep = 1;
		heCorr = false;
		
		// if no one injected a heuristic, we use a default one
		if(heuristic == null) {
			heuristic = new LayerwiseOEHeuristicRuntime();
			heuristic.init();
		}
		
		super.init();
		
		String exampleFileName = searchTreeFile.substring(searchTreeFile.lastIndexOf("/"));
		String resultPath = getLogPath();
		String exampleName = exampleFileName.substring(1, exampleFileName.lastIndexOf("."));
		
		String heu = "";
		String penalty = "";
		String acc = "";
		String tr = "";
		
		if(this.heuristic.getClass().getName().contains("OEHeuristic")){
			heu = "celoe";
			penalty = String.valueOf(((LayerwiseOEHeuristicRuntime) this.heuristic).getExpansionPenaltyFactor());
		}
		else if(this.heuristic.getClass().getName().contains("UCT")) {
			heu = "uct";
			penalty = String.valueOf(((UCTHeuristic) this.heuristic).getExpansionPenaltyFactor());
		}
		else {
//			heu = this.heuristic.getClass().getSimpleName();
		}
			
		
		if(this.learningProblem.getAccuracyMethod() instanceof AccMethodPredAcc) {
			acc = "pre";
		}
		else if (this.learningProblem.getAccuracyMethod() instanceof AccMethodPredAccNegOnly) {
			acc = "neg";
		}
		else {
//			acc = this.learningProblem.getAccuracyMethod().getClass().getSimpleName();
		}
		
		if(this.traverseWholeTree) {
			tr = "tra";
			tr += "_" + String.valueOf(this.traverseBreakingThreshold);
		}			
		else {
			tr = "glo";
		}
		
		
		if(!heu.isEmpty())
			exampleName += "_" + heu;
		
		if(!penalty.isEmpty())
			exampleName += "_" + penalty;
		
		if(!acc.isEmpty())
			exampleName += "_" + acc;
		
		if(!tr.isEmpty())
			exampleName += "_" + tr;		
		
		
		logFile =  resultPath + exampleName + ".log";
		searchTreeFile = resultPath + exampleName + ".tree";
		File log = new File(logFile);	
		if (log.getParentFile() != null) {
			log.getAbsoluteFile().getParentFile().mkdirs();
		}
		Files.clearFile(log);
		if (writeSearchTree) {						
			File f = new File(searchTreeFile);	
			if (f.getParentFile() != null) {
				f.getAbsoluteFile().getParentFile().mkdirs();
			}
			Files.clearFile(f);
		}
	
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
		s += " - heCorr: " + heCorr + "\n";
		s += this.heuristic.toString();
		s += " - acc: " + this.learningProblem.getAccuracyMethod().getClass().getSimpleName() + "\n";
		s += " - traverse: " + this.traverseWholeTree + ", breaking: " + this.traverseBreakingThreshold + "\n";
		s += " - start: " + startClass + "\n";
		try {
			saveLog(logFile, s);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		addNode(startClass, null);
		
		while (!terminationCriteriaSatisfied()) {
			
//			logger.info("iteration: " + countIterations);
			showIfBetterSolutionsFound();

			// chose best node according to heuristics
			long selectStart = System.nanoTime();
			nextNode = getNextNodeToExpand();
			long selectEnd = System.nanoTime();
			selectTime += (selectEnd - selectStart);
			
//			logger.info("selected: " + nodeToString(nextNode));
			int horizExp = nextNode.getHorizontalExpansion();
			
			// apply refinement operator
//			List<LayerwiseSearchTreeNode> newNodes = new ArrayList<LayerwiseSearchTreeNode>();
			
			int nrNewNodes = 0, nrExpansion = 0;
//			while(nrNewNodes <= newNodesLowerbound) {
				
				TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
//				logger.info("nr refinements: " + refinements.size());
				while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
					// pick element from set
					OWLClassExpression refinement = refinements.pollFirst();

					// get length of class expression
					int length = OWLClassExpressionUtils.getLength(refinement);
					
					// we ignore all refinements with lower length and too high depth
					// (this also avoids duplicate node children)
					if(heCorr && length > horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
						// add node to search tree
						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
						if(newNode != null) {
							nrNewNodes++;
						}
					}
					if(!heCorr && length >= horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
						if(newNode != null) {
							nrNewNodes++;
						}
					}
				}
				nrExpansion ++;
//				logger.info("added " + nrNewNodes + " new nodes to " + nextNode);
//			}
			
			// without the condition, uncle works better
//			if(nrNewNodes>0)
			if(traverseWholeTree && this.heuristic instanceof UCTHeuristic)
				nextNode.setRecentlyExpanded(nrExpansion);

			if (writeSearchTree) {
				writeSearchTree(nextNode);
			}
			
			showIfBetterSolutionsFound();
			
//			if(nrNewNodes>0)
			// if the heuristic is based on uct, then we need backpropagation
			// otherwise, it is just level-wise tree search using celoe heuristic
			if(traverseWholeTree && this.heuristic instanceof UCTHeuristic) {
				backpropagate(nextNode);
				// write the search tree (if configured)
				if (writeSearchTree) {
					writeSearchTree();
				}
			}

			countIterations++;
		}
		
		if(singleSuggestionMode) {
			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
		}
		
		// print some stats
		printAlgorithmRunStats(searchTree.size());			
		
		isRunning = false;
	}
	
	private double getNodeScore (LayerwiseSearchTreeNode node) {
		if(node.getScore() != Double.NaN)
			return node.getScore();
		else
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
			highest_score = getNodeScore(iterator.next());
		}
			
		
		int nrBestNodes = 1;
		while(iterator.hasNext()) {
			// Only look at the nodes with the highest CELOE score
			double score = getNodeScore(iterator.next());			
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
	
		if (traverseWholeTree) {
            /* Traverse the tree from root according to UCT */
            LayerwiseSearchTreeNode currentNode = searchTree.getRoot();            
            while (!currentNode.getChildren().isEmpty()) {
//            		List<LayerwiseSearchTreeNode> children = new ArrayList<LayerwiseSearchTreeNode>();
//            		children.addAll(currentNode.getChildren());
//            		Collections.sort(children, heuristic.reversed());
//            		LayerwiseSearchTreeNode child = selectBestNode(children.iterator());
            		
            		LayerwiseSearchTreeNode child = currentNode.descendingIterator().next();
            		// if the current node is better than all its children
            		// then select the current node       
            		double currentScore = getNodeScore(currentNode);
            		double childScore = getNodeScore(child);
            		
            		if (childScore - currentScore <=  traverseBreakingThreshold) {
            			break;
            		}            		
//            		 otherwise, go to the next level 
            		currentNode = child;
            }
            return currentNode;
        } 
		else {
//			Iterator<LayerwiseSearchTreeNode> it = searchTree.descendingIterator();
//
//			while(it.hasNext()) {
//				LayerwiseSearchTreeNode node = it.next();
//				if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//						return node;
//				} else {
//					if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//						return node;
//					}
//				}
//			}
//			// this should practically never be called, since for any reasonable learning
//			// task, we will always have at least one node with less than 100% accuracy
//			throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
			throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
		}
	}
	
	protected List<LayerwiseSearchTreeNode> selectBestNodes (Iterator<LayerwiseSearchTreeNode> iterator){
		
		// if empty input, then return null
		if(!iterator.hasNext())
			return null; 
				
		
		List<LayerwiseSearchTreeNode> candidates = new ArrayList<LayerwiseSearchTreeNode>();
		candidates.add(iterator.next());
		if(!iterator.hasNext())
			return candidates;
		
		// since the input is sorted, then the first one has the best score
		double highest_score = getNodeScore(candidates.get(0));	
		while(iterator.hasNext()) {
			LayerwiseSearchTreeNode node = iterator.next();
			double score = getNodeScore(node);
			
			// since sorted, if the current score smaller than the best one
			// we have collected all candidates --> break the loop
			if (score < highest_score) {
				break;
			}

			// Add suitable nodes to the list
			if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
				candidates.add(node);
				highest_score = score;
			} else {
				if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
					candidates.add(node);
					highest_score = score;
				}
			}
		}
		
		return candidates;
	}
	
	// select one node from a sorted iterator: best node at the front
	protected LayerwiseSearchTreeNode selectBestNode (Iterator<LayerwiseSearchTreeNode> iterator) {
		List<LayerwiseSearchTreeNode> candidates = selectBestNodes(iterator);
		
//		System.out.println("selecting best node");
		
		if (candidates.size() == 1) {
			return candidates.get(0);
		} 
		else if(candidates.isEmpty()){
            // this should practically never be called, since for any reasonable learning
            // task, we will always have at least one node with less than 100% accuracy
            throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
        }
		
		return candidates.get(0);
		
//		// if there are several candidates: sort by length
//		if(candidates.size() > 1) {					
//			// sort the candidates by length
//			Collections.sort(candidates, new Comparator<LayerwiseSearchTreeNode>() {
//				@Override
//				public int compare(LayerwiseSearchTreeNode a, LayerwiseSearchTreeNode b) {
//					return OWLClassExpressionUtils.getLength(a.getExpression()) - OWLClassExpressionUtils.getLength(b.getExpression());
//				}							    
//			});							
//		}
//		
////		System.out.println("sorted by length");
////		System.out.println("candidates: " + candidates.toString());
//		
//		// find the shortest nodes 
//		Iterator<LayerwiseSearchTreeNode> candidatesIt = candidates.iterator();
//		List<LayerwiseSearchTreeNode> shortestNodes = new ArrayList<LayerwiseSearchTreeNode>();
//		shortestNodes.add(candidatesIt.next());
////		System.out.println("candidates: " + shortestNodes.toString());
//		int shortest = OWLClassExpressionUtils.getLength(shortestNodes.get(0).getExpression());
//		while(candidatesIt.hasNext()) {
//			LayerwiseSearchTreeNode next = candidatesIt.next();
//			if(OWLClassExpressionUtils.getLength(next.getExpression()) > shortest) {
//				break;
//			}
//			shortestNodes.add(next);
////			System.out.println("candidates: " + shortestNodes.toString());
//		}
//		
////		System.out.println("candidates: " + shortestNodes.toString());
//		
//		// if undecided, randomly pick one
//		if(shortestNodes.size() > 1) {
//			int i = rnd.nextInt(shortestNodes.size()-1);
////			System.out.println("picking " + i);
//    			return shortestNodes.get(i);
//		}
//		else {
//			// it cannot be other case
//			return shortestNodes.get(0);
//		}		
	} 
	
	// expand node horizontically
	protected TreeSet<OWLClassExpression> refineNode(LayerwiseSearchTreeNode node) {
		
		long refineStart = System.nanoTime();
		// we have to remove and add the node since its heuristic evaluation changes through the expansion
		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
		// otherwise you may see rarely occurring but critical false ordering in the nodes set)
		searchTree.updatePrepare(node);
		int horizExp = node.getHorizontalExpansion();
		
		int localHeStep = heStep;
//		if(node.isRoot() && node.getExpansionCounter() == 0) {
//			localHeStep = 3;
//		}
		
		TreeSet<OWLClassExpression> refinements = new TreeSet<OWLClassExpression>();
		if(heCorr)
			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1+localHeStep);
		else
			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+localHeStep);
		
		if(localHeStep == 0)
			node.incHorizontalExpansion();
		else {
			for(int i = 0; i < localHeStep; i++) {
				node.incHorizontalExpansion();
			}
		}		
		
		node.setRefinementCount(refinements.size());
		searchTree.updateDone(node);
		
		long refineEnd = System.nanoTime();
		refineTime += (refineEnd - refineStart);
		
		return refinements;
	}
	
	/**
	 * recursively update the uct statistics from this node to the root
	 * this is the backpropagation of UCT
	 * @return
	 */
	private void backpropagate (LayerwiseSearchTreeNode node) {
		
		long backStart = System.nanoTime();
		node.updateCounter();
		//		searchTree.update(node);

		if(!node.isRoot()) {
			backpropagate(node.getParent());
		}else {
			// during backpropagation, once we meet the root node, we update the global counter of UCT
			if(heuristic instanceof UCTHeuristic)
				UCTHeuristic.incTotalExpansion();
		}
		long backEnd = System.nanoTime();
		backTime += (backEnd - backStart);
	}
	
	/**
	 * Add node to search tree if it is not too weak.
	 * @return TRUE if node was added and FALSE otherwise
	 */
	protected LayerwiseSearchTreeNode addNode(OWLClassExpression description, LayerwiseSearchTreeNode parentNode) {
		
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
		
		// quality of class expression (return if too weak)
		double accuracy = learningProblem.getAccuracyOrTooWeak(description, noise);
//		double pAccuracy = ((PosNegLPStandard) learningProblem).getAccuracyOrTooWeakExact(description, noise);
		
		/**
		 * workaround for the reasoner bug
		 */
//		if(bestPredAcc < pAccuracy)
//			bestPredAcc = pAccuracy;
//		if(bestNegOnlyAcc < accuracy)
//			bestNegOnlyAcc = accuracy;
		
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
		
//		LayerwiseSearchTreeNode node = new LayerwiseSearchTreeNode(description, accuracy);
		
		long treeStart = System.nanoTime();
		LayerwiseSearchTreeNode node = new LayerwiseSearchTreeNode(description, accuracy, heCorr, heuristic);
		searchTree.addNode(parentNode, node);
			
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
		if(isCandidate) {
			//TODO: rewrite forte/uncle_small will produce "male and married only nothing" from "male and married max 0 thing"
			//      and the reasoner will have different instance retrieval results based on this
			OWLClassExpression niceDescription = rewrite(node.getExpression());
//			OWLClassExpression niceDescription = description;

			if(niceDescription.equals(classToDescribe)) {
				return null;
			}
			
			if(!isDescriptionAllowed(niceDescription, node.getExpression())) {
				return null;
			}
			
			// another test: none of the other suggested descriptions should be
			// a subdescription of this one unless accuracy is different
			// => comment: on the one hand, this appears to be too strict, because once A is a solution then everything containing
			// A is not a candidate; on the other hand this suppresses many meaningless extensions of A
			boolean shorterDescriptionExists = false;
			if(forceMutualDifference) {
				for(EvaluatedDescription<? extends Score> ed : bestEvaluatedDescriptions.getSet()) {
					if(Math.abs(ed.getAccuracy()-accuracy) <= 0.00001 && ConceptTransformation.isSubdescription(niceDescription, ed.getDescription())) {
//						System.out.println("shorter: " + ed.getDescription());
						shorterDescriptionExists = true;
						break;
					}
				}
			}
			
//			System.out.println("shorter description? " + shorterDescriptionExists + " nice: " + niceDescription);
			if(!shorterDescriptionExists) {
				if(!filterFollowsFromKB || !((ClassLearningProblem)learningProblem).followsFromKB(niceDescription)) {
//					System.out.println(node + "->" + niceDescription);
					bestEvaluatedDescriptions.add(niceDescription, accuracy, learningProblem);
//					System.out.println("acc: " + accuracy);
//					System.out.println(bestEvaluatedDescriptions);
				}
			}
			
//			bestEvaluatedDescriptions.add(node.getDescription(), accuracy, learningProblem);
			
//			System.out.println(bestEvaluatedDescriptions.getSet().size());
		}
		long rewriteEnd = System.nanoTime();
		rewriteTime += (rewriteEnd - rewriteStart);
		
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
		treeString.append(", celoe:");
		treeString.append(getNodeScore(node));
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
	 * @return the traverseWholeTree
	 */
	public boolean isTraverseWholeTree() {
		return traverseWholeTree;
	}

	/**
	 * @param traverseWholeTree the traverseWholeTree to set
	 */
	public void setTraverseWholeTree(boolean traverseWholeTree) {
		this.traverseWholeTree = traverseWholeTree;
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
		return new CELOELayerwise(this);
	}	
}