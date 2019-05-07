package org.dllearner.algorithms.layerwise;
//package org.dllearner.algorithms.mcts;
//
//import com.jamonapi.Monitor;
//import com.jamonapi.MonitorFactory;
//import org.dllearner.algorithms.celoe.OENode;
//import org.dllearner.core.AbstractHeuristic;
//import org.dllearner.core.ComponentAnn;
//import org.dllearner.core.ComponentInitException;
//import org.dllearner.core.EvaluatedDescription;
//import org.dllearner.core.Score;
//import org.dllearner.core.config.ConfigOption;
//import org.dllearner.learningproblems.ClassLearningProblem;
//import org.dllearner.utilities.Files;
//import org.dllearner.utilities.datastructures.SearchTree;
//import org.dllearner.utilities.owl.ConceptTransformation;
//import org.dllearner.utilities.owl.OWLClassExpressionUtils;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.*;
//
//@ComponentAnn(name="MCTS", shortName="mcts", version=0.1, description="Monte Carlo Tree Search for OWL Learning")
//public class MCTS extends CELOE_MCTS {
//
//	/* Heuristic used for selection with UCT score */
//	//    private UCTHeuristic heuristic;
//
//	// ========== Configuration Options ==========
//
//	/*
//	 * During the selection step, if there is not a unique best node according to CELOE, we use the UCT value.
//	 * There are two ways implemented to do this:
//	 *      1. Traverse the search tree from the root, and always choose the best child node or the current node
//	 *         if it has a better UCT score than all its children
//	 *      2. Sort all nodes according to their UCT score and select the best
//	 * If this variable is set to true, then method 1 is used, otherwise method 2.
//	 */
//	@ConfigOption(defaultValue="true", description = "whether the tree should be traversed using uct")
//	private boolean uctTraverseWholeTree = true;
//
//	// For logging purposes
//	private static final Logger logger = LoggerFactory.getLogger(MCTS.class);
//
//	@ConfigOption(defaultValue = "false", description = "whether to write all MCTS steps to a log file")
//	private boolean logSteps;
//
//	// Details about each iteration of the algorithm, only used if logSteps == true
//	private JSONArray steps;
//
//	// Output file for MCTS-specific steps of the algorithm
//	@ConfigOption(defaultValue = "log/mcts_steps.log", description = "Path to MCTS-specific log file")
//	private String logFilePath;
//
//	// For statistics purposes
//	private int timesUctRequired = 0;
//	private int timesUctNotRequired = 0;
//	private int countIterations = 0;
//
//	private List<UCTNode> nodeList = new ArrayList<UCTNode>();
//
//	@Override
//	public void init() throws ComponentInitException {
//		super.init();
//
//		writeSearchTree = true;
//		heStep = 1;
//		heCorr = false;
//
//		initialized = true;
//	}
//
//
//	@Override
//	public void start() {
//		// Mark component as running
//		stop = false;
//		isRunning = true;
//
//		// Log starting time
//		nanoStartTime = System.nanoTime();
//		logger.info("start class:" + super.getStartClass());
//
//		// Reset variables
//		reset();
//		UCTNode root = addNode(super.getStartClass(), null);
//		nodeList.add(root);
//
//
//		while (!terminationCriteriaSatisfied()) {
//			showIfBetterSolutionsFound();
//
//			/*
//			 * 1. Selection step of MCTS
//			 * Chose the best node according to heuristics
//			 */
////			UCTNode nextNode = getNextNodeToExpand();
//			List<UCTNode> nextNodes = traverse();
//			if(nextNodes.size() > 1) {
//				System.out.println("iteration " + countIterations + ": ");
//				for(UCTNode node : nextNodes) {
//					System.out.println(" - " + node.toString());	
//				}				
//			}
////			System.out.println("iteration: " + countIterations);
////			System.out.println("next nodes: " + nextNodes.size());
//			
//			boolean needsUpdate = false;
//			for(UCTNode nextNode : nextNodes) {
//				int horizExp = nextNode.getHorizontalExpansion();
//
//				/*
//				 * 2. Expansion step of MCTS
//				 * (Apply the refinement operator)
//				 */
//				TreeSet<OWLClassExpression> refinements = refineNode(nextNode);				
//				// Keep track of the new children that we added
//				Set<UCTNode> newNodes = new HashSet<>();
//
//				while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
//					// pick element from set
//					OWLClassExpression refinement = refinements.pollFirst();
//
//					// get length of class expression
//					int length = OWLClassExpressionUtils.getLength(refinement);
//
//					// we ignore all refinements with lower length and too high depth
//					// (this also avoids duplicate node children)
//					assert refinement != null;
//					if(length >= horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
//						// add node to search tree
//						UCTNode newNode = addNode(refinement, nextNode);
//						if(newNode != null) {
//							newNodes.add(newNode);							
//						}
//					} else {
//						if (!(OWLClassExpressionUtils.getDepth(refinement) <= maxDepth)) {
//							System.out.println("max depth reached");
//						}
//					}
//				}
//				
//				if(!newNodes.isEmpty()) {
//					nodeList.addAll(newNodes);
//					nextNode.setRecentlyExpanded(1);
//					needsUpdate = true;
//				}								
//			}
//			
//			// write the search tree (if configured)
//			if (isWriteSearchTree()) {
//				writeSearchTree(nextNodes);
//			}
//
//			// after expansion, we update the tree 
//			if(needsUpdate) {
//				backpropagate(nextNodes.get(nextNodes.size()-1));
//				// and update the ordering
//				Collections.sort(nodeList, heuristic.reversed());
//			}
//
//			if (isWriteSearchTree()) {
//				writeSearchTreeAfterPropagation();
//			}
//
//			showIfBetterSolutionsFound();
//
//			// update the global min and max horizontal expansion values
//			
////			for(UCTNode nextNode : nextNodes)
////				updateMinMaxHorizExp(nextNode);
//
//			countIterations++;
//		}
//
//		// detailed log (if configured)
//		if (logSteps) {
//			logSteps();
//		}
//
//		// print some stats
//		printAlgorithmRunStats();
//
//		System.out.println("Iterations: " + countIterations);
//		System.out.println("Times UCT required: " + timesUctRequired);
//		System.out.println("Times UCT not required: " + timesUctNotRequired);
//		System.out.println("Percentage UCT required: " + ((double)timesUctRequired)/((double)timesUctNotRequired+timesUctRequired));
//		System.out.println("Nodes in search tree: " + searchTree.size());
//		System.out.println("Expressions tested: " + expressionTests);
//
//		// print solution(s)
//		logger.info("solutions:\n" + getSolutionString());
//
//		isRunning = false;
//	}
//
//	
//	protected TreeSet<OWLClassExpression> refineNode(UCTNode node) {
//		MonitorFactory.getTimeMonitor("refineNode").start();
//		// we have to remove and add the node since its heuristic evaluation changes through the expansion
//		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
//		// otherwise you may see rarely occurring but critical false ordering in the nodes set)
//		searchTree.updatePrepare(node);
//		int horizExp = node.getHorizontalExpansion();
//		
//		if(node.isRoot() && node.getExpansionCounter()==0)
//			heStep = 1;
//		else
//			heStep = 1;
//		
//		TreeSet<OWLClassExpression> refinements = new TreeSet<OWLClassExpression>();
//		if(heCorr)
//			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1+heStep);
//		else
//			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+heStep);
//		
//		if(heStep == 0)
//			node.incHorizontalExpansion();
//		else {
//			for(int i = 0; i < heStep; i++) {
//				node.incHorizontalExpansion();
//			}
//		}		
//		
//		node.setRefinementCount(refinements.size());
////		System.out.println("refined node: " + node);
//		searchTree.updateDone(node);
//		MonitorFactory.getTimeMonitor("refineNode").stop();
//		return refinements;
//	}
//
//	/**
//	 * recursively update the uct statistics from this node to the root
//	 * this is the backpropagation of UCT
//	 * @return
//	 */
//	private void backpropagate (UCTNode node) {
//		node.updateUCT();
//		//		searchTree.update(node);
//
//		if(!node.isRoot()) {
//			backpropagate(((UCTNode) node.getParent()));
//		}else {
//			// during backpropagation, once we meet the root node, we update the global counter of UCT
//			UCTHeuristic.incTotalExpansion();
//		}
//	}
//
//	// ===================================
//
//	// Logs the current step of MCTS in detail
//	private void logCurrentStep(OENode selectedNode, Set<OENode> newChildren, List<OENode> simulation) {
//		JSONObject step = new JSONObject();
//		step.put("selected", selectedNode.getDescription());
//
//		JSONArray children = new JSONArray();
//		for (OENode e : newChildren) {
//			children.put(e.getDescription().toString());
//		}
//		step.put("children", children);
//
//		if (!newChildren.isEmpty()) {
//			JSONArray sim = new JSONArray();
//			for (OENode n : simulation) {
//				sim.put(n.getDescription());
//			}
//			step.put("simulation", sim);
//		}
//
//		if (steps == null) {
//			steps = new JSONArray();
//		}
//		steps.put(step);
//	}
//
//	// Writes all the logged steps to a file
//	private void logSteps() {
//		JSONObject logObject = new JSONObject();
//		logObject.put("tree", getSearchTreeFile());
//		logObject.put("steps", steps);
//
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath));
//			writer.write(logObject.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * Add node to search tree if it is not too weak.
//	 * @return TRUE if node was added and FALSE otherwise
//	 */
//	protected UCTNode addNode(OWLClassExpression description, UCTNode parentNode) {
//
//		MonitorFactory.getTimeMonitor("addNode").start();
//
//		// redundancy check (return if redundant)
//		boolean nonRedundant = descriptions.add(description);
//		if(!nonRedundant) {
//			return null;
//		}
//
//		// check whether the class expression is allowed
//		if(!isDescriptionAllowed(description, parentNode)) {
//			return null;
//		}
//
//		// quality of class expression (return if too weak)
//		double accuracy = learningProblem.getAccuracyOrTooWeak(description, noise);
//
//		// issue a warning if accuracy is not between 0 and 1 or -1 (too weak)
//		if(accuracy > 1.0 || (accuracy < 0.0 && accuracy != -1)) {
//			throw new RuntimeException("Invalid accuracy value " + accuracy + " for class expression " + description +
//					". This could be caused by a bug in the heuristic measure and should be reported to the DL-Learner bug tracker.");
//		}
//
//		expressionTests++;
//
//		// return FALSE if 'too weak'
//		if(accuracy == -1) {
//			return null;
//		}
//
//		UCTNode node = new UCTNode(description, accuracy, heCorr);
//		searchTree.addNode(parentNode, node);
////		node.updateGain();
//
//		// in some cases (e.g. mutation) fully evaluating even a single class expression is too expensive
//		// due to the high number of examples -- so we just stick to the approximate accuracy
//		if(singleSuggestionMode) {
//			if(accuracy > bestAccuracy) {
//				bestAccuracy = accuracy;
//				bestDescription = description;
//				logger.info("more accurate (" + dfPercent.format(bestAccuracy) + ") class expression found: " + descriptionToString(bestDescription)); // + getTemporaryString(bestDescription));
//			}
//			return node;
//		}
//
//		// maybe add to best descriptions (method keeps set size fixed);
//		// we need to make sure that this does not get called more often than
//		// necessary since rewriting is expensive
//		boolean isCandidate = !bestEvaluatedDescriptions.isFull();
//		if(!isCandidate) {
//			EvaluatedDescription<? extends Score> worst = bestEvaluatedDescriptions.getWorst();
//			double accThreshold = worst.getAccuracy();
//			isCandidate =
//					(accuracy > accThreshold ||
//							(accuracy >= accThreshold && OWLClassExpressionUtils.getLength(description) < worst.getDescriptionLength()));
//		}
//
//		if(isCandidate) {
//			//TODO: rewrite forte/uncle_small will produce "male and married only nothing" from "male and married max 0 thing"
//			//      and the reasoner will have different instance retrieval results based on this
//			OWLClassExpression niceDescription = rewrite(node.getExpression());
//			//			OWLClassExpression niceDescription = description;
//
//			if(niceDescription.equals(classToDescribe)) {
//				return null;
//			}
//
//			if(!isDescriptionAllowed(niceDescription, node)) {
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
//
//		}
//
//		return node;
//	}
//
//
//	/**
//	 * Determines the node which will be selected and possibly expanded next by the algorithm.
//	 *
//	 * First, we look at how the CELOE heuristic would decide, i.e. if there is a node that has the unique
//	 * highest CELOE score, then we select that node. Otherwise, the MCTSHeuristic is used for selection among
//	 * the nodes that share the highest CELOE score.
//	 *
//	 * @return The selected node
//	 */	
//	private UCTNode getNextNodeToExpand() {
//
////		Iterator<OENode> it = searchTree.descendingIterator();		
////		Collections.sort(nodeList, heuristic.reversed());
//		Iterator<UCTNode> it = nodeList.iterator();
//		UCTNode firstBestNode = (UCTNode) it.next();
//		List<UCTNode> candidates = new ArrayList<UCTNode>();
//		candidates.add(firstBestNode);
//		double bestScore = heuristic.getNodeScore(firstBestNode);
//		while(it.hasNext()) {
//			UCTNode node = (UCTNode) it.next();
//			if(heuristic.getNodeScore(node) < bestScore) {
//				break;
//			}
//			if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//				candidates.add(node);
//			} else {
//				if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//					candidates.add(node);
//				}
//			}
//		}
////
////		if (candidates.size() == 1) {
////			// Only one node with the highest score
////			timesCeloeDecided++;
////			return candidates.get(0);
////		} else {
////			timesCeloeUndecided++;
////		}
//
//		if (uctTraverseWholeTree) {
//			/* Traverse the tree from root according to UCT */
//			UCTNode currentNode = (UCTNode) searchTree.getRoot();
//			while (!currentNode.getChildren().isEmpty()) {
//				UCTNode lastNode = currentNode;
//				/* Continue traversing the tree with one of the node's children or stop at the current node,
//				 * if it has a better score than all its children. This can be useful, because all nodes of the tree
//				 * can be expanded further, not just the leaf nodes.
//				 */
//				currentNode = (UCTNode) Collections.max(currentNode.getChildren(), heuristic);
//				if (heuristic.getNodeScore(lastNode) > heuristic.getNodeScore(currentNode)) {
//					currentNode = lastNode;
//					break;
//				}
//			}
//			return currentNode;
//		} else {
//
//			// if we are selecting the best node from the tree, and there are some of them
//			// we randomly select one
//			return MCTSUtil.sampleUniform(candidates);
//		}    
//	}
//	
//	private List<UCTNode> traverse (){
//		/* Traverse the tree from root according to UCT */
//		List<UCTNode> candidates = new ArrayList<UCTNode>();	
//		
//		UCTNode currentNode = (UCTNode) searchTree.getRoot();			
//		UCTNode child = getChildToExpand(currentNode);
//		
////		if(worthExpand(currentNode, child))
////			candidates.add(currentNode);
//		
//		while(child != null) {			
//			
//			currentNode = child;
//			child = getChildToExpand(currentNode);
//			
//			if(child == null || worthExpand(currentNode, child)) {
//				candidates.add(currentNode);
//			}						
//		}
//	
//		if(child == null && !candidates.contains(currentNode))
//			candidates.add(currentNode);
//		return candidates;
//	}
//	
//	private boolean worthExpand (UCTNode node, UCTNode bestChild) {
//		if(heuristic.getNodeScore(node) >= heuristic.getNodeScore(bestChild))
//			return true;
////		if(heuristic.getNodeScore(bestChild) - heuristic.getNodeScore(node) < heuristic.getNodeScore(node))
////			return true;
////		if(node.getHorizontalExpansion() - OWLClassExpressionUtils.getLength(node.getExpression()) < 4)
////			return true;
//		return false;
//	}
//	
//	private UCTNode getChildToExpand(UCTNode node) {
//		
//		List<OENode> children = new ArrayList<OENode>();		
//		children.addAll(node.getChildren());
//		if(children.isEmpty())
//			return null;
//		
//		children.sort(heuristic.reversed());
//				
//		Iterator<OENode> it = children.iterator();
//		UCTNode firstBestNode = (UCTNode) it.next();
//		double bestScore = heuristic.getNodeScore(firstBestNode);
//		
//		UCTNode best = firstBestNode;
//		int shortest = OWLClassExpressionUtils.getLength(best.getExpression());
//		while(it.hasNext()) {
//			UCTNode next = (UCTNode) it.next();
//			if(heuristic.getNodeScore(next) < bestScore) {
//				break;
//			}
//			int length = OWLClassExpressionUtils.getLength(next.getExpression());
//			if(length < shortest) {
//				best = next;
//				shortest = length;
//			}
//		}
//		
//		return best;
//	
//	}
//
//	/*
//	 * Checks whether we should consider this node for expansion
//	 */
//	private boolean nodeSuitableForExpansion(OENode node) {
//		if (isExpandAccuracy100Nodes() &&
//				node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//			return true;
//		} else return node.getAccuracy() < 1.0 ||
//				node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription());
//	}
//
//
//	// ========== Tree output ==========
//
//	private StringBuilder nodeToString(OENode node) {
//		StringBuilder treeString = new StringBuilder();
//
//		treeString.append(node.getDescription().toString());
//		treeString.append(" [acc:");
//		treeString.append(String.format("%.0f%%", 100*node.getAccuracy()));
//		treeString.append(", he:");
//		treeString.append(node.getHorizontalExpansion());
//		//        treeString.append(", celoe:");
//		//        treeString.append(super.getHeuristic().getNodeScore(node));
//		treeString.append(", cnt:");
//		treeString.append(((UCTNode) node).getExpansionCounter());
//		treeString.append(", acnt:");
//		treeString.append(((UCTNode) node).getAccumulatedExpansionCounter());
//		treeString.append(", exploitation:");
//		treeString.append(String.format("%.3f", ((UCTHeuristic) heuristic).getExploitation((UCTNode) node)));
//		treeString.append(", exploration:");
//		treeString.append(String.format("%.3f", ((UCTHeuristic) heuristic).getExploration((UCTNode) node)));
//		treeString.append(", uct:");
//		treeString.append(String.format("%.3f", heuristic.getNodeScore(node)));
//		treeString.append("]\n");
//
//		return treeString;
//	}
//
//	private StringBuilder toTreeString(OENode node, int depth) {
//		StringBuilder treeString = new StringBuilder();
//		for (int i = 0; i < depth - 1; i++) {
//			treeString.append("  ");
//		}
//		if (depth != 0) {
//			treeString.append("|--> ");
//		}
//
//		treeString.append(nodeToString(node));
//
//		for (OENode child : node.getChildren()) {
//			treeString.append(toTreeString(child, depth+1));
//		}
//		return treeString;
//	}
//
//	protected void writeSearchTree(List<UCTNode> selectedNodes) {
//		StringBuilder treeString = new StringBuilder("------------- Iteration " + countIterations + " -------------\n"); 
//		treeString.append("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
//
//		for(OENode selected : selectedNodes)
//			treeString.append("selected node: " + nodeToString(selected));
//		treeString.append(toTreeString(searchTree.getRoot(), 0)).append("\n");
//
//		// replace or append
//		if (replaceSearchTree) {
//			Files.createFile(new File(searchTreeFile), treeString.toString());
//		} else {
//			Files.appendToFile(new File(searchTreeFile), treeString.toString());
//		}
//	}
//
//	protected void writeSearchTreeAfterPropagation() {
//		StringBuilder treeString = toTreeString(super.searchTree.getRoot(), 0);
//		treeString.append("\n");
//
//		// replace or append
//		if (isReplaceSearchTree()) {
//			Files.createFile(new File(getSearchTreeFile()), treeString.toString());
//		} else {
//			Files.appendToFile(new File(getSearchTreeFile()), treeString.toString());
//		}
//	}
//
//
//	// ========== Getters and setters ==========
//
//	public AbstractHeuristic getHeuristic() {
//		return heuristic;
//	}
//
//	public boolean isUctTraverseWholeTree() {
//		return uctTraverseWholeTree;
//	}
//
//	public void setUctTraverseWholeTree(boolean uctTraverseWholeTree) {
//		this.uctTraverseWholeTree = uctTraverseWholeTree;
//	}
//
//	//    public boolean isAddSimulationResultToTree() {
//	//        return addSimulationResultToTree;
//	//    }
//	//
//	//    public void setAddSimulationResultToTree(boolean addSimulationResultToTree) {
//	//        this.addSimulationResultToTree = addSimulationResultToTree;
//	//    }
//
//	public void setLogSteps(boolean logSteps) {
//		this.logSteps = logSteps;
//	}
//
//	public void setLogFilePath(String logFilePath) {
//		this.logFilePath = logFilePath;
//	}
//}
