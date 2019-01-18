/**
 * 
 */
package org.dllearner.aml;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class LogicNode implements NodeType{

	private LogicNodeType type;
	
	public LogicNode (LogicNodeType type) {
		this.type = type;
	}
	
	public LogicNodeType getType() {
		return this.type;
	}
	
	public String toString() {
		return "logic";
	}	
		
}
