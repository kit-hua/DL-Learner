package org.dllearner.algorithms.pattern;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.prefs.Preferences;

import org.coode.owlapi.functionalrenderer.OWLFunctionalSyntaxRenderer;
import org.dllearner.kb.dataset.OWLOntologyDataset;
import org.dllearner.kb.repository.OntologyRepository;
import org.dllearner.kb.repository.OntologyRepositoryEntry;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.AbstractOWLRenderer;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;


public class OWLAxiomPatternFinder {
	
	private static Queue<String> classVarQueue = new LinkedList<String>();
	private static Queue<String> propertyVarQueue = new LinkedList<String>();
	private static Queue<String> individualVarQueue = new LinkedList<String>();
	private static Queue<String> datatypeVarQueue = new LinkedList<String>();
	
	static{
		for(int i = 65; i <= 90; i++){
			classVarQueue.add(String.valueOf((char)i));
		}
		for(int i = 97; i <= 111; i++){
			individualVarQueue.add(String.valueOf((char)i));
		}
		for(int i = 112; i <= 122; i++){
			propertyVarQueue.add(String.valueOf((char)i));
		}
		
	};
	
	private OntologyRepository repository;
	private OWLOntologyManager manager;
	private OWLDataFactory dataFactory;
	
	private Connection conn;
	private PreparedStatement selectOntologyIdPs;
	private PreparedStatement insertOntologyPs;
	private PreparedStatement selectPatternIdPs;
	private PreparedStatement insertPatternIdPs;
	private PreparedStatement insertOntologyPatternPs;
	
	private OWLObjectRenderer axiomRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

	public OWLAxiomPatternFinder(OWLOntologyDataset dataset) {
		
	}
	
	public OWLAxiomPatternFinder(OntologyRepository repository) {
		this.repository = repository;
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		
		initDBConnection();
		createTables();
		try {
			selectOntologyIdPs = conn.prepareStatement("SELECT id FROM Ontology WHERE url=?");
			insertOntologyPs = conn.prepareStatement("INSERT INTO Ontology (url, iri, repository) VALUES(?,?,?)");
			selectPatternIdPs = conn.prepareStatement("SELECT id FROM Pattern WHERE pattern=?");
			insertPatternIdPs = conn.prepareStatement("INSERT INTO Pattern (pattern,pattern_pretty) VALUES(?,?)");
			insertOntologyPatternPs = conn.prepareStatement("INSERT INTO Ontology_Pattern (ontology_id, pattern_id, occurrences) VALUES(?,?,?)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private String render(OWLAxiom axiom){
		try {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = man.createOntology();
			man.addAxiom(ontology, axiom);
			StringWriter sw = new StringWriter();
			org.coode.owlapi.functionalrenderer.OWLObjectRenderer r = new org.coode.owlapi.functionalrenderer.OWLObjectRenderer(man, ontology, sw);
			axiom.accept(r);
			return sw.toString();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void initDBConnection() {
		try {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("db_settings.ini");
			Preferences prefs = new IniPreferences(is);
			String dbServer = prefs.node("database").get("server", null);
			String dbName = prefs.node("database").get("name", null);
			String dbUser = prefs.node("database").get("user", null);
			String dbPass = prefs.node("database").get("pass", null);

			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://" + dbServer + "/" + dbName;
			conn = DriverManager.getConnection(url, dbUser, dbPass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createTables(){
		try {
			Statement statement = conn.createStatement();
			
			statement.execute("CREATE TABLE IF NOT EXISTS Pattern (" 
			        + "id MEDIUMINT NOT NULL AUTO_INCREMENT,"
					+ "pattern VARCHAR(2000) NOT NULL,"
					+ "pattern_pretty VARCHAR(2000) NOT NULL,"
					+ "PRIMARY KEY(id),"
					+ "INDEX(pattern)) DEFAULT CHARSET=utf8");
			
			statement.execute("CREATE TABLE IF NOT EXISTS Ontology (" 
			        + "id MEDIUMINT NOT NULL AUTO_INCREMENT,"
					+ "url VARCHAR(2000) NOT NULL,"
					+ "iri VARCHAR(2000) NOT NULL,"
					+ "repository VARCHAR(200) NOT NULL,"
					+ "PRIMARY KEY(id),"
					+ "INDEX(url)) DEFAULT CHARSET=utf8");
			
			statement.execute("CREATE TABLE IF NOT EXISTS Ontology_Pattern (" 
			        + "ontology_id MEDIUMINT NOT NULL,"
					+ "pattern_id MEDIUMINT NOT NULL,"
					+ "occurrences INTEGER(8) NOT NULL,"
					+ "FOREIGN KEY (ontology_id) REFERENCES Ontology(id),"
					+ "FOREIGN KEY (pattern_id) REFERENCES Pattern(id),"
					+ "PRIMARY KEY(ontology_id, pattern_id)) DEFAULT CHARSET=utf8");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	private int addPattern(OWLAxiom axiom){
		String axiomString = render(axiom);
		//check for existing entry
		try {
			selectPatternIdPs.setString(1, axiomString);
			ResultSet rs = selectPatternIdPs.executeQuery();
			if(rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//add pattern entry
		try {
			insertPatternIdPs.setString(1, axiomString);
			insertPatternIdPs.setString(2, axiomRenderer.render(axiom));
			insertPatternIdPs.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//get the auto generated ID
		try {
			selectPatternIdPs.setString(1, axiomString);
			ResultSet rs = selectPatternIdPs.executeQuery();
			if(rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private boolean ontologyProcessed(URI uri){
		//check if ontology was already processed
		try {
			selectOntologyIdPs.setString(1, uri.toString());
			ResultSet rs = selectOntologyIdPs.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private int addOntology(URI physicalURI, OWLOntology ontology){
		String url = physicalURI.toString();
		String ontologyIRI = ontology.getOntologyID().getOntologyIRI().toString();
		//check for existing entry
		try {
			selectOntologyIdPs.setString(1, url);
			ResultSet rs = selectOntologyIdPs.executeQuery();
			if(rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//add ontology entry
		try {
			insertOntologyPs.setString(1, url);
			insertOntologyPs.setString(2, ontologyIRI);
			insertOntologyPs.setString(3, repository.getName());
			insertOntologyPs.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//get the auto generated ID
		try {
			selectOntologyIdPs.setString(1, url);
			ResultSet rs = selectOntologyIdPs.executeQuery();
			if(rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private void addOntologyPatterns(URI physicalURI, OWLOntology ontology, Multiset<OWLAxiom> patterns){
		int ontologyId = addOntology(physicalURI, ontology);
		for (OWLAxiom pattern : patterns.elementSet()) {
			try {
				int patternId = addPattern(pattern);
				int occurrences = patterns.count(pattern);
				insertOntologyPatternPs.setInt(1, ontologyId);
				insertOntologyPatternPs.setInt(2, patternId);
				insertOntologyPatternPs.setInt(3, occurrences);
				insertOntologyPatternPs.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() {
		OWLAxiomRenamer renamer = new OWLAxiomRenamer(dataFactory);
		Collection<OntologyRepositoryEntry> entries = repository.getEntries();
		Multiset<OWLAxiom> allAxiomPatterns = HashMultiset.create();
		for (OntologyRepositoryEntry entry : entries) {
			URI uri = entry.getPhysicalURI();
			if (!ontologyProcessed(uri)) {
				System.out.println("Loading " + uri);
				try {
					OWLOntology ontology = manager.loadOntology(IRI.create(uri));
					Multiset<OWLAxiom> axiomPatterns = HashMultiset.create();
					for (OWLLogicalAxiom axiom : ontology.getLogicalAxioms()) {
						OWLAxiom renamedAxiom = renamer.rename(axiom);
						axiomPatterns.add(renamedAxiom);
					}
					allAxiomPatterns.addAll(axiomPatterns);
					addOntologyPatterns(uri, ontology, axiomPatterns);
					for (OWLAxiom owlAxiom : Multisets.copyHighestCountFirst(allAxiomPatterns).elementSet()) {
//						System.out.println(owlAxiom + ": " + allAxiomPatterns.count(owlAxiom));
					}
					manager.removeOntology(ontology);
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
	public static void main(String[] args) throws Exception {
		
		String ontologyURL = "ontologyURL";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = man.getOWLDataFactory();
		OWLFunctionalDataPropertyAxiom axiom = dataFactory.getOWLFunctionalDataPropertyAxiom(dataFactory.getOWLDataProperty(IRI.create("http://ex.org/p")));
		OWLOntology ontology = man.createOntology();
		man.addAxiom(ontology, axiom);
		StringWriter sw = new StringWriter();
		org.coode.owlapi.functionalrenderer.OWLObjectRenderer r = new org.coode.owlapi.functionalrenderer.OWLObjectRenderer(man, ontology, sw);
		axiom.accept(r);
		System.out.println(sw.toString());
	}
}