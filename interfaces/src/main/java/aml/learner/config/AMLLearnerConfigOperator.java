package aml.learner.config;

import org.json.simple.JSONObject;

public class AMLLearnerConfigOperator implements IAMLLearnerConfig{
	
	private static final String TYPE = "type";
	private static final String NEGATION = "useNegation";
	private static final String ALL = "useAllConstructor";
	private static final String CARDINALITY = "useCardinalityRestrictions";
	private static final String DATAHASVALUE = "useDataHasValueConstructor";
	private static final String NUMERIC = "useNumericDatatypes";
	
	private String type = "aml";
	private boolean negation = false;
	private boolean all = false;
	private boolean cardinality = true;
	private boolean dataHasValue = true;
	private boolean numeric = true;
	
	
	public AMLLearnerConfigOperator (JSONObject json) {
		
		if(json != null)
			fromJson(json);	
	}

	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub
		type = (json.get(TYPE) == null) ? type : (String) json.get(TYPE); 
		
		negation = (json.get(NEGATION) == null) ? negation : Boolean.valueOf((String) json.get(NEGATION));
		all = (json.get(ALL) == null) ? all : Boolean.valueOf((String) json.get(ALL));
		
		cardinality = (json.get(CARDINALITY) == null) ? cardinality : Boolean.valueOf((String) json.get(CARDINALITY));
		dataHasValue = (json.get(DATAHASVALUE) == null) ? dataHasValue : Boolean.valueOf((String) json.get(DATAHASVALUE));
		numeric = (json.get(NUMERIC) == null) ? numeric : Boolean.valueOf((String) json.get(NUMERIC));
		
	}

	@Override
	public String toDLLearnerString() {
		String s = "\n//refinement operator\n";
				
		s += "op.type = \"" + type + "\"\n";
		s += "op.useNegation = " + String.valueOf(negation) +"\n";
		s += "op.useAllConstructor = " + String.valueOf(all) +"\n";
		s += "op.useCardinalityRestrictions = " + String.valueOf(cardinality) +"\n";
		s += "//op.useDataHasValueConstructor = " + String.valueOf(dataHasValue) +"\n";
		s += "//op.useNumericDatatypes = " + String.valueOf(numeric) +"\n";
		
		return s;	
		
	}
	
	
}
