package aml.learner.cli;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import constants.AMLObjectPropertyIRIs;

public class HasSiblingRewriter implements OWLClassExpressionVisitorEx<OWLClassExpression>{
	
	private OWLDataFactory df;
	
	private OWLObjectProperty isIEOfSiblingIE;
	private OWLObjectProperty isIEOfSiblingEI;
	private OWLObjectProperty isEIOfSiblingIE;
	private OWLObjectProperty isEIOfSiblingEI;
	
	private OWLObjectProperty hasIE;
	private OWLObjectProperty hasEI;
	private OWLObjectProperty isIEOf;
	private OWLObjectProperty isEIOf;
	
	public HasSiblingRewriter(OWLDataFactory df) {
		this.df = df;
		
		isIEOfSiblingIE = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_IE_SIBLING_OF_IE);
		isIEOfSiblingEI = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_IE_SIBLING_OF_EI);
		isEIOfSiblingIE = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_EI_SIBLING_OF_IE);
		isEIOfSiblingEI = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_EI_SIBLING_OF_EI);
		
		hasIE = df.getOWLObjectProperty(AMLObjectPropertyIRIs.HAS_IE);
		hasEI = df.getOWLObjectProperty(AMLObjectPropertyIRIs.HAS_EI);
		isIEOf = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_IE_OF);
		isEIOf = df.getOWLObjectProperty(AMLObjectPropertyIRIs.IS_EI_OF);		
	}

	@Override
	public OWLClassExpression visit(OWLClass ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
	
		Set<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
		for(OWLClassExpression op : ce.getOperands()) {
			ops.add(op.accept(this));
		}
		return df.getOWLObjectIntersectionOf(ops);
	}

	@Override
	public OWLClassExpression visit(OWLObjectUnionOf ce) {

		Set<OWLClassExpression> ops = new HashSet<OWLClassExpression>();
		for(OWLClassExpression op : ce.getOperands()) {
			ops.add(op.accept(this));
		}
		return df.getOWLObjectIntersectionOf(ops);
	}

	@Override
	public OWLClassExpression visit(OWLObjectComplementOf ce) {

		return df.getOWLObjectComplementOf(ce.getOperand().accept(this));
	}

	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {

		// TODO Auto-generated method stub
		OWLPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		if(property.equals(isIEOfSiblingIE)) {
			OWLObjectSomeValuesFrom in = df.getOWLObjectSomeValuesFrom(hasIE, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
			return out;
		}
		
		if(property.equals(isIEOfSiblingEI)) {
			OWLObjectSomeValuesFrom in = df.getOWLObjectSomeValuesFrom(hasEI, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
			return out;
		}
		
		if(property.equals(isEIOfSiblingIE)) {
			OWLObjectSomeValuesFrom in = df.getOWLObjectSomeValuesFrom(hasIE, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
			return out;
		}
		
		if(property.equals(isEIOfSiblingEI)) {
			OWLObjectSomeValuesFrom in = df.getOWLObjectSomeValuesFrom(hasEI, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
			return out;
		}
		
		return df.getOWLObjectSomeValuesFrom((OWLObjectPropertyExpression) property, filler.accept(this));
	}

	@Override
	public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
		// TODO Auto-generated method stub
		/**
		 * Unknown yet how to deal with this.
		 */
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasValue ce) {
		// TODO Auto-generated method stub
		OWLPropertyExpression property = ce.getProperty();
		OWLIndividual filler = ce.getFiller();
		
		if(property.equals(isIEOfSiblingIE)) {
			OWLObjectHasValue in = df.getOWLObjectHasValue(hasIE, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
			return out;
		}
		
		if(property.equals(isIEOfSiblingEI)) {
			OWLObjectHasValue in = df.getOWLObjectHasValue(hasEI, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
			return out;
		}
		
		if(property.equals(isEIOfSiblingIE)) {
			OWLObjectHasValue in = df.getOWLObjectHasValue(hasIE, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
			return out;
		}
		
		if(property.equals(isEIOfSiblingEI)) {
			OWLObjectHasValue in = df.getOWLObjectHasValue(hasEI, filler);
			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
			return out;
		}
		
		return ce;
	}

	@Override
	public OWLClassExpression visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
//		OWLPropertyExpression property = ce.getProperty();
//		OWLClassExpression filler = ce.getFiller();
//		int i = ce.getCardinality();
//		
//		if(property.equals(isIEOfSiblingIE)) {
//			OWLObjectMinCardinality in = df.getOWLObjectMinCardinality(hasIE, filler);
//			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
//			return out;
//		}
//		
//		if(property.equals(isIEOfSiblingEI)) {
//			OWLObjectHasValue in = df.getOWLObjectHasValue(hasEI, filler);
//			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isIEOf, in);
//			return out;
//		}
//		
//		if(property.equals(isEIOfSiblingIE)) {
//			OWLObjectHasValue in = df.getOWLObjectHasValue(hasIE, filler);
//			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
//			return out;
//		}
//		
//		if(property.equals(isEIOfSiblingEI)) {
//			OWLObjectHasValue in = df.getOWLObjectHasValue(hasEI, filler);
//			OWLObjectSomeValuesFrom out = df.getOWLObjectSomeValuesFrom(isEIOf, in);
//			return out;
//		}
//		
//		return ce;
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectHasSelf ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLObjectOneOf ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataHasValue ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OWLClassExpression visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		return null;
	}

}
