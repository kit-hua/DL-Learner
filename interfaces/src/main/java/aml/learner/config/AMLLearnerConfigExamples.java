package aml.learner.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AMLLearnerConfigExamples implements IAMLLearnerConfig{

	private static final String POSITIVES = "positive";
	private static final String NEGATIVES = "negative";
	
	private List<String> positives = new ArrayList<String>();
	private List<String> negatives = new ArrayList<String>();
	
	public AMLLearnerConfigExamples(JSONObject json) {
		// TODO Auto-generated constructor stub
		fromJson(json);
	}
	
	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub

        JSONArray positives = (JSONArray) json.get(POSITIVES);
        JSONArray negatives = (JSONArray) json.get(NEGATIVES);
        
        Iterator<String> posIterator = positives.iterator();
        while (posIterator.hasNext()) {
        		this.positives.add(posIterator.next());
        }
        
        Iterator<String> negIterator = negatives.iterator();
        while (negIterator.hasNext()) {
        		this.negatives.add(negIterator.next());
        }
	}

	@Override
	public String toDLLearnerString() {
		// TODO Auto-generated method stub
		
		String s = "";
		
		s += "lp.positiveExamples = {\n";		
		for(String pos : positives) {
			s += "\"kb:" + pos + "\",\n";
		}
		s = s.substring(0, s.length()-2);
		s += "\n}\n";
		
		s += "lp.negativeExamples = {\n";		
		for(String neg : negatives) {
			s += "\"kb:" + neg + "\",\n";
		}
		s = s.substring(0, s.length()-2);
		s += "\n}\n";
		
		
		return s;
	}

}
