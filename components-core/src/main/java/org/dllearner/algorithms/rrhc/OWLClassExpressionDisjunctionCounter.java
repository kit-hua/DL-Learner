package org.dllearner.algorithms.rrhc;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataRangeVisitor;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

public class OWLClassExpressionDisjunctionCounter implements OWLClassExpressionVisitor, OWLDataRangeVisitor{

	private int numDisjunctions;
	
	public OWLClassExpressionDisjunctionCounter () {
		this.numDisjunctions = 0;
	}
	
	public int getNumDisjunctions (OWLClassExpression ce) {
		ce.accept(this);
		return numDisjunctions;
	}

	@Override
	public void visit(OWLClass ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		// TODO Auto-generated method stub
		for(OWLClassExpression op : ce.getOperandsAsList()) {
			op.accept(this);
		}
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		// TODO Auto-generated method stub
		numDisjunctions = ce.getOperands().size() - 1;
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLObjectHasValue ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLObjectHasSelf ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		// TODO Auto-generated method stub
		this.numDisjunctions += ce.getIndividuals().size() - 1;
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		// TODO Auto-generated method stub
		ce.getFiller().accept(this);
	}

	@Override
	public void visit(OWLDatatype node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLDataOneOf node) {
		// TODO Auto-generated method stub
		this.numDisjunctions += node.getValues().size() - 1;
	}

	@Override
	public void visit(OWLDataComplementOf node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OWLDataIntersectionOf node) {
		// TODO Auto-generated method stub
		for(OWLDataRange op : node.getOperands())
			op.accept(this);
		
	}

	@Override
	public void visit(OWLDataUnionOf node) {
		// TODO Auto-generated method stub
		this.numDisjunctions += node.getOperands().size() -1 ;
	}

	@Override
	public void visit(OWLDatatypeRestriction node) {
		// TODO Auto-generated method stub

	}
	
	
}
