package aml.learner.config;

import org.json.simple.JSONObject;


public class AMLLearnerConfigAlgorithm implements IAMLLearnerConfig{
	
	private static final String ACM = "acm";
	private static final String TIME = "time";
	private static final String SIZE = "size";
	private static final String TREE = "tree";
	private static final String TREE_WRITE = "write";
	private static final String TREE_FILE = "file";
	private static final String TYPE = "type";
	public static final String RRHC = "rrhc";
	public static final String CELOE = "celoe";
	
	private JSONObject acm = null;
	private String time = "10";
	private String size = "10";
	private boolean writeTree = false;
	private String treeFile = "D:/repositories/aml/aml_framework/src/main/resources/test/tree";	
	private String type = RRHC;
	
	public AMLLearnerConfigAlgorithm (JSONObject json) {
		fromJson(json);
	}

	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub
		acm = json.get(ACM) == null ? acm : (JSONObject) json.get(ACM);
		type = (String) json.get(TYPE);
        time = (String) json.get(TIME);
        size = (String) json.get(SIZE);
                
        JSONObject tree = (JSONObject) json.get(TREE);
        
        if(tree !=null ) {
        		writeTree = tree.get(TREE_WRITE) == null ? writeTree : Boolean.valueOf((String) tree.get(TREE_WRITE));
        		treeFile = tree.get(TREE_FILE) == null ? treeFile : (String) tree.get(TREE_FILE);
        }
   
	}

	@Override
	public String toDLLearnerString() {
		// TODO Auto-generated method stub
				
		String s = "\n// algorithm\n"; 
		
				
		/**
		 * user settings
		 */
		
		s += "alg.type = \"" + type + "\"\n";
		
		if(acm != null) {
			AMLLearnerConfigACM acmConfig = new AMLLearnerConfigACM(acm);
			String acmStr = acmConfig.toDLLearnerString();
			
			if(acmStr != "") {
				acmStr = acmStr.replaceAll("\n", "");
				acmStr = acmStr.replaceAll("\\(", "\\( ");
				acmStr = acmStr.replaceAll("\\)", " \\)");
				s += "alg.startClass = \"" + acmStr + "\"\n";
			}				 
		}
		
		s += "alg.maxExecutionTimeInSeconds = " + time + "\n";
		s += "alg.maxNrOfResults = " + size + "\n";
		
		s += "alg.writeSearchTree = " + writeTree + "\n";
		s += "alg.searchTreeFile = \"" + treeFile + "\"\n";	
		
		/**
		 * default settings
		 */
		s += "alg.terminateOnNoiseReached = false\n";
		s += "alg.noisePercentage = 0\n";
		
		if(type.equals(RRHC)) {
			s += "alg.heStep = 0\n";
			s += "alg.heCorrection = false\n";
			s += "alg.newNodesLowerbound = 0\n";
			s += "alg.traverseBreakingThreshold = 0\n";
		}				
		
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
