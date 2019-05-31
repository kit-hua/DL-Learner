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
package org.dllearner.algorithms.rrch;

import com.google.common.collect.Sets;

import org.apache.jena.base.Sys;
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
 * This is a copy of the CELOE algorithm for benchmarking purposes
 * the major modification is the correction of horizontal expansion
 * some features are added for better logging
 */
@SuppressWarnings("CloneDoesntCallSuperClone")
@ComponentAnn(name="CELOEOriginal", shortName="celoe_ori", version=1.0, description="CELOEOriginal is an adapted and extended version of the CELOE algorithm for benchmarking purposes")
public class CELOEOriginal extends CELOEBase implements Cloneable{

	private static final Logger logger = LoggerFactory.getLogger(CELOEOriginal.class);

	@ConfigOption(defaultValue="celoe_heuristic")
	protected AbstractHeuristic heuristic;
	
	protected SearchTree<OENode> searchTree;
	
	
	List<OENode> selectedNodes = new ArrayList<OENode>();
	

	public CELOEOriginal() {}

	public CELOEOriginal(CELOEOriginal celoe){
		super(celoe);
		setHeuristic(celoe.heuristic);
	}

	public CELOEOriginal(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
	}
	
	@Override
	public void init() throws ComponentInitException {
		
		
		// if no one injected a heuristic, we use a default one
		if(heuristic == null) {
			heuristic = new OEHeuristicRuntime();
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
		OENode nextNode;

		String s = "\nCurrent config: \n";
		s += " - logging to: " + logFile + "\n";
		s += " - heStep: " + heStep + "\n";
		s += " - heCorr: " + heCorrection + "\n";
		s += this.heuristic.toString() + "\n";
		s += " - acc: " + this.learningProblem.getAccuracyMethod().getClass().getSimpleName() + "\n";
		s += " - start: " + startClass + "\n";
		try {
			saveLog(s, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		OENode root = addNode(startClass, null);
		if (writeSearchTree) {
			writeSearchTree(root);
		}
		
		countIterations++;
		long runtimeLastRec = getCurrentRuntimeInMilliSeconds();		
		while (!terminationCriteriaSatisfied()) {
			
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
			TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
			while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {		
				
				// pick element from set
				OWLClassExpression refinement = refinements.pollFirst();
				
				// get length of class expression
				int length = OWLClassExpressionUtils.getLength(refinement);
				
				// we ignore all refinements with lower length and too high depth
				// (this also avoids duplicate node children)
				if(heCorrection && length > horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
					// add node to search tree
					addNode(refinement, nextNode);
				}
				if(!heCorrection && length >= horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
					addNode(refinement, nextNode);
				}
			}

			if (writeSearchTree) {
				writeSearchTree(nextNode);
			}
			
			showIfBetterSolutionsFound();						
		}		
		
		if(singleSuggestionMode) {
			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
		}
		
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
		
		isRunning = false;
	}
	
	protected void printSelectedNodes(boolean verbose) {
		try {
			saveLog("\nselected nodes in each iteration:", false);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for(OENode node : selectedNodes) {
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
		
		List<OENode> nodes = new ArrayList<OENode>();
		nodes.add(searchTree.getRoot());
		printTreeStructure(nodes);
	}
	
	private void printTreeStructure(List<OENode> nodesOnLevel) {
		if(nodesOnLevel.isEmpty())
			return;
		try {
			saveLog(String.valueOf(nodesOnLevel.size()), false);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<OENode> nextLevel = new ArrayList<OENode>();
		for(OENode node : nodesOnLevel) {
			nextLevel.addAll(node.getChildren());			
		}
		printTreeStructure(nextLevel);
	}
	
	private double getNodeScore (OENode node) {
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
	protected void getCELOEDecidability (Iterator<OENode> iterator, boolean sorted) {

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
	
	private OENode getNextNodeToExpand() {
		Iterator<OENode> it = searchTree.descendingIterator();

		while(it.hasNext()) {
			OENode node = it.next();
			if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
					return node;
			} else {
				if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
					return node;
				}
			}
		}
		// this should practically never be called, since for any reasonable learning
		// task, we will always have at least one node with less than 100% accuracy
		throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
		
	}
	
	// expand node horizontically
	protected TreeSet<OWLClassExpression> refineNode(OENode node) {
		
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
		if(heCorrection)
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
	 * Add node to search tree if it is not too weak.
	 * @return TRUE if node was added and FALSE otherwise
	 */
	protected OENode addNode(OWLClassExpression description, OENode parentNode) {
		
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
		OENode node = new OENode(description, accuracy, heCorrection);
		searchTree.addNode(parentNode, node);
		
//		if(accuracy == 1) {
//			try {
//				saveLog(node.getId() + ":" + node.toString(), true);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
			
			
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
		searchTree = new SearchTree<>(heuristic);
		super.reset();
	}
	
	
	private StringBuilder nodeToString (OENode node) {
		StringBuilder treeString = new StringBuilder();
		String nodeStr = node.toString();
		nodeStr = nodeStr.substring(0, nodeStr.length()-1);
		treeString.append(nodeStr);
		treeString.append(", celoe:");
		treeString.append(getNodeScore(node));
		
		treeString.append("]\n");
		
		return treeString;
	}
	
	private StringBuilder toTreeString(OENode node, int depth) {
		StringBuilder treeString = new StringBuilder();
		for (int i = 0; i < depth - 1; i++) {
			treeString.append("  ");
		}
		if (depth != 0) {
			treeString.append("|--> ");
		}
		
		treeString.append(nodeToString(node));

		for (OENode child : node.getChildren()) {
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
	
	protected void writeSearchTree(OENode selected) {
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

	public AbstractHeuristic getHeuristic() {
		return heuristic;
	}

	@Autowired(required=false)
	public void setHeuristic(AbstractHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	/* (non-Javadoc)
			 * @see java.lang.Object#clone()
			 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new CELOEOriginal(this);
	}	
}