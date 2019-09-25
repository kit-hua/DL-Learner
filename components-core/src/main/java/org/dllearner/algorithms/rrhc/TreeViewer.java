package org.dllearner.algorithms.rrhc;

import org.aksw.jena_sparql_api.utils.CnfUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;
import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class TreeViewer extends JPanel {

	class TreeNode {
		String concept = "";
		float accuracy;
		int horizontalExpansion = 0;
		int level = 0;
		List<TreeNode> children = new LinkedList<>();

		int x = 0;
		int y = 0;
		int maxChildX = 0;

        double celoe = 0;
		boolean isSelected = false;

		Set<TreeNode> getAllNodes (){        	
			Set<TreeNode> nodes = new HashSet<TreeViewer.TreeNode>();
			nodes.add(this);
			for(TreeNode child : children) {
				nodes.addAll(child.getAllNodes());
			}

			return nodes;
		} 

		Set<String> getAllConcepts (){
			Set<TreeNode> nodes = getAllNodes();
			Set<String> concepts = new HashSet<String>();
			for(TreeNode node : nodes) {
				concepts.add(node.concept);
			}

			return concepts;
		}
	}

	class Tree {
		TreeNode root;
		ArrayList<List<TreeNode>> layers = new ArrayList<>();
		//        double maxCeloeScore = Integer.MIN_VALUE;
		double maxScore = Integer.MIN_VALUE;
		static final int maxSize = 1000;
		Map<String, TreeNode> conceptNodes;        
	}

	private List<Tree> allTrees = new LinkedList<>();
	private int currentTreeIndex = 0;

	private static JLabel currentConceptLabel;
	private static JLabel currentTreeLabel;
	private static JLabel currentStepLabel;

	//    private boolean showCeloe = false;
//	private boolean showUct = true;

	private static final String BenchmarkDir = "/Users/aris/Documents/repositories/ipr/aml/aml_learning/benchmarks/0703/configs/family-benchmark/test/";

//	private boolean showHeuristicValues = false;	
//	private boolean singleStepMode = false;
	private JSONArray steps;
	enum Step {
		SELECTION,
		EXPANSION
	}
	private Step currentSubStep = Step.SELECTION;	
	protected static DecimalFormat dfPercent = new DecimalFormat("0.00%");

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setSize(1024,768);
		frame.setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("DL-Learner Tree Viewer");

		// Canvas for drawing the tree on to
		TreeViewer canvas = new TreeViewer();
		canvas.setSize(2000, 2000);
		canvas.setPreferredSize(new Dimension(2000, 2000));
		canvas.setLayout(null);

		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(null);
		sidePanel.setSize(300, 600);
		sidePanel.setLocation(810, 5);
		frame.add(sidePanel);

		// Open button
		JButton opentreeButton = new JButton("Tree");
		opentreeButton.setBounds(0, 5, 90, 40);
		opentreeButton.addActionListener(e -> {
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File(BenchmarkDir));
			fc.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {					
					return "Tree logs (*.tree)";
				}

				@Override
				public boolean accept(File f) {
					if(f.isDirectory())
						return true;
					else						
						return f.getName().endsWith(".tree");
				}
			});
			fc.showOpenDialog(frame);
			if (canvas.openTree(fc.getSelectedFile())) {
				frame.setTitle("DL-Learner Tree Viewer: " + fc.getSelectedFile().getPath());
				canvas.showTree(0);
			} else {
				JOptionPane.showMessageDialog(new JFrame(), "Couldn't open tree file", "Dialog",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		sidePanel.add(opentreeButton);

		// Current tree number
		currentTreeLabel = new JLabel("Step 0 / 0");
		currentTreeLabel.setBounds(0, 70, 180, 20);
		sidePanel.add(currentTreeLabel);

		JButton previousTreeButton = new JButton("Prev");
		previousTreeButton.setBounds(0, 100, 90, 30);
		previousTreeButton.addActionListener(e -> {
			previousTree(canvas);
		});
		sidePanel.add(previousTreeButton);

		JButton nextTreeButton = new JButton("Next");
		nextTreeButton.setBounds(90, 100, 90, 30);
		nextTreeButton.addActionListener(e -> {
			nextTree(canvas);
		});
		sidePanel.add(nextTreeButton);


//		JButton previousStepButton = new JButton("<");
//		previousStepButton.setBounds(0, 180, 90, 30);
//		previousStepButton.addActionListener(e -> {
//			prevStep(canvas);
//		});
//		sidePanel.add(previousStepButton);
//
//		JButton nextStepButton = new JButton(">");
//		nextStepButton.setBounds(90, 180, 90, 30);
//		nextStepButton.addActionListener(e -> {
//			nextStep(canvas);
//		});
//		sidePanel.add(nextStepButton);

//		currentStepLabel = new JLabel("Selection");
//		currentStepLabel.setBounds(0, 215, 180, 20);
//		sidePanel.add(currentStepLabel);

		JTextField gotoLevelText = new JTextField();
		gotoLevelText.setBounds(0, 140, 100, 20);
		sidePanel.add(gotoLevelText);

		JButton gotoButton = new JButton("Go");
		gotoButton.setBounds(105, 140, 60, 20);
		gotoButton.addActionListener(e -> canvas.showTree(Integer.parseInt(gotoLevelText.getText())-1));
		sidePanel.add(gotoButton);
		
		// Current concept
		currentConceptLabel = new JLabel("-");
		currentConceptLabel.setBounds(5, 710, 800, 20);
		frame.add(currentConceptLabel);

		// The scroll pane containing the canvas
		JScrollPane scrollPane = new JScrollPane(canvas,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(5, 5, 800, 700);
		frame.add(scrollPane);


		frame.getRootPane().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				// This is only called when the user releases the mouse button.
				int w = frame.getWidth();
				int h = frame.getHeight();
				scrollPane.setSize(w-205, h-70);
				scrollPane.getViewport().setViewPosition(new Point(1000,100));
				sidePanel.setLocation(w-195, 0);           
				currentConceptLabel.setLocation(5, h-60);
				currentConceptLabel.setSize(w-205, 20);
				frame.repaint();
			}
		});

		frame.setVisible(true);
	}


	private static void nextTree(TreeViewer canvas) {
		int newIndex = Math.min(canvas.allTrees.size() - 1, canvas.currentTreeIndex + 1);
		canvas.showTree(newIndex);
	}

	private static void previousTree(TreeViewer canvas) {
		int newIndex = Math.max(0, canvas.currentTreeIndex - 1);
		canvas.showTree(newIndex);
	}

	private static void nextStep(TreeViewer canvas) {
		switch (canvas.currentSubStep) {
		case SELECTION:
			canvas.currentSubStep = Step.EXPANSION;
			nextTree(canvas);
			break;
		case EXPANSION:
			canvas.currentSubStep = Step.SELECTION;
			nextTree(canvas);
			break;
		}
		setStepLabel(canvas);
		canvas.repaint();
	}

	private static void prevStep(TreeViewer canvas) {
		switch (canvas.currentSubStep) {
		case SELECTION:
			canvas.currentSubStep = Step.EXPANSION;
			break;
		case EXPANSION:
			canvas.currentSubStep = Step.SELECTION;
			previousTree(canvas);
			break;
		}
		setStepLabel(canvas);
		canvas.repaint();
	}

	private static void setStepLabel(TreeViewer canvas) {
		switch (canvas.currentSubStep) {
		case SELECTION:
			currentStepLabel.setText("Selection");
			break;
		case EXPANSION:
			currentStepLabel.setText("Expansion");
			break;
		}
	}

	private boolean openTree(File treeFile) {
		System.out.println("Opening " + treeFile.getPath());

		// Clear all nodes
		removeAll();
		currentTreeIndex = -1;
		allTrees.clear();

		Map<Integer, TreeNode> lastNodeOnLevel = new HashMap<>();
		Tree currentTree = null;

		boolean treeAdded = false;

		String selectedDesc = "";
		List<String> selectedDescs = new ArrayList<String>();

		try (BufferedReader br = new BufferedReader(new FileReader(treeFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("best node:")) {
					continue;
				}

				if(line.contains("Iteration")) {
					selectedDescs.clear();
					continue;
				}

				/* Finish the current tree (there is always a blank line between trees) */
				if (line.equals("")) {
					// Tree finished
					allTrees.add(currentTree);
					currentTree = null;
					treeAdded = true;
					if (allTrees.size() > Tree.maxSize) {
						break;
					}
					continue;
				}

				String selectedNodeIndicator = "selected node: ";
				if(line.contains(selectedNodeIndicator)) {
					String selectedNode = line.substring(selectedNodeIndicator.length());
					selectedDesc = selectedNode.substring(0, selectedNode.indexOf("["));
					selectedDesc = selectedDesc.trim();
					selectedDescs.add(selectedDesc);
					continue;
				}

				/* If this line does not represent a node, then skip it.
				 * (If it's a node, it always has an accuracy) */
				if (!line.contains("acc")) {
					continue;
				}                           

				/* Get the current node's description (concept) */
				int arrowIndex = line.indexOf("|-->");
				int level;
				String concept;
				if (arrowIndex == -1) {
					level = 0;
					concept = line.substring(0, line.indexOf("[acc:"));
				} else {
					level = arrowIndex / 2 + 1;
					concept = line.substring(arrowIndex+5, line.indexOf("[acc:")-1);
				}
				concept = concept.trim();

				String celoeIndicator = "celoe:";
				int celoeIdx = line.indexOf(celoeIndicator);
				String accIndicator = "acc:";
				int accIdx = line.indexOf(accIndicator);
				String heIndicator = "he:";
				int heIdx = line.indexOf(heIndicator);
				
				float accuracy = Float.parseFloat(line.substring(accIdx+accIndicator.length(), line.indexOf("%, ", accIdx))) / 100;			
				
				int he = Integer.parseInt(line.substring(heIdx+heIndicator.length(), line.indexOf(", ", heIdx)));

				float celoe = Float.NaN;
				if(celoeIdx != -1) {
					String value = line.substring(celoeIdx+celoeIndicator.length(), line.indexOf("]", celoeIdx));
					if(!value.equals("NaN"))
						celoe = Float.parseFloat(value);
				}

				/* Create tree node */
				TreeNode currentNode = new TreeNode();
				currentNode.concept = concept;
				currentNode.accuracy = accuracy;
				currentNode.horizontalExpansion = he;
				currentNode.level = level;
				currentNode.celoe = celoe;

				if(selectedDescs.contains(concept)) {
					currentNode.isSelected = true;
				}            

				/* The last node that was added to the current layer of the tree is this node.
				 * This is relevant when adding children in the next line. */
				lastNodeOnLevel.put(level, currentNode);

				/* Create new tree if this is the first node. */
				if (currentTree == null) {
					currentTree = new Tree();
					currentTree.root = currentNode;
					currentTree.conceptNodes = new HashMap<>();
					treeAdded = false;
				} else {
					/* Add this node as a child to the last node of the level above */
					lastNodeOnLevel.get(level-1).children.add(currentNode);
				}
				currentTree.conceptNodes.put(currentNode.concept, currentNode);

				/* Add this node to the layers of the current tree. Allocate new list if necessary. */
				if (currentTree.layers.size() <= level) {
					currentTree.layers.add(new LinkedList<>());
				}
				currentTree.layers.get(level).add(currentNode);
				//                if (currentNode.celoe > currentTree.maxCeloeScore) {
				//                    currentTree.maxCeloeScore = currentNode.celoe;
				//                }
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// Add the last tree if it hasn't been added yet
		if (!treeAdded) {
			allTrees.add(currentTree);
		}

//		singleStepMode = false;

		return true;
	}

	private String getNodeProperty(String nodeLine, String propertyName) {
		int startIndex = nodeLine.indexOf(propertyName+":");
		if (startIndex == -1) {
			return null;
		}
		startIndex += propertyName.length() + 1;
		int endIndex = nodeLine.indexOf(",", startIndex);
		if (endIndex == -1) {
			endIndex = nodeLine.indexOf("]", startIndex);
		}
		if (endIndex == -1) {
			return null;
		}
		String s = nodeLine.substring(startIndex, endIndex);
		return s;
	}


	private void showTree(int i) {
		if (allTrees.size() <= i) return;
		if (currentTreeIndex == i) return;
		if (i < 0) return;

		removeAll();

		Tree tree = allTrees.get(i);

		TreeNode lastRoot = null;
		Set<TreeNode> lastNodes = new HashSet<TreeViewer.TreeNode>();
		Set<String> lastConcepts = new HashSet<String>();
		if(i>0) {
			lastRoot = allTrees.get(i-1).root;
			lastConcepts = lastRoot.getAllConcepts();
		}


		currentTreeIndex = i;
//		String status = "";
//		if(i%2==0)
//			status = "expansion";
//		else
//			status = "update";
//		currentTreeLabel.setText("Tree " + (currentTreeIndex+1) + " / " + allTrees.size() + ": " + status);
		currentTreeLabel.setText("Tree " + (currentTreeIndex+1) + " / " + allTrees.size());

		int maxConceptsPerLayer = 0;
		for (List<TreeNode> l : tree.layers) {
			maxConceptsPerLayer = Math.max(maxConceptsPerLayer, l.size());
		}

		createNodeButton(tree.root, lastConcepts, 5);

		setPreferredSize(new Dimension(tree.root.maxChildX-15,80+(tree.layers.size()-1)*150));

		repaint();
	}

	private void createNodeButton(TreeNode node, Set<String> lastConcepts, int x) {
		node.maxChildX = 0;
		int xCounter = 0;
		int nextX = x;

		for (TreeNode c : node.children) {
			createNodeButton(c, lastConcepts, nextX);
			node.maxChildX = Math.max(node.maxChildX, c.maxChildX);
			nextX = node.maxChildX;
			xCounter += c.x;
		}

		int thisX = x;
		if (!node.children.isEmpty()) {
			thisX = xCounter / node.children.size();
		} else {
			node.maxChildX = thisX+220;
		}

		String buttonText = capString(node.concept, 30) + "\n"
				+ "acc: " + dfPercent.format(node.accuracy)
				+ ", he: " + node.horizontalExpansion;
		
		if(node.celoe != Float.NaN) {
			buttonText += ", cel: " + String.format("%.3f", node.celoe);
		}
				
		JButton nodeButton = new JButton("<html><center>" + buttonText.replaceAll("\\n", "<br>") + "</center></html>");
		node.x = thisX;
		node.y = 10+node.level*150;
		nodeButton.setBounds(node.x, node.y, 230, 70);	

		if(node.isSelected) {
			nodeButton.setBackground(Color.RED);
			nodeButton.setOpaque(true);
		}

		if(!lastConcepts.isEmpty() && !lastConcepts.contains(node.concept)) {
			nodeButton.setBackground(Color.YELLOW);
			nodeButton.setOpaque(true);
		}
		
		if (node.accuracy == 1) {
			nodeButton.setBackground(Color.GREEN);
			nodeButton.setForeground(Color.GREEN);
			nodeButton.setOpaque(true);
		}

		nodeButton.addActionListener(e -> currentConceptLabel.setText(node.concept
				+ ", Accuracy: " + node.accuracy
				+ ", HE: " + node.horizontalExpansion
				));
		add(nodeButton);
	}


	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!(allTrees.size() > currentTreeIndex)) return;

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2));

		Tree tree = allTrees.get(currentTreeIndex);
		for (int j = 0; j < tree.layers.size(); j++) {
			List<TreeNode> layer = tree.layers.get(j);
			for (TreeNode n : layer) {
				int startX = n.x+100;
				int startY = n.y+60;
				for (TreeNode c : n.children) {
					int endX = c.x+100;
					int endY = c.y;

					g2.draw(new Line2D.Float(startX, startY, endX, endY));
					/*
                    if (endX < startX) {
                        g.drawArc(startX, startY, Math.abs(startX-endX), Math.abs(startY-endY), -90, 0);
                    } else {
                        g.drawArc(startX, startY, Math.abs(startX-endX), Math.abs(startY-endY), -90, 0);
                    }
					 */
				}
			}
		}

//		switch (currentSubStep) {
//			case SELECTION:
//				String currentConcept = steps.getJSONObject(currentTreeIndex + 1).getString("selected");
//				TreeNode selectedNode = tree.conceptNodes.get(currentConcept);
//				g2.setColor(Color.RED);
//				g2.draw(new Rectangle(selectedNode.x - 10, selectedNode.y - 10, 220, 80));
//				break;
//	
//			case EXPANSION:
//				JSONArray currentChildren = steps.getJSONObject(currentTreeIndex).getJSONArray("children");
//				for (int i = 0; i < currentChildren.length(); i++) {
//					TreeNode n = tree.conceptNodes.get(currentChildren.getString(i));
//					g2.setColor(Color.RED);
//					g2.draw(new Rectangle(n.x - 10, n.y - 10, 220, 80));
//				}
//				break;
//		}
	}

	private static String capString(String s, int length) {
		if (s.length() <= length) {
			return s;
		} else {
			return s.substring(0, length-3) + "...";
		}
	}
}
