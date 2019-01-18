/**
 * 
 */
package org.dllearner.aml;

import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class DataNode implements NodeType{

	private OWLClassExpression expr;
	private boolean isExpandable;
	
	public DataNode (OWLClassExpression expr) {
		this.expr = expr;
		isExpandable = OWLClassExpressionUtils.getChildren(this.expr).isEmpty();
	}
	
	public OWLClassExpression getData () {
		return this.expr;
	}
	
	public boolean isExpandable() {
		return this.isExpandable;
	}
	
	public String toString() {
		return "data";
	}	
}
