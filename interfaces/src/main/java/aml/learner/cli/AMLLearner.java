package aml.learner.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.jena.base.Sys;
import org.apache.jena.riot.system.InitRIOT;
import org.basex.core.Context;
import org.basex.query.QueryException;
import org.basex.query.QueryIOException;
import org.basex.query.QueryProcessor;
import org.basex.query.value.Value;
import org.basex.query.value.item.Item;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigOption;
import org.json.simple.parser.ParseException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CAEX215.CAEX215Factory;
import CAEX215.CAEXFileType;
import CAEX215.CAEXObject;
import CAEX215.InstanceHierarchyType;
import CAEX215.InternalElementType;
import aml.learner.config.AMLLearnerConfig;
import aml.learner.config.AMLLearnerSolution;
import aml.learner.gui.SimpleGui;
import aml.learner.gui.TextAreaOutputStream;
import concept.model.AMLConceptConfig;
import concept.model.AMLQueryConfig;
import concept.model.GenericAMLConceptModel;
import concept.tree.GenericTreeNode;
import concept.util.GenericAMLConceptModelUtils;
import constants.AMLClassIRIs;
import exporter.AMLExporter;
import generator.TransitiveClosure;
import generator.XQueryGenerator;
import serializer.BaseX2AMLConverter;
import translation.expression.AMLConceptTree;
import translation.expression.TranslationUtils;
import xquery.AuxiliaryXQueryNode;
import xquery.XQueryReturn;


/**
 * The AMLLearner framework
 * 	- load dl-learner config and learn OWL classes
 *  - OWL -> ACM
 *  - ACM -> nAQL
 *  - nAQL -> XQuery
 *  - XQuery -> AML (extracted data)
 * @author aris
 *
 */
public class AMLLearner {//implements Runnable{
	
	static {
		if (System.getProperty("log4j.configuration") == null)
			System.setProperty("log4j.configuration", "log4j.properties");
	}

	private static Logger logger = LoggerFactory.getLogger(AMLLearner.class);	

	private ConceptLearner conceptLearner;
	private static final String xqueryRoot = "CAEXFile/InstanceHierarchy";
	private static final String home = "D:/repositories/aml/aml_framework/src/main/resources/test/";
	private static final String JSONFILE = "json";
	private static final String CONFFILE = "conf";
	private static final String AMLFILE = "aml";
	    
	private boolean doSerialize = false;
	//TextAreaOutputStream stream = TextAreaOutputStream.getInstance(SimpleGui.logArea);
	
	private BlockingQueue<Boolean> triggerQueue;
	private BlockingQueue<AMLLearnerSolution> solutionQueue;
	private BlockingQueue<NavigableSet<? extends EvaluatedDescription<? extends Score>>> resultQueue;
	
	private String configFile;
	private int numResults; 
	private List<OWLClassExpression> results;
	
	private IRI caexType;
	
	public AMLLearner (String configFile, BlockingQueue<Boolean> triggerQueue, BlockingQueue<AMLLearnerSolution> solutionQueue, int numResults) {
		this.configFile = configFile;
		this.triggerQueue = triggerQueue;
		this.solutionQueue = solutionQueue;
		this.numResults = numResults;
		
		this.results = new ArrayList<OWLClassExpression>();
		
		this.resultQueue = new LinkedBlockingQueue<NavigableSet<? extends EvaluatedDescription<? extends Score>>>();
	}

	
	private void saveQueryResult (GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> query, Value result) throws QueryException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
				
		// the output aml file
		String resultfile = home + "output.aml";
		
		CAEXFileType caex = CAEX215Factory.eINSTANCE.createCAEXFileType();
		AMLExporter exporter = new AMLExporter();
		
		BaseX2AMLConverter serializer = new BaseX2AMLConverter();				
		InstanceHierarchyType ih = CAEX215Factory.eINSTANCE.createInstanceHierarchyType();
		ih.setName("result_" + query.data.getObj().getName());
		
		if(serializer.toCaex(result, ih))
			caex.getInstanceHierarchy().add(ih);
		
		exporter.write(caex, resultfile);
	
	}
	
	private Value executeXQuery (String xquery) throws QueryException {
		Context context = new Context();
		
		QueryProcessor processor = new QueryProcessor(xquery, context);
		Value result = processor.value();						
		
		// Close the database context
		context.close();
		processor.close();
		
		return result;
	}
	
	public String getDLLearnerConfig (String file) {
		
		String extension = file.substring(file.lastIndexOf(".") + 1);
		
		if(extension.equals(CONFFILE)) {
			return file;
		}
		
		if(extension.equals(JSONFILE)) {
			String filepath = file.substring(0, file.lastIndexOf("/"));
			String filename = file.substring(file.lastIndexOf("/") + 1);
			String simplename = filename.substring(0, filename.lastIndexOf("."));
			
			String dllearnerConfigFile = filepath + "/" + simplename + ".conf";
			
			try {
				getDLLearnerConfig(file, dllearnerConfigFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return dllearnerConfigFile;
		}
		
		return null;			
	}
	
	private void getDLLearnerConfig (String amlConfFile, String dllearnerConfFile) throws IOException, org.json.simple.parser.ParseException {
		AMLLearnerConfig config = new AMLLearnerConfig(amlConfFile);
		caexType = config.getCAEXTypeIri();
		System.out.println(" - lifting AML data to OWL ontology");
        String s = config.toDLLearnerString();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(dllearnerConfFile));
        writer.write(s);
         
        writer.close();  
	}
	
	
	public String getXQueryFromACM (GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> concept, boolean semantically, boolean withLink, boolean simple) throws InstantiationException, IllegalAccessException {

		GenericTreeNode<GenericAMLConceptModel<AMLQueryConfig>> query = GenericAMLConceptModelUtils.fromACMToAMLQueryModel(concept);
		
		/**
		 * TODO: currently, statically setting the root node to be descendant
		 */
		((AMLQueryConfig) query.data.getConfig()).setDescendant(true);
		XQueryGenerator queryGen = new XQueryGenerator(query, home + "data_src.aml", xqueryRoot);
						
		String xquery = queryGen.translateToXQuery(semantically, withLink, simple);
		
		AuxiliaryXQueryNode.resetIdx();
		XQueryReturn.resetIdx();
		TransitiveClosure.resetIdx();
		
		return xquery;
	} 
	
	public void saveACM (String acmFile, Map<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> allAcms) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		
		CAEXFileType caex = CAEX215Factory.eINSTANCE.createCAEXFileType();
		InstanceHierarchyType ih = CAEX215Factory.eINSTANCE.createInstanceHierarchyType();
		ih.setName("ACM models");
		
		int ceIdx = 1;
		for (Map.Entry<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> entry : allAcms.entrySet()) {
			List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> acms = entry.getValue();
			
			int acmIdx = 1;			
			for(GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> acm : acms) {
				CAEXObject obj = GenericAMLConceptModelUtils.toConfiguredCAEXObject(acm);
				
				// the tree must be an IE
				// if the target object is an EI, then it is a subnode of some IE
				InternalElementType ie = (InternalElementType) obj;
				
				if(acms.size() == 1)
					ie.setName("q" + ceIdx);
				else
					ie.setName("q" + ceIdx + "-" + acmIdx);
				
				ih.getInternalElement().add(ie);
				acmIdx++;
			}			
			ceIdx++;
		}

		caex.getInstanceHierarchy().add(ih);
		AMLExporter exporter = new AMLExporter(caex);
		exporter.write(acmFile);
// 		System.out.println(" - saved ACM file");
		print(" - saved ACM file");
	}
	
	
	public void learn (String dllearnerConfig) {
			
		File file = new File(dllearnerConfig);
		if(!file.exists()) {
			System.out.println("File \"" + file + "\" does not exist.");
//			logger.info("File \"" + file + "\" does not exist.");
			System.exit(0);
		}				
//		conceptLearner = new ConceptLearner(queue, file);
		conceptLearner = new ConceptLearner(resultQueue, triggerQueue, file);
//		conceptLearner.run();
		Thread thread = new Thread(conceptLearner);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		conceptLearner.run();				
//		results = conceptLearner.getLearnedConcepts(numResults, true);
		results = getLearnedConcepts(numResults, true);
		
//		for(OWLClassExpression ce : results) {
//			try {
//				solutionQueue.put(ce.toString());
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
	
	/**
	 * get the learne results from the algorithm
	 * @param nrConcepts
	 * @return
	 */
	public List<OWLClassExpression> getLearnedConcepts (int nrConcepts, boolean onlyCorrect) {

		NavigableSet<? extends EvaluatedDescription<? extends Score>> results = resultQueue.poll();
		Iterator<? extends EvaluatedDescription<? extends Score>> iter = results.descendingIterator();
		
		List<OWLClassExpression> ces = new ArrayList<OWLClassExpression>();
		int idx = 1;

		while(iter.hasNext()) {
			if(nrConcepts > 0 && idx > nrConcepts) {
				break;
			}

			EvaluatedDescription<? extends Score> desc = iter.next();
			if(onlyCorrect && desc.getAccuracy() < 1.0) {
				break;
			}
			ces.add(desc.getDescription());
			idx++;
		}		
		return ces;
	}
	
	
	public List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> fromResultToACMs (OWLClassExpression result) {
		
		List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> acms = new ArrayList<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>();

		try {
			acms = TranslationUtils.toAMLConceptModel(result, caexType, false);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return acms;
	}
	
	
	/**
	 * for each of the learned OWL concepts, generate the ACMs 
	 * @return
	 */
	public Map<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> fromResultsToACMs (List<OWLClassExpression> results) {
		
		Map<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> allAcms = new HashMap<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>>();
		
		int idx = 1;
		for(OWLClassExpression concept : results) {
			
			System.out.println("\n\n------------- Transforming Solution [" + idx + "]: [" +  concept + "] to ACM --------------");
//			logger.info("\n\n------------- Transforming Solution [" + idx + "]: [" +  concept + "] to ACM --------------");
			
			List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> acms;
			try {
//				acms = fromOWLToACM(concept);
				acms = TranslationUtils.toAMLConceptModel(concept, caexType, false);
				allAcms.put(concept, acms);
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
			
			idx++;
		}
		
		return allAcms;
	}
	
	public void fromACMToQuery (GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> acm, AMLLearnerSolution solution) {				
		
		// ======================= AML Concept Model -> Core nAQL Model -> XQuery ======================= //
		String xqueryCommand = null;
		try {
			xqueryCommand = getXQueryFromACM(acm, true, false, true);
			print(xqueryCommand);
			solution.setXquery(xqueryCommand);
		} catch (InstantiationException | IllegalAccessException | TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ======================= XQuery execution ======================= //
		Value result;
		try {
			result = executeXQuery(xqueryCommand);
			print("\nRESULTS:" + result.size() + "\n");

			String resultStr = "";
			for(Item r : result) {
				resultStr += r.serialize().toString() + "\n";								
				print(r.serialize().toString());
			}
			solution.setQueryResult(resultStr);
			
			if(doSerialize)
				saveQueryResult(acm, result);
		} catch (QueryException | QueryIOException | ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
							
	}
	
	private void runBatch () {
		// TODO Auto-generated method stub
		System.out.println("\nAMLLearner Pipeline Demo\n");
		System.out.println(" 1. concept learning");
		System.out.println("   - load (AML) config file");
		System.out.println("   - run learner");
		System.out.println("   - output concepts");
		
		System.out.println(" 2. OWL -> ACM");
		System.out.println("   - build AND-tree from OWL class");
		System.out.println("   - remove inverse roles in the AND-tree");
		System.out.println("   - build ACM");
		System.out.println("   - clean ACM (fuse nested attributes)");
		
		System.out.println(" 3. ACM -> XQuery");
		System.out.println("   - ACM to core nAQL models");
		System.out.println("   - core nAQL models to XQuery");
		System.out.println("   - query execution");
		System.out.println("   - save query results\n");
		
		// TODO Auto-generated method stub

		// ======================= STEP 0: Learning ======================= //
		String dllearnerConfig = getDLLearnerConfig(configFile);		
		learn(dllearnerConfig);
		
		// ======================= STEP 1: OWL Class -> AML Concept Model ======================= //
		
		Map<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> allAcms = fromResultsToACMs(this.results);
	//	System.out.println("\n -------------------------------------------------------- \n");
	//	System.out.println("\n - saving the ACMs to an AML file");
		print("\n -------------------------------------------------------- \n");
		print("\n - saving the ACMs to an AML file");
		
		String acmPath = configFile.substring(0, configFile.lastIndexOf("/")) + "/" + "learned_acm.aml";
		try {
			saveACM(acmPath, allAcms);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ======================= STEP 2: AML Concept Model -> Core nAQL Model -> XQuery ======================= //
		for (Map.Entry<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> entry : allAcms.entrySet()) {
			OWLClassExpression ce = entry.getKey();
			List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> acms = entry.getValue();
			
			for(GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> acm : acms) {
	//			System.out.println("\n -------------------------------------------------------- \n");
	//			System.out.println("\n- Generating the XQuery for ACM:\n\n" + acm.toStringWithIndent(2) + "\n");
				
				print("\n -------------------------------------------------------- \n");
				print("\n- Generating the XQuery for ACM:\n\n" + acm.toStringWithIndent(2) + "\n");
				
				// We assume that each ACM from the AML input represents one query: no union of ACMs 
//				fromACMToQuery(acm);	
			}			
		}
	}

	//@Override
	public void run() {
			
		// TODO Auto-generated method stub
		System.out.println("\nAMLLearner Pipeline Demo\n");
		System.out.println(" 1. concept learning");
		System.out.println("   - load (AML) config file");
		System.out.println("   - run learner");
		System.out.println("   - output concepts");
		
		System.out.println(" 2. OWL -> ACM");
		System.out.println("   - build AND-tree from OWL class");
		System.out.println("   - remove inverse roles in the AND-tree");
		System.out.println("   - build ACM");
		System.out.println("   - clean ACM (fuse nested attributes)");
		
		System.out.println(" 3. ACM -> XQuery");
		System.out.println("   - ACM to core nAQL models");
		System.out.println("   - core nAQL models to XQuery");
		System.out.println("   - query execution");
		System.out.println("   - save query results\n");
		
		System.out.println("======================================================");
		System.out.println("======================================================\n\n\n");
		
		// TODO Auto-generated method stub

		// ======================= STEP 0: Learning ======================= //
		System.out.println("\n-------------------");
		System.out.println("1. concept learning");
		System.out.println("-------------------\n");
		String dllearnerConfig = getDLLearnerConfig(configFile);		
		learn(dllearnerConfig);
		
		// ======================= STEP 1: OWL Class -> AML Concept Model ======================= //
		AMLLearnerSolution solution = new AMLLearnerSolution();
		Map<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>> allAcms = new HashMap<OWLClassExpression, List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>>>();
		int idx = 1;
		for(OWLClassExpression result : this.results) {
			solution.setConcept(result);
			
			System.out.println("\n-------------");
			System.out.println("2. OWL -> ACM");
			System.out.println("-------------\n");
			System.out.println(" - Transforming Solution [" + idx + "]: [" +  result + "] to ACM --------------");
			List<GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>>> acms = fromResultToACMs(result);
			allAcms.put(result, acms);
			
			// ======================= STEP 2: AML Concept Model -> Core nAQL Model -> XQuery ======================= //
			System.out.println("\n----------------");
			System.out.println("3. ACM -> XQuery");
			System.out.println("----------------\n");
			for(GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> acm : acms) {
				System.out.println(" - Generating the XQuery for ACM:\n\n" + acm.toStringWithIndent(2) + "\n");				
				fromACMToQuery(acm, solution);
				// We assume that each ACM from the AML input represents one query: no union of ACMs
			}
			
			try {
				solutionQueue.put(solution);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			idx++;
		}
		
		print("\n -------------------------------------------------------- \n");
		print("\n - saving the ACMs to an AML file");
		String acmPath = configFile.substring(0, configFile.lastIndexOf("/")) + "/" + "learned_acm.aml";
		try {
			saveACM(acmPath, allAcms);
		} catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}  
	
	private void print(String s) {
		synchronized (System.out) {
			System.out.println(s);	
		}
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.out.println("hello");
		BlockingQueue<Boolean> stopQueue = new LinkedBlockingQueue<Boolean>();
		BlockingQueue<AMLLearnerSolution> solutionQueue = new LinkedBlockingQueue<AMLLearnerSolution>();		
		AMLLearner learner = new AMLLearner("D:/repositories/aml/aml_framework/src/main/resources/test/amlACM.json", stopQueue, solutionQueue, 5);		
		learner.run();
//		 Thread thread = new Thread(learner);
//         thread.start();
	}
}