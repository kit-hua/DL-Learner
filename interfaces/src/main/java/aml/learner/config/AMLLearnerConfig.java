package aml.learner.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.atlas.json.JSON;
import org.eclipse.emf.ecore.EPackage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormatFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import CAEX215.CAEX215Package;
import CAEX215.CAEXFileType;
import constants.AMLClassIRIs;
import constants.Consts;
import importer.AMLImporter;
import parser.AMLParser;
import translation.ontology.AML2OWLOntology;

public class AMLLearnerConfig implements IAMLLearnerConfig{
	
	private static String AML = "aml";
	private static String REASONER = "reasoner";
	private static String OBJTYPE = "type";
	private static String OPERATOR = "op";
	private static String ALGORITHM = "alg";
	private static String HEURISTIC = "heuristic";
	private static String EXAMPLES = "examples";
	
	private JSONObject op;
	private JSONObject alg;
	private JSONObject heuristic;
	private JSONObject examples;
	
	private String amlSourceFile;
	private String reasoner;
	private String objType;
	
	private String amlConfigFile;

	public AMLLearnerConfig(String amlConfigFile) throws FileNotFoundException, IOException, ParseException {
		// TODO Auto-generated constructor stub
		this.amlConfigFile = amlConfigFile;
		
		JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(amlConfigFile));
        JSONObject json = (JSONObject) obj;
		fromJson(json);
	}
	
	@Override
	public void fromJson(JSONObject json) {
		// TODO Auto-generated method stub
		op = (JSONObject)json.get(OPERATOR);
		alg = (JSONObject) json.get(ALGORITHM);
		heuristic = (JSONObject) json.get(HEURISTIC);
		examples = (JSONObject) json.get(EXAMPLES);
		
		amlSourceFile = (String) json.get(AML);
		reasoner = (String) json.get(REASONER);
		objType = (String) json.get(OBJTYPE);
	}

	@Override
	public String toDLLearnerString() {
		// TODO Auto-generated method stub
		String s = "";
		
		s += getKnowledgeSource();
		s += getReasoner();
		
		AMLLearnerConfigOperator opConfig = new AMLLearnerConfigOperator(op);
		s += opConfig.toDLLearnerString();
		
		AMLLearnerConfigAlgorithm algorithmConfig = new AMLLearnerConfigAlgorithm(alg);
		s += algorithmConfig.toDLLearnerString();
		
		AMLLearnerConfigHeuristic heuristicConfig = new AMLLearnerConfigHeuristic(heuristic);
		if(algorithmConfig.getType().equals(AMLLearnerConfigAlgorithm.CELOE)){
			heuristicConfig.setType(AMLLearnerConfigHeuristic.CELOE);
		}
		s += heuristicConfig.toDLLearnerString();		
		
		s += getAccuracyMethod();
		s += getLearningProblem();
		
		return s;
	}
	
	private String getKnowledgeSource () {
		String s = "\n// automatically generated config file from: " + amlConfigFile + "\n\n";
		
		s += "prefixes = [ (\"kb\", \"http://www.ipr.kit.edu/aml/\") ]\n";
		
		s += "\n// knowledge source definition\n";
		
		s += "ks.type = \"OWL File\"\n";
		try {
			s += "ks.fileName = \"" + getOWLOntologyFromAML() + "\"\n";
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
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
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		return s;
	}
	
	private String getReasoner () {
		String s = "\n//reasoner\n";
		
		s += "reasoner.type = \"" + reasoner + "\"\n";
		s += "reasoner.sources = { ks }\n";
		
		return s;
	}
	
	private String getAccuracyMethod () {
		String s = "\n// accuracy method\n";
		
		s += "acc.type = \"pred_acc\"\n";
		
		return s;
	}
	
	private String getLearningProblem () {
		String s = "\n// learning problem\n";
		
		s += "lp.type = \"posNegStandard\"\n";
		s += "lp.accuracyMethod = acc\n";
		
		AMLLearnerConfigExamples exampleConfig = new AMLLearnerConfigExamples(examples);
		s += exampleConfig.toDLLearnerString();
		
		return s;
	}
	
	
	/**
	 * From the given AML file, generate the OWL ontology, save it in the same folder using the same file name
	 * @return the full path of the generated OWL ontology
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws DOMException
	 * @throws DatatypeConfigurationException
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 */
	private String getOWLOntologyFromAML () throws ParserConfigurationException, SAXException, IOException, DOMException, DatatypeConfigurationException, OWLOntologyCreationException, OWLOntologyStorageException {
		
		String amlPath = amlSourceFile.substring(0, amlSourceFile.lastIndexOf("/"));
		String amlName = amlSourceFile.substring(amlSourceFile.lastIndexOf("/")+1);
		
		// parse the aml file
//		System.out.println("=============== Loading AML file from file system ================\n");	
//		AMLParser parser = new AMLParser(Consts.resources + "/aml/" + amlName);		
		AMLParser parser = new AMLParser(amlPath + "/" + amlName);		
//		EPackage modelPackage = CAEX215Package.eINSTANCE;
		EPackage modelPackage = CAEX215.CAEX215Package.eINSTANCE;
		
		// import the aml file 
		AMLImporter importer = new AMLImporter(modelPackage);		
		CAEXFileType aml = (CAEXFileType) importer.doImport(parser.getDoc(), false);

		// initialize the transformer
		AML2OWLOntology tr = new AML2OWLOntology(aml, null);
		        
        // create the target ontology
        tr.createOnt(IRI.create(Consts.aml_pref));
        
        // traverse the aml file and apply transformation
        tr.transform();        
        
        // save the transformed ontology to disk
        RDFXMLDocumentFormatFactory factory = new RDFXMLDocumentFormatFactory();        
        String owlname = amlName.substring(0, amlName.indexOf("."));
        String owlfile = amlPath + "/" + owlname + ".owl";
        tr.save(tr.output_ont, factory.createFormat(), owlfile);
        System.out.println(" - successfully saved ontology to file: " + owlfile + "\n");
        
        return owlfile;
	}

	/**
	 * @return the objType
	 */
	public IRI getCAEXTypeIri() {
		if(objType.equals("IE"))
			return AMLClassIRIs.INTERNAL_ELEMENT;
		else if(objType.equals("EI"))
			return AMLClassIRIs.EXTERNAL_INTERFACE;
		else
			return null;
	}
	

}
