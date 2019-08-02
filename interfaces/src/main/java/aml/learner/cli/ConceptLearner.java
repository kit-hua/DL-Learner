package aml.learner.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.dllearner.configuration.IConfiguration;
import org.dllearner.configuration.spring.ApplicationContextBuilder;
import org.dllearner.configuration.spring.DefaultApplicationContextBuilder;
import org.dllearner.confparser.ConfParserConfiguration;
import org.dllearner.confparser.ParseException;
import org.dllearner.core.AbstractCELA;
import org.dllearner.core.AbstractClassExpressionLearningProblem;
import org.dllearner.core.AbstractLearningProblem;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ClassExpressionLearningAlgorithm;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.ReasoningMethodUnsupportedException;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import aml.learner.gui.SimpleGui;
import aml.learner.gui.TextAreaOutputStream;

/**
 * The wrapper to dl-learner learning process
 * @author aris
 *
 */
@ComponentAnn(name = "AML Query Learner", version = 0, shortName = "aql")
public class ConceptLearner implements Runnable{

	static {
		if (System.getProperty("log4j.configuration") == null)
			System.setProperty("log4j.configuration", "log4j.properties");
	}

	private static Logger logger = LoggerFactory.getLogger(ConceptLearner.class);	
	@ConfigOption(defaultValue = "INFO", description = "Configure logger log level from conf file. Available levels: \"FATAL\", \"ERROR\", \"WARN\", \"INFO\", \"DEBUG\", \"TRACE\". "
			+ "Note, to see results, at least \"INFO\" is required.")
	protected String logLevel = "INFO";
	
	TextAreaOutputStream stream = TextAreaOutputStream.getInstance(SimpleGui.logArea);


	ApplicationContext context;

	private BlockingQueue<Boolean> triggerQueue = new LinkedBlockingQueue<Boolean>();
	private BlockingQueue<NavigableSet<? extends EvaluatedDescription<? extends Score>>> resultQueue;
	private File dllearnerConfigFile;

	private LearningAlgorithm algorithm;

//	private NavigableSet<? extends EvaluatedDescription<? extends Score>> results;
	
	private volatile boolean learningTerminated = false;
	
	Thread listener;	
	Thread learner;

	public ConceptLearner(BlockingQueue<Boolean> triggerQueue, File dllearnerConfigFile) {
		this.triggerQueue = triggerQueue;
		this.dllearnerConfigFile = dllearnerConfigFile;
	}
	
	public ConceptLearner(BlockingQueue<NavigableSet<? extends EvaluatedDescription<? extends Score>>> resultQueue, BlockingQueue<Boolean> triggerQueue, File dllearnerConfigFile) {
		this.resultQueue = resultQueue;
		this.triggerQueue = triggerQueue;
		this.dllearnerConfigFile = dllearnerConfigFile;
	}


//	/**
//	 * get the learne results from the algorithm
//	 * @param nrConcepts
//	 * @return
//	 */
//	public List<OWLClassExpression> getLearnedConcepts (int nrConcepts, boolean onlyCorrect) {
//
//		Iterator<? extends EvaluatedDescription<? extends Score>> iter = results.descendingIterator();		
//		List<OWLClassExpression> ces = new ArrayList<OWLClassExpression>();
//		int idx = 1;
//
//		while(iter.hasNext()) {
//			if(nrConcepts > 0 && idx > nrConcepts) {
//				break;
//			}
//
//			EvaluatedDescription<? extends Score> desc = iter.next();
//			if(onlyCorrect && desc.getAccuracy() < 1.0) {
//				break;
//			}
//			ces.add(desc.getDescription());
//			idx++;
//		}		
//		return ces;
//	}
	
	public void run () {
		try {
			listen();
			learn();			
			learner.join();
			learningTerminated = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}


	public void listen () {
		listener = new Thread(new Runnable() {           
			public void run() {
				while(!learningTerminated) {
					Boolean stop = triggerQueue.poll();
					if(stop != null && stop) {
						((AbstractCELA) algorithm).stop();
						learningTerminated = true;
						Thread.currentThread().interrupt();
//						System.out.println("learning terminated, stop listening");
					}					
				}				
			} 
		});
		listener.start();
	}

	public void learn () throws FileNotFoundException {

		learner = new Thread(new Runnable() {
			
			public void run() { 
				System.out.println(" - " + dllearnerConfigFile.getName() + " STARTS\n");
				Resource dllearnerResource = new FileSystemResource(dllearnerConfigFile);
				AbstractReasonerComponent.allLength = 0;
				AbstractReasonerComponent.retCountAcc = 0;
				AbstractReasonerComponent.retCountMat = 0;
				AbstractReasonerComponent.retCountRef = 0;
		
				List<Resource> springConfigResources = new ArrayList<>();
		
				try {
					//DL-Learner Configuration Object
					IConfiguration configuration = new ConfParserConfiguration(dllearnerResource);
					ApplicationContextBuilder builder = new DefaultApplicationContextBuilder();
					context =  builder.buildApplicationContext(configuration,springConfigResources);
		
					// TODO: later we could check which command line interface is specified in the conf file
					// for now we just use the default one
					//			if(context.getBean(AbstractLearningProblem.class) instanceof AbstractClassExpressionLearningProblem) {
					//				lp = context.getBean(AbstractClassExpressionLearningProblem.class);
					//			} else {
					//
					//			}
		
					Map<String, LearningAlgorithm> learningAlgs = context.getBeansOfType(LearningAlgorithm.class);
					//				knowledgeSource = context.getBeansOfType(Knowledge1Source.class).entrySet().iterator().next().getValue();
					for(Entry<String, LearningAlgorithm> entry : learningAlgs.entrySet()){
						algorithm = entry.getValue();
						logger.info("Running algorithm instance \"" + entry.getKey() + "\" (" + algorithm.getClass().getSimpleName() + ")");
						algorithm.start();
//						results = ((AbstractCELA) algorithm).getCurrentlyBestEvaluatedDescriptions();
						resultQueue.put(((AbstractCELA) algorithm).getCurrentlyBestEvaluatedDescriptions());
					}
		
//					System.out.println("============================ " + dllearnerConfigFile.getName() + " ENDS ============================\n");

		
				} catch (Exception e) {
					String stacktraceFileName = "log/error.log";
		
					//Find the primary cause of the exception.
					Throwable primaryCause = findPrimaryCause(e);
		
					// Get the Root Error Message
					logger.error("An Error Has Occurred During Processing.");
					if (primaryCause != null) {
						logger.error(primaryCause.getMessage());
					}
					logger.debug("Stack Trace: ", e);
					logger.error("Terminating DL-Learner...and writing stacktrace to: " + stacktraceFileName);
					createIfNotExists(new File(stacktraceFileName));
		
					FileOutputStream fos;
					try {
						fos = new FileOutputStream(stacktraceFileName);
						PrintStream ps = new PrintStream(fos);
						e.printStackTrace(ps);
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}					
				}
            }
		});
		learner.start();
	}

	private Throwable findPrimaryCause(Exception e) {
		// The throwables from the stack of the exception
		Throwable[] throwables = ExceptionUtils.getThrowables(e);

		//Look For a Component Init Exception and use that as the primary cause of failure, if we find it
		int componentInitExceptionIndex = ExceptionUtils.indexOfThrowable(e, ComponentInitException.class);

		Throwable primaryCause;
		if(componentInitExceptionIndex > -1) {
			primaryCause = throwables[componentInitExceptionIndex];
		}else {
			//No Component Init Exception on the Stack Trace, so we'll use the root as the primary cause.
			primaryCause = ExceptionUtils.getRootCause(e);
		}
		return primaryCause;
	}

	private boolean createIfNotExists(File f) {
		if (f.exists()) return true;

		File p = f.getParentFile();
		if (p != null && !p.exists()) p.mkdirs();

		try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public AbstractReasonerComponent getMainReasonerComponent() {
		AbstractReasonerComponent rc = null;
		// there can be 2 reasoner beans
		Map<String, AbstractReasonerComponent> reasonerBeans = context.getBeansOfType(AbstractReasonerComponent.class);

		if (reasonerBeans.size() > 1) {
			for (Map.Entry<String, AbstractReasonerComponent> entry : reasonerBeans.entrySet()) {
				String key = entry.getKey();
				AbstractReasonerComponent value = entry.getValue();

				if (value instanceof ClosedWorldReasoner) {
					rc = value;
				}

			}
		} else {
			rc = context.getBean(AbstractReasonerComponent.class);
		}

		return rc;
	}

	/**
	 * @return the learningTerminated
	 */
	public boolean isLearningTerminated() {
		return learningTerminated;
	}

	/**
	 * @param learningTerminated the learningTerminated to set
	 */
	public void setLearningTerminated(boolean learningTerminated) {
		this.learningTerminated = learningTerminated;
	}


}
