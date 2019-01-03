package org.dllearner.statistics;

import java.util.HashMap;
import java.util.Map;

public class EvaluationData {
	
	private Map<String, Summary> data = new HashMap<String, Summary>();
	private String name;
	
	public EvaluationData(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}		

	public boolean addData(String label, Summary summary) {
		
		if(data.containsKey(label)) {
			System.out.println("label " + label + "already exists!");
			return false;
		}
		data.put(label, summary);
		return true;
	}
	
	public Summary getData(String label) {
		if(data.containsKey(label)) {
			return data.get(label);
		}else {
			System.out.println("label " + label + "does not exist!");
			return null;
		}
			
	}

}
