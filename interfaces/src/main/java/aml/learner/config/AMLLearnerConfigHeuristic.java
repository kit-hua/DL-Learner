package aml.learner.config;

import org.json.simple.JSONObject;

public class AMLLearnerConfigHeuristic implements IAMLLearnerConfig{
	
	private static final String TYPE = "type";
	private static final String EXPANSION = "expansionPenaltyFactor";
	private static final String REFINEMENT = "nodeRefinementPenalty";
	private static final String START = "startNodeBonus";
	private static final String GAIN = "gainBonusFactor";
	
	public static final String RRHC = "celoe_heuristic_lw";
	public static final String CELOE = "celoe_heuristic";
	
	private String type = RRHC;
	private double expansionPenalty = 0.02;
	private double refinementPenalty = 0;
	private double startBonus = 0;
	private double gainBonus = 0.2;
	
	public AMLLearnerConfigHeuristic (JSONObject json) {
		if(json != null)
			fromJson(json);
	}

	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub
		type = (json.get(TYPE) == null) ? type : (String) json.get(TYPE); 
		
		expansionPenalty = (json.get(EXPANSION) == null) ? expansionPenalty : Double.valueOf((String) json.get(EXPANSION));
		refinementPenalty = (json.get(REFINEMENT) == null) ? refinementPenalty : Double.valueOf((String) json.get(REFINEMENT));
		startBonus = (json.get(START) == null) ? startBonus : Double.valueOf((String) json.get(START));
		gainBonus = (json.get(GAIN) == null) ? gainBonus : Double.valueOf((String) json.get(GAIN));
	}

	@Override
	public String toDLLearnerString() {
		String s = "\n// heurisitc\n";
		
		s += "h.type = \"" + type + "\"\n";
		s += "h.expansionPenaltyFactor = " + expansionPenalty + "\n";
		s += "h.nodeRefinementPenalty = " + refinementPenalty + "\n";
		s += "h.startNodeBonus = " + startBonus + "\n";
		s += "h.gainBonusFactor = " + gainBonus + "\n";
		
		return s;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
