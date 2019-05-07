package org.dllearner.algorithms.layerwise;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.dllearner.core.AbstractCELA;
import org.dllearner.core.AbstractClassExpressionLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.owl.ClassHierarchy;
import org.dllearner.core.owl.DatatypePropertyHierarchy;
import org.dllearner.core.owl.ObjectPropertyHierarchy;
import org.dllearner.learningproblems.ClassAsInstanceLearningProblem;
import org.dllearner.learningproblems.ClassLearningProblem;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.refinementoperators.AbstractRefinementOperator;
import org.dllearner.refinementoperators.CustomHierarchyRefinementOperator;
import org.dllearner.refinementoperators.CustomStartRefinementOperator;
import org.dllearner.refinementoperators.LengthLimitedRefinementOperator;
import org.dllearner.refinementoperators.OperatorInverter;
import org.dllearner.refinementoperators.ReasoningBasedRefinementOperator;
import org.dllearner.refinementoperators.RhoDRDown;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.OWLAPIUtils;
import org.dllearner.utilities.owl.ConceptTransformation;
import org.dllearner.utilities.owl.EvaluatedDescriptionSet;
import org.dllearner.utilities.owl.OWLAPIRenderers;
import org.dllearner.utilities.owl.OWLClassExpressionMinimizer;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.dllearner.utilities.owl.PropertyContext;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public abstract class CELOEBase extends AbstractCELA implements Cloneable{

	protected static final Logger logger = LoggerFactory.getLogger(CELOEBase.class);

	@ConfigOption(description = "the refinement operator instance to use")
	protected LengthLimitedRefinementOperator operator;

	@ConfigOption(defaultValue = "owl:Thing",
			description = "You can specify a start class for the algorithm. To do this, you have to use Manchester OWL syntax either with full IRIs or prefixed IRIs.",
			exampleValue = "ex:Male or http://example.org/ontology/Female")
	protected OWLClassExpression startClass;

	// if true, then each solution is evaluated exactly instead of approximately
	// protected boolean exactBestDescriptionEvaluation = false;
	@ConfigOption(defaultValue="false", description="Use this if you are interested in only one suggestion and your learning problem has many (more than 1000) examples.")
	protected boolean singleSuggestionMode;
	
	// important: do not initialise those with empty sets
	// null = no settings for allowance / ignorance
	// empty set = allow / ignore nothing (it is often not desired to allow no class!)
	@ConfigOption(defaultValue="false", description="specifies whether to write a search tree")
	protected boolean writeSearchTree = false;

	@ConfigOption(defaultValue="log/searchTree.txt", description="file to use for the search tree")
	protected String searchTreeFile = "log/searchTree.txt";	

	@ConfigOption(defaultValue="false", description="specifies whether to replace the search tree in the log file after each run or append the new search tree")
	protected boolean replaceSearchTree = false;

	@ConfigOption(defaultValue="10", description="Sets the maximum number of results one is interested in. (Setting this to a lower value may increase performance as the learning algorithm has to store/evaluate/beautify less descriptions).")
	protected int maxNrOfResults = 10;

	@ConfigOption(defaultValue="0.0", description="the (approximated) percentage of noise within the examples")
	protected double noisePercentage = 0.0;

	@ConfigOption(defaultValue="false", description="If true, then the results will not contain suggestions, which already follow logically from the knowledge base. Be careful, since this requires a potentially expensive consistency check for candidate solutions.")
	protected boolean filterDescriptionsFollowingFromKB = false;

	@ConfigOption(defaultValue="false", description="If true, the algorithm tries to find a good starting point close to an existing definition/super class of the given class in the knowledge base.")
	protected boolean reuseExistingDescription = false;

	@ConfigOption(defaultValue="0", description="The maximum number of candidate hypothesis the algorithm is allowed to test (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
	protected int maxClassExpressionTests = 0;

	@ConfigOption(defaultValue="0", description = "The maximum number of candidate hypothesis the algorithm is allowed after an improvement in accuracy (0 = no limit). The algorithm will stop afterwards. (The real number of tests can be slightly higher, because this criterion usually won't be checked after each single test.)")
	protected int maxClassExpressionTestsAfterImprovement = 0;

	@ConfigOption(defaultValue = "0", description = "maximum execution of the algorithm in seconds after last improvement")
	protected int maxExecutionTimeInSecondsAfterImprovement = 0;

	@ConfigOption(defaultValue="false", description="specifies whether to terminate when noise criterion is met")
	protected boolean terminateOnNoiseReached = false;

	@ConfigOption(defaultValue="7", description="maximum depth of description")
	protected double maxDepth = 7;

	@ConfigOption(defaultValue="false", description="algorithm will terminate immediately when a correct definition is found")
	protected boolean stopOnFirstDefinition = true;
		
	@ConfigOption(defaultValue = "false",  description = "whether to try and refine solutions which already have accuracy value of 1")
	protected boolean expandAccuracy100Nodes = false;	
	
	
	
	/**
	 * ------------------------------------------------------------------------------------------------------------------------------
	 * internal variables
	 * ------------------------------------------------------------------------------------------------------------------------------ 
	 */
	// all descriptions in the search tree plus those which were too weak (for fast redundancy check)
	protected TreeSet<OWLClassExpression> descriptions;
	protected OWLClassExpression bestDescription;
	protected double bestAccuracy = Double.MIN_VALUE;
	protected OWLClass classToDescribe;
	// examples are either 1.) instances of the class to describe 2.) positive examples
	// 3.) union of pos.+neg. examples depending on the learning problem at hand
	protected Set<OWLIndividual> examples;
	// CELOE was originally created for learning classes in ontologies, but also
	// works for other learning problem types
	protected boolean isClassLearningProblem;
	protected boolean isEquivalenceProblem;
	// important parameters (non-config options but internal)
	protected double noise;
	protected boolean filterFollowsFromKB = false;
	// less important parameters
	// forces that one solution cannot be subexpression of another expression; this option is useful to get diversity
	// but it can also suppress quite useful expressions
	protected boolean forceMutualDifference = false;
	// statistical variables
	protected int expressionTests = 0;
	protected long totalRuntimeNs = 0;
	@SuppressWarnings("unused")
	protected long timeLastImprovement = 0;
	protected double currentHighestAccuracy;

	// option to keep track of best score during algorithm run
	protected boolean keepTrackOfBestScore = false;
	protected SortedMap<Long, Double> runtimeVsBestScore = new TreeMap<>();

	protected ClassHierarchy classHierarchy;
	protected ObjectPropertyHierarchy objectPropertyHierarchy;
	protected DatatypePropertyHierarchy datatypePropertyHierarchy;
	protected int expressionTestCountLastImprovement;
	/**
	 * --------------------------------------------------------------------------------------------------------------------------------
	 * ------------------------------------------------------------------------------------------------------------------------------
	 */
	

	/**
	 * ------------------------------------------------------------------------------------------------------------------------------
	 * Extended internal variables
	 * ------------------------------------------------------------------------------------------------------------------------------  
	 */	
	protected int timesCeloeDecided = 0;
	protected int timesCeloeUndecided = 0;
	
	protected String logFile = "";
	protected String pathPfx = "";
	protected int heStep;
	protected boolean heCorr;	 
	protected static Random rnd = new Random();	
	protected long backTime = 0;
	protected double bestPredAcc = Double.MIN_VALUE;
	protected double bestNegOnlyAcc = Double.MIN_VALUE;	
	/**
	 * --------------------------------------------------------------------------------------------------------------------------------
	 * ------------------------------------------------------------------------------------------------------------------------------
	 */


	public CELOEBase() {}

	public CELOEBase(CELOEBase celoe){
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

	public CELOEBase(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
	}

	public static Collection<Class<? extends AbstractClassExpressionLearningProblem>> supportedLearningProblems() {
		Collection<Class<? extends AbstractClassExpressionLearningProblem>> problems = new LinkedList<>();
		problems.add(AbstractClassExpressionLearningProblem.class);
		return problems;
	}
	
	protected String getLogPath () {
		if(heStep == 0 && heCorr)
			pathPfx = "original";
		
		if(heStep != 0 && heCorr)
			pathPfx = "he" + String.valueOf(heStep) + "corr";
		
		if(heStep != 0 && !heCorr)
			pathPfx = "he" + String.valueOf(heStep);
		
		String resultPath = searchTreeFile.substring(0, searchTreeFile.lastIndexOf("/"));		
		
		return resultPath + "/" + pathPfx + "/";
	}
	
	@Override
	public void init() throws ComponentInitException {
		
		/*
		 * Batch config settings
		 * original celoe: heStep = 0, heCorr = true
		 */
//		writeSearchTree = true;
//		traverseWholeTree = true;
//		newNodesLowerbound = 0
		heStep = 1;
		heCorr = false;
		
		baseURI = reasoner.getBaseURI();
		prefixes = reasoner.getPrefixes();
			
		if(maxExecutionTimeInSeconds != 0 && maxExecutionTimeInSecondsAfterImprovement != 0) {
			maxExecutionTimeInSeconds = Math.min(maxExecutionTimeInSeconds, maxExecutionTimeInSecondsAfterImprovement);
		}
		
		// TODO add comment
		classHierarchy = initClassHierarchy();
		objectPropertyHierarchy = initObjectPropertyHierarchy();
		datatypePropertyHierarchy = initDataPropertyHierarchy();
		
		minimizer = new OWLClassExpressionMinimizer(dataFactory, reasoner);
		
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
	}
	
	/*
	 * Compute the start class in the search space from which the refinement will start.
	 * We use the intersection of super classes for definitions (since it needs to
	 * capture all instances), but owl:Thing for learning subclasses (since it is
	 * superfluous to add super classes in this case)
	 */
	protected OWLClassExpression computeStartClass() {
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
	
	protected boolean terminationCriteriaSatisfied() {
		return
		stop ||
//		(maxClassExpressionTestsAfterImprovement != 0 && (expressionTests - expressionTestCountLastImprovement >= maxClassExpressionTestsAfterImprovement)) ||
//		(maxClassExpressionTests != 0 && (expressionTests >= maxClassExpressionTests)) ||
//		(maxExecutionTimeInSecondsAfterImprovement != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSecondsAfterImprovement* 1000000000L))) ||
//		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
		(maxExecutionTimeInSeconds != 0 && ((System.nanoTime() - nanoStartTime - logTime) >= (maxExecutionTimeInSeconds* 1000000000L))) ||
		(terminateOnNoiseReached && (100*getCurrentlyBestAccuracy()>=100-noisePercentage)) ||
		(stopOnFirstDefinition && (getCurrentlyBestAccuracy() >= 1));
	}
	
	protected void reset() {
		// set all values back to their default values (used for running
		// the algorithm more than once)
		descriptions = new TreeSet<>();
		bestEvaluatedDescriptions.getSet().clear();
		expressionTests = 0;
		runtimeVsBestScore.clear();
	}
	
	protected void printAlgorithmRunStats(int size) {
		if (stop) {
			logger.info("Algorithm stopped ("+expressionTests+" descriptions tested). " + size + " nodes in the search tree.\n");
			String s = "Algorithm stopped ("+expressionTests+" descriptions tested). " + size + " nodes in the search tree.";
			try {
				saveLog(logFile, s);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			totalRuntimeNs = System.nanoTime()-nanoStartTime;
            String s = "\nAlgorithm terminated successfully (time: " + Helper.prettyPrintNanoSeconds(totalRuntimeNs) + ", "+expressionTests+" descriptions tested, "  + size + " nodes in the search tree).\n";
            	s += reasoner.toString();
        		try {
        			saveLog(logFile, s);
        		} catch (FileNotFoundException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
            	
            	s = "Iterations: " + countIterations + "\n";
        		s += "Times CELOE undecided: " + timesCeloeUndecided + "\n";
        		s += "Times CELOE decided: " + timesCeloeDecided + "\n";
        		s += "% CELOE undecided: "+ ((double)timesCeloeUndecided)/((double)timesCeloeUndecided+timesCeloeDecided) + "\n";
        		s += "Nodes in search tree: " + size + "\n";
        		s += "Expressions tested: " + expressionTests + "\n";
        		s += "time: " + Helper.prettyPrintNanoSeconds(System.nanoTime() - nanoStartTime - logTime) + " (log: " + Helper.prettyPrintNanoSeconds(logTime) + " )\n";
//        		s += " - log time: " + Helper.prettyPrintNanoSeconds(logTime) + "\n";
        		s += " - refinement time: " + Helper.prettyPrintNanoSeconds(refineTime) + "\n";        		
        		s += " - tree time: " + Helper.prettyPrintNanoSeconds(treeTime) + "\n";
        		s += " - rewrite time: " + Helper.prettyPrintNanoSeconds(rewriteTime) + "\n";
        		s += " - select time: " + Helper.prettyPrintNanoSeconds(selectTime) + "\n";
        		s += " - back time: " + Helper.prettyPrintNanoSeconds(backTime) + "\n\n";
        		
        		s += "solutions:\n" + getSolutionString();
        		try {
        			saveLog(logFile, s);
        		} catch (FileNotFoundException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
 		}
	}
	
	protected void showIfBetterSolutionsFound() {
		if(!singleSuggestionMode && bestEvaluatedDescriptions.getBestAccuracy() > currentHighestAccuracy) {
			currentHighestAccuracy = bestEvaluatedDescriptions.getBestAccuracy();
			expressionTestCountLastImprovement = expressionTests;
			timeLastImprovement = System.nanoTime();
			long durationInMillis = getCurrentRuntimeInMilliSeconds();
			String durationStr = getDurationAsString(durationInMillis);

			// track new best accuracy if enabled
			if(keepTrackOfBestScore) {
				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), currentHighestAccuracy);
//				runtimeVsBestScore.put(getCurrentRuntimeInMilliSeconds(), bestPredAcc);
			}

			try {
				saveLog(logFile, "more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
//				saveLog(logFile, "more accurate (" + dfPercent.format(bestPredAcc) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// checks whether the class expression is allowed
	protected boolean isDescriptionAllowed(OWLClassExpression description, OWLClassExpression parent) {
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
			return true;
		}
		
		// perform forall sanity tests
		if (parent != null &&
				(ConceptTransformation.getForallOccurences(description) > ConceptTransformation.getForallOccurences(parent))) {
			// we have an additional \forall construct, so we now fetch the contexts
			// in which it occurs
			SortedSet<PropertyContext> contexts = ConceptTransformation.getForallContexts(description);
			SortedSet<PropertyContext> parentContexts = ConceptTransformation.getForallContexts(parent);
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
	protected boolean occursOnFirstLevel(OWLClassExpression description, OWLClassExpression cls) {
		return !cls.isOWLThing() && (description instanceof OWLNaryBooleanClassExpression && ((OWLNaryBooleanClassExpression) description).getOperands().contains(cls));
//        return description.containsConjunct(cls) ||
//                (description instanceof OWLObjectUnionOf && ((OWLObjectUnionOf) description).getOperands().contains(cls));
	}
	
	// determine whether a named class occurs on the outermost level, i.e. property depth 0
		// (it can still be at higher depth, e.g. if intersections are nested in unions)
		protected boolean occursOnSecondLevel(OWLClassExpression description, OWLClassExpression cls) {
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

	protected double getCurrentlyBestAccuracy() {
		return bestEvaluatedDescriptions.getBest().getAccuracy();		
//		return bestPredAcc;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public void stop() {
		stop = true;
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
	

	protected void saveLog(String filename, String log) throws FileNotFoundException {		
		long logStart = System.nanoTime(); 
		logger.info(log);
		try
		{
			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
			fw.write(log+"\n");
			fw.close();
		}
		catch(IOException ioe)
		{
			System.err.println("IOException: " + ioe.getMessage());
		}
		long logEnd = System.nanoTime();
		logTime += (logEnd - logStart);
	}

	
}