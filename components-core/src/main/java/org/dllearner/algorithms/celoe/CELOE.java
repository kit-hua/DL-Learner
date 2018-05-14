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
package org.dllearner.algorithms.celoe;

import com.google.common.collect.Sets;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import org.dllearner.algorithms.ocel.ExampleBasedNode;
import org.dllearner.core.*;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.owl.ClassHierarchy;
import org.dllearner.core.owl.DatatypePropertyHierarchy;
import org.dllearner.core.owl.ObjectPropertyHierarchy;
import org.dllearner.kb.OWLAPIOntology;
import org.dllearner.learningproblems.ClassAsInstanceLearningProblem;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.OWLAPIReasoner;
import org.dllearner.reasoning.ReasonerImplementation;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.refinementoperators.*;
import org.dllearner.statistics.ExcelTable;
import org.dllearner.statistics.LearningData;
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
import java.io.IOException;
import java.text.DecimalFormat;
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
@ComponentAnn(name="CELOE", shortName="celoe", version=1.0, description="CELOE is an adapted and extended version of the OCEL algorithm applied for the ontology engineering use case. See http://jens-lehmann.org/files/2011/celoe.pdf for reference.")
public class CELOE extends AbstractCELA implements Cloneable{

	private static final Logger logger = LoggerFactory.getLogger(CELOE.class);
	private static final Marker sparql_debug = MarkerFactory.getMarker("SD");
	
	private boolean isRunning = false;
	private boolean stop = false;
	
//	private OEHeuristicStable heuristicStable = new OEHeuristicStable();
//	private OEHeuristicRuntime heuristicRuntime = new OEHeuristicRuntime();
	
	@ConfigOption(description = "the refinement operator instance to use")
	private LengthLimitedRefinementOperator operator;

	private SearchTree<OENode> searchTree;
	@ConfigOption(defaultValue="celoe_heuristic")
	private AbstractHeuristic heuristic; // = new OEHeuristicRuntime();
	// the class with which we start the refinement process
	@ConfigOption(defaultValue = "owl:Thing",
			description = "You can specify a start class for the algorithm. To do this, you have to use Manchester OWL syntax either with full IRIs or prefixed IRIs.",
			exampleValue = "ex:Male or http://example.org/ontology/Female")
	private OWLClassExpression startClass;
	
	// all descriptions in the search tree plus those which were too weak (for fast redundancy check)
	private TreeSet<OWLClassExpression> descriptions;
	
	
	// if true, then each solution is evaluated exactly instead of approximately
	// private boolean exactBestDescriptionEvaluation = false;
	@ConfigOption(defaultValue="false", description="Use this if you are interested in only one suggestion and your learning problem has many (more than 1000) examples.")
	private boolean singleSuggestionMode;
	private OWLClassExpression bestDescription;
	private double bestAccuracy = Double.MIN_VALUE;
	
	private OWLClass classToDescribe;
	// examples are either 1.) instances of the class to describe 2.) positive examples
	// 3.) union of pos.+neg. examples depending on the learning problem at hand
	private Set<OWLIndividual> examples;
	
	// CELOE was originally created for learning classes in ontologies, but also
	// works for other learning problem types
	private boolean isClassLearningProblem;
	private boolean isEquivalenceProblem;
	
	// important parameters (non-config options but internal)
	private double noise;

	private boolean filterFollowsFromKB = false;
	
	// less important parameters
	// forces that one solution cannot be subexpression of another expression; this option is useful to get diversity
	// but it can also suppress quite useful expressions
	private boolean forceMutualDifference = false;
	
	// utility variables
	
	// statistical variables
	private int expressionTests = 0;
	private int minHorizExp = 0;
	private int maxHorizExp = 0;
	private long totalRuntimeNs = 0;
	
	// TODO: turn those into config options
	

	// important: do not initialise those with empty sets
	// null = no settings for allowance / ignorance
	// empty set = allow / ignore nothing (it is often not desired to allow no class!)
	@ConfigOption(defaultValue="false", description="specifies whether to write a search tree")
	private boolean writeSearchTree = false;

	@ConfigOption(defaultValue="log/searchTree.txt", description="file to use for the search tree")
	private String searchTreeFile = "log/searchTree.txt";

	@ConfigOption(defaultValue="false", description="specifies whether to replace the search tree in the log file after each run or append the new search tree")
	private boolean replaceSearchTree = false;
	
	@ConfigOption(defaultValue="10", description="Sets the maximum number of results one is interested in. (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions).")
	private int maxNrOfResults = 10;

	@ConfigOption(defaultValue="0.0", description="the (approximated) percentage of noise within the examples")
	private double noisePercentage = 0.0;

	@ConfigOption(defaultValue="false", description="If true, then the results will not contain suggestions, which already follow logically from the knowledge base. Be careful, since this requires a potentially expensive consistency check for candidate solutions.")
	private boolean filterDescriptionsFollowingFromKB = false;

	@ConfigOption(defaultValue="false", description="If true, the algorithm tries to find a good starting point close to an existing definition/super class of the given class in the knowledge base.")
	private boolean reuseExistingDescription = false;

	@ConfigOption(defaultValue="0", description="The maximum number of candidate hypothesis the algorithm is allowed to test (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
	private int maxClassExpressionTests = 0;

	@ConfigOption(defaultValue="0", description = "The maximum number of candidate hypothesis the algorithm is allowed after an improvement in accuracy (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
	private int maxClassExpressionTestsAfterImprovement = 0;
	
	@ConfigOption(defaultValue = "0", description = "maximum execution of the algorithm in seconds after last improvement")
	private int maxExecutionTimeInSecondsAfterImprovement = 0;
	
	@ConfigOption(defaultValue="false", description="specifies whether to terminate when noise criterion is met")
	private boolean terminateOnNoiseReached = false;
	
	@ConfigOption(defaultValue="7", description="maximum depth of description")
	private double maxDepth = 7;

	@ConfigOption(defaultValue="false", description="algorithm will terminate immediately when a correct definition is found")
	private boolean stopOnFirstDefinition = false;
	
	private int expressionTestCountLastImprovement;
	
	
	@SuppressWarnings("unused")
	private long timeLastImprovement = 0;
	@ConfigOption(defaultValue = "false",  description = "whether to try and refine solutions which already have accuracy value of 1")
	private boolean expandAccuracy100Nodes = false;
	private double currentHighestAccuracy;

	// option to keep track of best score during algorithm run
	private boolean keepTrackOfBestScore = false;
	private SortedMap<Long, Double> runtimeVsBestScore = new TreeMap<>();
	
	private File searchTreeFileFile;
	private String treeString = "";
	private int loop = 1;
	private long logTime = 0;
	private long refineTime = 0;
	private long treeTime = 0;
	private String projectPath;
	private String projectName;
	private String opName;
	
	public CELOE() {}
	
	public CELOE(CELOE celoe){
		setReasoner(celoe.reasoner);
		setLearningProblem(celoe.learningProblem);
		
		setAllowedConcepts(celoe.getAllowedConcepts());
		setAllowedObjectProperties(celoe.getAllowedObjectProperties());
		setAllowedDataProperties(celoe.getAllowedDataProperties());
		
		setIgnoredConcepts(celoe.ignoredConcepts);
		setIgnoredObjectProperties(celoe.getIgnoredObjectProperties());
		setIgnoredDataProperties(celoe.getIgnoredDataProperties());
		
		setExpandAccuracy100Nodes(celoe.expandAccuracy100Nodes);
		setFilterDescriptionsFollowingFromKB(celoe.filterDescriptionsFollowingFromKB);
		setHeuristic(celoe.heuristic);
		
		setMaxClassExpressionTests(celoe.maxClassExpressionTests);
		setMaxClassExpressionTestsAfterImprovement(celoe.maxClassExpressionTestsAfterImprovement);
		setMaxDepth(celoe.maxDepth);
		setMaxExecutionTimeInSeconds(celoe.getMaxExecutionTimeInSeconds());
		setMaxExecutionTimeInSecondsAfterImprovement(celoe.maxExecutionTimeInSecondsAfterImprovement);
		setMaxNrOfResults(celoe.maxNrOfResults);
		setNoisePercentage(celoe.noisePercentage);
		
		LengthLimitedRefinementOperator op = new RhoDRDown((RhoDRDown)celoe.operator);
		
		try {
			op.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		}
		setOperator(op);
		
		
		setReuseExistingDescription(celoe.reuseExistingDescription);
		setSingleSuggestionMode(celoe.singleSuggestionMode);
		setStartClass(celoe.startClass);
		setStopOnFirstDefinition(celoe.stopOnFirstDefinition);
		setTerminateOnNoiseReached(celoe.terminateOnNoiseReached);
		setUseMinimizer(celoe.isUseMinimizer());
		
		setWriteSearchTree(celoe.writeSearchTree);
		setReplaceSearchTree(celoe.replaceSearchTree);
	}
	
	public CELOE(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
	}

	public static Collection<Class<? extends AbstractClassExpressionLearningProblem>> supportedLearningProblems() {
		Collection<Class<? extends AbstractClassExpressionLearningProblem>> problems = new LinkedList<>();
		problems.add(AbstractClassExpressionLearningProblem.class);
		return problems;
	}
	
	@Override
	public void init() throws ComponentInitException {
		baseURI = reasoner.getBaseURI();
		prefixes = reasoner.getPrefixes();
			
		if(maxExecutionTimeInSeconds != 0 && maxExecutionTimeInSecondsAfterImprovement != 0) {
			maxExecutionTimeInSeconds = Math.min(maxExecutionTimeInSeconds, maxExecutionTimeInSecondsAfterImprovement);
		}
		
		// TODO add comment
		ClassHierarchy classHierarchy = initClassHierarchy();
		ObjectPropertyHierarchy objectPropertyHierarchy = initObjectPropertyHierarchy();
		DatatypePropertyHierarchy datatypePropertyHierarchy = initDataPropertyHierarchy();

		// if no one injected a heuristic, we use a default one
		if(heuristic == null) {
			heuristic = new OEHeuristicRuntime();
			heuristic.init();
		}
		
		minimizer = new OWLClassExpressionMinimizer(dataFactory, reasoner);
		
//		if (writeSearchTree) {
//			File f = new File(searchTreeFile);
//			if (f.getParentFile() != null) {
//				f.getParentFile().mkdirs();
//			}
//			Files.clearFile(f);
//		}

		int lastSlash = searchTreeFile.lastIndexOf("/");
		projectName = searchTreeFile.substring(lastSlash+1, searchTreeFile.length());
		String logPath = searchTreeFile.substring(0, lastSlash);
		lastSlash = logPath.lastIndexOf("/");
		projectPath = logPath.substring(0, lastSlash);
		lastSlash = projectPath.lastIndexOf("/");
		projectName = projectPath.substring(lastSlash+1, projectPath.length()) + "_" + projectName;
		
		if(operator.getClass().toString().contains("AML"))
			opName = "aml";
		else
			opName = "rho";
		
		searchTreeFileFile = new File(searchTreeFile);
		if(writeSearchTree)
			Files.clearFile(searchTreeFileFile);
		
		// start at owl:Thing by default
		startClass = OWLAPIUtils.classExpressionPropertyExpanderChecked(this.startClass, reasoner, dataFactory, this::computeStartClass, logger);

		bestEvaluatedDescriptions = new EvaluatedDescriptionSet(maxNrOfResults);
		
		isClassLearningProblem = (learningProblem instanceof ClassLearningProblem);
		
		// we put important parameters in class variables
		noise = noisePercentage/100d;

		// (filterFollowsFromKB is automatically set to false if the problem
		// is not a class learning problem
		filterFollowsFromKB = filterDescriptionsFollowingFromKB && isClassLearningProblem;
		
		// actions specific to ontology engineering
		if(isClassLearningProblem) {
			ClassLearningProblem problem = (ClassLearningProblem) learningProblem;
			classToDescribe = problem.getClassToDescribe();
			isEquivalenceProblem = problem.isEquivalenceProblem();
			
			examples = reasoner.getIndividuals(classToDescribe);
		} else if(learningProblem instanceof PosOnlyLP) {
			examples = ((PosOnlyLP)learningProblem).getPositiveExamples();
		} else if(learningProblem instanceof PosNegLP) {
			examples = Sets.union(((PosNegLP)learningProblem).getPositiveExamples(),((PosNegLP)learningProblem).getNegativeExamples());
		}
		
		// create a refinement operator and pass all configuration
		// variables to it
		if (operator == null) {
			// we use a default operator and inject the class hierarchy for now			
			operator = new RhoDRDown();
			((CustomStartRefinementOperator) operator).setStartClass(startClass);
			((ReasoningBasedRefinementOperator) operator).setReasoner(reasoner);
		}
		if (operator instanceof CustomHierarchyRefinementOperator) {
			((CustomHierarchyRefinementOperator) operator).setClassHierarchy(classHierarchy);
			((CustomHierarchyRefinementOperator) operator).setObjectPropertyHierarchy(objectPropertyHierarchy);
			((CustomHierarchyRefinementOperator) operator).setDataPropertyHierarchy(datatypePropertyHierarchy);
		}
		
		if (!((AbstractRefinementOperator) operator).isInitialized())
			operator.init();
		
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

		logger.info("start class:" + startClass);		
		addNode(startClass, null);
		OWLClassExpression bd = null;
		
		writeSearchTree = false;
				
		while (!terminationCriteriaSatisfied()) {	
			
			/**
			 * @Hua: note, some of the best evaluated descriptions are not saved in the search tree
			 * they are rewritten from the bests candidates and are semantically equivalent to them. 
			 * They are just for better reading. 
			 * For example, hasB.Top is stored in tree, but represented as hasB.B in the list
			 */
			//System.out.println("bests: " + bestEvaluatedDescriptions);
			
			long t1 = System.nanoTime();
			String durationStr = ifBetterSolutionsFound();
			if(durationStr != null)
			{
				System.out.println("\nloop " + loop + ": ");
				logger.info(" - more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
				if(bestEvaluatedDescriptions.getBestAccuracy() == 1.0)
				{
					this.stop = true;
					if(bd == null || !bd.equals(bestEvaluatedDescriptions.getBest().getDescription())) {
						bd = bestEvaluatedDescriptions.getBest().getDescription();
						logger.info(" - class expression found: " + descriptionToString(bd));
//						System.out.println(" - Totally " + loop + " iterations required!");
//						System.out.println("\n--------------------------");						
					}								
				}
			}else
			{
				if(loop%10000 == 0) {
					System.out.println("\nloop " + loop + ":");
					System.out.println("depth: " + searchTree.getDepth());
					System.out.println("nr of nodes: " + searchTree.size());					
					System.out.println("expanding: " + getNextNodeToExpand());
				}
			}
			logTime += System.nanoTime() - t1;
//			showIfBetterSolutionsFound();
			
			
//			System.out.println("current max depth: " + this.getMaxDepth());

			/**
			 * @Hua: the search tree is again sorted: best node at the end
			 * 		 this picks the "best" node to expand, and will continue refine it if it not becomes worse
			 */
			// chose best node according to heuristics
//			System.out.println("search tree: ");			
//			Iterator<OENode> it = searchTree.descendingIterator();
//			while(it.hasNext())
//				System.out.println(" - " + it.next());
			nextNode = getNextNodeToExpand();
			
//			OENode cpNode = new OENode(nextNode.getDescription(), nextNode.accuracy);
//			cpNode.horizontalExpansion = nextNode.horizontalExpansion;
//			nextNode.horizontalExpansion = 7;
//			System.out.println("next node: " + nextNode.getDescription());
			int horizExp = nextNode.getHorizontalExpansion();
			
			long t2 = System.nanoTime();
			if (writeSearchTree) {
				treeString = "loop " + loop + "\n";
//				treeString += "best node: " + bestEvaluatedDescriptions.getBest() + "\n";
//				StringBuilder treeString = new StringBuilder("");
				treeString += "expanding: " + nextNode.getDescription() + " [h:" + this.heuristic.getNodeScore(nextNode) + ", acc:" + nextNode.getAccuracy() + ", he:" + (nextNode.getHorizontalExpansion()) + "]\n";
			}
			logTime += System.nanoTime() - t2;
			
			/**
			 * @Hua: this will refine the node up to the desired expansion n
			 * so all refinements below n will also be generated and checked again
			 * the refinement also reorganize the sequence of the search tree: visited node will be shifted back to head
			 */
			// apply refinement operator
			long refineStart = System.nanoTime();
			TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
			refineTime += System.nanoTime() - refineStart;
//			// write the search tree (if configured)
//			if (writeSearchTree) {
//				writeSearchTree(refinements, nextNode);
//			}
//			if(nextNode.getParent() == null) {
//				System.out.println("\ncurrent max expansion: " + this.getMaximumHorizontalExpansion());
//				System.out.println("current min expansion: " + this.getMinimumHorizontalExpansion());
//				System.out.println("best node: " + bestEvaluatedDescriptions.getBest());
//				System.out.println("bests: " + bestEvaluatedDescriptions);
//				System.out.println(refinements.size() + " possible refinements of: " + cpNode);
//				for(OWLClassExpression refine : refinements) {
//					System.out.println(" - " + refine.toString());
//				}	
//			}
			
			long treeStart = System.nanoTime();
			while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {
				
				// pick element from set
				OWLClassExpression refinement = refinements.pollFirst();
				int d = OWLClassExpressionUtils.getDepth(refinement);
				
				// get length of class expression
				int length = OWLClassExpressionUtils.getLength(refinement);
				
//				System.out.println("refinement " + refinement + ": ");
//				System.out.println(" - length: " + length);
//				System.out.println(" - depth: " + d);
				
				/**
				 * @Hua: refinements with lower length are those already checked in previous interations
				 * 		 a node is only added if it is not too weak
				 */
				// we ignore all refinements with lower length and too high depth
				// (this also avoids duplicate node children)
				if(length >= horizExp && d <= maxDepth) {
					// add node to search tree
					addNode(refinement, nextNode);
				}else {
//					System.out.println(" : ignored");
				}				
			}
			treeTime += System.nanoTime() - treeStart;
			
			long t3 = System.nanoTime();
			if (writeSearchTree) {
				treeString += TreeUtils.toTreeString(searchTree) + "\n";		
				if(searchTreeFileFile.length() > 10000000) {
					searchTreeFileFile = new File(searchTreeFile + "_" + loop);				
					Files.createFile(searchTreeFileFile, treeString);					
				}	
				Files.appendToFile(searchTreeFileFile, treeString);				
			}
			logTime += System.nanoTime() - t3;
			
//			showIfBetterSolutionsFound();
			
			// update the global min and max horizontal expansion values
			updateMinMaxHorizExp(nextNode);
					
			// write the search tree (if configured)
//			if (writeSearchTree) {
//				writeSearchTree(refinements, nextNode);
//			}

//			if (writeSearchTree) {
//				File f = new File(searchTreeFile);
//				if (f.getParentFile() != null) {
//					f.getParentFile().mkdirs();
//				}
//				Files.clearFile(f);
//			}
			
			
			loop++;
			
//			logger.info(descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
			
//			if(bestEvaluatedDescriptions.getBestAccuracy() == 1.0)
//			{
////				this.stop = true;
//				if(bd == null || !bd.equals(bestEvaluatedDescriptions.getBest().getDescription())) {
//					bd = bestEvaluatedDescriptions.getBest().getDescription();
//					logger.info("class expression found: " + descriptionToString(bd));
//					System.out.println("Totally " + loop + " iterations required!");
//					System.out.println("\n--------------------------");						
//				}								
//			}
				
		}
		
		if(singleSuggestionMode) {
			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
		}
		
		printStats(true);
		
		saveStatistics("/Users/aris/Documents/repositories/ipr/aml_import/resources/output/statistics.xlsx", opName);
		
		// print some stats
		printAlgorithmRunStats();
		
		// print solution(s)
		logger.info("\nsolutions:\n" + getSolutionString());
		
		isRunning = false;
	}
	
	/*
	 * Compute the start class in the search space from which the refinement will start.
	 * We use the intersection of super classes for definitions (since it needs to
	 * capture all instances), but owl:Thing for learning subclasses (since it is
	 * superfluous to add super classes in this case)
	 */
	private OWLClassExpression computeStartClass() {
		OWLClassExpression startClass = dataFactory.getOWLThing();
		
		if(isClassLearningProblem) {
			if(isEquivalenceProblem) {
				Set<OWLClassExpression> existingDefinitions = reasoner.getAssertedDefinitions(classToDescribe);
				if(reuseExistingDescription && (existingDefinitions.size() > 0)) {
					// the existing definition is reused, which in the simplest case means to
					// use it as a start class or, if it is already too specific, generalise it
					
					// pick the longest existing definition as candidate
					OWLClassExpression existingDefinition = null;
					int highestLength = 0;
					for(OWLClassExpression exDef : existingDefinitions) {
						if(OWLClassExpressionUtils.getLength(exDef) > highestLength) {
							existingDefinition = exDef;
							highestLength = OWLClassExpressionUtils.getLength(exDef);
						}
					}
					
					LinkedList<OWLClassExpression> startClassCandidates = new LinkedList<>();
					startClassCandidates.add(existingDefinition);
					// hack for RhoDRDown
					if(operator instanceof RhoDRDown) {
						((RhoDRDown)operator).setDropDisjuncts(true);
					}
					LengthLimitedRefinementOperator upwardOperator = new OperatorInverter(operator);
					
					// use upward refinement until we find an appropriate start class
					boolean startClassFound = false;
					OWLClassExpression candidate;
					do {
						candidate = startClassCandidates.pollFirst();
						if(((ClassLearningProblem)learningProblem).getRecall(candidate)<1.0) {
							// add upward refinements to list
							Set<OWLClassExpression> refinements = upwardOperator.refine(candidate, OWLClassExpressionUtils.getLength(candidate));
//							System.out.println("ref: " + refinements);
							LinkedList<OWLClassExpression> refinementList = new LinkedList<>(refinements);
//							Collections.reverse(refinementList);
//							System.out.println("list: " + refinementList);
							startClassCandidates.addAll(refinementList);
//							System.out.println("candidates: " + startClassCandidates);
						} else {
							startClassFound = true;
						}
					} while(!startClassFound);
					startClass = candidate;
					
					if(startClass.equals(existingDefinition)) {
						logger.info("Reusing existing class expression " + OWLAPIRenderers.toManchesterOWLSyntax(startClass) + " as start class for learning algorithm.");
					} else {
						logger.info("Generalised existing class expression " + OWLAPIRenderers.toManchesterOWLSyntax(existingDefinition) + " to " + OWLAPIRenderers.toManchesterOWLSyntax(startClass) + ", which is used as start class for the learning algorithm.");
					}
					
					if(operator instanceof RhoDRDown) {
						((RhoDRDown)operator).setDropDisjuncts(false);
					}
					
				} else {
					Set<OWLClassExpression> superClasses = reasoner.getClassHierarchy().getSuperClasses(classToDescribe, true);
					if(superClasses.size() > 1) {
						startClass = dataFactory.getOWLObjectIntersectionOf(superClasses);
					} else if(superClasses.size() == 1){
						startClass = (OWLClassExpression) superClasses.toArray()[0];
					} else {
						startClass = dataFactory.getOWLThing();
						logger.warn(classToDescribe + " is equivalent to owl:Thing. Usually, it is not " +
								"sensible to learn a class expression in this case.");
					}
				}
			}
		}
		return startClass;
	}
	
	private OENode getNextNodeToExpand() {
		// we expand the best node of those, which have not achieved 100% accuracy
		// already and have a horizontal expansion equal to their length
		// (rationale: further extension is likely to add irrelevant syntactical constructs)
		Iterator<OENode> it = searchTree.descendingIterator();
		if (logger.isTraceEnabled()) {
			for (OENode N:searchTree.getNodeSet()) {
				logger.trace(sparql_debug,"`getnext:"+N);
			}
		}

		while(it.hasNext()) {
			OENode node = it.next();
			logger.trace(sparql_debug,"``"+node+node.getAccuracy());
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
	private TreeSet<OWLClassExpression> refineNode(OENode node) {
		logger.trace(sparql_debug,"REFINE NODE " + node);
		MonitorFactory.getTimeMonitor("refineNode").start();
		// we have to remove and add the node since its heuristic evaluation changes through the expansion
		// (you *must not* include any criteria in the heuristic which are modified outside of this method,
		// otherwise you may see rarely occurring but critical false ordering in the nodes set)
		searchTree.updatePrepare(node);
		int horizExp = node.getHorizontalExpansion();
		/**
		 * @Hua: why +1
		 */
		TreeSet<OWLClassExpression> refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp);
//		TreeSet<OWLClassExpression> refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1);
		
//		System.out.println(refinements.size() + " possible refinements of: " + node);
//		System.out.println("expanding " + node + ": " + refinements.size() + " refinements");
//		System.out.println("refinements: " + refinements);
		node.incHorizontalExpansion();
		node.setRefinementCount(refinements.size());
//		System.out.println("refined node: " + node);
		searchTree.updateDone(node);
		MonitorFactory.getTimeMonitor("refineNode").stop();
		return refinements;
	}
	
	/**
	 * Add node to search tree if it is not too weak.
	 * @return TRUE if node was added and FALSE otherwise
	 */
	private boolean addNode(OWLClassExpression description, OENode parentNode) {
		String sparql_debug_out = "";
		if (logger.isTraceEnabled()) sparql_debug_out = "DESC: " + description;
		MonitorFactory.getTimeMonitor("addNode").start();
		
		// redundancy check (return if redundant)
		boolean nonRedundant = descriptions.add(description);
		if(!nonRedundant) {
			logger.trace(sparql_debug, sparql_debug_out + "REDUNDANT");
			return false;
		}
		
		// check whether the class expression is allowed
		if(!isDescriptionAllowed(description, parentNode)) {
			logger.trace(sparql_debug, sparql_debug_out + "NOT ALLOWED");
			return false;
		}
		
		// quality of class expression (return if too weak)
		Monitor mon = MonitorFactory.start("lp");
		logger.trace(sparql_debug, sparql_debug_out);
		
		/**
		 * @Hua: Calls accuarcy from the learning problem
		 * getAccuracyOrTooWeak will call getAccuracyOrTooWeak2 internally, which is the user specified accuracy e.g. AccMethodFMeasure.getAccuracyOrTooWeak
		 * So this will be the same as the real accuracy
		 */
		double accuracy = learningProblem.getAccuracyOrTooWeak(description, noise);
		logger.trace(sparql_debug, "`acc:"+accuracy);
		mon.stop();
		
		// issue a warning if accuracy is not between 0 and 1 or -1 (too weak)
		if(accuracy > 1.0 || (accuracy < 0.0 && accuracy != -1)) {
			throw new RuntimeException("Invalid accuracy value " + accuracy + " for class expression " + description +
					". This could be caused by a bug in the heuristic measure and should be reported to the DL-Learner bug tracker.");
		}
		
		/**
		 * @Hua: each refinement which makes further will be checked
		 */
		expressionTests++;
		
		// return FALSE if 'too weak'
		if(accuracy == -1) {
//			System.out.println(" : too weak");
			return false;
		}else {
//			System.out.println(" : new candidate");
		}
			
		
		OENode node = new OENode(description, accuracy);
		searchTree.addNode(parentNode, node);
		
		// in some cases (e.g. mutation) fully evaluating even a single class expression is too expensive
		// due to the high number of examples -- so we just stick to the approximate accuracy
		if(singleSuggestionMode) {
			if(accuracy > bestAccuracy) {
				bestAccuracy = accuracy;
				bestDescription = description;
				logger.info("more accurate (" + dfPercent.format(bestAccuracy) + ") class expression found: " + descriptionToString(bestDescription)); // + getTemporaryString(bestDescription));
			}
			return true;
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
		
		if(isCandidate) {
			OWLClassExpression niceDescription = rewrite(node.getExpression());

			if(niceDescription.equals(classToDescribe)) {
				return false;
			}
			
			if(!isDescriptionAllowed(niceDescription, node)) {
				return false;
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
		
		return true;
	}
	
	// checks whether the class expression is allowed
	private boolean isDescriptionAllowed(OWLClassExpression description, OENode parentNode) {
		if(isClassLearningProblem) {
			if(isEquivalenceProblem) {
				// the class to learn must not appear on the outermost property level
				if(occursOnFirstLevel(description, classToDescribe)) {
					return false;
				}
				if(occursOnSecondLevel(description, classToDescribe)) {
					return false;
				}
			} else {
				// none of the superclasses of the class to learn must appear on the
				// outermost property level
				TreeSet<OWLClassExpression> toTest = new TreeSet<>();
				toTest.add(classToDescribe);
				while(!toTest.isEmpty()) {
					OWLClassExpression d = toTest.pollFirst();
					if(occursOnFirstLevel(description, d)) {
						return false;
					}
					toTest.addAll(reasoner.getClassHierarchy().getSuperClasses(d));
				}
			}
		} else if (learningProblem instanceof ClassAsInstanceLearningProblem) {
//			System.out.println("4444444");
			return true;
		}
		
		// perform forall sanity tests
		if (parentNode != null &&
				(ConceptTransformation.getForallOccurences(description) > ConceptTransformation.getForallOccurences(parentNode.getDescription()))) {
			// we have an additional \forall construct, so we now fetch the contexts
			// in which it occurs
			SortedSet<PropertyContext> contexts = ConceptTransformation.getForallContexts(description);
			SortedSet<PropertyContext> parentContexts = ConceptTransformation.getForallContexts(parentNode.getDescription());
			contexts.removeAll(parentContexts);
//			System.out.println("parent description: " + parentNode.getDescription());
//			System.out.println("description: " + description);
//			System.out.println("contexts: " + contexts);
			// we now have to perform sanity checks: if \forall is used, then there
			// should be at least on class instance which has a filler at the given context
			for(PropertyContext context : contexts) {
				// transform [r,s] to \exists r.\exists s.\top
				OWLClassExpression existentialContext = context.toExistentialContext();
				boolean fillerFound = false;
				if(reasoner instanceof SPARQLReasoner) {
					SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(existentialContext);
					fillerFound = !Sets.intersection(individuals, examples).isEmpty();
				} else {
					for(OWLIndividual instance : examples) {
						if(reasoner.hasType(existentialContext, instance)) {
							fillerFound = true;
							break;
						}
					}
				}
				
				// if we do not find a filler, this means that putting \forall at
				// that position is not meaningful
				if(!fillerFound) {
					return false;
				}
			}
		}
		
		// we do not want to have negations of sibling classes on the outermost level
		// (they are expressed more naturally by saying that the siblings are disjoint,
		// so it is reasonable not to include them in solutions)
//		Set<OWLClassExpression> siblingClasses = reasoner.getClassHierarchy().getSiblingClasses(classToDescribe);
//		for now, we just disable negation
		
		return true;
	}
	
	// determine whether a named class occurs on the outermost level, i.e. property depth 0
	// (it can still be at higher depth, e.g. if intersections are nested in unions)
	private boolean occursOnFirstLevel(OWLClassExpression description, OWLClassExpression cls) {
		return !cls.isOWLThing() && (description instanceof OWLNaryBooleanClassExpression && ((OWLNaryBooleanClassExpression) description).getOperands().contains(cls));
//        return description.containsConjunct(cls) ||
//                (description instanceof OWLObjectUnionOf && ((OWLObjectUnionOf) description).getOperands().contains(cls));
	}
	
	// determine whether a named class occurs on the outermost level, i.e. property depth 0
		// (it can still be at higher depth, e.g. if intersections are nested in unions)
		private boolean occursOnSecondLevel(OWLClassExpression description, OWLClassExpression cls) {
//			SortedSet<OWLClassExpression> superClasses = reasoner.getSuperClasses(cls);
//			if(description instanceof OWLObjectIntersectionOf) {
//				List<OWLClassExpression> operands = ((OWLObjectIntersectionOf) description).getOperandsAsList();
//
//				for (OWLClassExpression op : operands) {
//					if(superClasses.contains(op) ||
//							(op instanceof OWLObjectUnionOf && !Sets.intersection(((OWLObjectUnionOf)op).getOperands(),superClasses).isEmpty())) {
//						for (OWLClassExpression op2 : operands) {
//							if((op2 instanceof OWLObjectUnionOf && ((OWLObjectUnionOf)op2).getOperands().contains(cls))) {
//								return true;
//							}
//						}
//					}
//				}
//
//				for (OWLClassExpression op1 : operands) {
//					for (OWLClassExpression op2 : operands) {
//						if(!op1.isAnonymous() && op2 instanceof OWLObjectUnionOf) {
//							 for (OWLClassExpression op3 : ((OWLObjectUnionOf)op2).getOperands()) {
//								if(!op3.isAnonymous()) {// A AND B with Disj(A,B)
//									if(reasoner.isDisjoint(op1.asOWLClass(), op3.asOWLClass())) {
//										return true;
//									}
//								} else {// A AND NOT A
//									if(op3 instanceof OWLObjectComplementOf && ((OWLObjectComplementOf)op3).getOperand().equals(op1)) {
//										return true;
//									}
//								}
//							}
//						}
//					}
//				}
//			}
			
			return false;
	    }
	
	private boolean terminationCriteriaSatisfied() {
		return
		stop ||
		(maxClassExpressionTestsAfterImprovement != 0 && (expressionTests - expressionTestCountLastImprovement >= maxClassExpressionTestsAfterImprovement)) ||
		(maxClassExpressionTests != 0 && (expressionTests >= maxClassExpressionTests)) ||
		(maxExecutionTimeInSecondsAfterImprovement != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSecondsAfterImprovement* 1000000000L))) ||
//		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime - logTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
		(terminateOnNoiseReached && (100*getCurrentlyBestAccuracy()>=100-noisePercentage)) ||
		(stopOnFirstDefinition && (getCurrentlyBestAccuracy() >= 1));
	}
	
	private void reset() {
		// set all values back to their default values (used for running
		// the algorithm more than once)
		searchTree = new SearchTree<>(heuristic);
		descriptions = new TreeSet<>();
		bestEvaluatedDescriptions.getSet().clear();
		expressionTests = 0;
		runtimeVsBestScore.clear();
	}
	
	private void printStats(boolean finalStats) {
		long algorithmRuntime = System.nanoTime() - nanoStartTime;
		long computationTime = algorithmRuntime - logTime;
		
//		Iterator<ExampleBasedNode> it = searchTreeStable.descendingIterator();
//		int ind = 0;
//		while (it.hasNext()) {
//			ExampleBasedNode node = it.next();			
//			ind++;
//		}
		DecimalFormat df = new DecimalFormat();
		
		if(!finalStats)
		{			
			System.out.println("nr of nodes: " + searchTree.size());
			System.out.println("tree depth: " + searchTree.getDepth());
			System.out.println("rules tested: " + expressionTests);			
		}else
		{						
			double logPercetange = 100 * logTime/(double)algorithmRuntime;
			double computePercentage = 100 * computationTime/(double)algorithmRuntime;
			
			long reasoningTime = reasoner.getOverallReasoningTimeNs();
			double reasoningPercentage = 100 * reasoningTime / (double) computationTime;
		
			long subTime = reasoner.getSubsumptionReasoningTimeNs();
			double subPercentage = 100 * subTime / (double) computationTime;
			
			double refinementPercentage = 100 * refineTime / (double) computationTime;
			double instanceCheckPercentage = 100 * reasoner.getInstanceCheckReasoningTimeNs() / (double) computationTime;									
			
			logger.info("========== FINAL STATS ==========");
			System.out.println("Totally " + loop + " iterations required");
			System.out.println("nr of nodes: " + searchTree.size());		
			System.out.println("tree depth: " + searchTree.getDepth());
			System.out.println("rules tested: " + expressionTests);
						
			System.out.println("Total runtime: " + Helper.prettyPrintNanoSeconds(algorithmRuntime));
			System.out.println("Total log time: " + Helper.prettyPrintNanoSeconds(logTime) + " (" + df.format(logPercetange) + "%)");
			System.out.println("Total computation time: " + Helper.prettyPrintNanoSeconds(computationTime) + " (" + df.format(computePercentage) + "%)");
//			logger.info(" - expand node time: " + Helper.prettyPrintNanoSeconds(extendNodeTimeNs) + " (" + df.format(expandPercetange) + "%)");
			logger.info("   - refinement: " + Helper.prettyPrintNanoSeconds(refineTime) + "(" + df.format(refinementPercentage) + "%)");
			logger.info("   - reasoning: " + Helper.prettyPrintNanoSeconds(reasoningTime) + " (" + df.format(reasoningPercentage) + "%)");
			logger.info("   - instance check time: " + Helper.prettyPrintNanoSeconds(reasoner.getInstanceCheckReasoningTimeNs()) + "(" + df.format(instanceCheckPercentage) + "%)");
			logger.info("   - subsumption: " + Helper.prettyPrintNanoSeconds(subTime) + "(" + df.format(subPercentage) + "%)");
		
		}
	}
	
	private void saveStatistics(String table, String op) {
		/**
		 * Write to excel
		 */					
		ExcelTable statistics;
		try {
			if(op.equals("rho"))
				statistics = new ExcelTable(table, 0);
			else if(op.equals("aml"))
				statistics = new ExcelTable(table, 1);
			else {
				System.err.println("unknwon operator " + op + "! Not saving statistics");
				return;
			}
			
			long algorithmRuntime = System.nanoTime() - nanoStartTime;
			long computationTime = algorithmRuntime - logTime;
			long reasoningTime = reasoner.getOverallReasoningTimeNs();		
			long subTime = reasoner.getSubsumptionReasoningTimeNs();
						
			LearningData data = new LearningData(projectName);
			data.setIterations(loop);
			data.setDepth(searchTree.getDepth());
			data.setNrOfNodes(searchTree.size());
			data.setNrOfRules(expressionTests);
			data.setRunTime(TimeUnit.MILLISECONDS.convert(algorithmRuntime, TimeUnit.NANOSECONDS));
			data.setLogTime(TimeUnit.MILLISECONDS.convert(logTime, TimeUnit.NANOSECONDS));
			data.setComputeTime(TimeUnit.MILLISECONDS.convert(computationTime, TimeUnit.NANOSECONDS));
			data.setRefinement(TimeUnit.MILLISECONDS.convert(refineTime, TimeUnit.NANOSECONDS));
			data.setReasoning(TimeUnit.MILLISECONDS.convert(reasoningTime, TimeUnit.NANOSECONDS));
			data.setInstCheck(TimeUnit.MILLISECONDS.convert(reasoner.getInstanceCheckReasoningTimeNs(), TimeUnit.NANOSECONDS));
			data.setSubsumption(TimeUnit.MILLISECONDS.convert(subTime, TimeUnit.NANOSECONDS));
			statistics.newData(data.getName(), data);
					
			statistics.write(table);
			System.out.println("successfully saved " + table + " to disk");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}
	
	
	private void printAlgorithmRunStats() {
		if (stop) {
//			logger.info("Algorithm stopped ("+expressionTests+" descriptions tested). " + searchTree.size() + " nodes in the search tree.\n");
			logger.info("Algorithm stopped");
			logger.info(" - iterations required: " + (loop-1)); 
			logger.info(" - tested descriptions: " + expressionTests);
			logger.info(" - nodes in the search tree: " + searchTree.size());
		} else {
			totalRuntimeNs = System.nanoTime()-nanoStartTime;
			logger.info("Algorithm terminated successfully (time: " + Helper.prettyPrintNanoSeconds(totalRuntimeNs) + ", "+expressionTests+" descriptions tested, "  + searchTree.size() + " nodes in the search tree).\n");
            logger.info(reasoner.toString());
		}
	}
	
	private void showIfBetterSolutionsFound() {
		if(!singleSuggestionMode && bestEvaluatedDescriptions.getBestAccuracy() > currentHighestAccuracy) {
			currentHighestAccuracy = bestEvaluatedDescriptions.getBestAccuracy();
			expressionTestCountLastImprovement = expressionTests;
			timeLastImprovement = System.nanoTime();
			long durationInMillis = getCurrentRuntimeInMilliSeconds();
			String durationStr = getDurationAsString(durationInMillis);

			// track new best accuracy if enabled
			if(keepTrackOfBestScore) {
				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), currentHighestAccuracy);
			}
			logger.info("more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
		}
	}
	
	private String ifBetterSolutionsFound() {
		if(!singleSuggestionMode && bestEvaluatedDescriptions.getBestAccuracy() > currentHighestAccuracy) {
			currentHighestAccuracy = bestEvaluatedDescriptions.getBestAccuracy();
			expressionTestCountLastImprovement = expressionTests;
			timeLastImprovement = System.nanoTime();
			long durationInMillis = getCurrentRuntimeInMilliSeconds();
			String durationStr = getDurationAsString(durationInMillis);

			// track new best accuracy if enabled
			if(keepTrackOfBestScore) {
				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), currentHighestAccuracy);
			}
			return durationStr;
		}
		return null;
	}
	
	private void writeSearchTree(TreeSet<OWLClassExpression> refinements) {
		StringBuilder treeString = new StringBuilder("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
		if (refinements.size() > 1) {
			treeString.append("all expanded nodes:\n");
			for (OWLClassExpression ref : refinements) {
				treeString.append("   ").append(ref).append("\n");
			}
		}
		treeString.append(TreeUtils.toTreeString(searchTree)).append("\n");

		// replace or append
		if (replaceSearchTree) {
			Files.createFile(new File(searchTreeFile), treeString.toString());
		} else {
			Files.appendToFile(new File(searchTreeFile), treeString.toString());
		}
	}
	
	/**
	 * @Hua: write the expanding node to the tree string
	 * @param refinements
	 */
	private void writeSearchTree(TreeSet<OWLClassExpression> refinements, OENode node) {
		StringBuilder treeString = new StringBuilder("best node: ").append(bestEvaluatedDescriptions.getBest()).append("\n");
//		StringBuilder treeString = new StringBuilder("");
		treeString.append("expanding: ").append(node.getDescription()).append(" [acc:").append(node.getAccuracy()).append(", he:").append(node.getHorizontalExpansion()-1).append("]\n");
		if (refinements.size() > 1) {
			treeString.append("all expanded nodes:\n");
			for (OWLClassExpression ref : refinements) {
				treeString.append("   ").append(ref).append("\n");
			}
		}
		treeString.append(TreeUtils.toTreeString(searchTree)).append("\n");

		// replace or append
		if (replaceSearchTree) {
			Files.createFile(new File(searchTreeFile), treeString.toString());
		} else {
			Files.appendToFile(new File(searchTreeFile), treeString.toString());
		}
	}
	
	private void updateMinMaxHorizExp(OENode node) {
		int newHorizExp = node.getHorizontalExpansion();
		
		// update maximum value
		maxHorizExp = Math.max(maxHorizExp, newHorizExp);
		
		// we just expanded a node with minimum horizontal expansion;
		// we need to check whether it was the last one
		if(minHorizExp == newHorizExp - 1) {
			
			// the best accuracy that a node can achieve
			double scoreThreshold = heuristic.getNodeScore(node) + 1 - node.getAccuracy();
			
			for(OENode n : searchTree.descendingSet()) {
				if(n != node) {
					if(n.getHorizontalExpansion() == minHorizExp) {
						// we can stop instantly when another node with min.
						return;
					}
					if(heuristic.getNodeScore(n) < scoreThreshold) {
						// we can stop traversing nodes when their score is too low
						break;
					}
				}
			}
			
			// inc. minimum since we found no other node which also has min. horiz. exp.
			minHorizExp++;
			
//			System.out.println("minimum horizontal expansion is now " + minHorizExp);
		}
	}
	
	@Override
	public OWLClassExpression getCurrentlyBestDescription() {
		EvaluatedDescription<? extends Score> ed = getCurrentlyBestEvaluatedDescription();
		return ed == null ? null : ed.getDescription();
	}

	@Override
	public List<OWLClassExpression> getCurrentlyBestDescriptions() {
		return bestEvaluatedDescriptions.toDescriptionList();
	}
	
	@Override
	public EvaluatedDescription<? extends Score> getCurrentlyBestEvaluatedDescription() {
		return bestEvaluatedDescriptions.getBest();
	}
	
	@Override
	public NavigableSet<? extends EvaluatedDescription<? extends Score>> getCurrentlyBestEvaluatedDescriptions() {
		return bestEvaluatedDescriptions.getSet();
	}
	
	public double getCurrentlyBestAccuracy() {
		return bestEvaluatedDescriptions.getBest().getAccuracy();
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public void stop() {
		stop = true;
	}

	public int getMaximumHorizontalExpansion() {
		return maxHorizExp;
	}

	public int getMinimumHorizontalExpansion() {
		return minHorizExp;
	}
	
	/**
	 * @return the expressionTests
	 */
	public int getClassExpressionTests() {
		return expressionTests;
	}

	public LengthLimitedRefinementOperator getOperator() {
		return operator;
	}

	@Autowired(required=false)
	public void setOperator(LengthLimitedRefinementOperator operator) {
		this.operator = operator;
	}

	public OWLClassExpression getStartClass() {
		return startClass;
	}

	public void setStartClass(OWLClassExpression startClass) {
		this.startClass = startClass;
	}
	
	public boolean isWriteSearchTree() {
		return writeSearchTree;
	}

	public void setWriteSearchTree(boolean writeSearchTree) {
		this.writeSearchTree = writeSearchTree;
	}

	public String getSearchTreeFile() {
		return searchTreeFile;
	}

	public void setSearchTreeFile(String searchTreeFile) {
		this.searchTreeFile = searchTreeFile;
	}

	public int getMaxNrOfResults() {
		return maxNrOfResults;
	}

	public void setMaxNrOfResults(int maxNrOfResults) {
		this.maxNrOfResults = maxNrOfResults;
	}

	public double getNoisePercentage() {
		return noisePercentage;
	}

	public void setNoisePercentage(double noisePercentage) {
		this.noisePercentage = noisePercentage;
	}

	public boolean isFilterDescriptionsFollowingFromKB() {
		return filterDescriptionsFollowingFromKB;
	}

	public void setFilterDescriptionsFollowingFromKB(boolean filterDescriptionsFollowingFromKB) {
		this.filterDescriptionsFollowingFromKB = filterDescriptionsFollowingFromKB;
	}

	public boolean isReplaceSearchTree() {
		return replaceSearchTree;
	}

	public void setReplaceSearchTree(boolean replaceSearchTree) {
		this.replaceSearchTree = replaceSearchTree;
	}

	public boolean isTerminateOnNoiseReached() {
		return terminateOnNoiseReached;
	}

	public void setTerminateOnNoiseReached(boolean terminateOnNoiseReached) {
		this.terminateOnNoiseReached = terminateOnNoiseReached;
	}

	public boolean isReuseExistingDescription() {
		return reuseExistingDescription;
	}

	public void setReuseExistingDescription(boolean reuseExistingDescription) {
		this.reuseExistingDescription = reuseExistingDescription;
	}

	public AbstractHeuristic getHeuristic() {
		return heuristic;
	}

	@Autowired(required=false)
	public void setHeuristic(AbstractHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	public int getMaxExecutionTimeInSecondsAfterImprovement() {
		return maxExecutionTimeInSecondsAfterImprovement;
	}

	public void setMaxExecutionTimeInSecondsAfterImprovement(
			int maxExecutionTimeInSecondsAfterImprovement) {
		this.maxExecutionTimeInSecondsAfterImprovement = maxExecutionTimeInSecondsAfterImprovement;
	}
	
	public boolean isSingleSuggestionMode() {
		return singleSuggestionMode;
	}

	public void setSingleSuggestionMode(boolean singleSuggestionMode) {
		this.singleSuggestionMode = singleSuggestionMode;
	}

	public int getMaxClassExpressionTests() {
		return maxClassExpressionTests;
	}

	public void setMaxClassExpressionTests(int maxClassExpressionTests) {
		this.maxClassExpressionTests = maxClassExpressionTests;
	}

	public int getMaxClassExpressionTestsAfterImprovement() {
		return maxClassExpressionTestsAfterImprovement;
	}

	public void setMaxClassExpressionTestsAfterImprovement(
			int maxClassExpressionTestsAfterImprovement) {
		this.maxClassExpressionTestsAfterImprovement = maxClassExpressionTestsAfterImprovement;
	}

	public double getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(double maxDepth) {
		this.maxDepth = maxDepth;
	}
	
	public boolean isStopOnFirstDefinition() {
		return stopOnFirstDefinition;
	}

	public void setStopOnFirstDefinition(boolean stopOnFirstDefinition) {
		this.stopOnFirstDefinition = stopOnFirstDefinition;
	}

	public long getTotalRuntimeNs() {
		return totalRuntimeNs;
	}
	
	/**
	 * @return the expandAccuracy100Nodes
	 */
	public boolean isExpandAccuracy100Nodes() {
		return expandAccuracy100Nodes;
	}

	/**
	 * @param expandAccuracy100Nodes the expandAccuracy100Nodes to set
	 */
	public void setExpandAccuracy100Nodes(boolean expandAccuracy100Nodes) {
		this.expandAccuracy100Nodes = expandAccuracy100Nodes;
	}

	/**
	 * Whether to keep track of the best score during the algorithm run.
	 *
	 * @param keepTrackOfBestScore
	 */
	public void setKeepTrackOfBestScore(boolean keepTrackOfBestScore) {
		this.keepTrackOfBestScore = keepTrackOfBestScore;
	}

	/**
	 * @return a map containing time points at which a hypothesis with a better score than before has been found
	 */
	public SortedMap<Long, Double> getRuntimeVsBestScore() {
		return runtimeVsBestScore;
	}

	/**
	 * Return a map that contains
	 * <ol>
	 *     <li>entries with time points at which a hypothesis with a better score than before has been found</li>
	 *     <li>entries with the current best score for each defined interval time point</li>
	 * </ol>
	 *
	 * @param ticksIntervalTimeValue at which time point the current best score is tracked periodically
	 * @param ticksIntervalTimeUnit the time unit of the periodic time point values
	 *
	 * @return the map
	 *
	 */
	public SortedMap<Long, Double> getRuntimeVsBestScore(long ticksIntervalTimeValue, TimeUnit ticksIntervalTimeUnit) {
		SortedMap<Long, Double> map = new TreeMap<>(runtimeVsBestScore);

		// add entries for fixed time points if enabled
		if(ticksIntervalTimeValue > 0) {
			long ticksIntervalInMs = TimeUnit.MILLISECONDS.convert(ticksIntervalTimeValue, ticksIntervalTimeUnit);

			// add  t = 0 -> 0
			map.put(0L, 0d);

			for(long t = ticksIntervalInMs; t <= TimeUnit.SECONDS.toMillis(maxExecutionTimeInSeconds); t += ticksIntervalInMs) {
				// add value of last entry before this time point
				map.put(t, map.get(runtimeVsBestScore.headMap(t).lastKey()));
			}

			// add  entry for t = totalRuntime
			long totalRuntimeMs = Math.min(TimeUnit.SECONDS.toMillis(maxExecutionTimeInSeconds), TimeUnit.NANOSECONDS.toMillis(totalRuntimeNs));
			map.put(totalRuntimeMs, map.get(map.lastKey()));
		}

		return map;
	}

	/* (non-Javadoc)
			 * @see java.lang.Object#clone()
			 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new CELOE(this);
	}

	/**
	 * @Hua: commented for convenience 
	 */
/*	public static void main(String[] args) throws Exception{
//		File file = new File("../examples/swore/swore.rdf");
//		OWLClass classToDescribe = new OWLClassImpl(IRI.create("http://ns.softwiki.de/req/CustomerRequirement"));
		File file = new File("../examples/father.owl");
		OWLClass classToDescribe = new OWLClassImpl(IRI.create("http://example.com/father#male"));
		
		OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
		
		AbstractKnowledgeSource ks = new OWLAPIOntology(ontology);
		ks.init();
		
		OWLAPIReasoner baseReasoner = new OWLAPIReasoner(ks);
		baseReasoner.setReasonerImplementation(ReasonerImplementation.HERMIT);
        baseReasoner.init();
		ClosedWorldReasoner rc = new ClosedWorldReasoner(ks);
		rc.setReasonerComponent(baseReasoner);
		rc.init();
		
		ClassLearningProblem lp = new ClassLearningProblem(rc);
//		lp.setEquivalence(false);
		lp.setClassToDescribe(classToDescribe);
		lp.init();
		
		RhoDRDown op = new RhoDRDown();
		op.setReasoner(rc);
		op.setUseNegation(false);
		op.setUseHasValueConstructor(false);
		op.setUseCardinalityRestrictions(true);
		op.setUseExistsConstructor(true);
		op.setUseAllConstructor(true);
		op.init();
		
		
		
		//(male ⊓ (∀ hasChild.⊤)) ⊔ (∃ hasChild.(∃ hasChild.male))
		OWLDataFactory df = new OWLDataFactoryImpl();
		OWLClass male = df.getOWLClass(IRI.create("http://example.com/father#male"));
		OWLClassExpression ce = df.getOWLObjectIntersectionOf(
									df.getOWLObjectUnionOf(
											male,
											df.getOWLObjectIntersectionOf(
													male, male),
											df.getOWLObjectSomeValuesFrom(
												df.getOWLObjectProperty(IRI.create("http://example.com/father#hasChild")),
												df.getOWLThing())
									),
									df.getOWLObjectAllValuesFrom(
											df.getOWLObjectProperty(IRI.create("http://example.com/father#hasChild")),
											df.getOWLThing()
											)
				);
		System.out.println(ce);
		OWLClassExpressionMinimizer min = new OWLClassExpressionMinimizer(df, rc);
		ce = min.minimizeClone(ce);
		System.out.println(ce);
		
		CELOE alg = new CELOE(lp, rc);
		alg.setMaxExecutionTimeInSeconds(10);
		alg.setOperator(op);
		alg.setWriteSearchTree(true);
		alg.setSearchTreeFile("log/search-tree.log");
		alg.setReplaceSearchTree(true);
		alg.init();
		alg.setKeepTrackOfBestScore(true);
		
		alg.start();

		SortedMap<Long, Double> map = alg.getRuntimeVsBestScore(1, TimeUnit.SECONDS);
		System.out.println(MapUtils.asTSV(map, "runtime", "best_score"));

	}
	*/
}