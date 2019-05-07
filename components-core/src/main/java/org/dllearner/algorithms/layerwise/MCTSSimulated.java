package org.dllearner.algorithms.layerwise;
//package org.dllearner.algorithms.mcts;
//
//import com.jamonapi.MonitorFactory;
//import org.dllearner.algorithms.celoe.OENode;
//import org.dllearner.core.AbstractHeuristic;
//import org.dllearner.core.ComponentAnn;
//import org.dllearner.core.ComponentInitException;
//import org.dllearner.core.config.ConfigOption;
//import org.dllearner.utilities.Files;
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
//@ComponentAnn(name="MCTSSimulated", shortName="mcts_sim", version=0.1, description="Monte Carlo Tree Search for OWL Learning")
//public class MCTSSimulated extends CELOE_MCTS {
//
//    /* Heuristic used for selection with UCT score */
//    private MCTSHeuristic heuristic;
//
//    /* The simulator used in the simulation step */
//    private MCTSSimulator simulator;
//
//    // ========== Configuration Options ==========
//
//    /*
//     * During the selection step, if there is not a unique best node according to CELOE, we use the UCT value.
//     * There are two ways implemented to do this:
//     *      1. Traverse the search tree from the root, and always choose the best child node or the current node
//     *         if it has a better UCT score than all its children
//     *      2. Sort all nodes according to their UCT score and select the best
//     * If this variable is set to true, then method 1 is used, otherwise method 2.
//     */
//    @ConfigOption(defaultValue="false", description = "whether the tree should be traversed using uct")
//    private boolean uctTraverseWholeTree = false;
//
//    @ConfigOption(defaultValue = "false", description = "whether the resulting description of a simulation should be added to the search tree")
//    private boolean addSimulationResultToTree = false;
//
//
//    // For logging purposes
//    private static final Logger logger = LoggerFactory.getLogger(MCTSSimulated.class);
//
//    @ConfigOption(defaultValue = "false", description = "whether to write all MCTS steps to a log file")
//    private boolean logSteps;
//
//    // Details about each iteration of the algorithm, only used if logSteps == true
//    private JSONArray steps;
//
//    // Output file for MCTS-specific steps of the algorithm
//    @ConfigOption(defaultValue = "log/mcts_steps.log", description = "Path to MCTS-specific log file")
//    private String logFilePath;
//
//    // For statistics purposes
//    private int timesUctRequired = 0;
//    private int timesUctNotRequired = 0;
//    private int totalSimulationSteps = 0;
//    private int totalSimulations = 0;
//
//
//    @Override
//    public void init() throws ComponentInitException {
//        super.init();
//
//        /* Initialize UCT heuristic */
//        heuristic = new MCTSHeuristic();
//        heuristic.init();
//
//        /* Initialize the simulator */
//        simulator = new MCTSSimulator();
//        simulator.setLearningProblem(getLearningProblem());
//        RhoDRDownMCTS simOperator = new RhoDRDownMCTS();        // Modified refinement operator for simulation purposes
//        simOperator.setStartClass(getStartClass());
//        simOperator.setReasoner(reasoner);
//        simOperator.setClassHierarchy(classHierarchy);
//        simOperator.setObjectPropertyHierarchy(objectPropertyHierarchy);
//        simOperator.setDataPropertyHierarchy(datatypePropertyHierarchy);
//        simOperator.init();
//        simulator.setRefinementOperator(simOperator);
//        simulator.setMaxConceptDepth(maxDepth);
//
//        initialized = true;
//    }
//
//
//    @Override
//    public void start() {
//        // Mark component as running
//        stop = false;
//        isRunning = true;
//
//        // Log starting time
//        nanoStartTime = System.nanoTime();
//        logger.info("start class:" + super.getStartClass());
//
//        // Reset variables
//        reset();
//        addNode(super.getStartClass(), null);
//
//        // The next node we will expand
//        OENode nextNode;
//
//        int countIterations = 0;
//
//        while (!terminationCriteriaSatisfied()) {
//            showIfBetterSolutionsFound();
//
//            /*
//             * 1. Selection step of MCTS
//             * Chose the best node according to heuristics
//             */
//            nextNode = getNextNodeToExpand();
//
//            int horizExp = nextNode.getHorizontalExpansion();
//
//            /*
//             * 2. Expansion step of MCTS
//             * (Apply the refinement operator)
//             */
//            TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
//            // Keep track of the new children that we added
//            Set<OENode> newNodes = new HashSet<>();
//
//            while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
//                // pick element from set
//                OWLClassExpression refinement = refinements.pollFirst();
//
//                // get length of class expression
//                int length = OWLClassExpressionUtils.getLength(refinement);
//
//                // we ignore all refinements with lower length and too high depth
//                // (this also avoids duplicate node children)
//                assert refinement != null;
//                if(length > horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
//                    // add node to search tree
//                    OENode newNode = addNode(refinement, nextNode);
//                    if (newNode != null) {
//                        newNodes.add(newNode);
//                    }
//                } else {
//                    if (!(OWLClassExpressionUtils.getDepth(refinement) <= maxDepth)) {
//                        System.out.println("max depth reached");
//                    }
//                }
//            }
//
//            /*
//             * If there are new children created for the selected node, then we pick one of them and start a simulation
//             * from it. After that, we can use the result to do backpropagation in order to make the UCT scores more
//             * accurate.
//             */
//            if (!newNodes.isEmpty()) {
//                /*
//                 * 3. Simulation step of MCTS
//                 * Randomly select one of the new nodes. This is done using weighted sampling. Nodes with a higher
//                 * CELOE score will get selected more likely.
//                 */
//                OENode randomLeaf = MCTSUtil.sampleWeighted(newNodes, super.getHeuristic());
//                simulate(randomLeaf);
//
//                /*
//                 * 4. Backpropagation step of MCTS
//                 */
//                backpropagate(randomLeaf);
//            }
//
//
//            showIfBetterSolutionsFound();
//
//            // update the global min and max horizontal expansion values
////            updateMinMaxHorizExp(nextNode);
//
//            // write the search tree (if configured)
//            if (isWriteSearchTree()) {
//                writeSearchTree(refinements);
//            }
//
//            // detailed log (if configured)
//            if (logSteps) {
//                logCurrentStep(nextNode, newNodes, simulator.getLastSimulation());
//            }
//
//            countIterations++;
//        }
//
//        // detailed log (if configured)
//        if (logSteps) {
//            logSteps();
//        }
//
//        // print some stats
//        printAlgorithmRunStats();
//
//        System.out.println("Iterations: " + countIterations);
//        System.out.println("Times UCT required: " + timesUctRequired);
//        System.out.println("Times UCT not required: " + timesUctNotRequired);
//        System.out.println("Percentage UCT required: " + ((double)timesUctRequired)/((double)timesUctNotRequired+timesUctRequired));
//        System.out.println("Nodes in search tree: " + searchTree.size());
//        System.out.println("Expressions tested: " + expressionTests);
//        System.out.println("Total simulations: " + totalSimulations);
//        System.out.println("Average simulation step count: " + (double)totalSimulationSteps/totalSimulations);
//        System.out.println("Total simulation time: " + simulator.getTotalSimulationTime()+"ms");
//
//        //logger.info(MonitorFactory.getReport());
//
//
//        // print solution(s)
//        logger.info("solutions:\n" + getSolutionString());
//
//        isRunning = false;
//    }
//
//
//    // ===================================
//
//    // Logs the current step of MCTS in detail
//    private void logCurrentStep(OENode selectedNode, Set<OENode> newChildren, List<OENode> simulation) {
//        JSONObject step = new JSONObject();
//        step.put("selected", selectedNode.getDescription());
//
//        JSONArray children = new JSONArray();
//        for (OENode e : newChildren) {
//            children.put(e.getDescription().toString());
//        }
//        step.put("children", children);
//
//        if (!newChildren.isEmpty()) {
//            JSONArray sim = new JSONArray();
//            for (OENode n : simulation) {
//                sim.put(n.getDescription());
//            }
//            step.put("simulation", sim);
//        }
//
//        if (steps == null) {
//            steps = new JSONArray();
//        }
//        steps.put(step);
//    }
//
//    // Writes all the logged steps to a file
//    private void logSteps() {
//        JSONObject logObject = new JSONObject();
//        logObject.put("tree", getSearchTreeFile());
//        logObject.put("steps", steps);
//
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath));
//            writer.write(logObject.toString());
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * Do a simulation on the specified leaf.
//     * If specified in the configuration it also adds the result to the search tree.
//     *
//     * @param leaf
//     *      The node the simulation should be started from
//     */
//    private void simulate(OENode leaf) {
//        OWLClassExpression simulationResult = simulator.simulate(leaf);
//
//        /* If simulation result should be added to tree and is more accurate than the original node, then add it */
//        if (addSimulationResultToTree && simulator.getFinalAccuracy() > leaf.getAccuracy()) {
//            addNode(simulationResult, leaf);
//        }
//
//        totalSimulationSteps += simulator.getLastSimulation().size();
//        totalSimulations++;
//    }
//
//    /**
//     * Back-propagate the accuracy gain from the last simulation for improving the UCT heuristic value.
//     *
//     * @param leaf
//     *      The node that the last simulation was started from
//     */
//    private void backpropagate(OENode leaf) {
//        /* Calculate the accuracy gain */
//        double acc1 = leaf.getAccuracy();
//        double acc2 = simulator.getFinalAccuracy();
//        double acc_gain = acc2 - acc1;
//
//        /*
//         * Propagate the result through the tree, starting from the selected leaf up to the root.
//         * Adding a high accuracy gain result to the heuristic will increase its score.
//         * For more details, see MCTSHeuristic.java
//         */
//        OENode currentNode = leaf;
//        do {
//            heuristic.addSimulationResult(currentNode, acc_gain);
//            if (currentNode.isRoot()) break;
//            currentNode = currentNode.getParent();
//        } while (true);
//        heuristic.updateAllScores();
//    }
//
//
//    /**
//     * Determines the node which will be selected and possibly expanded next by the algorithm.
//     *
//     * First, we look at how the CELOE heuristic would decide, i.e. if there is a node that has the unique
//     * highest CELOE score, then we select that node. Otherwise, the MCTSHeuristic is used for selection among
//     * the nodes that share the highest CELOE score.
//     *
//     * @return The selected node
//     */
//    private OENode getNextNodeToExpand() {
//        Iterator<OENode> it = searchTree.descendingIterator();
//
//        // Keep track of all nodes with the current highest CELOE score
//        LinkedList<OENode> celoe_nodes = new LinkedList<>();
//        double highest_score = 0;
//        boolean highest_score_set = false;      // so we can account for negative max scores
//
//        /* Find out which node(s) have the highest CELOE score */
//        while(it.hasNext()) {
//            OENode node = it.next();
//
//            // Only look at the nodes with the highest CELOE score
//            double score = super.getHeuristic().getNodeScore(node);
//            if (highest_score_set && score < highest_score) {
//                break;
//            }
//
//            // Add suitable nodes to the list
//            if (nodeSuitableForExpansion(node)) {
//                celoe_nodes.add(node);
//                highest_score_set = true;
//                highest_score = score;
//            }
//        }
//
//        if (celoe_nodes.isEmpty()) {
//            // this should practically never be called, since for any reasonable learning
//            // task, we will always have at least one node with less than 100% accuracy
//            throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
//            //return searchTree.getRoot();
//        } else if (celoe_nodes.size() == 1) {
//            /* Only one node with the highest CELOE score */
//            timesUctNotRequired++;
//            return celoe_nodes.getFirst();
//        } else {
//            /* Multiple nodes with highest CELOE score */
//            timesUctRequired++;
//
//            if (uctTraverseWholeTree) {
//                /* Traverse the tree from root according to UCT */
//                OENode currentNode = searchTree.getRoot();
//                while (!currentNode.getChildren().isEmpty()) {
//                    OENode lastNode = currentNode;
//                    /* Continue traversing the tree with one of the node's children or stop at the current node,
//                     * if it has a better score than all its children. This can be useful, because all nodes of the tree
//                     * can be expanded further, not just the leaf nodes.
//                     */
//                    currentNode = Collections.max(currentNode.getChildren(), heuristic);
//                    if (heuristic.getNodeScore(lastNode) > heuristic.getNodeScore(currentNode)) {
//                        currentNode = lastNode;
//                        break;
//                    }
//                }
//
//                return currentNode;
//            } else {
//                /* Sort all nodes according to UCT and take one of the best nodes at random */
//                return MCTSUtil.sampleUniformFromBest(celoe_nodes, this.heuristic);
//            }
//        }
//    }
//
//    /*
//     * Checks whether we should consider this node for expansion
//     */
//    private boolean nodeSuitableForExpansion(OENode node) {
//        if (isExpandAccuracy100Nodes() &&
//                node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//            return true;
//        } else return node.getAccuracy() < 1.0 ||
//                node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription());
//    }
//
//
//    // ========== Tree output ==========
//
//    private StringBuilder toTreeString(OENode node, int depth) {
//        StringBuilder treeString = new StringBuilder();
//        for (int i = 0; i < depth - 1; i++) {
//            treeString.append("  ");
//        }
//        if (depth != 0) {
//            treeString.append("|--> ");
//        }
//        //treeString.append(node.toString()).append("\n");
//        treeString.append(node.getDescription().toString());
//        treeString.append(" [acc:");
//        treeString.append(String.format("%.0f%%", 100*node.getAccuracy()));
//        treeString.append(", he:");
//        treeString.append(node.getHorizontalExpansion());
//        treeString.append(", celoe:");
//        treeString.append(super.getHeuristic().getNodeScore(node));
//        treeString.append(", uct:");
//        treeString.append(heuristic.getNodeScore(node));
//        treeString.append("]\n");
//
//        for (OENode child : node.getChildren()) {
//            treeString.append(toTreeString(child, depth+1));
//        }
//        return treeString;
//    }
//
//    protected void writeSearchTree(TreeSet<OWLClassExpression> refinements) {
//        StringBuilder treeString = new StringBuilder("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
//        if (refinements.size() > 1) {
//            treeString.append("all expanded nodes:\n");
//            for (OWLClassExpression ref : refinements) {
//                treeString.append("   ").append(ref).append("\n");
//            }
//        }
//        treeString.append(toTreeString(super.searchTree.getRoot(), 0)).append("\n");
//
//        // replace or append
//        if (isReplaceSearchTree()) {
//            Files.createFile(new File(getSearchTreeFile()), treeString.toString());
//        } else {
//            Files.appendToFile(new File(getSearchTreeFile()), treeString.toString());
//        }
//    }
//
//
//    // ========== Getters and setters ==========
//
//    public AbstractHeuristic getHeuristic() {
//        return heuristic;
//    }
//
//    public boolean isUctTraverseWholeTree() {
//        return uctTraverseWholeTree;
//    }
//
//    public void setUctTraverseWholeTree(boolean uctTraverseWholeTree) {
//        this.uctTraverseWholeTree = uctTraverseWholeTree;
//    }
//
//    public boolean isAddSimulationResultToTree() {
//        return addSimulationResultToTree;
//    }
//
//    public void setAddSimulationResultToTree(boolean addSimulationResultToTree) {
//        this.addSimulationResultToTree = addSimulationResultToTree;
//    }
//
//    public void setLogSteps(boolean logSteps) {
//        this.logSteps = logSteps;
//    }
//
//    public void setLogFilePath(String logFilePath) {
//        this.logFilePath = logFilePath;
//    }
//}
