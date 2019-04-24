package org.dllearner.algorithms.aml;

import com.google.common.collect.Sets;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import org.apache.jena.base.Sys;
import org.dllearner.accuracymethods.AccMethodFMeasure;
import org.dllearner.accuracymethods.AccMethodPredAcc;
import org.dllearner.accuracymethods.AccMethodTwoValued;
import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.algorithms.celoe.OEHeuristicRuntime;
import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.algorithms.ocel.ExampleBasedNode;
import org.dllearner.core.*;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.owl.ClassHierarchy;
import org.dllearner.core.owl.DatatypePropertyHierarchy;
import org.dllearner.core.owl.ObjectPropertyHierarchy;
import org.dllearner.kb.KBFile;
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
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("CloneDoesntCallSuperClone")
@ComponentAnn(name="CELOEAml", shortName="celoe_aml", version=1.0, description="CELOE is an adapted and extended version of the OCEL algorithm applied for the ontology engineering use case. See http://jens-lehmann.org/files/2011/celoe.pdf for reference.")
public class CELOEAml extends CELOEAmlBase{

	private static final Logger logger = LoggerFactory.getLogger(CELOEAml.class);

	@ConfigOption(defaultValue="", description="project path")
	protected String projectPath = "";
	@ConfigOption(defaultValue="", description="test name")
	protected String caseName = "";

	protected String projectName;
	protected String logPath = "";
	protected String logBaseName = "";
	protected File treeFile;
	protected String opName;
	protected String accName;

	protected String treeString = "";
	protected int loop = 1;
	protected long logTime = 0;
	protected long refineTime = 0;
	protected long treeTime = 0;
	//	protected long treeTime1 = 0;
	//	protected long treeTime2 = 0;
	//	protected long treeTime3 = 0;

	protected boolean doPrint = false;
	protected boolean verbose = false;
	ExcelTable statistics;
	protected boolean writeStatistics = true;

	public CELOEAml() {
		super();
	}

	public CELOEAml(CELOEAml celoe2){
		setReasoner(celoe2.reasoner);
		setLearningProblem(celoe2.learningProblem);

		setAllowedConcepts(celoe2.getAllowedConcepts());
		setAllowedObjectProperties(celoe2.getAllowedObjectProperties());
		setAllowedDataProperties(celoe2.getAllowedDataProperties());

		setIgnoredConcepts(celoe2.ignoredConcepts);
		setIgnoredObjectProperties(celoe2.getIgnoredObjectProperties());
		setIgnoredDataProperties(celoe2.getIgnoredDataProperties());

		setExpandAccuracy100Nodes(celoe2.expandAccuracy100Nodes);
		setFilterDescriptionsFollowingFromKB(celoe2.filterDescriptionsFollowingFromKB);
		setHeuristic(celoe2.heuristic);

		setMaxClassExpressionTests(celoe2.maxClassExpressionTests);
		setMaxClassExpressionTestsAfterImprovement(celoe2.maxClassExpressionTestsAfterImprovement);
		setMaxDepth(celoe2.maxDepth);
		setMaxExecutionTimeInSeconds(celoe2.getMaxExecutionTimeInSeconds());
		setMaxExecutionTimeInSecondsAfterImprovement(celoe2.maxExecutionTimeInSecondsAfterImprovement);
		setMaxNrOfResults(celoe2.maxNrOfResults);
		setNoisePercentage(celoe2.noisePercentage);

		LengthLimitedRefinementOperator op = new RhoDRDown((RhoDRDown)celoe2.operator);

		try {
			op.init();
		} catch (ComponentInitException e) {
			e.printStackTrace();
		}
		setOperator(op);


		setReuseExistingDescription(celoe2.reuseExistingDescription);
		setSingleSuggestionMode(celoe2.singleSuggestionMode);
		setStartClass(celoe2.startClass);
		setStopOnFirstDefinition(celoe2.stopOnFirstDefinition);
		setTerminateOnNoiseReached(celoe2.terminateOnNoiseReached);
		setUseMinimizer(celoe2.isUseMinimizer());

		setWriteSearchTree(celoe2.writeSearchTree);
		setReplaceSearchTree(celoe2.replaceSearchTree);
		setProjectPath(celoe2.projectPath);
	}
	
	public CELOEAml(AbstractClassExpressionLearningProblem problem, AbstractReasonerComponent reasoner) {
		super(problem, reasoner);
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
		
		/**
		 * @Hua: parse the input for benchmark
		 */

		if(learningProblem.getAccuracyMethod() instanceof AccMethodFMeasure)
			accName = "fm";
		else if(learningProblem.getAccuracyMethod() instanceof AccMethodPredAcc)
			accName = "pred";
		else
			accName = "accX";	

		// operator
		if(operator instanceof AMLOperator)
			opName = "aml";
		else if(operator instanceof RhoDRDown)
			opName = "rho";
		else {
			opName = "opX";
			writeStatistics = false;
		}

		String ksFileName = "";
		String ksDirName = "";
		for (KnowledgeSource ks : reasoner.getSources()) {
			if(ks.getClass().getSimpleName().contains("KBFile")) {
				ksFileName = ((KBFile) ks).getFileName();
				ksDirName = ((KBFile) ks).getBaseDir();
			}
			if(ks.getClass().getSimpleName().contains("OWLFile")) {
				ksFileName = ((org.dllearner.kb.OWLFile) ks).getFileName();
				ksDirName = ((org.dllearner.kb.OWLFile) ks).getBaseDir();
			}				
		}				

		// parse test case
		int lastSlash = projectPath.lastIndexOf("/");
		if(lastSlash != -1) {
			projectName = projectPath.substring(lastSlash+1, projectPath.length());
			caseName += "_" + accName + "_" + opName + "_" + ((OEHeuristicRuntime) heuristic).getExpansionPenaltyFactor();
		}else
			writeStatistics = false;

		// search file
		if(!writeStatistics && caseName != null && caseName != "") {
			logBaseName = searchTreeFile + "/" + caseName;
			treeFile = new File(logBaseName + ".tree");
			if (treeFile.getParentFile() != null) {
				treeFile.getParentFile().mkdirs();
			}		
			if(writeSearchTree)
				Files.clearFile(treeFile);
		}else
			writeStatistics = false;

		initialized = true;

	}

	@Override
	public void start() {

		reasoner.resetTimer();
		//		doPrint = true;
		//		verbose = true;
		stop = false;
		isRunning = true;
		reset();
		nanoStartTime = System.nanoTime();

		refineTime = 0;
		treeTime = 0;
		logTime = 0;

		currentHighestAccuracy = 0.0;
		OENode nextNode;

		//		logger.info("start class:" + startClass);		
		addNode(startClass, null);
		OWLClassExpression bd = null;		

		long duration = 0;
		long oneMinute = 60000;
		int di = 0;
		while (!terminationCriteriaSatisfied()) {	

			duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoStartTime - logTime)  - oneMinute*di;
			if(duration - oneMinute  > 0) {
				di++;
				duration = 0;				
				System.out.println(di + " minutes passed...");
			}

			if(verbose) {
				logger.info("loop: " + loop);
				logger.info("- Nr nodes: " + searchTree.size() + "\n");
				logger.info("- Nr rules: " + expressionTests + "\n");
			}			

			/**
			 * @Hua: note, some of the best evaluated descriptions are not saved in the search tree
			 * they are rewritten from the bests candidates and are semantically equivalent to them. 
			 * They are just for better reading. 
			 * For example, hasB.Top is stored in tree, but represented as hasB.B in the list
			 */

			if(bestEvaluatedDescriptions.getBestAccuracy() == 1.0)
			{
				this.stop = true;
				if(bd == null || !bd.equals(bestEvaluatedDescriptions.getBest().getDescription())) {
					bd = bestEvaluatedDescriptions.getBest().getDescription();
					logger.info("   - class expression found: " + descriptionToString(bd));
					logger.info("   - required time: " + Helper.prettyPrintNanoSeconds(System.nanoTime() - nanoStartTime - logTime));
				}				

				if(verbose) {
					logger.info("tree time: " + Helper.prettyPrintNanoSeconds(treeTime));
					logger.info("reasoning time: " + Helper.prettyPrintNanoSeconds(reasoner.getOverallReasoningTimeNs()));
				}

			}

			long t1 = System.nanoTime();
			if(doPrint) {
				String durationStr = ifBetterSolutionsFound();
				if(durationStr != null)
				{
					System.out.println("\nloop " + loop + ": ");
					logger.info(" - more accurate (" + dfPercent.format(currentHighestAccuracy) + ") class expression found after " + durationStr + ": " + descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));					
				}else
				{
					if(loop%1 == 0 && verbose) {
						logger.info("\nloop " + loop + ":");
						logger.info("expanding: " + getNextNodeToExpand());
						if(verbose) {
							logger.info("depth: " + searchTree.getDepth());
							logger.info("nr of nodes: " + searchTree.size());
							logger.info(getStates(false));
						}
					}
				}
			}
			logTime += System.nanoTime() - t1;

			/**
			 * @Hua: the search tree is again sorted: best node at the end
			 * 		 this picks the "best" node to expand, and will continue refine it if it not becomes worse
			 */
			// chose best node according to heuristics
			nextNode = getNextNodeToExpand();

			int horizExp = nextNode.getHorizontalExpansion();

			long t2 = System.nanoTime();
			if (writeSearchTree) {
				treeString = "loop " + loop + "\n";
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
			long r1 = reasoner.getOverallReasoningTimeNs();
			TreeSet<OWLClassExpression> refinements = refineNode(nextNode);
			long r2 = reasoner.getOverallReasoningTimeNs();			
			refineTime += System.nanoTime() - refineStart - (r2-r1);

			while(!refinements.isEmpty() && !terminationCriteriaSatisfied()) {				
				// pick element from set
				OWLClassExpression refinement = refinements.pollFirst();
				int d = OWLClassExpressionUtils.getDepth(refinement);

				// get length of class expression
				int length = OWLClassExpressionUtils.getLength(refinement);

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

			long t3 = System.nanoTime();
			if (writeSearchTree) {
				treeString += TreeUtils.toTreeString(searchTree) + "\n";		
				if(treeFile.length() > 10000000) {
					treeFile = new File(logBaseName + "_" + loop + ".tree");				
					Files.createFile(treeFile, treeString);					
				}	
				Files.appendToFile(treeFile, treeString);				
			}
			logTime += System.nanoTime() - t3;		

			// update the global min and max horizontal expansion values
			updateMinMaxHorizExp(nextNode);						

			loop++;						
		}

		if(singleSuggestionMode) {
			bestEvaluatedDescriptions.add(bestDescription, bestAccuracy, learningProblem);
		}

		String states = getStates(true);
		if(logBaseName != null && logBaseName != "") {
			String stateFile = logBaseName + ".log";			
			try {
				saveStates(stateFile, states);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				

		if(writeStatistics) {
			System.out.println("writing " + projectName + "_" + opName + " to file: " + projectPath+"/statistics.xlsx"); 
			saveStatistics(projectPath+"/statistics.xlsx", opName);
		}

		if(doPrint)
			System.out.println(states);

		// print some stats
		//		printAlgorithmRunStats();

		// print solution(s)
		//		logger.info("\nsolutions:\n" + getSolutionString());

		isRunning = false;
	}


	// expand node horizontically
	protected TreeSet<OWLClassExpression> refineNode(OENode node) {
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
//		TreeSet<OWLClassExpression> refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp);
		TreeSet<OWLClassExpression> refinements = (TreeSet<OWLClassExpression>) operator.refine(node.getDescription(), horizExp+1);

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
	protected boolean addNode(OWLClassExpression description, OENode parentNode) {

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

		long treeStart = System.nanoTime();		
		OENode node = new OENode(description, accuracy);
		searchTree.addNode(parentNode, node);
		treeTime += System.nanoTime() - treeStart;

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


	/**
	 * @Hua: get the processing state of the algorithm 
	 * @param finalStats
	 * @return
	 */
	private String getStates(boolean finalStats) {
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
			String states = "\nloop " + loop + ":" + "\n";
			states += "nr of nodes: " + searchTree.size() + "\n";
			states += "tree depth: " + searchTree.getDepth() + "\n";
			states += "rules tested: " + expressionTests + "\n";				
			return states;
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

			double treePercentage = 100 * treeTime / (double) computationTime;

			int lastSlash = searchTreeFile.lastIndexOf("/");
			String test = searchTreeFile.substring(lastSlash+1, searchTreeFile.length()) + "_" + opName;

			String states = "==================" + test + "==================\n\n";
			states += "Iterations: " + loop + "\n";
			states += "Tree depth: " + searchTree.getDepth() + "\n";
			states += "Nr nodes: " + searchTree.size() + "\n";
			states += "Nr rules: " + expressionTests + "\n";

			states += "Total runtime: " + Helper.prettyPrintNanoSeconds(algorithmRuntime) + "\n";
			states += "Total log time: " + Helper.prettyPrintNanoSeconds(logTime) + " (" + df.format(logPercetange) + "%)" + "\n";
			states += "Total computation time: " + Helper.prettyPrintNanoSeconds(computationTime) + " (" + df.format(computePercentage) + "%)" + "\n";
			states += "   - refinement: " + Helper.prettyPrintNanoSeconds(refineTime) + "(" + df.format(refinementPercentage) + "%)" + "\n";
			states += "   - reasoning: " + Helper.prettyPrintNanoSeconds(reasoningTime) + " (" + df.format(reasoningPercentage) + "%)" + "\n";
			states += "   - reasoning: " + Helper.prettyPrintNanoSeconds(treeTime) + " (" + df.format(treePercentage) + "%)" + "\n";
			states += "   - instance check time: " + Helper.prettyPrintNanoSeconds(reasoner.getInstanceCheckReasoningTimeNs()) + "(" + df.format(instanceCheckPercentage) + "%)" + "\n";
			states += "   - subsumption: " + Helper.prettyPrintNanoSeconds(subTime) + "(" + df.format(subPercentage) + "%)" + "\n";

			states += getSolutionString() + "\n\n";
			return states;

			//			logger.info("========== FINAL STATS ==========");
			//			System.out.println("Iterations: " + loop);
			//			System.out.println("Tree depth: " + searchTree.getDepth());
			//			System.out.println("Nr nodes: " + searchTree.size());					
			//			System.out.println("Nr rules: " + expressionTests);
			//						
			//			System.out.println("Total runtime: " + Helper.prettyPrintNanoSeconds(algorithmRuntime));
			//			System.out.println("Total log time: " + Helper.prettyPrintNanoSeconds(logTime) + " (" + df.format(logPercetange) + "%)");
			//			System.out.println("Total computation time: " + Helper.prettyPrintNanoSeconds(computationTime) + " (" + df.format(computePercentage) + "%)");
			////			logger.info(" - expand node time: " + Helper.prettyPrintNanoSeconds(extendNodeTimeNs) + " (" + df.format(expandPercetange) + "%)");
			//			logger.info("   - refinement: " + Helper.prettyPrintNanoSeconds(refineTime) + "(" + df.format(refinementPercentage) + "%)");
			//			logger.info("   - reasoning: " + Helper.prettyPrintNanoSeconds(reasoningTime) + " (" + df.format(reasoningPercentage) + "%)");
			//			logger.info("   - instance check time: " + Helper.prettyPrintNanoSeconds(reasoner.getInstanceCheckReasoningTimeNs()) + "(" + df.format(instanceCheckPercentage) + "%)");
			//			logger.info("   - subsumption: " + Helper.prettyPrintNanoSeconds(subTime) + "(" + df.format(subPercentage) + "%)");

		}
	}

	/**
	 * @Hua: save the statistics to the EXCEL table
	 * @param table
	 * @param op
	 */
	private void saveStatistics(String table, String op) {
		/**
		 * Write to excel
		 */							
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
			data.setTreeTime(TimeUnit.MILLISECONDS.convert(treeTime, TimeUnit.NANOSECONDS));
			data.setReasoning(TimeUnit.MILLISECONDS.convert(reasoningTime, TimeUnit.NANOSECONDS));
			data.setInstCheck(TimeUnit.MILLISECONDS.convert(reasoner.getInstanceCheckReasoningTimeNs(), TimeUnit.NANOSECONDS));
			data.setSubsumption(TimeUnit.MILLISECONDS.convert(subTime, TimeUnit.NANOSECONDS));
			if(bestEvaluatedDescriptions.getBestAccuracy() == 1.0)
				data.setSolution(descriptionToString(bestEvaluatedDescriptions.getBest().getDescription()));
			statistics.newData(data.getName(), data);

			statistics.write(table);
			//			System.out.println("successfully saved " + table + " to disk");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}

	/**
	 * @Hua: write the final state log
	 * @param filename
	 * @param states
	 * @throws FileNotFoundException
	 */
	private void saveStates(String filename, String states) throws FileNotFoundException {
		try
		{
			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
			fw.write(states);
			fw.close();
		}
		catch(IOException ioe)
		{
			System.err.println("IOException: " + ioe.getMessage());
		}
	}

	/**
	 * @Hua: whether a better solution has been found
	 * @return
	 */
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


	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
		if(projectPath.endsWith("/"))
			this.searchTreeFile = projectPath + "log";
		else
			this.searchTreeFile = projectPath + "/log";

	}


	/**
	 * @return the testName
	 */
	public String getCaseName() {
		return caseName;
	}

	/**
	 * @param testName the testName to set
	 */
	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new CELOEAml(this);
	}
	
	public OWLClassExpression getBestDescription() {
		return this.bestEvaluatedDescriptions.getBest().getDescription();
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