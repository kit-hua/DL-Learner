package org.dllearner.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.dllearner.configuration.IConfiguration;
import org.dllearner.configuration.spring.ApplicationContextBuilder;
import org.dllearner.configuration.spring.DefaultApplicationContextBuilder;
import org.dllearner.confparser.ConfParserConfiguration;
import org.dllearner.confparser.ParseException;
import org.dllearner.core.ReasoningMethodUnsupportedException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class CLIExt extends CLI {
	// convenience way to run components-ext from maven
	// TODO: replace with better solution
	
	public static void main(String[] args) throws ParseException, IOException, ReasoningMethodUnsupportedException {
		    		
		
//		System.out.println("DL-Learner " + Info.build + " [TODO: read pom.version and put it here (make sure that the code for getting the version also works in the release build!)] command line interface");
		System.out.println("DL-Learner command line interface");
		
		// currently, CLI has exactly one parameter - the conf file
		if(args.length == 0) {
			System.out.println("You need to give a conf file as argument.");
			System.exit(0);
		}
		
		// read file and print and print a message if it does not exist
		File file = new File(args[args.length - 1]);
		if(!file.exists()) {
			System.out.println("File \"" + file + "\" does not exist.");
			System.exit(0);
		}
		
		Resource confFile = new FileSystemResource(file);
		
		List<Resource> springConfigResources = new ArrayList<>();

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
                cli = new CLIExt();
            }
            cli.setContext(context);
            cli.setConfFile(file);
            cli.run();
        } catch (Exception e) {
            String stacktraceFileName = "log/error.log";
            
            //Find the primary cause of the exception.
            Throwable primaryCause = findPrimaryCause(e);

            // Get the Root Error Message
//            logger.error("An Error Has Occurred During Processing.");
            if (primaryCause != null) {
//            	logger.error(primaryCause.getMessage());
            }
//            logger.debug("Stack Trace: ", e);
//            logger.error("Terminating DL-Learner...and writing stacktrace to: " + stacktraceFileName);
            createIfNotExists(new File(stacktraceFileName));

            FileOutputStream fos = new FileOutputStream(stacktraceFileName);
            PrintStream ps = new PrintStream(fos);
            e.printStackTrace(ps);
        }
    }
}
