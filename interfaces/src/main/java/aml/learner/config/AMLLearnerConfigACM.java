package aml.learner.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.dllearner.utilities.owl.ManchesterOWLSyntaxRendererFullPathImpl;
import org.json.simple.JSONObject;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import CAEX215.CAEX215Package;
import CAEX215.CAEXFileType;
import CAEX215.InstanceHierarchyType;
import CAEX215.InternalElementType;
import concept.model.AMLConceptConfig;
import concept.model.GenericAMLConceptModel;
import concept.tree.GenericTreeNode;
import concept.util.GenericAMLConceptModelUtils;
import importer.AMLImporter;
import parser.AMLParser;
import translation.expression.AML2OWLConverter;

public class AMLLearnerConfigACM implements IAMLLearnerConfig{
	
	private static final String FILE = "file";
	private static final String ID = "id";
	
	private String filename;
	private String id;
	
	private ManchesterOWLSyntaxRendererFullPathImpl manchesterRenderer = new ManchesterOWLSyntaxRendererFullPathImpl();
	
	public AMLLearnerConfigACM (JSONObject json) {
		// TODO Auto-generated constructor stub
		fromJson(json);
	}

	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub
        filename = (String) json.get(FILE);
        id = (String) json.get(ID);        
	}

	@Override
	public String toDLLearnerString() {
		// TODO Auto-generated method stub
		try {
			OWLClassExpression ce = getOWLClass(getACM());
			if(ce == null) {
				StackTraceElement[] stackTrace = new Throwable().getStackTrace();
				System.err.println(stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName() + ": cannot find the acm in the file " + filename);
				return "";
			}
			
			return manchesterRenderer.render(ce);
				
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	private GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> getACM () throws DOMException, DatatypeConfigurationException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException {
		
		AMLParser parser = new AMLParser(filename);
		Document caex = parser.getDoc();		
		
		AMLImporter importer = new AMLImporter(CAEX215.CAEX215Package.eINSTANCE);				
		
		CAEXFileType aml = (CAEXFileType) importer.doImport(caex, false);	
		GenericAMLConceptModelUtils interpreter = new GenericAMLConceptModelUtils();
		
		// the acm model is always an IE for now
		// if the target object is an EI, we will know it by "distinguished" flag
		for(InstanceHierarchyType ih : aml.getInstanceHierarchy()) {
			for(InternalElementType ie : ih.getInternalElement()) {
				if(ie.getID().equals(id)) {
					return interpreter.parse(ie, AMLConceptConfig.class);
				}
			}
		}
		
		return null;
	}
	
	private OWLClassExpression getOWLClass (GenericTreeNode<GenericAMLConceptModel<AMLConceptConfig>> acm) {
		
		if(acm == null)
			return null;
		
		AML2OWLConverter converter = new AML2OWLConverter();
		return converter.toOWLClassExpression(acm.getRoot());		
	}

}
