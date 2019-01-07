/**
 * 
 */
package aml;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.dllearner.cli.CLI;
import org.dllearner.cli.CLIBase2;
import org.dllearner.cli.CrossValidation2;
import org.dllearner.configuration.IConfiguration;
import org.dllearner.configuration.spring.ApplicationContextBuilder;
import org.dllearner.configuration.spring.DefaultApplicationContextBuilder;
import org.dllearner.confparser.ConfParserConfiguration;
import org.dllearner.confparser.ParseException;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.refinementoperators.RefinementOperator;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class Benchmarker extends CLIBase2 {

	private static Logger logger = LoggerFactory.getLogger(aml.Benchmarker.class);

	private LearningAlgorithm algorithm;
	private KnowledgeSource knowledgeSource;
	
	// some CLI options
	@ConfigOption(defaultValue = "false", description = "Run in Cross-Validation mode")
	private boolean performCrossValidation = false;
	@ConfigOption(defaultValue = "10", description = "Number of folds in Cross-Validation mode")
	private int nrOfFolds = 10;

	private AbstractClassExpressionLearningProblem lp;

	private AbstractReasonerComponent rs;

	private AbstractCELA la;
	

	public Benchmarker() {
		
	}
	
	public Benchmarker(File confFile) {
		this();
		this.confFile = confFile;
	}
	
	// separate init methods, because some scripts may want to just get the application
	// context from a conf file without actually running it
	@Override
	public void init() throws IOException {
    	if(context == null) {
		    super.init();
            
            knowledgeSource = context.getBean(KnowledgeSource.class);
            rs = getMainReasonerComponent();
    		la = context.getBean(AbstractCELA.class);
    		lp = context.getBean(AbstractClassExpressionLearningProblem.class);
    	}
	}
	
    @Override
    public void run() {
    	try {
			org.apache.log4j.Logger.getLogger("org.dllearner").setLevel(Level.toLevel(logLevel.toUpperCase()));
		} catch (Exception e) {
			logger.warn("Error setting log level to " + logLevel);
		}

		rs = getMainReasonerComponent();
		
		
			if (performCrossValidation) {
				la = context.getBeansOfType(AbstractCELA.class).entrySet().iterator().next().getValue();
				
				PosNegLP lp = context.getBean(PosNegLP.class);
				new CrossValidation2(la,lp,rs,nrOfFolds,false);
			} else {
				if(context.getBean(AbstractLearningProblem.class) instanceof AbstractClassExpressionLearningProblem) {
					lp = context.getBean(AbstractClassExpressionLearningProblem.class);
				} 
				Map<String, LearningAlgorithm> learningAlgs = context.getBeansOfType(LearningAlgorithm.class);
//				knowledgeSource = context.getBeansOfType(Knowledge1Source.class).entrySet().iterator().next().getValue();
				for(Entry<String, LearningAlgorithm> entry : learningAlgs.entrySet()){
					algorithm = entry.getValue();
//					logger.info("Running algorithm instance \"" + entry.getKey() + "\" (" + algorithm.getClass().getSimpleName() + ")");
					algorithm.start();
				}
			}
    }

	/**
	 * @return the lp
	 */
	public AbstractClassExpressionLearningProblem getLearningProblem() {
		return lp;
	}
	
	/**
	 * @return the rs
	 */
	public AbstractReasonerComponent getReasonerComponent() {
		return rs;
	}
	
	/**
	 * @return the la
	 */
	public AbstractCELA getLearningAlgorithm() {
		return la;
	}
	
	private void test(File conf, int runs) throws FileNotFoundException {
		
		Resource confFile = new FileSystemResource(conf);		
		List<Resource> springConfigResources = new ArrayList<>();
		int i = 1;
		while(i<=runs) {
			System.out.println("\n - run " + i + ": ");
			 try {
		            //DL-Learner Configuration Object
		            IConfiguration configuration = new ConfParserConfiguration(confFile);

		            ApplicationContextBuilder builder = new DefaultApplicationContextBuilder();
		            ApplicationContext context =  builder.buildApplicationContext(configuration,springConfigResources);

		            // TODO: later we could check which command line interface is specified in the conf file
		            // for now we just use the default one

		            CLIBase2 cli;
		            
		            if(context.containsBean("cli")) {
		                cli = (CLIBase2) context.getBean("cli");
		            } else {
		                cli = new CLI();
		            }
		            cli.setContext(context);
		            cli.setConfFile(conf);
		            cli.run();
		            
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

		            FileOutputStream fos = new FileOutputStream(stacktraceFileName);
		            PrintStream ps = new PrintStream(fos);
		            e.printStackTrace(ps);
		        }
			 i++;
		}
	}
    
	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 * @throws ReasoningMethodUnsupportedException
	 */
	public static void main(String[] args) throws ParseException, IOException, ReasoningMethodUnsupportedException {
	
		System.out.println("Benchmarking AML concept learning");
				
		String projectPath = "";
		int runs = 1;
		
		if(args.length == 0) {
			System.out.println("No argument found for the project path, quit...");
			System.exit(0);
		}else {
			// read file and print and print a message if it does not exist
			projectPath = args[0];
			if(!projectPath.endsWith("/"))
				projectPath += "/";
			
			if(args.length == 2) {
				runs = Integer.parseInt(args[1]);
			}
		}
		
		projectPath += "benchmark/";
		
		Benchmarker benchmarker = new Benchmarker();
		// go through all benchmarks in the project path
		File[] tests = new File(projectPath).listFiles();
		for(File test : tests) {
			if (test.isFile() && test.getName().contains(".conf")) {
		    		System.out.println("\n\nDoing use case: " + test.getName());
		    		benchmarker.test(test, runs);
//		    		if(test.getName().contains("aml"))
//		    			Files.move(Paths.get(test.getAbsolutePath()), Paths.get(projectPath+"done/aml/"+test.getName()), StandardCopyOption.REPLACE_EXISTING);
//		    		else
//		    			Files.move(Paths.get(test.getAbsolutePath()), Paths.get(projectPath+"done/rho/"+test.getName()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		System.out.println("learning finished");				
    }

	public boolean isPerformCrossValidation() {
		return performCrossValidation;
	}

	public void setPerformCrossValidation(boolean performCrossValiation) {
		this.performCrossValidation = performCrossValiation;
	}

	public int getNrOfFolds() {
		return nrOfFolds;
	}

	public void setNrOfFolds(int nrOfFolds) {
		this.nrOfFolds = nrOfFolds;
	}

	//	public LearningAlgorithm getLearningAlgorithm() {
//		return algorithm;
//	}
	
	public KnowledgeSource getKnowledgeSource() {
		return knowledgeSource;
	}
	
}