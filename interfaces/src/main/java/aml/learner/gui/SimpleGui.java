package aml.learner.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.basex.query.QueryException;
import org.basex.query.QueryIOException;
import org.dllearner.confparser.ParseException;
import org.dllearner.core.ReasoningMethodUnsupportedException;
import org.semanticweb.owlapi.model.OWLClassExpression;

import aml.learner.cli.AMLLearner;
import aml.learner.config.AMLLearnerConfig;
import aml.learner.config.AMLLearnerMessage;
import aml.learner.config.AMLLearnerSolution;
import concept.model.AMLConceptConfig;
import concept.model.GenericAMLConceptModel;
import concept.tree.GenericTreeNode;

public class SimpleGui extends JFrame {
	/**
	 * The text area which is used for displaying logging information.
	 */
	public static JTextArea logArea = new JTextArea(50, 100);
	private TextAreaOutputStream stream = TextAreaOutputStream.getInstance(logArea);
	
    JFileChooser fc;
    static private final String HOME = "/Users/aris/Documents/repositories/ipr/aml/aml_framework/src/main/resources/test/";
    FileNameExtensionFilter JSON = new FileNameExtensionFilter("AML Config Files", "json");
	FileNameExtensionFilter CONF = new FileNameExtensionFilter("DL-Learner Config Files", "conf");
	FileNameExtensionFilter AML = new FileNameExtensionFilter("AML Files", "aml");

	private JTextArea resultArea = new JTextArea(50, 100); 
	private JButton buttonStart = new JButton("Start");
	private JButton buttonStop = new JButton("Stop");
	
	private BlockingQueue<AMLLearnerSolution> solutionQueue;
	
//	public SimpleGui(AMLLearnerMessage msg) {
//		this(msg.getStopQueue(), msg.getSolutionQueue());
//	}

	public SimpleGui(BlockingQueue<Boolean> stopQueue, BlockingQueue<AMLLearnerSolution> solutionQueue) {
		super("AMLLearner Demo");
		
		Font font = new Font("Segoe Script", Font.ROMAN_BASELINE, 12);
		this.solutionQueue = solutionQueue;
		
		logArea.setEditable(false);		
        logArea.setFont(font);
		DefaultCaret logCaret = (DefaultCaret)logArea.getCaret();
		logCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		resultArea.setEditable(false);
		resultArea.setFont(font);
		DefaultCaret resultCaret = (DefaultCaret)resultArea.getCaret();
		resultCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		fc = new JFileChooser(HOME);	   

		// creates the GUI
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 10, 10);
		constraints.anchor = GridBagConstraints.WEST;

		add(buttonStart, constraints);

		constraints.gridx = 1;
		add(buttonStop, constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;

		add(new JScrollPane(logArea), constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		resultArea.setPreferredSize(new Dimension(40, 0));

		add(new JScrollPane(resultArea), constraints);
		

		// adds event handler for button Start
		buttonStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {	
				
				fc.setFileFilter(JSON);
	            int returnVal = fc.showOpenDialog(SimpleGui.this);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	            		
	                String file = fc.getSelectedFile().getAbsolutePath();
	                try {
						logArea.getDocument().remove(0, logArea.getDocument().getLength());
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	                
//	                AMLLearner learner = new AMLLearner(file, stopQueue, solutionQueue, 5);	                
//	                Thread thread = new Thread(learner);
//	                thread.start();

	            } else {
	                System.out.println("Open command cancelled by user.");
	            }
			}
		});

		// adds event handler for button Clear
		buttonStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {

				try {
					stopQueue.put(true);						
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);    // centers on screen
		updateResult();
	}
	
	private void updateResult () {
		Thread updater = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				int i = 0;
				while(true) {
					AMLLearnerSolution solution = solutionQueue.poll();
//					System.out.println("new result");
					String s = "\n\n";					
					if(solution != null) {
						s += "------------\n";
						s += "solution [" + (i++) + "]: \n";
						s += "------------\n";
						s += solution.toString();						
						resultArea.append(s);
					}										
				}
			}
		});
		
		updater.start();
	}

	
	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 * @throws ReasoningMethodUnsupportedException
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws QueryException 
	 * @throws TransformerException 
	 * @throws TransformerFactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 * @throws org.json.simple.parser.ParseException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws ParseException, IOException, ReasoningMethodUnsupportedException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, QueryException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, org.json.simple.parser.ParseException, InterruptedException {
	
		BlockingQueue<Boolean> queue = new LinkedBlockingQueue<Boolean>();
		BlockingQueue<AMLLearnerSolution> solutionQueue = new LinkedBlockingQueue<AMLLearnerSolution>();
		TextAreaOutputStream stream = TextAreaOutputStream.getInstance(SimpleGui.logArea);
//		SimpleGui ui = new SimpleGui(queue);
//		ui.setVisible(true);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SimpleGui ui = new SimpleGui(queue, solutionQueue);
				ui.setVisible(true);
			}
		});
	}

}