package org.dllearner.algorithms.aml;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class LearningData {
	
	private Map<String, Long> data = new HashMap<String, Long>();
	
	private String solution;
	
	private String name;
	
	private double logPercentage = -1; 
	private double computePercentage = -1;
	private double refinementPercentage = -1;
	private double reasoningPercentage = -1;
	private double treePercentage = -1;
	private double instCheckPercentage = -1;
	private double subsumptionPercentage = -1;
	
	public LearningData() {}
	
	public LearningData(String name) {
		this.name = name;		
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public long getIterations() {
		return data.get("Iterations");
	}

	public void setIterations(long iterations) {
		data.put("Iterations", iterations);
	}

	public long getDepth() {
		return data.get("TreeDepth");
	}

	public void setDepth(long depth) {
		data.put("TreeDepth", depth);
	}

	public long getNrOfNodes() {
		return data.get("#Nodes");
	}

	public void setNrOfNodes(long nrOfNodes) {
		data.put("#Nodes", nrOfNodes);
	}

	public long getNrOfRules() {
		return data.get("#Rules");
	}

	public void setNrOfRules(long nrOfRules) {
		data.put("#Rules", nrOfRules);
	}

	public long getRunTime() {
		return data.get("RunTime");
	}

	public void setRunTime(long runTime) {
		data.put("RunTime", runTime);
	}

	public long getLogTime() {
		return data.get("LogTime");
	}

	public void setLogTime(long logTime) {
		data.put("LogTime", logTime);
	}

	public long getComputeTime() {
		
		return data.get("ComputeTime");
	}

	public void setComputeTime(long computeTime) {
		data.put("ComputeTime", computeTime);
	}

	public long getRefinement() {
		return data.get("RefinementTime");
	}

	public void setRefinement(long refinement) {
		data.put("RefinementTime", refinement);
	}

	public long getReasoning() {
		return data.get("ReasoningTime");
	}

	public void setReasoning(long reasoning) {
		data.put("ReasoningTime", reasoning);
	}
	
	public long getTreeTime() {
		return data.get("TreeTime");	
	}
	
	public void setTreeTime(long treeTime) {
		data.put("TreeTime", treeTime);
	}

	public long getInstCheck() {
		return data.get("InstCheckTime");
	}

	public void setInstCheck(long instCheck) {
		data.put("InstCheckTime", instCheck);
	}

	public long getSubsumption() {
		return data.get("SubsumptionTime");
	}

	public void setSubsumption(long subsumption) {
		data.put("SubsumptionTime", subsumption);
	}

	public double getComputePercentage() {
		if(computePercentage == -1)
			return 100 * getComputeTime()/(double)getRunTime();
		return computePercentage;
	}
	
	public double getLogPercentage() {
		if (logPercentage == -1)
			return 100 * getLogTime()/(double)getRunTime();
		return logPercentage;
	}
	
	public double getRefinementPercentage() {
		if(refinementPercentage == -1)
			return 100 * getRefinement()/(double)getComputeTime();
		return refinementPercentage;
	}
	
	public double getReasoningPercentage() {
		if(reasoningPercentage == -1)
			return 100 * getReasoning()/(double)getComputeTime();
		return reasoningPercentage;
	}
	
	public double getTreePercentage() {
		if(treePercentage == -1)
			return 100 * getTreeTime() /(double)getComputeTime();
		return treePercentage;
	}
	
	public double getInstCheckPercentage() {
		if(instCheckPercentage == -1)
			return 100 * getInstCheck()/(double)getComputeTime();
		return instCheckPercentage;
	}
	
	public double getSubsumptionPercentage() {
		if(subsumptionPercentage == -1)
			return 100 * getSubsumption()/(double)getComputeTime();
		return	subsumptionPercentage;
	}
	
	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

	public void setData(Map<String, Long> data) {
		this.data = data;
	}

	public void setLogPercentage(double logPercentage) {
		this.logPercentage = logPercentage;
	}

	public void setComputePercentage(double computePercentage) {
		this.computePercentage = computePercentage;
	}

	public void setRefinementPercentage(double refinementPercentage) {
		this.refinementPercentage = refinementPercentage;
	}

	public void setReasoningPercentage(double reasoningPercentage) {
		this.reasoningPercentage = reasoningPercentage;
	}

	public void setTreePercentage(double treePercentage) {
		this.treePercentage = treePercentage;
	}

	public void setInstCheckPercentage(double instCheckPercentage) {
		this.instCheckPercentage = instCheckPercentage;
	}

	public void setSubsumptionPercentage(double subsumptionPercentage) {
		this.subsumptionPercentage = subsumptionPercentage;
	}

	public Map<String, Long> getData(){
		return data;
	}
	
	public int getSize() {
		return data.size();
	}
	
	public String toString() {
		return name + ": " + data.toString();
	}
	
	public String getDataString() {
		return data.toString();
	}
}