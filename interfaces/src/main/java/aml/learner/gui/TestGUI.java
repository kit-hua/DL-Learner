package aml.learner.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.GridBagConstraints;
import javax.swing.JSplitPane;
import java.awt.Button;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;

import aml.learner.cli.AMLLearner;
import aml.learner.config.AMLLearnerSolution;

import java.awt.Window.Type;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestGUI {

	private JFrame frmAmllearnerGui;
	JSplitPane splitPane = new JSplitPane();
	
	JPanel panelSolution = new JPanel();
	JScrollPane scrollPaneSolution = new JScrollPane();
	JTextArea textPaneSolution = new JTextArea();
	JButton btnStop = new JButton("Stop");
	
	JPanel panelLog = new JPanel();
	JScrollPane scrollPaneLog = new JScrollPane();
	JTextArea textPaneLog = new JTextArea();
	JButton btnStart = new JButton("Start");
	
	boolean textPaneLogActive = false;
	boolean textPaneSolutionActive = false;
	
	JFileChooser fc;
    static private final String HOME = "/Users/aris/Documents/repositories/ipr/aml/aml_framework/src/main/resources/test/";
    FileNameExtensionFilter JSON = new FileNameExtensionFilter("AML Config Files", "json");
	FileNameExtensionFilter CONF = new FileNameExtensionFilter("DL-Learner Config Files", "conf");
	FileNameExtensionFilter AML = new FileNameExtensionFilter("AML Files", "aml");
	

	private TextAreaOutputStream stream = TextAreaOutputStream.getInstance(textPaneLog);
	
	BlockingQueue<Boolean> stopQueue = new LinkedBlockingQueue<Boolean>();
	BlockingQueue<AMLLearnerSolution> solutionQueue = new LinkedBlockingQueue<AMLLearnerSolution>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		BlockingQueue<Boolean> stopQueue = new LinkedBlockingQueue<Boolean>();
		BlockingQueue<AMLLearnerSolution> solutionQueue = new LinkedBlockingQueue<AMLLearnerSolution>();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestGUI window = new TestGUI();
					window.frmAmllearnerGui.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
//		Thread t = new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				int i = 0;
//				while(true) {
//					System.out.println(i++);
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//		});
//		t.start();
	}

	/**
	 * Create the application.
	 */
	public TestGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmAmllearnerGui = new JFrame();
		frmAmllearnerGui.setType(Type.POPUP);
		frmAmllearnerGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmAmllearnerGui.setSize(1200, 800);
		frmAmllearnerGui.setLocationRelativeTo(null);    // centers on screen
		frmAmllearnerGui.setTitle("AMLLearner GUI");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		frmAmllearnerGui.getContentPane().setLayout(gridBagLayout);
				
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 0;
		frmAmllearnerGui.getContentPane().add(splitPane, gbc_splitPane);
		
		fc = new JFileChooser(HOME);
		
		//////////////////////////////////////////////////////////////
		// Log Panel
		//////////////////////////////////////////////////////////////
		
		splitPane.setLeftComponent(panelLog);
		GridBagLayout gbl_panelLog = new GridBagLayout();
		gbl_panelLog.columnWidths = new int[]{200, 0};
		gbl_panelLog.rowHeights = new int[]{0, 0, 0};
		gbl_panelLog.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panelLog.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panelLog.setLayout(gbl_panelLog);			
		
		// log scroll pane
		GridBagConstraints gbc_scrollPaneLog = new GridBagConstraints();
		gbc_scrollPaneLog.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneLog.gridx = 0;
		gbc_scrollPaneLog.gridy = 1;
		panelLog.add(scrollPaneLog, gbc_scrollPaneLog);
		
		// log text pane
		scrollPaneLog.setViewportView(textPaneLog);
		DefaultCaret logCaret = (DefaultCaret)textPaneLog.getCaret();
		logCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		// log button
		btnStart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
//				stream.setActive(true);
				
				fc.setFileFilter(JSON);
	            int returnVal = fc.showOpenDialog(frmAmllearnerGui);

	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	            		
	                String file = fc.getSelectedFile().getAbsolutePath();
	                textPaneLog.setText("");
	                
//	                AMLLearner learner = new AMLLearner(file, stopQueue, solutionQueue, 5);	                
//	                Thread thread = new Thread(learner);
//	                thread.start();

	            } else {
	                System.out.println("Open command cancelled by user.");
	            }
			}
		});
		GridBagConstraints gbc_btnStart = new GridBagConstraints();
		gbc_btnStart.insets = new Insets(0, 0, 5, 0);
		gbc_btnStart.gridx = 0;
		gbc_btnStart.gridy = 0;
		panelLog.add(btnStart, gbc_btnStart);
		
		
		//////////////////////////////////////////////////////////////
		// Solution Panel
		//////////////////////////////////////////////////////////////
		
		// solution panel
		splitPane.setRightComponent(panelSolution);
		GridBagLayout gbl_panelSolution = new GridBagLayout();
		gbl_panelSolution.columnWidths = new int[]{200, 0};
		gbl_panelSolution.rowHeights = new int[]{0, 0, 0};
		gbl_panelSolution.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panelSolution.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panelSolution.setLayout(gbl_panelSolution);
				
		// solution scroll panel
		GridBagConstraints gbc_scrollPaneSolution = new GridBagConstraints();
		gbc_scrollPaneSolution.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSolution.gridx = 0;
		gbc_scrollPaneSolution.gridy = 1;
		panelSolution.add(scrollPaneSolution, gbc_scrollPaneSolution);

		// solution text pane
		scrollPaneSolution.setViewportView(textPaneSolution);
		DefaultCaret solutionCaret = (DefaultCaret)textPaneSolution.getCaret();
		solutionCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		// solution button
		btnStop.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
//				stream.setActive(false);
			}
		});		
		GridBagConstraints gbc_btnStop = new GridBagConstraints();
		gbc_btnStop.insets = new Insets(0, 0, 5, 0);
		gbc_btnStop.gridx = 0;
		gbc_btnStop.gridy = 0;
		panelSolution.add(btnStop, gbc_btnStop);
		
	}
	
	private void append (JTextArea pane, String s) {

		StyledDocument document = (StyledDocument) pane.getDocument();				
		try {
			document.insertString(document.getLength(), s, null);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
						append(textPaneSolution, s);
					}										
				}
			}
		});
		
		updater.start();
	}
}
