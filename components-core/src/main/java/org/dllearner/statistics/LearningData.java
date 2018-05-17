package org.dllearner.statistics;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class LearningData {
	
	public Map<String, Long> data = new HashMap<String, Long>();
	
	public String solution;
	
	private String name;
	
	public LearningData(String name) {
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
		return 100 * getComputeTime()/(double)getRunTime();
	}
	
	public double getLogPercentage() {
		return 100 * getLogTime()/(double)getRunTime();
	}
	
	public double getRefinementPercentage() {
		return 100 * getRefinement()/(double)getComputeTime();
	}
	
	public double getReasoningPercentage() {
		return 100 * getReasoning()/(double)getComputeTime();
	}
	
	public double getTreePercentage() {
		return 100 * getTreeTime() /(double)getComputeTime();
	}
	
	public double getInstCheckPercentage() {
		return 100 * getInstCheck()/(double)getComputeTime();
	}
	
	public double getSubsumptionPercentage() {
		return 100 * getSubsumption()/(double)getComputeTime();
	}
	
	public String getSolution() {
		return solution;
	}

	public void setSolution(String solution) {
		this.solution = solution;
	}

	public Map<String, Long> getData(){
		return data;
	}
	
	public int getSize() {
		return data.size();
	}
}
