///**
// * Copyright (C) 2007 - 2016, Jens Lehmann
// *
// * This file is part of DL-Learner.
// *
// * DL-Learner is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 3 of the License, or
// * (at your option) any later version.
// *
// * DL-Learner is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.dllearner.algorithms.layerwise;
//
//import com.google.common.collect.Sets;
//
//import org.dllearner.accuracymethods.AccMethodPredAcc;
//import org.dllearner.accuracymethods.AccMethodPredAccNegOnly;
//import org.dllearner.algorithms.celoe.OEHeuristicRuntime;
//import org.dllearner.core.*;
//import org.dllearner.core.config.ConfigOption;
//import org.dllearner.core.owl.ClassHierarchy;
//import org.dllearner.core.owl.DatatypePropertyHierarchy;
//import org.dllearner.core.owl.ObjectPropertyHierarchy;
//import org.dllearner.kb.OWLAPIOntology;
//import org.dllearner.learningproblems.ClassAsInstanceLearningProblem;
//import org.dllearner.learningproblems.ClassLearningProblem;
//import org.dllearner.learningproblems.PosNegLP;
//import org.dllearner.learningproblems.PosNegLPStandard;
//import org.dllearner.learningproblems.PosOnlyLP;
//import org.dllearner.reasoning.ClosedWorldReasoner;
//import org.dllearner.reasoning.OWLAPIReasoner;
//import org.dllearner.reasoning.ReasonerImplementation;
//import org.dllearner.reasoning.SPARQLReasoner;
//import org.dllearner.refinementoperators.*;
//import org.dllearner.utilities.*;
//import org.dllearner.utilities.datastructures.SearchTree;
//import org.dllearner.utilities.owl.*;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Marker;
//import org.slf4j.MarkerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
//import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
///**
// * The CELOE (Class Expression Learner for Ontology Engineering) algorithm.
// * It adapts and extends the standard supervised learning algorithm for the
// * ontology engineering use case.
// * 
// * @author Jens Lehmann
// *
// */
//@SuppressWarnings("CloneDoesntCallSuperClone")
//@ComponentAnn(name="CELOELayerwise", shortName="celoe_lw", version=1.0, description="CELOELayerwise is an adapted and extended version of the CELOE algorithm")
//public class CELOELayerwise2 extends AbstractCELA implements Cloneable{
//
//	private static final Logger logger = LoggerFactory.getLogger(CELOELayerwise2.class);
//
//	@ConfigOption(description = "the refinement operator instance to use")
//	protected LengthLimitedRefinementOperator operator;
//
////	protected SearchTree<OENode> searchTree;
//	protected LayerwiseSearchTree<LayerwiseSearchTreeNode> searchTree;
//	@ConfigOption(defaultValue="celoe_heuristic")
////	protected AbstractHeuristic heuristic; // = new OEHeuristicRuntime();
//	protected LayerwiseAbstractHeuristic heuristic;
//	// the class with which we start the refinement process
//	@ConfigOption(defaultValue = "owl:Thing",
//			description = "You can specify a start class for the algorithm. To do this, you have to use Manchester OWL syntax either with full IRIs or prefixed IRIs.",
//			exampleValue = "ex:Male or http://example.org/ontology/Female")
//	private OWLClassExpression startClass;
//
//	// all descriptions in the search tree plus those which were too weak (for fast redundancy check)
//	protected TreeSet<OWLClassExpression> descriptions;
//
//
//	// if true, then each solution is evaluated exactly instead of approximately
//	// private boolean exactBestDescriptionEvaluation = false;
//	@ConfigOption(defaultValue="false", description="Use this if you are interested in only one suggestion and your learning problem has many (more than 1000) examples.")
//	protected boolean singleSuggestionMode;
//	protected OWLClassExpression bestDescription;
//	protected double bestAccuracy = Double.MIN_VALUE;
//
//	protected OWLClass classToDescribe;
//	// examples are either 1.) instances of the class to describe 2.) positive examples
//	// 3.) union of pos.+neg. examples depending on the learning problem at hand
//	private Set<OWLIndividual> examples;
//
//	// CELOE was originally created for learning classes in ontologies, but also
//	// works for other learning problem types
//	private boolean isClassLearningProblem;
//	private boolean isEquivalenceProblem;
//
//	// important parameters (non-config options but internal)
//	protected double noise;
//
//	protected boolean filterFollowsFromKB = false;
//
//	// less important parameters
//	// forces that one solution cannot be subexpression of another expression; this option is useful to get diversity
//	// but it can also suppress quite useful expressions
//	protected boolean forceMutualDifference = false;
//
//	// utility variables
//
//	// statistical variables
//	protected int expressionTests = 0;
////	private int minHorizExp = 0;
////	private int maxHorizExp = 0;
//	private long totalRuntimeNs = 0;
//
//	// TODO: turn those into config options
//
//
//	// important: do not initialise those with empty sets
//	// null = no settings for allowance / ignorance
//	// empty set = allow / ignore nothing (it is often not desired to allow no class!)
//	@ConfigOption(defaultValue="false", description="specifies whether to write a search tree")
//	protected boolean writeSearchTree = false;
//
//	@ConfigOption(defaultValue="log/searchTree.txt", description="file to use for the search tree")
//	protected String searchTreeFile = "log/searchTree.txt";	
//
//	@ConfigOption(defaultValue="false", description="specifies whether to replace the search tree in the log file after each run or append the new search tree")
//	protected boolean replaceSearchTree = false;
//
//	@ConfigOption(defaultValue="10", description="Sets the maximum number of results one is interested in. (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions).")
//	private int maxNrOfResults = 10;
//
//	@ConfigOption(defaultValue="0.0", description="the (approximated) percentage of noise within the examples")
//	private double noisePercentage = 0.0;
//
//	@ConfigOption(defaultValue="false", description="If true, then the results will not contain suggestions, which already follow logically from the knowledge base. Be careful, since this requires a potentially expensive consistency check for candidate solutions.")
//	private boolean filterDescriptionsFollowingFromKB = false;
//
//	@ConfigOption(defaultValue="false", description="If true, the algorithm tries to find a good starting point close to an existing definition/super class of the given class in the knowledge base.")
//	private boolean reuseExistingDescription = false;
//
//	@ConfigOption(defaultValue="0", description="The maximum number of candidate hypothesis the algorithm is allowed to test (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
//	private int maxClassExpressionTests = 0;
//
//	@ConfigOption(defaultValue="0", description = "The maximum number of candidate hypothesis the algorithm is allowed after an improvement in accuracy (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
//	private int maxClassExpressionTestsAfterImprovement = 0;
//
//	@ConfigOption(defaultValue = "0", description = "maximum execution of the algorithm in seconds after last improvement")
//	private int maxExecutionTimeInSecondsAfterImprovement = 0;
//
//	@ConfigOption(defaultValue="false", description="specifies whether to terminate when noise criterion is met")
//	private boolean terminateOnNoiseReached = false;
//
//	@ConfigOption(defaultValue="7", description="maximum depth of description")
//	protected double maxDepth = 7;
//
//	@ConfigOption(defaultValue="false", description="algorithm will terminate immediately when a correct definition is found")
//	private boolean stopOnFirstDefinition = true;
//	
//	@SuppressWarnings("unused")
//	private long timeLastImprovement = 0;
//	@ConfigOption(defaultValue = "false",  description = "whether to try and refine solutions which already have accuracy value of 1")
//	private boolean expandAccuracy100Nodes = false;
//	private double currentHighestAccuracy;
//
//	// option to keep track of best score during algorithm run
//	private boolean keepTrackOfBestScore = false;
//	private SortedMap<Long, Double> runtimeVsBestScore = new TreeMap<>();
//
//	protected ClassHierarchy classHierarchy;
//	protected ObjectPropertyHierarchy objectPropertyHierarchy;
//	protected DatatypePropertyHierarchy datatypePropertyHierarchy;
//	
//	
//	/**
//	 * ------------------------------------------------------------------------------------------------------------------------------
//	 * Layer-wise tree traverse parameters 
//	 */	
//	@ConfigOption(defaultValue="false", description="each iteration will traverse the complete tree to find the next node instead of picking the global best one")
//	protected boolean traverseWholeTree = false;
//	
//	@ConfigOption(defaultValue="0", description="each refinement must introduce at least so many new nodes into the tree")
//	protected int newNodesLowerbound = 0;
//	
//	@ConfigOption(defaultValue="0.01", description="break the traverse if the score increase smaller than this threshold")
//	protected double traverseBreakingThreshold = 0.01;
//	
//	/**
//	 * --------------------------------------------------------------------------------------------------------------------------------
//	 */
//
//	private int expressionTestCountLastImprovement;
//
//	protected int timesCeloeDecided = 0;
//	protected int timesCeloeUndecided = 0;
//	
//	private String logFile = "";
//	private String pathPfx = "";
//	protected int heStep;
//	protected boolean heCorr;	 
//	protected static Random rnd = new Random();	
//	protected long backTime = 0;
////	protected double bestPredAcc = Double.MIN_VALUE;
////	protected double bestNegOnlyAcc = Double.MIN_VALUE;	
//
//
//	public CELOELayerwise2() {}
//
//	public CELOELayerwise2(CELOELayerwise2 celoe){
//		setReasoner(celoe.reasoner);
//		setLearningProblem(celoe.learningProblem);
//
//		setAllowedConcepts(celoe.getAllowedConcepts());
//		setAllowedObjectProperties(celoe.getAllowedObjectProperties());
//		setAllowedDataProperties(celoe.getAllowedDataProperties());
//
//		setIgnoredConcepts(celoe.ignoredConcepts);
//		setIgnoredObjectProperties(celoe.getIgnoredObjectProperties());
//		setIgnoredDataProperties(celoe.getIgnoredDataProperties());
//
//		setExpandAccuracy100Nodes(celoe.expandAccuracy100Nodes);
//		setFilterDescriptionsFollowingFromKB(celoe.filterDescriptionsFollowingFromKB);
//		setHeuristic(celoe.heuristic);
//
//		setMaxClassExpressionTests(celoe.maxClassExpressionTests);
//		setMaxClassExpressionTestsAfterImprovement(celoe.maxClassExpressionTestsAfterImprovement);
//		setMaxDepth(celoe.maxDepth);
//		setMaxExecutionTimeInSeconds(celoe.getMaxExecutionTimeInSeconds());
//		setMaxExecutionTimeInSecondsAfterImprovement(celoe.maxExecutionTimeInSecondsAfterImprovement);
//		setMaxNrOfResults(celoe.maxNrOfResults);
//		setNoisePercentage(celoe.noisePercentage);
//
//		LengthLimitedRefinementOperator op = new RhoDRDown((RhoDRDown)celoe.operator);
//		try {
//			op.init();
//		} catch (ComponentInitException e) {
//			e.printStackTrace();
//		}
//		setOperator(op);
//
//
//		setReuseExistingDescription(celoe.reuseExistingDescription);
//		setSingleSuggestionMode(celoe.singleSuggestionMode);
//		setStartClass(celoe.startClass);
//		setStopOnFirstDefinition(celoe.stopOnFirstDefinition);
//		setTerminateOnNoiseReached(celoe.terminateOnNoiseReached);
//		setUseMinimizer(celoe.isUseMinimizer());
//
//		setWriteSearchTree(celoe.writeSearchTree);
//		setReplaceSearchTree(celoe.replaceSearchTree);
//	}
//
//	public CELOELayerwise2(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
//		super(problem, reasoner);
//	}
//
//	public static Collection<Class<? extends AbstractClassExpressionLearningProblem>> supportedLearningProblems() {
//		Collection<Class<? extends AbstractClassExpressionLearningProblem>> problems = new LinkedList<>();
//		problems.add(AbstractClassExpressionLearningProblem.class);
//		return problems;
//	}
//	
//	@Override
//	public void init() throws ComponentInitException {
//		
//		/*
//		 * Batch config settings
//		 * original celoe: heStep = 0, heCorr = true
//		 */
////		writeSearchTree = true;
////		traverseWholeTree = true;
////		newNodesLowerbound = 0
//		heStep = 1;
//		heCorr = false;
//		
//		baseURI = reasoner.getBaseURI();
//		prefixes = reasoner.getPrefixes();
//			
//		if(maxExecutionTimeInSeconds != 0 && maxExecutionTimeInSecondsAfterImprovement != 0) {
//			maxExecutionTimeInSeconds = Math.min(maxExecutionTimeInSeconds, maxExecutionTimeInSecondsAfterImprovement);
//		}
//		
//		// TODO add comment
//		classHierarchy = initClassHierarchy();
//		objectPropertyHierarchy = initObjectPropertyHierarchy();
//		datatypePropertyHierarchy = initDataPropertyHierarchy();
//
//		// if no one injected a heuristic, we use a default one
//		if(heuristic == null) {
////			heuristic = new OEHeuristicRuntime();
//			heuristic = new LayerwiseOEHeuristicRuntime();
//			heuristic.init();
//		}
//		
//		minimizer = new OWLClassExpressionMinimizer(dataFactory, reasoner);
//		
//		if(heStep == 0 && heCorr)
//			pathPfx = "original";
//		
//		if(heStep != 0 && heCorr)
//			pathPfx = "he" + String.valueOf(heStep) + "corr";
//		
//		if(heStep != 0 && !heCorr)
//			pathPfx = "he" + String.valueOf(heStep);
//		
//		String exampleFileName = searchTreeFile.substring(searchTreeFile.lastIndexOf("/"));
//		String resultPath = searchTreeFile.substring(0, searchTreeFile.lastIndexOf("/"));
//		String exampleName = exampleFileName.substring(0, exampleFileName.lastIndexOf("."));
//		
//		String heu = "";
//		String penalty = "";
//		String acc = "";
//		String tr = "";		
//		if(this.heuristic instanceof LayerwiseOEHeuristicRuntime) {
//			heu = "celoe";
//			penalty = String.valueOf(((LayerwiseOEHeuristicRuntime) this.heuristic).getExpansionPenaltyFactor());
//		}
//		else if(this.heuristic instanceof UCTHeuristic) {
//			heu = "count";
//			penalty = String.valueOf(UCTHeuristic.getExpansionPenaltyFactor());
//		}
//		else {
////			heu = this.heuristic.getClass().getSimpleName();
//		}
//			
//		
//		if(this.learningProblem.getAccuracyMethod() instanceof AccMethodPredAcc) {
//			acc = "pre";
//		}
//		else if (this.learningProblem.getAccuracyMethod() instanceof AccMethodPredAccNegOnly) {
//			acc = "neg";
//		}
//		else {
////			acc = this.learningProblem.getAccuracyMethod().getClass().getSimpleName();
//		}
//		
//		if(this.traverseWholeTree) {
//			tr = "tra";
//			tr += "_" + String.valueOf(this.traverseBreakingThreshold);
//		}			
//		else {
//			tr = "glo";
//		}
//		
//		
//		
//		if(!heu.isEmpty())
//			exampleName += "_" + heu;
//		
//		if(!penalty.isEmpty())
//			exampleName += "_" + penalty;
//		
//		if(!acc.isEmpty())
//			exampleName += "_" + acc;
//		
//		if(!tr.isEmpty())
//			exampleName += "_" + tr;		
//		
//		
//		logFile =  resultPath + "/" + pathPfx + exampleName + ".log";
//		searchTreeFile = resultPath + "/" + pathPfx + exampleName + ".tree";
//		File log = new File(logFile);	
//		if (log.getParentFile() != null) {
//			log.getAbsoluteFile().getParentFile().mkdirs();
//		}
//		Files.clearFile(log);
//		if (writeSearchTree) {						
//			File f = new File(searchTreeFile);	
//			if (f.getParentFile() != null) {
//				f.getAbsoluteFile().getParentFile().mkdirs();
//			}
//			Files.clearFile(f);
//		}
//		
//		// start at owl:Thing by default
//		startClass = OWLAPIUtils.classExpressionPropertyExpanderChecked(this.startClass, reasoner, dataFactory, this::computeStartClass, logger);
//
//		bestEvaluatedDescriptions = new EvaluatedDescriptionSet(maxNrOfResults);
//		
//		isClassLearningProblem = (learningProblem instanceof ClassLearningProblem);
//		
//		// we put important parameters in class variables
//		noise = noisePercentage/100d;
//
//		// (filterFollowsFromKB is automatically set to false if the problem
//		// is not a class learning problem
//		filterFollowsFromKB = filterDescriptionsFollowingFromKB && isClassLearningProblem;
//		
//		// actions specific to ontology engineering
//		if(isClassLearningProblem) {
//			ClassLearningProblem problem = (ClassLearningProblem) learningProblem;
//			classToDescribe = problem.getClassToDescribe();
//			isEquivalenceProblem = problem.isEquivalenceProblem();
//			
//			examples = reasoner.getIndividuals(classToDescribe);
//		} else if(learningProblem instanceof PosOnlyLP) {
//			examples = ((PosOnlyLP)learningProblem).getPositiveExamples();
//		} else if(learningProblem instanceof PosNegLP) {
//			examples = Sets.union(((PosNegLP)learningProblem).getPositiveExamples(),((PosNegLP)learningProblem).getNegativeExamples());
//		}
//		
//		// create a refinement operator and pass all configuration
//		// variables to it
//		if (operator == null) {
//			// we use a default operator and inject the class hierarchy for now
//			operator = new RhoDRDown();
//			((CustomStartRefinementOperator) operator).setStartClass(startClass);
//			((ReasoningBasedRefinementOperator) operator).setReasoner(reasoner);
//		}
//		if (operator instanceof CustomHierarchyRefinementOperator) {
//			((CustomHierarchyRefinementOperator) operator).setClassHierarchy(classHierarchy);
//			((CustomHierarchyRefinementOperator) operator).setObjectPropertyHierarchy(objectPropertyHierarchy);
//			((CustomHierarchyRefinementOperator) operator).setDataPropertyHierarchy(datatypePropertyHierarchy);
//		}
//		
//		if (!((AbstractRefinementOperator) operator).isInitialized())
//			operator.init();		
//		
//		initialized = true;
//	}
//	
//	@Override
//	public void start() {
//		stop = false;
//		isRunning = true;
//		reset();
//		nanoStartTime = System.nanoTime();
//		
//		currentHighestAccuracy = 0.0;
//		LayerwiseSearchTreeNode nextNode;
//
//		String s = "\nCurrent config: \n";
//		s += " - heStep: " + heStep + "\n";
//		s += " - heCorr: " + heCorr + "\n";
//		s += " - heuristic: " + this.heuristic.getClass().getSimpleName() + "\n";
//		s += " - acc: " + this.learningProblem.getAccuracyMethod().getClass().getSimpleName() + "\n";
//		s += " - traverse: " + this.traverseWholeTree + ", breaking: " + this.traverseBreakingThreshold + "\n";
//		s += " - start: " + startClass + "\n";
//		try {
//			saveLog(logFile, s);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		addNode(startClass, null);
//		
//		while (!terminationCriteriaSatisfied()) {
//			
////			logger.info("iteration: " + countIterations);
//			showIfBetterSolutionsFound();
//
//			// chose best node according to heuristics
//			long selectStart = System.nanoTime();
//			nextNode = getNextNodeToExpand();
//			long selectEnd = System.nanoTime();
//			selectTime += (selectEnd - selectStart);
//			
////			logger.info("selected: " + nodeToString(nextNode));
//			int horizExp = nextNode.getHorizontalExpansion();
//			
//			// apply refinement operator
////			List<LayerwiseSearchTreeNode> newNodes = new ArrayList<LayerwiseSearchTreeNode>();
//			
//			int nrNewNodes = 0, nrExpansion = 0;
////			while(nrNewNodes <= newNodesLowerbound) {
//				
//				TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
////				logger.info("nr refinements: " + refinements.size());
//				while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
//					// pick element from set
//					OWLClassExpression refinement = refinements.pollFirst();
//
//					// get length of class expression
//					int length = OWLClassExpressionUtils.getLength(refinement);
//					
//					// we ignore all refinements with lower length and too high depth
//					// (this also avoids duplicate node children)
//					if(heCorr && length > horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
//						// add node to search tree
//						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
//						if(newNode != null) {
//							nrNewNodes++;
////							if(nextNode.getBestChild() == null || heuristic.getNodeScore(newNode) > heuristic.getNodeScore(nextNode.getBestChild())) {
////								nextNode.setBestChild(newNode);
////								logger.info("setting best child of     " + nextNode.getExpression() + "    to    " + newNode.getExpression());
////							}
//						}
//					}
//					if(!heCorr && length >= horizExp && OWLClassExpressionUtils.getDepth(refinement) <= maxDepth) {
//						LayerwiseSearchTreeNode newNode = addNode(refinement, nextNode);
//						if(newNode != null) {
//							nrNewNodes++;
////							if(nextNode.getBestChild() == null || heuristic.getNodeScore(newNode) > heuristic.getNodeScore(nextNode.getBestChild())) {
////								nextNode.setBestChild(newNode);
////								logger.info("setting best child of     " + nextNode.getExpression() + "    to    " + newNode.getExpression());
////							}
//						}
//					}
//				}
//				nrExpansion ++;
////				logger.info("added " + nrNewNodes + " new nodes to " + nextNode);
////			}
//			
//			// without the condition, uncle works better
////			if(nrNewNodes>0)
//			if(traverseWholeTree && this.heuristic instanceof UCTHeuristic)
//				nextNode.setRecentlyExpanded(nrExpansion);
//
//			if (writeSearchTree) {
//				writeSearchTree(nextNode);
//			}
//			
//			showIfBetterSolutionsFound();
//			
////			if(nrNewNodes>0)
//			// if the heuristic is based on uct, then we need backpropagation
//			// otherwise, it is just level-wise tree search using celoe heuristic
//			if(traverseWholeTree && this.heuristic instanceof UCTHeuristic) {
//				backpropagate(nextNode);
//				// write the search tree (if configured)
//				if (writeSearchTree) {
//					writeSearchTree();
//				}
//			}
//
//			countIterations++;
//		}
//		
//		if(singleSuggestionMode) {
//			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
//		}
//		
//		// print some stats
//		printAlgorithmRunStats();			
//		
//		isRunning = false;
//	}
//	
//	/*
//	 * Compute the start class in the search space from which the refinement will start.
//	 * We use the intersection of super classes for definitions (since it needs to
//	 * capture all instances), but owl:Thing for learning subclasses (since it is
//	 * superfluous to add super classes in this case)
//	 */
//	protected OWLClassExpression computeStartClass() {
//		OWLClassExpression startClass = dataFactory.getOWLThing();
//		
//		if(isClassLearningProblem) {
//			if(isEquivalenceProblem) {
//				Set<OWLClassExpression> existingDefinitions = reasoner.getAssertedDefinitions(classToDescribe);
//				if(reuseExistingDescription && (existingDefinitions.size() > 0)) {
//					// the existing definition is reused, which in the simplest case means to
//					// use it as a start class or, if it is already too specific, generalise it
//					
//					// pick the longest existing definition as candidate
//					OWLClassExpression existingDefinition = null;
//					int highestLength = 0;
//					for(OWLClassExpression exDef : existingDefinitions) {
//						if(OWLClassExpressionUtils.getLength(exDef) > highestLength) {
//							existingDefinition = exDef;
//							highestLength = OWLClassExpressionUtils.getLength(exDef);
//						}
//					}
//					
//					LinkedList<OWLClassExpression> startClassCandidates = new LinkedList<>();
//					startClassCandidates.add(existingDefinition);
//					// hack for RhoDRDown
//					if(operator instanceof RhoDRDown) {
//						((RhoDRDown)operator).setDropDisjuncts(true);
//					}
//					LengthLimitedRefinementOperator upwardOperator = new OperatorInverter(operator);
//					
//					// use upward refinement until we find an appropriate start class
//					boolean startClassFound = false;
//					OWLClassExpression candidate;
//					do {
//						candidate = startClassCandidates.pollFirst();
//						if(((ClassLearningProblem)learningProblem).getRecall(candidate)<1.0) {
//							// add upward refinements to list
//							Set<OWLClassExpression> refinements = upwardOperator.refine(candidate, OWLClassExpressionUtils.getLength(candidate));
////							System.out.println("ref: " + refinements);
//							LinkedList<OWLClassExpression> refinementList = new LinkedList<>(refinements);
////							Collections.reverse(refinementList);
////							System.out.println("list: " + refinementList);
//							startClassCandidates.addAll(refinementList);
////							System.out.println("candidates: " + startClassCandidates);
//						} else {
//							startClassFound = true;
//						}
//					} while(!startClassFound);
//					startClass = candidate;
//					
//					if(startClass.equals(existingDefinition)) {
//						logger.info("Reusing existing class expression " + OWLAPIRenderers.toManchesterOWLSyntax(startClass) + " as start class for learning algorithm.");
//					} else {
//						logger.info("Generalised existing class expression " + OWLAPIRenderers.toManchesterOWLSyntax(existingDefinition) + " to " + OWLAPIRenderers.toManchesterOWLSyntax(startClass) + ", which is used as start class for the learning algorithm.");
//					}
//					
//					if(operator instanceof RhoDRDown) {
//						((RhoDRDown)operator).setDropDisjuncts(false);
//					}
//					
//				} else {
//					Set<OWLClassExpression> superClasses = reasoner.getClassHierarchy().getSuperClasses(classToDescribe, true);
//					if(superClasses.size() > 1) {
//						startClass = dataFactory.getOWLObjectIntersectionOf(superClasses);
//					} else if(superClasses.size() == 1){
//						startClass = (OWLClassExpression) superClasses.toArray()[0];
//					} else {
//						startClass = dataFactory.getOWLThing();
//						logger.warn(classToDescribe + " is equivalent to owl:Thing. Usually, it is not " +
//								"sensible to learn a class expression in this case.");
//					}
//				}
//			}
//		}
//		return startClass;
//	}
//	
//	/**
//	 * compute the decidability of celoe based on the score of the 
//	 * @param iterator
//	 * @param sorted
//	 */
//	protected void getCELOEDecidability (Iterator<LayerwiseSearchTreeNode> iterator, boolean sorted) {
//
//		if(!sorted)
//			return;
//		
//		double highest_score = Double.MIN_VALUE;
//		if(iterator.hasNext())
//			highest_score = heuristic.getNodeScore(iterator.next());
//		
//		int nrBestNodes = 1;
//		while(iterator.hasNext()) {
//			// Only look at the nodes with the highest CELOE score
//			double score = heuristic.getNodeScore(iterator.next());
//			if (score < highest_score) {
//				break;
//			}
//			nrBestNodes++;			
//		}
//
//		if (nrBestNodes == 1) {
//			timesCeloeDecided++;
//		} 
//		else {
//			timesCeloeUndecided++;
//		}
//	}
//	
//	private LayerwiseSearchTreeNode getNextNodeToExpand() {
//	
//		if (traverseWholeTree) {
//            /* Traverse the tree from root according to UCT */
//            LayerwiseSearchTreeNode currentNode = searchTree.getRoot();            
//            while (!currentNode.getChildren().isEmpty()) {
////            		List<LayerwiseSearchTreeNode> children = new ArrayList<LayerwiseSearchTreeNode>();
////            		children.addAll(currentNode.getChildren());
////            		Collections.sort(children, heuristic.reversed());
////            		LayerwiseSearchTreeNode child = selectBestNode(children.iterator());
//            		
//            		LayerwiseSearchTreeNode child = currentNode.descendingIterator().next();
//            		// if the current node is better than all its children
//            		// then select the current node       
//            		double currentScore = heuristic.getNodeScore(currentNode);
//            		double childScore = heuristic.getNodeScore(child);
//            		if (childScore - currentScore <=  traverseBreakingThreshold) {
//            			break;
//            		}            		
////            		 otherwise, go to the next level 
//            		currentNode = child;
//            }
//            return currentNode;
//        } 
//		else {
////			Iterator<LayerwiseSearchTreeNode> it = searchTree.descendingIterator();
////
////			while(it.hasNext()) {
////				LayerwiseSearchTreeNode node = it.next();
////				if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
////						return node;
////				} else {
////					if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
////						return node;
////					}
////				}
////			}
////			// this should practically never be called, since for any reasonable learning
////			// task, we will always have at least one node with less than 100% accuracy
////			throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
//			throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
//		}
//	}
//	
//	protected List<LayerwiseSearchTreeNode> selectBestNodes (Iterator<LayerwiseSearchTreeNode> iterator){
//		
//		// if empty input, then return null
//		if(!iterator.hasNext())
//			return null; 
//				
//		
//		List<LayerwiseSearchTreeNode> candidates = new ArrayList<LayerwiseSearchTreeNode>();
//		candidates.add(iterator.next());
//		if(!iterator.hasNext())
//			return candidates;
//		
//		// since the input is sorted, then the first one has the best score
//		double highest_score = heuristic.getNodeScore(candidates.get(0));	
//		while(iterator.hasNext()) {
//			LayerwiseSearchTreeNode node = iterator.next();
//			double score = heuristic.getNodeScore(node);
//			
//			// since sorted, if the current score smaller than the best one
//			// we have collected all candidates --> break the loop
//			if (score < highest_score) {
//				break;
//			}
//
//			// Add suitable nodes to the list
//			if (isExpandAccuracy100Nodes() && node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//				candidates.add(node);
//				highest_score = score;
//			} else {
//				if(node.getAccuracy() < 1.0 || node.getHorizontalExpansion() < OWLClassExpressionUtils.getLength(node.getDescription())) {
//					candidates.add(node);
//					highest_score = score;
//				}
//			}
//		}
//		
//		return candidates;
//	}
//	
//	// select one node from a sorted iterator: best node at the front
//	protected LayerwiseSearchTreeNode selectBestNode (Iterator<LayerwiseSearchTreeNode> iterator) {
//		List<LayerwiseSearchTreeNode> candidates = selectBestNodes(iterator);
//		
////		System.out.println("selecting best node");
//		
//		if (candidates.size() == 1) {
//			return candidates.get(0);
//		} 
//		else if(candidates.isEmpty()){
//            // this should practically never be called, since for any reasonable learning
//            // task, we will always have at least one node with less than 100% accuracy
//            throw new RuntimeException("CELOE could not find any node with lesser accuracy.");
//        }
//		
//		return candidates.get(0);
//		
////		// if there are several candidates: sort by length
////		if(candidates.size() > 1) {					
////			// sort the candidates by length
////			Collections.sort(candidates, new Comparator<LayerwiseSearchTreeNode>() {
////				@Override
////				public int compare(LayerwiseSearchTreeNode a, LayerwiseSearchTreeNode b) {
////					return OWLClassExpressionUtils.getLength(a.getExpression()) - OWLClassExpressionUtils.getLength(b.getExpression());
////				}							    
////			});							
////		}
////		
//////		System.out.println("sorted by length");
//////		System.out.println("candidates: " + candidates.toString());
////		
////		// find the shortest nodes 
////		Iterator<LayerwiseSearchTreeNode> candidatesIt = candidates.iterator();
////		List<LayerwiseSearchTreeNode> shortestNodes = new ArrayList<LayerwiseSearchTreeNode>();
////		shortestNodes.add(candidatesIt.next());
//////		System.out.println("candidates: " + shortestNodes.toString());
////		int shortest = OWLClassExpressionUtils.getLength(shortestNodes.get(0).getExpression());
////		while(candidatesIt.hasNext()) {
////			LayerwiseSearchTreeNode next = candidatesIt.next();
////			if(OWLClassExpressionUtils.getLength(next.getExpression()) > shortest) {
////				break;
////			}
////			shortestNodes.add(next);
//////			System.out.println("candidates: " + shortestNodes.toString());
////		}
////		
//////		System.out.println("candidates: " + shortestNodes.toString());
////		
////		// if undecided, randomly pick one
////		if(shortestNodes.size() > 1) {
////			int i = rnd.nextInt(shortestNodes.size()-1);
//////			System.out.println("picking " + i);
////    			return shortestNodes.get(i);
////		}
////		else {
////			// it cannot be other case
////			return shortestNodes.get(0);
////		}		
//	} 
//	
//	// expand node horizontically
//	protected TreeSet<OWLClassExpression> refineNode(LayerwiseSearchTreeNode node) {
//		
//		long refineStart = System.nanoTime();
//		// we have to remove and add the node since its heuristic evaluation changes through the expansion
//		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
//		// otherwise you may see rarely occurring but critical false ordering in the nodes set)
//		searchTree.updatePrepare(node);
//		int horizExp = node.getHorizontalExpansion();
//		
//		int localHeStep = heStep;
////		if(node.isRoot() && node.getExpansionCounter() == 0) {
////			localHeStep = 3;
////		}
//		
//		TreeSet<OWLClassExpression> refinements = new TreeSet<OWLClassExpression>();
//		if(heCorr)
//			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1+localHeStep);
//		else
//			refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+localHeStep);
//		
//		if(localHeStep == 0)
//			node.incHorizontalExpansion();
//		else {
//			for(int i = 0; i < localHeStep; i++) {
//				node.incHorizontalExpansion();
//			}
//		}		
//		
//		node.setRefinementCount(refinements.size());
//		searchTree.updateDone(node);
//		
//		long refineEnd = System.nanoTime();
//		refineTime += (refineEnd - refineStart);
//		
//		return refinements;
//	}
//	
//	/**
//	 * recursively update the uct statistics from this node to the root
//	 * this is the backpropagation of UCT
//	 * @return
//	 */
//	private void backpropagate (LayerwiseSearchTreeNode node) {
//		
//		long backStart = System.nanoTime();
//		node.updateCounter();
//		//		searchTree.update(node);
//
//		if(!node.isRoot()) {
//			backpropagate(node.getParent());
//		}else {
//			// during backpropagation, once we meet the root node, we update the global counter of UCT
//			if(heuristic instanceof UCTHeuristic)
//				UCTHeuristic.incTotalExpansion();
//		}
//		long backEnd = System.nanoTime();
//		backTime += (backEnd - backStart);
//	}
//	
//	/**
//	 * Add node to search tree if it is not too weak.
//	 * @return TRUE if node was added and FALSE otherwise
//	 */
//	protected LayerwiseSearchTreeNode addNode(OWLClassExpression description, LayerwiseSearchTreeNode parentNode) {
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
////		double pAccuracy = ((PosNegLPStandard) learningProblem).getAccuracyOrTooWeakExact(description, noise);
//		
//		/**
//		 * workaround for the reasoner bug
//		 */
////		if(bestPredAcc < pAccuracy)
////			bestPredAcc = pAccuracy;
////		if(bestNegOnlyAcc < accuracy)
////			bestNegOnlyAcc = accuracy;
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
////		LayerwiseSearchTreeNode node = new LayerwiseSearchTreeNode(description, accuracy);
//		
//		long treeStart = System.nanoTime();
//		LayerwiseSearchTreeNode node = new LayerwiseSearchTreeNode(description, accuracy, heCorr, heuristic);
//		searchTree.addNode(parentNode, node);
//			
//		long treeEnd = System.nanoTime();
//		treeTime += (treeEnd - treeStart);
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
//				(accuracy > accThreshold ||
//				(accuracy >= accThreshold && OWLClassExpressionUtils.getLength(description) < worst.getDescriptionLength()));
//		}
//		
//		long rewriteStart = System.nanoTime();
//		if(isCandidate) {
//			//TODO: rewrite forte/uncle_small will produce "male and married only nothing" from "male and married max 0 thing"
//			//      and the reasoner will have different instance retrieval results based on this
//			OWLClassExpression niceDescription = rewrite(node.getExpression());
////			OWLClassExpression niceDescription = description;
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
////						System.out.println("shorter: " + ed.getDescription());
//						shorterDescriptionExists = true;
//						break;
//					}
//				}
//			}
//			
////			System.out.println("shorter description? " + shorterDescriptionExists + " nice: " + niceDescription);
//			if(!shorterDescriptionExists) {
//				if(!filterFollowsFromKB || !((ClassLearningProblem)learningProblem).followsFromKB(niceDescription)) {
////					System.out.println(node + "->" + niceDescription);
//					bestEvaluatedDescriptions.add(niceDescription, accuracy, learningProblem);
////					System.out.println("acc: " + accuracy);
////					System.out.println(bestEvaluatedDescriptions);
//				}
//			}
//			
////			bestEvaluatedDescriptions.add(node.getDescription(), accuracy, learningProblem);
//			
////			System.out.println(bestEvaluatedDescriptions.getSet().size());
//		}
//		long rewriteEnd = System.nanoTime();
//		rewriteTime += (rewriteEnd - rewriteStart);
//		
//		return node;
//	}
//	
//	// checks whether the class expression is allowed
//	protected boolean isDescriptionAllowed(OWLClassExpression description, LayerwiseSearchTreeNode parentNode) {
//		if(isClassLearningProblem) {
//			if(isEquivalenceProblem) {
//				// the class to learn must not appear on the outermost property level
//				if(occursOnFirstLevel(description, classToDescribe)) {
//					return false;
//				}
//				if(occursOnSecondLevel(description, classToDescribe)) {
//					return false;
//				}
//			} else {
//				// none of the superclasses of the class to learn must appear on the
//				// outermost property level
//				TreeSet<OWLClassExpression> toTest = new TreeSet<>();
//				toTest.add(classToDescribe);
//				while(!toTest.isEmpty()) {
//					OWLClassExpression d = toTest.pollFirst();
//					if(occursOnFirstLevel(description, d)) {
//						return false;
//					}
//					toTest.addAll(reasoner.getClassHierarchy().getSuperClasses(d));
//				}
//			}
//		} else if (learningProblem instanceof ClassAsInstanceLearningProblem) {
//			return true;
//		}
//		
//		// perform forall sanity tests
//		if (parentNode != null &&
//				(ConceptTransformation.getForallOccurences(description) > ConceptTransformation.getForallOccurences(parentNode.getDescription()))) {
//			// we have an additional \forall construct, so we now fetch the contexts
//			// in which it occurs
//			SortedSet<PropertyContext> contexts = ConceptTransformation.getForallContexts(description);
//			SortedSet<PropertyContext> parentContexts = ConceptTransformation.getForallContexts(parentNode.getDescription());
//			contexts.removeAll(parentContexts);
////			System.out.println("parent description: " + parentNode.getDescription());
////			System.out.println("description: " + description);
////			System.out.println("contexts: " + contexts);
//			// we now have to perform sanity checks: if \forall is used, then there
//			// should be at least on class instance which has a filler at the given context
//			for(PropertyContext context : contexts) {
//				// transform [r,s] to \exists r.\exists s.\top
//				OWLClassExpression existentialContext = context.toExistentialContext();
//				boolean fillerFound = false;
//				if(reasoner instanceof SPARQLReasoner) {
//					SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(existentialContext);
//					fillerFound = !Sets.intersection(individuals, examples).isEmpty();
//				} else {
//					for(OWLIndividual instance : examples) {
//						if(reasoner.hasType(existentialContext, instance)) {
//							fillerFound = true;
//							break;
//						}
//					}
//				}
//				
//				// if we do not find a filler, this means that putting \forall at
//				// that position is not meaningful
//				if(!fillerFound) {
//					return false;
//				}
//			}
//		}
//		
//		// we do not want to have negations of sibling classes on the outermost level
//		// (they are expressed more naturally by saying that the siblings are disjoint,
//		// so it is reasonable not to include them in solutions)
////		Set<OWLClassExpression> siblingClasses = reasoner.getClassHierarchy().getSiblingClasses(classToDescribe);
////		for now, we just disable negation
//		
//		return true;
//	}
//	
//	// determine whether a named class occurs on the outermost level, i.e. property depth 0
//	// (it can still be at higher depth, e.g. if intersections are nested in unions)
//	private boolean occursOnFirstLevel(OWLClassExpression description, OWLClassExpression cls) {
//		return !cls.isOWLThing() && (description instanceof OWLNaryBooleanClassExpression && ((OWLNaryBooleanClassExpression) description).getOperands().contains(cls));
////        return description.containsConjunct(cls) ||
////                (description instanceof OWLObjectUnionOf && ((OWLObjectUnionOf) description).getOperands().contains(cls));
//	}
//	
//	// determine whether a named class occurs on the outermost level, i.e. property depth 0
//		// (it can still be at higher depth, e.g. if intersections are nested in unions)
//		private boolean occursOnSecondLevel(OWLClassExpression description, OWLClassExpression cls) {
////			SortedSet<OWLClassExpression> superClasses = reasoner.getSuperClasses(cls);
////			if(description instanceof OWLObjectIntersectionOf) {
////				List<OWLClassExpression> operands = ((OWLObjectIntersectionOf) description).getOperandsAsList();
////
////				for (OWLClassExpression op : operands) {
////					if(superClasses.contains(op) ||
////							(op instanceof OWLObjectUnionOf && !Sets.intersection(((OWLObjectUnionOf)op).getOperands(),superClasses).isEmpty())) {
////						for (OWLClassExpression op2 : operands) {
////							if((op2 instanceof OWLObjectUnionOf && ((OWLObjectUnionOf)op2).getOperands().contains(cls))) {
////								return true;
////							}
////						}
////					}
////				}
////
////				for (OWLClassExpression op1 : operands) {
////					for (OWLClassExpression op2 : operands) {
////						if(!op1.isAnonymous() && op2 instanceof OWLObjectUnionOf) {
////							 for (OWLClassExpression op3 : ((OWLObjectUnionOf)op2).getOperands()) {
////								if(!op3.isAnonymous()) {// A AND B with Disj(A,B)
////									if(reasoner.isDisjoint(op1.asOWLClass(), op3.asOWLClass())) {
////										return true;
////									}
////								} else {// A AND NOT A
////									if(op3 instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)op3).getOperand().equals(op1)) {
////										return true;
////									}
////								}
////							}
////						}
////					}
////				}
////			}
//			
//			return false;
//	    }
//	
//	protected boolean terminationCriteriaSatisfied() {
//		return
//		stop ||
////		(maxClassExpressionTestsAfterImprovement != 0 && (expressionTests - expressionTestCountLastImprovement >= maxClassExpressionTestsAfterImprovement)) ||
////		(maxClassExpressionTests != 0 && (expressionTests >= maxClassExpressionTests)) ||
////		(maxExecutionTimeInSecondsAfterImprovement != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSecondsAfterImprovement* 1000000000L))) ||
////		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
//		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime - logTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
//		(terminateOnNoiseReached && (100*getCurrentlyBestAccuracy()>=100-noisePercentage)) ||
//		(stopOnFirstDefinition && (getCurrentlyBestAccuracy() >= 1));
//	}
//	
//	protected void reset() {
//		// set all values back to their default values (used for running
//		// the algorithm more than once)
////		searchTree = new SearchTree<>(heuristic);
//		searchTree = new LayerwiseSearchTree<>(heuristic);
//		descriptions = new TreeSet<>();
//		bestEvaluatedDescriptions.getSet().clear();
//		expressionTests = 0;
//		runtimeVsBestScore.clear();
//	}
//	
//	protected void printAlgorithmRunStats() {
//		if (stop) {
//			logger.info("Algorithm stopped ("+expressionTests+" descriptions tested). " + searchTree.size() + " nodes in the search tree.\n");
//			String s = "Algorithm stopped ("+expressionTests+" descriptions tested). " + searchTree.size() + " nodes in the search tree.";
//			try {
//				saveLog(logFile, s);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			totalRuntimeNs = System.nanoTime()-nanoStartTime;
////			logger.info("Algorithm terminated successfully (time: " + Helper.prettyPrintNanoSeconds(totalRuntimeNs) + ", "+expressionTests+" descriptions tested, "  + searchTree.size() + " nodes in the search tree).\n");
////            logger.info(reasoner.toString());
//            String s = "\nAlgorithm terminated successfully (time: " + Helper.prettyPrintNanoSeconds(totalRuntimeNs) + ", "+expressionTests+" descriptions tested, "  + searchTree.size() + " nodes in the search tree).\n";
//            	s += reasoner.toString();
//        		try {
//        			saveLog(logFile, s);
//        		} catch (FileNotFoundException e) {
//        			// TODO Auto-generated catch block
//        			e.printStackTrace();
//        		}
//            	
//            	s = "Iterations: " + countIterations + "\n";
////        		s += "Times CELOE undecided: " + timesCeloeUndecided + "\n";
////        		s += "Times CELOE decided: " + timesCeloeDecided + "\n";
////        		s += "% CELOE undecided: "+ ((double)timesCeloeUndecided)/((double)timesCeloeUndecided+timesCeloeDecided) + "\n";
//        		s += "Nodes in search tree: " + searchTree.size() + "\n";
//        		s += "Expressions tested: " + expressionTests + "\n";
//        		s += "time: " + Helper.prettyPrintNanoSeconds(System.nanoTime() - nanoStartTime - logTime) + " (log: " + Helper.prettyPrintNanoSeconds(logTime) + " )\n";
////        		s += " - log time: " + Helper.prettyPrintNanoSeconds(logTime) + "\n";
//        		s += " - refinement time: " + Helper.prettyPrintNanoSeconds(refineTime) + "\n";        		
//        		s += " - tree time: " + Helper.prettyPrintNanoSeconds(treeTime) + "\n";
//        		s += " - rewrite time: " + Helper.prettyPrintNanoSeconds(rewriteTime) + "\n";
//        		s += " - select time: " + Helper.prettyPrintNanoSeconds(selectTime) + "\n";
//        		s += " - back time: " + Helper.prettyPrintNanoSeconds(backTime) + "\n\n";
//        		
//        		s += "solutions:\n" + getSolutionString();
//        		try {
//        			saveLog(logFile, s);
//        		} catch (FileNotFoundException e) {
//        			// TODO Auto-generated catch block
//        			e.printStackTrace();
//        		}
// 		}
//	}
//	
//	protected void showIfBetterSolutionsFound() {
//		if(!singleSuggestionMode && bestEvaluatedDescriptions.getBestAccuracy() > currentHighestAccuracy) {
//			currentHighestAccuracy = bestEvaluatedDescriptions.getBestAccuracy();
//			expressionTestCountLastImprovement = expressionTests;
//			timeLastImprovement = System.nanoTime();
//			long durationInMillis = getCurrentRuntimeInMilliSeconds();
//			String durationStr = getDurationAsString(durationInMillis);
//
//			// track new best accuracy if enabled
//			if(keepTrackOfBestScore) {
//				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), currentHighestAccuracy);
////				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), bestPredAcc);
//			}
////			logger.info("more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
////			logger.info("more accurate (" + dfPercent.format(bestPredAcc) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
//			try {
//				saveLog(logFile, "more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
////				saveLog(logFile, "more accurate (" + dfPercent.format(bestPredAcc) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
//	
//	private StringBuilder nodeToString (LayerwiseSearchTreeNode node) {
//		StringBuilder treeString = new StringBuilder();
////		treeString.append(node.getDescription().toString());
////		treeString.append(" [acc:");
////		treeString.append(String.format("%.0f%%", 100*node.getAccuracy()));
////		treeString.append(", he:");
////		treeString.append(node.getHorizontalExpansion());
//		String nodeStr = node.toString();
//		nodeStr = nodeStr.substring(0, nodeStr.length()-1);
//		treeString.append(nodeStr);
//		treeString.append(", celoe:");
//		treeString.append(heuristic.getNodeScore(node));
//		treeString.append("]\n");
//		
//		return treeString;
//	}
//	
//	private StringBuilder toTreeString(LayerwiseSearchTreeNode node, int depth) {
//		StringBuilder treeString = new StringBuilder();
//		for (int i = 0; i < depth - 1; i++) {
//			treeString.append("  ");
//		}
//		if (depth != 0) {
//			treeString.append("|--> ");
//		}
//		
//		treeString.append(nodeToString(node));
//		//treeString.append(node.toString()).append("\n");
////		treeString.append(node.getDescription().toString());
////		treeString.append(" [acc:");
////		treeString.append(String.format("%.0f%%", 100*node.getAccuracy()));
////		treeString.append(", he:");
////		treeString.append(node.getHorizontalExpansion());
////		treeString.append(", celoe:");
////		treeString.append(heuristic.getNodeScore(node));
////		treeString.append("]\n");
//
//		for (LayerwiseSearchTreeNode child : node.getChildren()) {
//			treeString.append(toTreeString(child, depth+1));
//		}
//		return treeString;
//	}
//	
//	protected void writeSearchTree() {
//		StringBuilder treeString = toTreeString(searchTree.getRoot(), 0);
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
//	protected void writeSearchTree(LayerwiseSearchTreeNode selected) {
//		long logStart = System.nanoTime();
//		StringBuilder treeString = new StringBuilder("------------- Iteration " + countIterations + " -------------\n");
//		treeString.append("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
//		treeString.append("selected node: " + nodeToString(selected));
//		treeString.append(toTreeString(searchTree.getRoot(), 0));
//		treeString.append("\n");
//
//		// replace or append
//		if (isReplaceSearchTree()) {
//			Files.createFile(new File(getSearchTreeFile()), treeString.toString());
//		} else {
//			Files.appendToFile(new File(getSearchTreeFile()), treeString.toString());
//		}
//		
//		long logEnd = System.nanoTime();
//		logTime += (logEnd - logStart);
//	}
//	
////	protected void updateMinMaxHorizExp(LayerwiseSearchTreeNode node) {
////		int newHorizExp = node.getHorizontalExpansion();
////		
////		// update maximum value
////		if(node.isRoot())
////			maxHorizExp = Math.max(maxHorizExp, newHorizExp+3);
////		else
////			maxHorizExp = Math.max(maxHorizExp, newHorizExp+heStep);
////		
////		// we just expanded a node with minimum horizontal expansion;
////		// we need to check whether it was the last one
////		if(minHorizExp == newHorizExp - 1) {
////			
////			// the best accuracy that a node can achieve
////			double scoreThreshold = heuristic.getNodeScore(node) + 1 - node.getAccuracy();
////			
////			for(LayerwiseSearchTreeNode n : searchTree.descendingSet()) {
////				if(n != node) {
////					if(n.getHorizontalExpansion() == minHorizExp) {
////						// we can stop instantly when another node with min.
////						return;
////					}
////					if(heuristic.getNodeScore(n) < scoreThreshold) {
////						// we can stop traversing nodes when their score is too low
////						break;
////					}
////				}
////			}
////			
////			// inc. minimum since we found no other node which also has min. horiz. exp.
////			minHorizExp++;
////			
//////			System.out.println("minimum horizontal expansion is now " + minHorizExp);
////		}
////	}
//	
//	@Override
//	public OWLClassExpression getCurrentlyBestDescription() {
//		EvaluatedDescription<? extends Score> ed = getCurrentlyBestEvaluatedDescription();
//		return ed == null ? null : ed.getDescription();
//	}
//
//	@Override
//	public List<OWLClassExpression> getCurrentlyBestDescriptions() {
//		return bestEvaluatedDescriptions.toDescriptionList();
//	}
//	
//	@Override
//	public EvaluatedDescription<? extends Score> getCurrentlyBestEvaluatedDescription() {
//		return bestEvaluatedDescriptions.getBest();
//	}
//	
//	@Override
//	public NavigableSet<? extends EvaluatedDescription<? extends Score>> getCurrentlyBestEvaluatedDescriptions() {
//		return bestEvaluatedDescriptions.getSet();
//	}
//
//	private double getCurrentlyBestAccuracy() {
//		return bestEvaluatedDescriptions.getBest().getAccuracy();		
////		return bestPredAcc;
//	}
//	
//	@Override
//	public boolean isRunning() {
//		return isRunning;
//	}
//	
//	@Override
//	public void stop() {
//		stop = true;
//	}
//
////	public int getMaximumHorizontalExpansion() {
////		return maxHorizExp;
////	}
////
////	public int getMinimumHorizontalExpansion() {
////		return minHorizExp;
////	}
//	
//	/**
//	 * @return the expressionTests
//	 */
//	public int getClassExpressionTests() {
//		return expressionTests;
//	}
//
//	public LengthLimitedRefinementOperator getOperator() {
//		return operator;
//	}
//
//	@Autowired(required=false)
//	public void setOperator(LengthLimitedRefinementOperator operator) {
//		this.operator = operator;
//	}
//
//	public OWLClassExpression getStartClass() {
//		return startClass;
//	}
//
//	public void setStartClass(OWLClassExpression startClass) {
//		this.startClass = startClass;
//	}
//	
//	public boolean isWriteSearchTree() {
//		return writeSearchTree;
//	}
//
//	public void setWriteSearchTree(boolean writeSearchTree) {
//		this.writeSearchTree = writeSearchTree;
//	}
//
//	public String getSearchTreeFile() {
//		return searchTreeFile;
//	}
//
//	public void setSearchTreeFile(String searchTreeFile) {
//		this.searchTreeFile = searchTreeFile;
//	}
//
//	public int getMaxNrOfResults() {
//		return maxNrOfResults;
//	}
//
//	public void setMaxNrOfResults(int maxNrOfResults) {
//		this.maxNrOfResults = maxNrOfResults;
//	}
//
//	public double getNoisePercentage() {
//		return noisePercentage;
//	}
//
//	public void setNoisePercentage(double noisePercentage) {
//		this.noisePercentage = noisePercentage;
//	}
//
//	public boolean isFilterDescriptionsFollowingFromKB() {
//		return filterDescriptionsFollowingFromKB;
//	}
//
//	public void setFilterDescriptionsFollowingFromKB(boolean filterDescriptionsFollowingFromKB) {
//		this.filterDescriptionsFollowingFromKB = filterDescriptionsFollowingFromKB;
//	}
//
//	public boolean isReplaceSearchTree() {
//		return replaceSearchTree;
//	}
//
//	public void setReplaceSearchTree(boolean replaceSearchTree) {
//		this.replaceSearchTree = replaceSearchTree;
//	}
//
//	public boolean isTerminateOnNoiseReached() {
//		return terminateOnNoiseReached;
//	}
//
//	public void setTerminateOnNoiseReached(boolean terminateOnNoiseReached) {
//		this.terminateOnNoiseReached = terminateOnNoiseReached;
//	}
//
//	public boolean isReuseExistingDescription() {
//		return reuseExistingDescription;
//	}
//
//	public void setReuseExistingDescription(boolean reuseExistingDescription) {
//		this.reuseExistingDescription = reuseExistingDescription;
//	}
//
//	public LayerwiseAbstractHeuristic getHeuristic() {
//		return heuristic;
//	}
//
//	@Autowired(required=false)
//	public void setHeuristic(LayerwiseAbstractHeuristic heuristic) {
//		this.heuristic = heuristic;
//	}
//
//	public int getMaxExecutionTimeInSecondsAfterImprovement() {
//		return maxExecutionTimeInSecondsAfterImprovement;
//	}
//
//	public void setMaxExecutionTimeInSecondsAfterImprovement(
//			int maxExecutionTimeInSecondsAfterImprovement) {
//		this.maxExecutionTimeInSecondsAfterImprovement = maxExecutionTimeInSecondsAfterImprovement;
//	}
//	
//	public boolean isSingleSuggestionMode() {
//		return singleSuggestionMode;
//	}
//
//	public void setSingleSuggestionMode(boolean singleSuggestionMode) {
//		this.singleSuggestionMode = singleSuggestionMode;
//	}
//
//	public int getMaxClassExpressionTests() {
//		return maxClassExpressionTests;
//	}
//
//	public void setMaxClassExpressionTests(int maxClassExpressionTests) {
//		this.maxClassExpressionTests = maxClassExpressionTests;
//	}
//
//	public int getMaxClassExpressionTestsAfterImprovement() {
//		return maxClassExpressionTestsAfterImprovement;
//	}
//
//	public void setMaxClassExpressionTestsAfterImprovement(
//			int maxClassExpressionTestsAfterImprovement) {
//		this.maxClassExpressionTestsAfterImprovement = maxClassExpressionTestsAfterImprovement;
//	}
//
//	public double getMaxDepth() {
//		return maxDepth;
//	}
//
//	public void setMaxDepth(double maxDepth) {
//		this.maxDepth = maxDepth;
//	}
//	
//	public boolean isStopOnFirstDefinition() {
//		return stopOnFirstDefinition;
//	}
//
//	public void setStopOnFirstDefinition(boolean stopOnFirstDefinition) {
//		this.stopOnFirstDefinition = stopOnFirstDefinition;
//	}
//
//	public long getTotalRuntimeNs() {
//		return totalRuntimeNs;
//	}
//	
//	/**
//	 * @return the expandAccuracy100Nodes
//	 */
//	public boolean isExpandAccuracy100Nodes() {
//		return expandAccuracy100Nodes;
//	}
//
//	/**
//	 * @param expandAccuracy100Nodes the expandAccuracy100Nodes to set
//	 */
//	public void setExpandAccuracy100Nodes(boolean expandAccuracy100Nodes) {
//		this.expandAccuracy100Nodes = expandAccuracy100Nodes;
//	}
//
//	/**
//	 * Whether to keep track of the best score during the algorithm run.
//	 *
//	 * @param keepTrackOfBestScore
//	 */
//	public void setKeepTrackOfBestScore(boolean keepTrackOfBestScore) {
//		this.keepTrackOfBestScore = keepTrackOfBestScore;
//	}
//
//	/**
//	 * @return a map containing time points at which a hypothesis with a better score than before has been found
//	 */
//	public SortedMap<Long, Double> getRuntimeVsBestScore() {
//		return runtimeVsBestScore;
//	}
//
//	/**
//	 * Return a map that contains
//	 * <ol>
//	 *     <li>entries with time points at which a hypothesis with a better score than before has been found</li>
//	 *     <li>entries with the current best score for each defined interval time point</li>
//	 * </ol>
//	 *
//	 * @param ticksIntervalTimeValue at which time point the current best score is tracked periodically
//	 * @param ticksIntervalTimeUnit the time unit of the periodic time point values
//	 *
//	 * @return the map
//	 *
//	 */
//	public SortedMap<Long, Double> getRuntimeVsBestScore(long ticksIntervalTimeValue, TimeUnit ticksIntervalTimeUnit) {
//		SortedMap<Long, Double> map = new TreeMap<>(runtimeVsBestScore);
//
//		// add entries for fixed time points if enabled
//		if(ticksIntervalTimeValue > 0) {
//			long ticksIntervalInMs = TimeUnit.MILLISECONDS.convert(ticksIntervalTimeValue, ticksIntervalTimeUnit);
//
//			// add  t = 0 -> 0
//			map.put(0L, 0d);
//
//			for(long t = ticksIntervalInMs; t <= TimeUnit.SECONDS.toMillis(maxExecutionTimeInSeconds); t += ticksIntervalInMs) {
//				// add value of last entry before this time point
//				map.put(t, map.get(runtimeVsBestScore.headMap(t).lastKey()));
//			}
//
//			// add  entry for t = totalRuntime
//			long totalRuntimeMs = Math.min(TimeUnit.SECONDS.toMillis(maxExecutionTimeInSeconds), TimeUnit.NANOSECONDS.toMillis(totalRuntimeNs));
//			map.put(totalRuntimeMs, map.get(map.lastKey()));
//		}
//
//		return map;
//	}
//	
//	/**
//	 * @return the traverseWholeTree
//	 */
//	public boolean isTraverseWholeTree() {
//		return traverseWholeTree;
//	}
//
//	/**
//	 * @param traverseWholeTree the traverseWholeTree to set
//	 */
//	public void setTraverseWholeTree(boolean traverseWholeTree) {
//		this.traverseWholeTree = traverseWholeTree;
//	}
//
//	/**
//	 * @return the newNodesLowerbound
//	 */
//	public int getNewNodesLowerbound() {
//		return newNodesLowerbound;
//	}
//
//	/**
//	 * @param newNodesLowerbound the newNodesLowerbound to set
//	 */
//	public void setNewNodesLowerbound(int newNodesLowerbound) {
//		this.newNodesLowerbound = newNodesLowerbound;
//	}
//
//	/**
//	 * @return the traverseBreakingThreshold
//	 */
//	public double getTraverseBreakingThreshold() {
//		return traverseBreakingThreshold;
//	}
//
//	/**
//	 * @param traverseBreakingThreshold the traverseBreakingThreshold to set
//	 */
//	public void setTraverseBreakingThreshold(double traverseBreakingThreshold) {
//		this.traverseBreakingThreshold = traverseBreakingThreshold;
//	}
//
//	private void saveLog(String filename, String log) throws FileNotFoundException {		
//		long logStart = System.nanoTime(); 
//		logger.info(log);
//		try
//		{
//			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
//			fw.write(log+"\n");
//			fw.close();
//		}
//		catch(IOException ioe)
//		{
//			System.err.println("IOException: " + ioe.getMessage());
//		}
//		long logEnd = System.nanoTime();
//		logTime += (logEnd - logStart);
//	}
//
//	/* (non-Javadoc)
//			 * @see java.lang.Object#clone()
//			 */
//	@Override
//	public Object clone() throws CloneNotSupportedException {
//		return new CELOELayerwise2(this);
//	}
//
////	public static void main(String[] args) throws Exception{
////		File file = new File("../examples/swore/swore.rdf");
////		OWLClass classToDescribe = new OWLClassImpl(IRI.create("http://ns.softwiki.de/req/CustomerRequirement"));
////		
////		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
////		
////		AbstractKnowledgeSource ks = new OWLAPIOntology(ontology);
////		ks.init();
////		
////		OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);
////		baseReasoner.setReasonerImplementation(ReasonerImplementation.HERMIT);
////        baseReasoner.init();
////		ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);
////		rc.setReasonerComponent(baseReasoner);
////		rc.init();
////		
////		ClassLearningProblem lp = new ClassLearningProblem(rc);
//////		lp.setEquivalence(false);
////		lp.setClassToDescribe(classToDescribe);
////		lp.init();
////		
////		RhoDRDown op = new RhoDRDown();
////		op.setReasoner(rc);
////		op.setUseNegation(false);
////		op.setUseHasValueConstructor(false);
////		op.setUseCardinalityRestrictions(true);
////		op.setUseExistsConstructor(true);
////		op.setUseAllConstructor(true);
////		op.init();
////		
////		
////		
////		//(male ⊓ (∀ hasChild.⊤)) ⊔ (∃ hasChild.(∃ hasChild.male))
////		OWLDataFactory df = new OWLDataFactoryImpl();
////		OWLClass male = df.getOWLClass(IRI.create("http://example.com/father#male"));
////		OWLClassExpression ce = df.getOWLObjectIntersectionOf(
////									df.getOWLObjectUnionOf(
////											male,
////											df.getOWLObjectIntersectionOf(
////													male, male),
////											df.getOWLObjectSomeValuesFrom(
////												df.getOWLObjectProperty(IRI.create("http://example.com/father#hasChild")),
////												df.getOWLThing())
////									),
////									df.getOWLObjectAllValuesFrom(
////											df.getOWLObjectProperty(IRI.create("http://example.com/father#hasChild")),
////											df.getOWLThing()
////											)
////				);
////		System.out.println(ce);
////		OWLClassExpressionMinimizer min = new OWLClassExpressionMinimizer(df, rc);
////		ce = min.minimizeClone(ce);
////		System.out.println(ce);
////		
////		CELOE_MCTS alg = new CELOE_MCTS(lp, rc);
////		alg.setMaxExecutionTimeInSeconds(10);
////		alg.setOperator(op);
////		alg.setWriteSearchTree(true);
////		alg.setSearchTreeFile("log/search-tree.log");
////		alg.setReplaceSearchTree(true);
////		alg.init();
////		alg.setKeepTrackOfBestScore(true);
////		
////		alg.start();
////
////		SortedMap<Long, Double> map = alg.getRuntimeVsBestScore(1, TimeUnit.SECONDS);
////		System.out.println(MapUtils.asTSV(map, "runtime", "best_score"));
////
////	}
//	
//}