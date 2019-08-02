package aml.learner.config;

import org.json.simple.JSONObject;

public interface IAMLLearnerConfig {

	public void fromJson (JSONObject json);
	
	public String toDLLearnerString ();
}
