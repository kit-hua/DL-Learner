package org.dllearner.cli;

import java.util.Collection;
import java.util.Iterator;

import org.dllearner.statistics.EvaluationData;
import org.dllearner.statistics.LearningData;
import org.dllearner.statistics.Summary;

public class DataMatrix {
	
	private LearningData rho;
	private LearningData aml;
	
	private String name;

	public DataMatrix(LearningData rho, LearningData aml) {
				
		if(!rho.getName().equals(aml.getName()))
			System.out.println("incompatible learning data!");
		else {
			this.rho = rho;
			this.aml = aml;
			this.name = rho.getName();
		}
	}
	
	public EvaluationData getEvaluation() {		
		
		EvaluationData eva = new EvaluationData(name);
		Iterator rhoValues = rho.getData().values().iterator();
		Iterator amlValues = aml.getData().values().iterator();
		Iterator labels = rho.getData().keySet().iterator();
		
		while(rhoValues.hasNext()) {
			long amlValue = (long) amlValues.next();
			long rhoValue = (long) rhoValues.next();
			String label = (String) labels.next();
			
			if(label.equals("#Rules") || label.equals("ComputeTime") || label.equals("RefinementTime")
					|| label.equals("ReasoningTime") || label.equals("TreeTime")) {
				Summary summary = new Summary(amlValue, rhoValue);
				eva.addData(label, summary);
			}
		}
		
		return eva;
	}
	
	
	public LearningData compareData() {
		LearningData comparison = new LearningData(name);
		
		Iterator rhoValues = rho.getData().values().iterator();
		Iterator amlValues = aml.getData().values().iterator();
		Iterator labels = rho.getData().keySet().iterator();
		while(rhoValues.hasNext()) {
			long amlValue = (long) amlValues.next();
			long rhoValue = (long) rhoValues.next();
			long diff = rhoValue - amlValue;
			double percent = 100 * amlValue/(double)rhoValue;
			String label = (String) labels.next();
			comparison.getData().put(label, diff);
//			System.out.println(label + ": ");
//			System.out.println(" - rho: " + rhoValue);
//			System.out.println(" - aml: " + amlValue);
//			System.out.println(" - diff: " + diff);
//			System.out.println(" - percent: " + percent);
			if(diff > 0) {			
				if(label.equals("LogTime"))
					comparison.setLogPercentage(percent);
				else if(label.equals("ComputeTime")) {
					comparison.setComputePercentage(percent);
				}
				else if(label.equals("RefinementTime"))
					comparison.setRefinementPercentage(percent);
				else if(label.equals("ReasoningTime"))
					comparison.setReasoningPercentage(percent);
				else if(label.equals("TreeTime"))
					comparison.setTreePercentage(percent);
				else if(label.equals("InstCheckTime"))
					comparison.setInstCheckPercentage(percent);
				else if(label.equals("SubsumptionTime"))
					comparison.setSubsumptionPercentage(percent);
			}
		}		
		
		comparison.setSolution(rho.getSolution() + " <---> " + aml.getSolution());
		return comparison;
	}
	
	public String toString() {
		String s = "matrix: " + name + "\n";
		s += " - rho: " + rho.getDataString() + "\n";
		s += " - aml: " + aml.getDataString() + "\n";
		return s;
	}
}
