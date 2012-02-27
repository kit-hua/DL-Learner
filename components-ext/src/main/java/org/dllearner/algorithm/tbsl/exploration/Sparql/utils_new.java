package org.dllearner.algorithm.tbsl.exploration.Sparql;

import java.sql.SQLException;
import java.util.ArrayList;

public class utils_new {

	/**
	 *  
	 * @param string
	 * @param fall 1=Property, 0=Resource, 2=OntologyClass/Yago, 2=resource+yago+ontlogy
	 * @return
	 * @throws SQLException 
	 */
	public static ArrayList<String> searchIndex(String string, int fall, mySQLDictionary myindex) throws SQLException{
		
		String originalString=string;
		string=string.replace("_", " ");
		string=string.replace("-", " ");
		string=string.replace(".", " ");
		String result=null;
		String tmp1=null;
		String tmp2 = null;
		ArrayList<String> result_List = new ArrayList<String>();
		
		if(fall==0 || fall==3){
			
			result=myindex.getResourceURI(string.toLowerCase());
			result_List.add(result);

		}
		if(fall==2||fall==3){
			
			tmp1=myindex.getontologyClassURI(string.toLowerCase());
			tmp2=myindex.getYagoURI(string.toLowerCase());
			if(tmp1!=null) result_List.add(tmp1);
			if(tmp2!=null) result_List.add(tmp2);
		}


		if(fall==1){
			tmp1=myindex.getPropertyURI(string.toLowerCase());
			tmp2=myindex.getontologyURI(string.toLowerCase());
			if(tmp1!=null) result_List.add(tmp1);
			if(tmp2!=null) result_List.add(tmp2);
			
		}
		
		return result_List;
	}


	
}