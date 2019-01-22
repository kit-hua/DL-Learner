/**
 * 
 */
package org.dllearner.aml;

import java.util.HashSet;
import java.util.Set;

import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
public class OWLCLassExpressionUtils2{
	
	private static OWLDataFactory dataFactory = new OWLDataFactoryImpl();
	
	public static boolean isExpandable(OWLClassExpression ce) {
//		return !getChildren(ce).isEmpty();
		if(isConjunctive(ce) || isDisjunctive(ce) || isFillerExpandable(ce))
			return true;
		
		return false;
	}
	
	public static boolean isFillerExpandable(OWLClassExpression ce) {
		
		if(ce instanceof OWLQuantifiedObjectRestriction) {
			if(((OWLQuantifiedObjectRestriction) ce).getFiller() instanceof OWLObjectIntersectionOf)
				return true;
			if(((OWLQuantifiedObjectRestriction) ce).getFiller() instanceof OWLObjectUnionOf)
				return true;
			if(((OWLQuantifiedObjectRestriction) ce).getFiller() instanceof OWLQuantifiedObjectRestriction)
				return true;
		}
		return false;
	}
	
	public static boolean isConjunctive(OWLClassExpression ce) {
		return ce instanceof OWLObjectIntersectionOf;
	}
	
	public static boolean isDisjunctive(OWLClassExpression ce) {
		return ce instanceof OWLObjectUnionOf;
	}
	
	public static OWLClassExpression getRoleFiller(OWLClassExpression expr) {
		
		OWLClassExpression filler = null;
		
		if(expr instanceof OWLQuantifiedObjectRestriction) {
			OWLQuantifiedObjectRestriction res = (OWLQuantifiedObjectRestriction) expr;
			filler = res.getFiller();
		}
		
		return filler;
	}
	
	public static OWLPropertyExpression getRole(OWLClassExpression expr) {
		
		OWLPropertyExpression property = null;
		
		if(expr instanceof OWLQuantifiedObjectRestriction) {
			OWLQuantifiedObjectRestriction res = (OWLQuantifiedObjectRestriction) expr;
			property = res.getProperty();
		}
		
		return property;
	}
	
	
}
