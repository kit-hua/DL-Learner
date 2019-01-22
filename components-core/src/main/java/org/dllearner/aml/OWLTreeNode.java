/**
 * 
 */
package org.dllearner.aml;

import org.dllearner.core.StringRenderer;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class OWLTreeNode extends SimpleTreeNode<OWLClassExpression>{
	
	private OWLObjectRenderer render = StringRenderer.getRenderer();
	
	public OWLTreeNode () {
		super();
	}
	
	public OWLTreeNode (OWLClassExpression ce) {
		super(ce);		
	}
	
	public OWLTreeNode (OWLTreeNode other) {
		super(other);
	}
	
	/**
	 * print the tree structure
	 */
	public String toString() {
		
		String s = "";
		
		if(this.data instanceof OWLClassExpression)
			s += StringRenderer.getRenderer().render((OWLClassExpression) this.data);
		else			
			s += this.data.toString();
		
		s += render.render(data);

		for (SimpleTreeNode<OWLClassExpression> child : this.children) {
			s += "\n";
			for(int i = 0; i < child.getDepth(); i++)
				s += "\t";
			
			s += child.toString();
		}
		
		return s;
	}
	

}
