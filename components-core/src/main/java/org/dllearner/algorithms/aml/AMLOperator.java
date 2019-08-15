package org.dllearner.algorithms.aml;

import com.google.common.collect.*;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.dllearner.core.*;
import org.dllearner.core.annotations.NoConfigOption;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.options.CommonConfigOptions;
import org.dllearner.core.owl.*;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.refinementoperators.CustomHierarchyRefinementOperator;
import org.dllearner.refinementoperators.CustomStartRefinementOperator;
import org.dllearner.refinementoperators.MathOperations;
import org.dllearner.refinementoperators.ReasoningBasedRefinementOperator;
import org.dllearner.refinementoperators.RefinementOperatorAdapter;
import org.dllearner.utilities.OWLAPIUtils;
import org.dllearner.utilities.owl.ConceptTransformation;
import org.dllearner.utilities.owl.OWLClassExpressionLengthMetric;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.dllearner.utilities.split.DefaultDateTimeValuesSplitter;
import org.dllearner.utilities.split.DefaultNumericValuesSplitter;
import org.dllearner.utilities.split.ValuesSplitter;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.primitives.Ints.max;
import static java.util.stream.Collectors.summingInt;

/**
 * @author Yingbing Hua, yingbing.hua@kit.edu
 *
 */
@ComponentAnn(name = "aml refinement operator", shortName = "aml", version = 0.8)
public class AMLOperator extends RefinementOperatorAdapter implements Component, CustomHierarchyRefinementOperator, CustomStartRefinementOperator, ReasoningBasedRefinementOperator {

	private static Logger logger = LoggerFactory.getLogger(AMLOperator.class);
	private final static Marker sparql_debug = new BasicMarkerFactory().getMarker("SD");

	private static final OWLClass OWL_THING = new OWLClassImpl(
            OWLRDFVocabulary.OWL_THING.getIRI());

	/**
	 * @Hua: change following config parameters to private for AML operator
	 */
	@ConfigOption(description = "the reasoner to use")	
	private AbstractReasonerComponent reasoner;

	//@ConfigOption(description = "the learning algorithm")
	//private

	// hierarchies
	@NoConfigOption
	private ClassHierarchy classHierarchy;
	@NoConfigOption
	private ObjectPropertyHierarchy objectPropertyHierarchy;
	@NoConfigOption
	private DatatypePropertyHierarchy dataPropertyHierarchy;

	// domains and ranges
	private Map<OWLObjectProperty,OWLClassExpression> opDomains = new TreeMap<>();
	private Map<OWLDataProperty,OWLClassExpression> dpDomains = new TreeMap<>();
	private Map<OWLObjectProperty,OWLClassExpression> opRanges = new TreeMap<>();

	// maximum number of fillers for each role
	private Map<OWLObjectPropertyExpression, Integer> maxNrOfFillers = new TreeMap<>();
	// limit for cardinality restrictions (this makes sense if we e.g. have compounds with up to
	// more than 200 atoms but we are only interested in atoms with certain characteristics and do
	// not want something like e.g. >= 204 hasAtom.NOT Carbon-87; which blows up the search space
	@ConfigOption(defaultValue = "5", description = "limit for cardinality restrictions (this makes sense if we e.g. have compounds with too many atoms)")
	private int cardinalityLimit = 5;

	// start concept (can be used to start from an arbitrary concept, needs
	// to be Thing or NamedClass), note that when you use e.g. Compound as
	// start class, then the algorithm should start the search with class
	// Compound (and not with Thing), because otherwise concepts like
	// NOT Carbon-87 will be returned which itself is not a subclass of Compound
	@ConfigOption(
			defaultValue = "owl:Thing",
			description = "You can specify a start class for the algorithm")
	private OWLClassExpression startClass = OWL_THING;

	// the length of concepts of top refinements, the first values is
	// for refinements of \rho_\top(\top), the second one for \rho_A(\top)
	private int topRefinementsLength = 0;
	private Map<OWLClassExpression, Integer> topARefinementsLength = new TreeMap<>();
	// M is finite and this value is the maximum length of any value in M
	/**
	 * @Hua: why 4 not 3?
	 * although it is good to use 4 to be able to increase the "length" of data some values of from 1 to 2
	 * however, we can not have data property restrictions to have length more than 2, 
	 * which will cause computeM() in trouble
	 */
	private int mMaxLength = 4;

	// the sets M_\top and M_A
	private Map<Integer,SortedSet<OWLClassExpression>> m = new TreeMap<>();
	private Map<OWLClassExpression,Map<Integer,SortedSet<OWLClassExpression>>> mA = new TreeMap<>();
	
	// @see MathOperations.getCombos
	/**
	 * combos is the logical possibilities of combining elements from M_B using disjunctions
	 * Possibilities w.r.t. maximal length of the refinement.
	 * For example, max length = 3, possible combos are (3), (1,2), (2,1), (1,1,1)
	 */
	private Map<Integer, List<List<Integer>>> combos = new HashMap<>();

	// refinements of the top concept ordered by length
	private Map<Integer, SortedSet<OWLClassExpression>> topRefinements = new TreeMap<>();
	private Map<OWLClassExpression,Map<Integer, SortedSet<OWLClassExpression>>> topARefinements = new TreeMap<>();

	// cumulated refinements of top (all from length one to the specified length)
	private Map<Integer, TreeSet<OWLClassExpression>> topRefinementsCumulative = new HashMap<>();
	private Map<OWLClassExpression,Map<Integer, TreeSet<OWLClassExpression>>> topARefinementsCumulative = new TreeMap<>();

	// app_A set of applicable properties for a given class (separate for
	// object properties, boolean datatypes, and double datatypes)
	private Map<OWLClassExpression, Set<OWLObjectProperty>> appOP = new TreeMap<>();
	private Map<OWLClassExpression, Set<OWLDataProperty>> appBD = new TreeMap<>();
	private Map<OWLClassExpression, Set<OWLDataProperty>> appNumeric = new TreeMap<>();
	private Map<OWLClassExpression, Set<OWLDataProperty>> appSD = new TreeMap<>();

	// most general applicable properties
	private Map<OWLClassExpression,Set<OWLObjectProperty>> mgr = new TreeMap<>();
	private Map<OWLClassExpression,Set<OWLDataProperty>> mgbd = new TreeMap<>();
	private Map<OWLClassExpression,Set<OWLDataProperty>> mgNumeric = new TreeMap<>();
	private Map<OWLClassExpression,Set<OWLDataProperty>> mgDT = new TreeMap<>();
	private Map<OWLClassExpression,Set<OWLDataProperty>> mgsd = new TreeMap<>();

	// splits for double datatype properties in ascending order
	private Map<OWLDataProperty,List<OWLLiteral>> splits = new TreeMap<>();

	@ConfigOption(description = "the number of generated split intervals for numeric types", defaultValue = "12")
	private int maxNrOfSplits = 12;

	// data structure for a simple frequent pattern matching preprocessing phase
	@ConfigOption(defaultValue = "3", description = "minimum number an individual or literal has to be seen in the " +
			"knowledge base before considering it for inclusion in concepts")
	private int frequencyThreshold = CommonConfigOptions.valueFrequencyThresholdDefault;
	private Map<OWLObjectPropertyExpression, Map<OWLIndividual, Integer>> valueFrequency = new HashMap<>();
	// data structure with identified frequent values
	private Map<OWLObjectPropertyExpression, Set<OWLIndividual>> frequentValues = new HashMap<>();
	// frequent data values
	private Map<OWLDataProperty, Set<OWLLiteral>> frequentDataValues = new HashMap<>();
	private Map<OWLDataProperty, Map<OWLLiteral, Integer>> dataValueFrequency = new HashMap<>();
	@ConfigOption(description = "whether to use hasValue on frequently occuring strings", defaultValue = "false")
	private boolean useDataHasValueConstructor = false;

	// statistics
	public long mComputationTimeNs = 0;
	public long topComputationTimeNs = 0;

	@ConfigOption(defaultValue="true")
	private boolean applyAllFilter = true;

	@ConfigOption(defaultValue="true", description = "throwing out all refinements with " +
			"duplicate \u2203 r for any r")
	private boolean applyExistsFilter = true;

	@ConfigOption(description="support of universal restrictions (owl:allValuesFrom), e.g. \u2200 r.C ", defaultValue="true")
	private boolean useAllConstructor = true;

	@ConfigOption(description="support of existential restrictions (owl:someValuesFrom), e.g. \u2203 r.C ", defaultValue="true")
	private boolean useExistsConstructor = true;

	@ConfigOption(description="support of has value constructor (owl:hasValue), e.g. \u2203 r.{a} ", defaultValue="false")
	private boolean useHasValueConstructor = false;

	@ConfigOption(description="support of qualified cardinality restrictions (owl:minCardinality, owl:maxCardinality, owl:exactCardinality), e.g. \u2265 3 r.C ", defaultValue="true")
	private boolean useCardinalityRestrictions = true;

	@ConfigOption(description="support of local reflexivity of an object property expression (owl:hasSelf), e.g. \u2203 loves.Self for a narcissistic", defaultValue="false")
	private boolean useHasSelf = false;

	@ConfigOption(description="support of negation (owl:complementOf), e.g. \u00AC C ", defaultValue="true")
	private boolean useNegation = true;

	@ConfigOption(description="support of inverse object properties (owl:inverseOf), e.g. r\u207B.C ", defaultValue="false")
	private boolean useInverse = false;

	@ConfigOption(description="support of boolean datatypes (xsd:boolean), e.g. \u2203 r.{true} ", defaultValue="true")
	private boolean useBooleanDatatypes = true;

	@ConfigOption(description="support of numeric datatypes (xsd:int, xsd:double, ...), e.g. \u2203 r.{true} ", defaultValue="true")
	private boolean useNumericDatatypes = true;

	@ConfigOption(defaultValue="true")
	private boolean useTimeDatatypes = true;

	@ConfigOption(description="support of string datatypes (xsd:string), e.g. \u2203 r.{\"SOME_STRING\"} ",defaultValue="false")
	private boolean useStringDatatypes = false;

	/**
	 * @Hua: changed to private for AML operator 
	 */
	@ConfigOption(defaultValue="true", description = "skip combination of intersection between disjoint classes")	
	private boolean disjointChecks = true;

	@ConfigOption(defaultValue="true")
	private boolean instanceBasedDisjoints = true;

	@ConfigOption(defaultValue="false", description = "if enabled, generalise by removing parts of a disjunction")
	private boolean dropDisjuncts = false;

	/**
	 * @Hua: changed to private for AML operator 
	 */
	@ConfigOption(description="universal restrictions on a property r are only used when there is already a cardinality and/or existential restriction on r",
			defaultValue="true")
	private boolean useSomeOnly = true;

	// caches for reasoner queries
	private Map<OWLClassExpression,Map<OWLClassExpression,Boolean>> cachedDisjoints = new TreeMap<>();

//	private Map<OWLClass,Map<OWLClass,Boolean>> abDisjoint = new TreeMap<OWLClass,Map<OWLClass,Boolean>>();
//	private Map<OWLClass,Map<OWLClass,Boolean>> notABDisjoint = new TreeMap<OWLClass,Map<OWLClass,Boolean>>();
//	private Map<OWLClass,Map<OWLClass,Boolean>> notABMeaningful = new TreeMap<OWLClass,Map<OWLClass,Boolean>>();

	@ConfigOption(description = "whether to generate object complement while refining", defaultValue = "false")
	private boolean useObjectValueNegation = false;

	@ConfigOption(description = "class expression length metric (should match learning algorithm usage)", defaultValue = "default cel_metric")
	private OWLClassExpressionLengthMetric lengthMetric = OWLClassExpressionLengthMetric.getDefaultMetric();
	
	private OWLDataFactory df = new OWLDataFactoryImpl();
	
	/**
	 * @Hua: preprocess AML roles and interfaces
	 */
	SortedSet<OWLClassExpression> topAMLRoles = new TreeSet<OWLClassExpression>();
	SortedSet<OWLClassExpression> topAMLInterfaces = new TreeSet<OWLClassExpression>();
	SortedSet<OWLClassExpression> botAMLRoles = new TreeSet<OWLClassExpression>();
	SortedSet<OWLClassExpression> botAMLInterfaces = new TreeSet<OWLClassExpression>();
	OWLClassExpression amlIE = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#IE"));
	OWLClassExpression amlEI = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#EI"));
	OWLClassExpression amlIL = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#IL"));

	public AMLOperator() {	}


	public AMLOperator(AMLOperator op) {
		setApplyAllFilter(op.applyAllFilter);
		setCardinalityLimit(op.cardinalityLimit);
		setClassHierarchy(op.classHierarchy);
		setDataPropertyHierarchy(op.dataPropertyHierarchy);
		setDropDisjuncts(op.dropDisjuncts);
		setFrequencyThreshold(op.frequencyThreshold);
		setInstanceBasedDisjoints(op.instanceBasedDisjoints);
		setObjectPropertyHierarchy(op.objectPropertyHierarchy);
		setReasoner(op.reasoner);
		setStartClass(op.startClass);
		setSubHierarchy(op.classHierarchy);
		setUseAllConstructor(op.useAllConstructor);
		setUseBooleanDatatypes(op.useBooleanDatatypes);
		setUseCardinalityRestrictions(op.useCardinalityRestrictions);
		setUseDataHasValueConstructor(op.useDataHasValueConstructor);
		setUseExistsConstructor(op.useExistsConstructor);
		setUseHasValueConstructor(op.useHasValueConstructor);
		setUseNegation(op.useNegation);
		setUseObjectValueNegation(op.useObjectValueNegation);
		setUseStringDatatypes(op.useStringDatatypes);
		setUseNumericDatatypes(op.useNumericDatatypes);
		initialized = false;
	}


	@Override
    public void init() throws ComponentInitException {
		/*
		if(initialized) {
			throw new ComponentInitException("Refinement operator cannot be initialised twice.");
		}
		*/

		if (classHierarchy == null) classHierarchy = reasoner.getClassHierarchy();
		if (dataPropertyHierarchy == null) dataPropertyHierarchy = reasoner.getDatatypePropertyHierarchy();
		if (objectPropertyHierarchy == null) objectPropertyHierarchy = reasoner.getObjectPropertyHierarchy();

		logger.debug("classHierarchy: " + classHierarchy);
		logger.debug("object properties: " + reasoner.getObjectProperties());

		// query reasoner for domains and ranges
		// (because they are used often in the operator)
		opDomains = reasoner.getObjectPropertyDomains();
		opRanges = reasoner.getObjectPropertyRanges();
		dpDomains = reasoner.getDataPropertyDomains();

		if (useHasValueConstructor) {
			for (OWLObjectProperty op : objectPropertyHierarchy.getEntities()) {
				// sets ordered by corresponding individual (which we ignore)
				Map<OWLIndividual, SortedSet<OWLIndividual>> propertyMembers = reasoner.getPropertyMembers(op);

				Collection<SortedSet<OWLIndividual>> fillerSets = propertyMembers.values();

				// compute frequency of individuals used as object
				Map<OWLIndividual, Integer> ind2Frequency = fillerSets.stream()
						.flatMap(Collection::stream)
						.collect(Collectors.groupingBy(Function.identity(), TreeMap::new, summingInt(s -> 1))); // (ind -> freqency)

				// keep track of this
				valueFrequency.put(op, ind2Frequency);

				// keep only individuals with frequency > threshold
				Set<OWLIndividual> frequentInds = ind2Frequency.entrySet().stream()
						.filter(e -> e.getValue() >= frequencyThreshold) // frequency >= threshold
						.map(Map.Entry::getKey)
						.collect(Collectors.toCollection(TreeSet::new));
				frequentValues.put(op, frequentInds);

				if(useInverse) {
					Map<OWLIndividual, Integer> opMap = new TreeMap<>();
					valueFrequency.put(op.getInverseProperty(), opMap);

					frequentInds = new TreeSet<>();

					for (Entry<OWLIndividual, SortedSet<OWLIndividual>> entry : propertyMembers
							.entrySet()) {
						OWLIndividual subject = entry.getKey();
						SortedSet<OWLIndividual> values = entry.getValue();

						opMap.put(subject, values.size());

						if (values.size() >= frequencyThreshold) {
							frequentInds.add(subject);
						}
					}
					frequentValues.put(op.getInverseProperty(), frequentInds);
				}
			}
		}

		if(useDataHasValueConstructor) {
			for(OWLDataProperty dp : dataPropertyHierarchy.getEntities()) {
				Map<OWLLiteral, Integer> dpMap = new TreeMap<>();
				dataValueFrequency.put(dp, dpMap);

//				long s1 = System.currentTimeMillis();
//				ConcurrentMap<OWLLiteral, Integer> lit2frequency = reasoner.getDatatypeMembers(dp).values()
//						.parallelStream()
//						.map(set -> set.stream().collect(Collectors.toList()))
//						.flatMap(list -> list.stream())
//						.collect(Collectors.toConcurrentMap(
//								Function.identity(), lit -> 1, Integer::sum));
//				long s2 = System.currentTimeMillis();
//				System.out.println(s2 - s1);

				// sets ordered by corresponding individual (which we ignore)
//				s1 = System.currentTimeMillis();
				Collection<SortedSet<OWLLiteral>> fillerSets = reasoner.getDatatypeMembers(dp).values();
				for(SortedSet<OWLLiteral> fillerSet : fillerSets) {
					for(OWLLiteral lit : fillerSet) {
						Integer frequency = dpMap.get(lit);

						if(frequency != null) {
							dpMap.put(lit, frequency+1);
						} else {
							dpMap.put(lit, 1);
						}
					}
				}
//				s2 = System.currentTimeMillis();
//				System.out.println(s2 - s1);

				// keep only frequent patterns
				Set<OWLLiteral> frequentInds = new TreeSet<>();
				for(OWLLiteral i : dpMap.keySet()) {
					if(dpMap.get(i) >= frequencyThreshold) {
						logger.trace("adding value "+i+", because "+dpMap.get(i) +">="+frequencyThreshold);
						frequentInds.add(i);
					}
				}
				frequentDataValues.put(dp, frequentInds);
			}
		}

		// we do not need the temporary set anymore and let the
		// garbage collector take care of it
		valueFrequency = null;
		dataValueFrequency.clear();// = null;

		// compute splits for numeric data properties
		if(useNumericDatatypes) {
			if(reasoner instanceof SPARQLReasoner
					&& !((SPARQLReasoner)reasoner).isUseGenericSplitsCode()) {
				// TODO SPARQL support for splits
				logger.warn("Numeric Facet restrictions are not (yet) implemented for " + AnnComponentManager.getName(reasoner) + ", option ignored");
			} else {
				ValuesSplitter splitter = new DefaultNumericValuesSplitter(reasoner, df, maxNrOfSplits);
				splits.putAll(splitter.computeSplits());
				if (logger.isDebugEnabled()) {
					logger.debug( sparql_debug, "Numeric Splits: {}", splits);
				}
			}
		}

		// compute splits for time data properties
		if (useTimeDatatypes) {
			if(reasoner instanceof SPARQLReasoner
					&& !((SPARQLReasoner)reasoner).isUseGenericSplitsCode()) {
				// TODO SPARQL support for splits
				logger.warn("Time based Facet restrictions are not (yet) implemented for " + AnnComponentManager.getName(reasoner) + ", option ignored");
			} else {
				ValuesSplitter splitter = new DefaultDateTimeValuesSplitter(reasoner, df, maxNrOfSplits);
				splits.putAll(splitter.computeSplits());
			}
		}

		// determine the maximum number of fillers for each role
		// (up to a specified cardinality maximum)
		if(useCardinalityRestrictions) {
			if(reasoner instanceof SPARQLReasoner) {
				logger.warn("Cardinality restrictions in Sparql not fully implemented, defaulting to 10.");
			}
			for(OWLObjectProperty op : objectPropertyHierarchy.getEntities()) {
				if(reasoner instanceof SPARQLReasoner) {
					// TODO SPARQL support for cardinalities
					maxNrOfFillers.put(op, 10);
				} else {
					int maxFillers = Math.min(cardinalityLimit,
							reasoner.getPropertyMembers(op).entrySet().stream()
									.mapToInt(entry -> entry.getValue().size())
									.max().orElse(0));
					maxNrOfFillers.put(op, maxFillers);

//					int percentile95 = (int) new Percentile().evaluate(
//							reasoner.getPropertyMembers(op).entrySet().stream()
//							.mapToDouble(entry -> (double)entry.getValue().size())
//							.toArray(), 95);
//					System.out.println("Prop " + op);
//					System.out.println("max: " + maxFillers);
//					System.out.println("95th: " + percentile95);

					// handle inverse properties
					if(useInverse) {
						maxFillers = 0;
	
						Multimap<OWLIndividual, OWLIndividual> map = HashMultimap.create();
	
						for (Entry<OWLIndividual, SortedSet<OWLIndividual>> entry : reasoner.getPropertyMembers(op).entrySet()) {
							OWLIndividual subject = entry.getKey();
							SortedSet<OWLIndividual> objects = entry.getValue();
	
							for (OWLIndividual obj : objects) {
								map.put(obj, subject);
							}
						}
	
						for (Entry<OWLIndividual, Collection<OWLIndividual>> entry : map.asMap().entrySet()) {
							Collection<OWLIndividual> inds = entry.getValue();
							if (inds.size() > maxFillers)
								maxFillers = inds.size();
							if (maxFillers >= cardinalityLimit) {
								maxFillers = cardinalityLimit;
								break;
							}
						}
						maxNrOfFillers.put(op.getInverseProperty(), maxFillers);
					}
				}
			}
		}

		startClass = OWLAPIUtils.classExpressionPropertyExpanderChecked(startClass, reasoner, df, logger);

		if(classHierarchy == null) {
			classHierarchy = reasoner.getClassHierarchy();
		}
		if(objectPropertyHierarchy == null) {
			objectPropertyHierarchy = reasoner.getObjectPropertyHierarchy();
		}
		if(dataPropertyHierarchy == null) {
			dataPropertyHierarchy = reasoner.getDatatypePropertyHierarchy();
		}
				

		SortedSet<OWLClassExpression> topConcepts = getClassCandidates(df.getOWLThing());
		SortedSet<OWLClassExpression> botConcepts = classHierarchy.getSuperClasses(df.getOWLNothing(), true);
		for(OWLClassExpression concept : topConcepts) {
			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
     			if (ap.getValue() instanceof OWLLiteral) {
                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
                     if (val.getLiteral().equals("RoleClass")) {
//                    	 	/**
//                    	 	 * @Hua: according to the paper, we need to test the disjointness
//                    	 	 * However, this will filter out all the dummy concepts directly
//                    	 	 */
//                    	 	if(!isDisjoint(concept, OWL_THING))
                    	 		topAMLRoles.add(concept);
                     }
                 }
     		}       
		}		
		for(OWLClassExpression concept : topConcepts) {
			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
     			if (ap.getValue() instanceof OWLLiteral) {
                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
                     if (val.getLiteral().equals("InterfaceClass")) {
//                    	 	/**
//                 	 	 * @Hua: according to the paper, we need to test the disjointness
//                 	 	 * However, this will filter out all the dummy concepts directly
//                 	 	 */
//                    	 	if(!isDisjoint(concept, OWL_THING))
                    	 		topAMLInterfaces.add(concept);
                     }
                 }
     		}       
		}
				
		for(OWLClassExpression concept : botConcepts) {
			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
     			if (ap.getValue() instanceof OWLLiteral) {
                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
                     if (val.getLiteral().equals("RoleClass")) {
                    	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//                    	 	/**
//                    	 	 * @Hua: according to the paper, we need to test the disjointness
//                    	 	 * However, this will filter out all the dummy concepts directly
//                    	 	 */
//                    	 	if(!isDisjoint(negatedCandidate, OWL_THING))
                    	 		botAMLRoles.add(negatedCandidate);
                     }
                 }
     		}       
		}
		for(OWLClassExpression concept : botConcepts) {
			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
     			if (ap.getValue() instanceof OWLLiteral) {
                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
                     if (val.getLiteral().equals("InterfaceClass")) {
                    	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//                    	 	/**
//                    	 	 * @Hua: according to the paper, we need to test the disjointness
//                    	 	 * However, this will filter out all the dummy concepts directly
//                    	 	 */
//                    	 	if(!isDisjoint(negatedCandidate, OWL_THING))
                    	 		botAMLInterfaces.add(negatedCandidate);
                     }
                 }
     		}       
		}
		
		initialized = true;		
	}

	private void isFinal() {
		if (initialized) throw new IllegalStateException(this.getClass() + " already initialised in " + Thread.currentThread().getStackTrace()[2].getMethodName());
	}

	@Override
	public Set<OWLClassExpression> refine(OWLClassExpression concept) {
		throw new RuntimeException();
	}

	@Override
	public Set<OWLClassExpression> refine(OWLClassExpression description, int maxLength) {
		// check that maxLength is valid
		if(maxLength < OWLClassExpressionUtils.getLength(description, lengthMetric)) {
			throw new Error("length has to be at least class expression length (class expression: " + description + " with length " + OWLClassExpressionUtils.getLength(description, lengthMetric) +", and max length: " + maxLength + ")");
		}
		return refine(description, maxLength, null, startClass);
	}

	@Override
	public Set<OWLClassExpression> refine(OWLClassExpression description, int maxLength,
			List<OWLClassExpression> knownRefinements) {
		return refine(description, maxLength, knownRefinements, startClass);
	}

	/**
	 * @Hua: currDomain is always the top concept for us
	 */
	@SuppressWarnings({"unchecked"})
	public Set<OWLClassExpression> refine(OWLClassExpression description, int maxLength,
			List<OWLClassExpression> knownRefinements, OWLClassExpression currDomain) {	
		
//		if(!currDomain.isOWLThing())
//			System.out.println("|- " + description + ", " + currDomain + ", " + maxLength);
//		System.out.println("description [" + description + "] with domain [" + currDomain + "] " + maxLength);

		// actions needing to be performed if this is the first time the
		// current domain is used
		if(!currDomain.isOWLThing() && !topARefinementsLength.containsKey(currDomain)){
			topARefinementsLength.put(currDomain, 0);
		}

		// check whether using list or set makes more sense
		// here; and whether HashSet or TreeSet should be used
		// => TreeSet because duplicates are possible
		Set<OWLClassExpression> refinements = new TreeSet<>();

		// used as temporary variable
		Set<OWLClassExpression> tmp;

		if(description.isOWLThing()) {
			/**
			 * @Hua: escape refinements of role and interfaces
			 * 	- we can test with roles and interfaces which are not of type AutomationMLBaseRole/AutomationMLBaseInterface
			 */
			// extends top refinements if necessary
			if(currDomain.isOWLThing()) {
				if(maxLength>topRefinementsLength)
					computeTopRefinements(maxLength);
				refinements = (TreeSet<OWLClassExpression>) topRefinementsCumulative.get(maxLength).clone();
			} else {
				if(maxLength>topARefinementsLength.get(currDomain)) {
					computeTopRefinements(maxLength, currDomain);
				}
				refinements = (TreeSet<OWLClassExpression>) topARefinementsCumulative.get(currDomain).get(maxLength).clone();
			}
//			refinements.addAll(classHierarchy.getMoreSpecialConcepts(description));
		} else if(description.isOWLNothing()) {
			// cannot be further refined
		} else if(!description.isAnonymous()) {
			refinements.addAll(classHierarchy.getSubClasses(description, true));
			refinements.remove(df.getOWLNothing());
		} else if (description instanceof OWLObjectComplementOf) {
			OWLClassExpression operand = ((OWLObjectComplementOf) description).getOperand();
			if(!operand.isAnonymous()){
				tmp = classHierarchy.getSuperClasses(operand, true);

				for(OWLClassExpression c : tmp) {
					if(!c.isOWLThing()){
						refinements.add(df.getOWLObjectComplementOf(c));
					}
				}
			}
		} else if (description instanceof OWLObjectIntersectionOf) {
			List<OWLClassExpression> operands = ((OWLObjectIntersectionOf) description).getOperandsAsList();
			// refine one of the elements
			for(OWLClassExpression child : operands) {
				// refine the child; the new max length is the current max length minus
				// the currently considered concept plus the length of the child
				// TODO: add better explanation
				int length = OWLClassExpressionUtils.getLength(description, lengthMetric);
				int childLength = OWLClassExpressionUtils.getLength(child, lengthMetric);
				tmp = refine(child, maxLength - length + childLength, null, currDomain);

				// create new intersection
				for(OWLClassExpression c : tmp) {
					if(!useSomeOnly || isCombinable(description, c)) {
						List<OWLClassExpression> newChildren = new ArrayList<>(operands);
						newChildren.add(c);
						newChildren.remove(child);
						Collections.sort(newChildren);
						OWLClassExpression mc = new OWLObjectIntersectionOfImplExt(newChildren);

						// clean concept and transform it to ordered negation normal form
						// (non-recursive variant because only depth 1 was modified)
						mc = ConceptTransformation.cleanConceptNonRecursive(mc);
						mc = ConceptTransformation.nnf(mc);

						// check whether the intersection is OK (sanity checks), then add it
						if(checkIntersection((OWLObjectIntersectionOf) mc))
							refinements.add(mc);
					}
				}

			}

		} else if (description instanceof OWLObjectUnionOf) {
			// refine one of the elements
			List<OWLClassExpression> operands = ((OWLObjectUnionOf) description).getOperandsAsList();
			for(OWLClassExpression child : operands) {
//				System.out.println("union child: " + child + " " + maxLength + " " + description.getLength() + " " + child.getLength());

				// refine child
				int length = OWLClassExpressionUtils.getLength(description, lengthMetric);
				int childLength = OWLClassExpressionUtils.getLength(child, lengthMetric);
				tmp = refine(child, maxLength - length + childLength, null, currDomain);

				// construct union (see above)
				for(OWLClassExpression c : tmp) {
					List<OWLClassExpression> newChildren = new ArrayList<>(operands);
					newChildren.remove(child);
					newChildren.add(c);
					Collections.sort(newChildren);
					OWLClassExpression md = new OWLObjectUnionOfImplExt(newChildren);

					// transform to ordered negation normal form
					md = ConceptTransformation.nnf(md);
					// note that we do not have to call clean here because a disjunction will
					// never be nested in another disjunction in this operator

					refinements.add(md);
				}
			}

			// if enabled, we can remove elements of the disjunction
			if(dropDisjuncts) {
				// A1 OR A2 => {A1,A2}
				if(operands.size() == 2) {
					refinements.add(operands.get(0));
					refinements.add(operands.get(1));
				} else {
					// copy children list and remove a different element in each turn
					for(int i=0; i<operands.size(); i++) {
						List<OWLClassExpression> newChildren = new LinkedList<>(operands);
						newChildren.remove(i);
						OWLObjectUnionOf md = new OWLObjectUnionOfImplExt(newChildren);
						refinements.add(md);
					}
				}
			}

		} else if (description instanceof OWLObjectSomeValuesFrom) {
			
//			if(maxLength > 6)
//			{
//				System.out.println();
//			}
			OWLObjectPropertyExpression role = ((OWLObjectSomeValuesFrom) description).getProperty();
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom) description).getFiller();

//			System.out.println("refining: [" + description + "] with filler [" + filler + "]");
			/**
			 * @Hua: extend object property refinements
			 *   - if filler is thing:
			 *   	- if role = hasIE: use role class to refine instead of checking range
			 *   	- if role = hasEI: use interface class to refine instead of checking range
			 *   - else: as now, since we have already chosen one role/interface and we want to specialize it
			 * we can test with sucs with multiple IEs of specific role classes: hasIE.Robot and hasIE.IO and hasIE.XX ....
			 */		

			if(role.toString().equals("hasIE")) {				
				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlIE);
//				System.out.println("refining " + description + "with role: " + role + "and domain IE");
			}
			else if(role.toString().equals("hasEI")) {				
				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlEI);
//				System.out.println("refining " + description + "with role: " + role + "and domain EI");
			}
			else if(role.toString().equals("hasIL")) {				
				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlIL);		
			}
			else {
				OWLClassExpression domain = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
				// rule 1: EXISTS r.D => EXISTS r.E
				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, domain);	
			}
						
			for(OWLClassExpression c : tmp){
				refinements.add(df.getOWLObjectSomeValuesFrom(role, c));
			}

			// rule 2: EXISTS r.D => EXISTS s.D or EXISTS r^-1.D => EXISTS s^-1.D
			Set<OWLObjectProperty> moreSpecialRoles = objectPropertyHierarchy.getMoreSpecialRoles(role.getNamedProperty());

			for (OWLObjectProperty moreSpecialRole : moreSpecialRoles) {
				refinements.add(df.getOWLObjectSomeValuesFrom(moreSpecialRole, filler));
			}

			// rule 3: EXISTS r.D => >= 2 r.D
			// (length increases by 1 so we have to check whether max length is sufficient)
			if(useCardinalityRestrictions) {// && !role.isAnonymous()) {
				if(maxLength > OWLClassExpressionUtils.getLength(description, lengthMetric) && maxNrOfFillers.get(role) > 1) {
					OWLObjectMinCardinality min = df.getOWLObjectMinCardinality(2,role,filler);
					refinements.add(min);
				}
			}

			// rule 4: EXISTS r.TOP => EXISTS r.{value}
			if(useHasValueConstructor && filler.isOWLThing()){ // && !role.isAnonymous()) {
				// watch out for frequent patterns
				Set<OWLIndividual> frequentInds = frequentValues.get(role);
				if(frequentInds != null) {
					for(OWLIndividual ind : frequentInds) {
						OWLObjectHasValue ovr = df.getOWLObjectHasValue(role, ind);
						refinements.add(ovr);
						if(useObjectValueNegation ){
							refinements.add(df.getOWLObjectComplementOf(ovr));
						}

					}
				}
			}

		} else if (description instanceof OWLObjectAllValuesFrom) {
			refinements.addAll(refine((OWLObjectAllValuesFrom) description, maxLength));
		} else if (description instanceof OWLObjectCardinalityRestriction) {
			OWLObjectPropertyExpression role = ((OWLObjectCardinalityRestriction) description).getProperty();
			OWLClassExpression filler = ((OWLObjectCardinalityRestriction) description).getFiller();
			OWLClassExpression range = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
			int cardinality = ((OWLObjectCardinalityRestriction) description).getCardinality();
			if(description instanceof OWLObjectMaxCardinality) {
				// rule 1: <= x r.C =>  <= x r.D
				if(useNegation || cardinality > 0){
					tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);

					for(OWLClassExpression d : tmp) {
						refinements.add(df.getOWLObjectMaxCardinality(cardinality,role,d));
					}
				}

				// rule 2: <= x r.C  =>  <= (x-1) r.C
//				int number = max.getNumber();
				if((useNegation && cardinality > 1) || (!useNegation && cardinality > 2)){
					refinements.add(df.getOWLObjectMaxCardinality(cardinality-1,role,filler));
				}

			} else if(description instanceof OWLObjectMinCardinality) {
				tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);

				for(OWLClassExpression d : tmp) {
					refinements.add(df.getOWLObjectMinCardinality(cardinality,role,d));
				}

				// >= x r.C  =>  >= (x+1) r.C
//				int number = min.getNumber();
				if(cardinality < maxNrOfFillers.get(role)){
					refinements.add(df.getOWLObjectMinCardinality(cardinality+1,role,filler));
				}
			} else if(description instanceof OWLObjectExactCardinality) {
				tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);

				for(OWLClassExpression d : tmp) {
					refinements.add(df.getOWLObjectExactCardinality(cardinality,role,d));
				}

				// >= x r.C  =>  >= (x+1) r.C
//				int number = min.getNumber();
				if(cardinality < maxNrOfFillers.get(role)){
					refinements.add(df.getOWLObjectExactCardinality(cardinality+1,role,filler));
				}
			}
		} else if (description instanceof OWLDataSomeValuesFrom) {
			OWLDataProperty dp = ((OWLDataSomeValuesFrom) description).getProperty().asOWLDataProperty();
			OWLDataRange dr = ((OWLDataSomeValuesFrom) description).getFiller();
			if(dr instanceof OWLDatatypeRestriction){
				OWLDatatype datatype = ((OWLDatatypeRestriction) dr).getDatatype();
				Set<OWLFacetRestriction> facetRestrictions = ((OWLDatatypeRestriction) dr).getFacetRestrictions();

				OWLDatatypeRestriction newDatatypeRestriction = null;
				if(OWLAPIUtils.isNumericDatatype(datatype) || OWLAPIUtils.dtDatatypes.contains(datatype)){
					for (OWLFacetRestriction facetRestriction : facetRestrictions) {
						OWLFacet facet = facetRestriction.getFacet();

						OWLLiteral value =  facetRestriction.getFacetValue();

						if(facet == OWLFacet.MAX_INCLUSIVE){
							// find out which split value was used
							int splitIndex = splits.get(dp).lastIndexOf(value);
							if(splitIndex == -1)
								throw new Error("split error");
							int newSplitIndex = splitIndex - 1;
							if(newSplitIndex >= 0) {
								OWLLiteral newValue = splits.get(dp).get(newSplitIndex);
								newDatatypeRestriction = asDatatypeRestriction(dp, newValue, facet);
							}
						} else if(facet == OWLFacet.MIN_INCLUSIVE){
							// find out which split value was used
							int splitIndex = splits.get(dp).lastIndexOf(value);
							if(splitIndex == -1)
								throw new Error("split error");
							int newSplitIndex = splitIndex + 1;
							if(newSplitIndex < splits.get(dp).size()) {
								OWLLiteral newValue = splits.get(dp).get(newSplitIndex);
								newDatatypeRestriction = asDatatypeRestriction(dp, newValue, facet);
							}
						}
					}
				}
				if(newDatatypeRestriction != null){
					refinements.add(df.getOWLDataSomeValuesFrom(dp, newDatatypeRestriction));
				}
			}

		} else if (description instanceof OWLDataHasValue) {
			OWLDataPropertyExpression dp = ((OWLDataHasValue) description).getProperty();
			OWLLiteral value = ((OWLDataHasValue) description).getFiller();

			if(!dp.isAnonymous()){
				Set<OWLDataProperty> subDPs = dataPropertyHierarchy.getMoreSpecialRoles(dp.asOWLDataProperty());
				for(OWLDataProperty subDP : subDPs) {
					refinements.add(df.getOWLDataHasValue(subDP, value));
				}
			}
		}

		/**
		 * @Hua: add a refinement of top to the current one
		 * This is actually part of the rule 3,4,5,6,8 in the PhD paper
		 * That is also way, that the rule 7 does not append a refinement to a intersection
		 * since it is done for each individual part of the intersection in other cases
		 * It is thus at the end of the refine method, because lots of other rules need this step
		 * So to avoid duplicate code, it makes a condition here and do this for all
		 * We additionally want check if the currDomain is EI or IL, 
		 * for which we do not want any further intersections with atomic concepts or object properties  
		 * but we still want data properties for EI and IL
		 * In fact, topARefinementsCumulative of EI and IL does not have object properties, but still have atomic concepts
		 */
		// if a refinement is not Bottom, Top, ALL r.Bottom a refinement of top can be appended
		if(!description.isOWLThing() && !description.isOWLNothing()
				&& !(description instanceof OWLObjectAllValuesFrom && ((OWLObjectAllValuesFrom)description).getFiller().isOWLNothing())) {
			// -1 because of the AND symbol which is appended
			int topRefLength = maxLength - OWLClassExpressionUtils.getLength(description, lengthMetric) - 1;

			// maybe we have to compute new top refinements here
			if(currDomain.isOWLThing()) {
				if(topRefLength > topRefinementsLength)
					computeTopRefinements(topRefLength);
			} else if(topRefLength > topARefinementsLength.get(currDomain))
				computeTopRefinements(topRefLength, currDomain);

			/**
			 * topRefLength is the allowed length of a refinement of THING 
			 * that is going to be appended
			 */
			if(topRefLength>0) {
				Set<OWLClassExpression> topRefs;
				if(currDomain.isOWLThing())
					topRefs = topRefinementsCumulative.get(topRefLength);
				else {
					/**
					 * For object properties, it can not be refined with concepts anymore
					 * since no AML IE/EI/IL would be referenced to two concepts simultaneously
					 * So we do not have things like: hasIE.(A and (B or C or ....))
					 * but we can have: hasIE.(A and (hasIE or hasEI ...))
					 */
					if(currDomain.equals(amlIE) || currDomain.equals(amlEI) || currDomain.equals(amlIL)) {
						topRefs = new TreeSet<OWLClassExpression>();
						for(OWLClassExpression topRef : topARefinementsCumulative.get(currDomain).get(topRefLength)) {									
							boolean anonymous = true;
							for(OWLClassExpression part : topRef.asDisjunctSet()) {
								if(!part.isAnonymous()) {
									anonymous = false;
									break;
								}
							}
							if(anonymous) {
//								System.out.println("topRef: " + topRef);																							
								topRefs.add(topRef);
							}
						}
					}else
						topRefs = topARefinementsCumulative.get(currDomain).get(topRefLength);
				}

				/**
				 * @Hua: this topRefs would contain things like AutomationMLBaseRole or AutomationMLBaseRole
				 */
				for(OWLClassExpression c : topRefs) {
					// true if refinement should be skipped due to filters,
					// false otherwise
					boolean skip = false;

					// if a refinement of of the form ALL r, we check whether ALL r
					// does not occur already
					if(applyAllFilter) {
						if(c instanceof OWLObjectAllValuesFrom) {
							if(description instanceof OWLNaryBooleanClassExpression){
								for(OWLClassExpression child : ((OWLNaryBooleanClassExpression) description).getOperands()) {
									if(child instanceof OWLObjectAllValuesFrom) {
										OWLObjectPropertyExpression r1 = ((OWLObjectAllValuesFrom) c).getProperty();
										OWLObjectPropertyExpression r2 = ((OWLObjectAllValuesFrom) child).getProperty();
										if(r1.equals(r2)){
											skip = true;
											break;
										}
									}
								}
							}
						}
					}

					// we only add \forall r.C to an intersection if there is
					// already some existential restriction \exists r.C
					if(useSomeOnly) {
						skip = !isCombinable(description, c);						
					}

					// check for double datatype properties
					/*
					if(c instanceof DatatypeSomeRestriction &&
							description instanceof DatatypeSomeRestriction) {
						DataRange dr = ((DatatypeSomeRestriction)c).getDataRange();
						DataRange dr2 = ((DatatypeSomeRestriction)description).getDataRange();
						// it does not make sense to have statements like height >= 1.8 AND height >= 1.7
						if((dr instanceof DoubleMaxValue && dr2 instanceof DoubleMaxValue)
							||(dr instanceof DoubleMinValue && dr2 instanceof DoubleMinValue))
							skip = true;
					}*/

					// perform a disjointness check when named classes are added;
					// this can avoid a lot of superfluous computation in the algorithm e.g.
					// when A1 looks good, so many refinements of the form (A1 OR (A2 AND A3))
					// are generated which are all equal to A1 due to disjointness of A2 and A3
					/**
					 * @Hua: if c is disjoint to the current description, e.g. a filler of hasIE
					 * then, c will be ignored, i.e. not added as intersection to description
					 * Only tested for named classes. For property expressions, e.g. description = hasIE some RC, 
					 * then all topRefs are added
					 */
					if(disjointChecks && !c.isAnonymous() && !description.isAnonymous() && isDisjoint(description, c)) {
						skip = true;
//						System.out.println(c + " ignored when refining " + description);
					}
//					else
//						skip = false;
					
//					System.out.println("description: " + description + ", c: " + c + " - " + skip);

					/**
					 * @Hua: Here we refine it with an intersection with one of the top refinements
					 * For quantification it means here we apply the second rule in the PhD paper
					 */
					if(!skip) {
						List<OWLClassExpression> operands = Lists.newArrayList(description, c);
						Collections.sort(operands);
						OWLObjectIntersectionOf mc = new OWLObjectIntersectionOfImplExt(operands);

						// clean and transform to ordered negation normal form
						mc = (OWLObjectIntersectionOf) ConceptTransformation.cleanConceptNonRecursive(mc);
						mc = (OWLObjectIntersectionOf) ConceptTransformation.nnf(mc);

						// last check before intersection is added
						if(checkIntersection(mc))
							refinements.add(mc);
					}
				}
			}
		}

//		for(OWLClassExpression refinement : refinements) {
//			if((refinement instanceof Intersection || refinement instanceof Union) && refinement.getChildren().size()<2) {
//				System.out.println(OWLClassExpression + " " + refinement + " " + currDomain + " " + maxLength);
//				System.exit(0);
//			}
//		}
//		System.out.println("++++++++\nREFINING: " + description + "   maxLength:" + maxLength);
//		System.out.println(refinements);
		return refinements;
	}

	private boolean isCombinable(OWLClassExpression ce, OWLClassExpression child) {
		boolean combinable = true;

		if(child instanceof OWLObjectAllValuesFrom) {
			boolean tmp = false;
			OWLObjectPropertyExpression r1 = ((OWLObjectAllValuesFrom) child).getProperty();
			if(ce instanceof OWLObjectIntersectionOf){
				for(OWLClassExpression operand : ((OWLObjectIntersectionOf) ce).getOperands()) {
					if(child instanceof OWLObjectSomeValuesFrom) {
						OWLObjectPropertyExpression r2 = ((OWLObjectSomeValuesFrom) operand).getProperty();
						if(r1.equals(r2)){
							tmp = true;
							break;
						}
					}
				}
			} else if(ce instanceof OWLObjectSomeValuesFrom) {
				OWLObjectPropertyExpression r2 = ((OWLObjectSomeValuesFrom) ce).getProperty();
				if(r1.equals(r2)){
					tmp = true;
				}
			}
			combinable = tmp;
		}
		return combinable;
	}

	private Set<OWLClassExpression> refine(OWLObjectAllValuesFrom ce, int maxLength) {
		Set<OWLClassExpression> refinements = new HashSet<>();

		OWLObjectPropertyExpression role = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();		

		/**
		 * @Hua: extend the rule 1 
		 */
		Set<OWLClassExpression> tmp;
		if(role.toString().equals("hasIE") ) {
			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlIE);			
		}
		else if(role.toString().equals("hasEI")) {
			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlEI);		
		}
		else if(role.toString().equals("hasIL")) {
			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, amlIL);		
		}
		else {
			// rule 1: ALL r.D => ALL r.E
			OWLClassExpression range = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
			tmp = refine(filler, maxLength-lengthMetric.objectAllValuesLength-lengthMetric.objectProperyLength, null, range);
		}
		
		// rule 1: ALL r.D => ALL r.E
//		OWLClassExpression range = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
//		Set<OWLClassExpression> tmp = refine(filler, maxLength-lengthMetric.objectAllValuesLength-lengthMetric.objectProperyLength, null, range);

		for(OWLClassExpression c : tmp) {
			refinements.add(df.getOWLObjectAllValuesFrom(role, c));
		}

		// rule 2: ALL r.D => ALL r.BOTTOM if D is a most specific atomic concept
		if(!filler.isOWLNothing() && !filler.isAnonymous() && tmp.size()==0) {
			refinements.add(df.getOWLObjectAllValuesFrom(role, df.getOWLNothing()));
		}

		// rule 3: ALL r.D => ALL s.D or ALL r^-1.D => ALL s^-1.D
		Set<OWLObjectProperty> subProperties = objectPropertyHierarchy.getMoreSpecialRoles(role.getNamedProperty());

		for (OWLObjectProperty subProperty : subProperties) {
			refinements.add(df.getOWLObjectAllValuesFrom(subProperty, filler));
		}

		// rule 4: ALL r.D => <= (maxFillers-1) r.D
		// (length increases by 1 so we have to check whether max length is sufficient)
		// => commented out because this is actually not a downward refinement
//		if(useCardinalityRestrictions) {
//			if(maxLength > ce.getLength() && maxNrOfFillers.get(ar)>1) {
//				ObjectMaxCardinalityRestriction max = new ObjectMaxCardinalityRestriction(maxNrOfFillers.get(ar)-1,role,description.getChild(0));
//				refinements.add(max);
//			}
//		}

		return refinements;
	}

	// when a child of an intersection is refined and reintegrated into the
	// intersection, we can perform some sanity checks;
	// method returns true if everything is OK and false otherwise
	// TODO: can be implemented more efficiently if the newly added child
	// is given as parameter
	public static boolean checkIntersection(OWLObjectIntersectionOf intersection) {
		// rule 1: max. restrictions at most once
		boolean maxDoubleOccurence = false;
		// rule 2: min restrictions at most once
		boolean minDoubleOccurence = false;
		// rule 3: no double occurences of boolean datatypes
		TreeSet<OWLDataProperty> occuredDP = new TreeSet<>();
		// rule 4: no double occurences of hasValue restrictions
		TreeSet<OWLObjectPropertyExpression> occuredVR = new TreeSet<>();
		// rule 5: max. restrictions at most once
				boolean maxIntOccurence = false;
				// rule 6: min restrictions at most once
				boolean minIntOccurence = false;

		for(OWLClassExpression child : intersection.getOperands()) {
			if(child instanceof OWLDataSomeValuesFrom) {
				OWLDataRange dr = ((OWLDataSomeValuesFrom)child).getFiller();
				if(dr instanceof OWLDatatypeRestriction){
					OWLDatatype datatype = ((OWLDatatypeRestriction) dr).getDatatype();
					for (OWLFacetRestriction facetRestriction : ((OWLDatatypeRestriction) dr).getFacetRestrictions()) {
						OWLFacet facet = facetRestriction.getFacet();
						if (facet == OWLFacet.MIN_INCLUSIVE) {
							if (datatype.isDouble()) {
								if (minDoubleOccurence) {
									return false;
								} else {
									minDoubleOccurence = true;
								}
							} else if (datatype.isInteger()) {
								if (minIntOccurence) {
									return false;
								} else {
									minIntOccurence = true;
								}
							}
						} else if (facet == OWLFacet.MAX_INCLUSIVE) {
							if (datatype.isDouble()) {
								if (maxDoubleOccurence) {
									return false;
								} else {
									maxDoubleOccurence = true;
								}
							} else if (datatype.isInteger()) {
								if (maxIntOccurence) {
									return false;
								} else {
									maxIntOccurence = true;
								}
							}
						}
					}
				}
			} else if(child instanceof OWLDataHasValue) {
				OWLDataProperty dp = ((OWLDataHasValue) child).getProperty().asOWLDataProperty();
//				System.out.println("dp: " + dp);
				// return false if the boolean property exists already
				if(!occuredDP.add(dp))
					return false;
			} else if(child instanceof OWLObjectHasValue) {
				OWLObjectPropertyExpression op = ((OWLObjectHasValue) child).getProperty();
				if(!occuredVR.add(op))
					return false;
			}
//			System.out.println(child.getClass());
		}
		return true;
	}

	/**
	 * By default, the operator does not specialize e.g. (A or B) to A, because
	 * it only guarantees weak completeness. Under certain circumstances, e.g.
	 * refinement of a fixed given concept, it can be useful to allow such
	 * refinements, which can be done by passing the parameter true to this method.
	 * @param dropDisjuncts Whether to remove disjuncts in refinement process.
	 */
	public void setDropDisjuncts(boolean dropDisjuncts) {
		this.dropDisjuncts = dropDisjuncts;
	}

	private void computeTopRefinements(int maxLength) {
		computeTopRefinements(maxLength, null);
	}

	/**
	 * @Hua: this is for computing disjunctions of the set M
	 * The param domain is used for range specifications of roles
	 * Since we always have range as Thing, we would always have domain = null.
	 * Similarly, topARefinements will always be empty
	 */
	private void computeTopRefinements(int maxLength, OWLClassExpression domain) {
//		System.out.println("computing refinements for top with length " + maxLength + " and domain " + domain);
		long topComputationTimeStartNs = System.nanoTime();
//		System.out.println("computing top refinements for " + domain + " up to length " + maxLength);

		if(domain == null && m.size() == 0)
			computeM();

		if(domain != null && !mA.containsKey(domain))
			computeM(domain);

		int refinementsLength;

		if(domain == null) {
			refinementsLength = topRefinementsLength;
		} else {
			if(!topARefinementsLength.containsKey(domain))
				topARefinementsLength.put(domain,0);

			refinementsLength = topARefinementsLength.get(domain);
		}

		// compute all possible combinations of the disjunction
		for(int i = refinementsLength+1; i <= maxLength; i++) {
			combos.put(i,MathOperations.getCombos(i, mMaxLength));

			// initialise the refinements with empty sets
			if(domain == null) {
				topRefinements.put(i, new TreeSet<>());
			} else {
				if(!topARefinements.containsKey(domain))
					topARefinements.put(domain, new TreeMap<>());
				topARefinements.get(domain).put(i, new TreeSet<>());
			}

			for(List<Integer> combo : combos.get(i)) {

				// combination is a single number => try to use M
				if(combo.size()==1) {
					// note we cannot use "put" instead of "addAll" because there
					// can be several combos for one length
					if(domain == null)
						topRefinements.get(i).addAll(m.get(i));
					else
						topARefinements.get(domain).get(i).addAll(mA.get(domain).get(i));
				// combinations has several numbers => generate disjunct
				} else {

					// check whether the combination makes sense, i.e. whether
					// all lengths mentioned in it have corresponding elements
					// e.g. when negation is deactivated there won't be elements of
					// length 2 in M
					boolean validCombo = true;
					for(Integer j : combo) {
						if((domain == null && m.get(j).size()==0) ||
								(domain != null && mA.get(domain).get(j).size()==0))
							validCombo = false;
					}

					if(validCombo) {

						SortedSet<OWLObjectUnionOf> baseSet = new TreeSet<>();
						for(Integer j : combo) {
							if(domain == null)
								baseSet = MathOperations.incCrossProduct(baseSet, m.get(j));
							else
								baseSet = MathOperations.incCrossProduct(baseSet, mA.get(domain).get(j));
						}

						// convert all concepts in ordered negation normal form
						Set<OWLObjectUnionOf> tmp = new HashSet<>();
						for(OWLClassExpression concept : baseSet) {
							tmp.add((OWLObjectUnionOf) ConceptTransformation.nnf(concept));
						}
						baseSet = new TreeSet<>(tmp);

						// apply the exists filter (throwing out all refinements with
						// double \exists r for any r)
						// TODO: similar filtering can be done for boolean datatype
						// properties
						if(applyExistsFilter) {
							Iterator<OWLObjectUnionOf> it = baseSet.iterator();
							while(it.hasNext()) {
								if(MathOperations.containsDoubleObjectSomeRestriction(it.next()))
									it.remove();
							}
						}

						// add computed refinements
						if(domain == null) {
							topRefinements.get(i).addAll(baseSet);
						} else {
							topARefinements.get(domain).get(i).addAll(baseSet);
						}
					}
				}
			}

			// create cumulative versions of refinements such that they can
			// be accessed easily
			TreeSet<OWLClassExpression> cumulativeRefinements = new TreeSet<>();
			for(int j=1; j<=i; j++) {
				if(domain == null) {
					cumulativeRefinements.addAll(topRefinements.get(j));
				} else {
					cumulativeRefinements.addAll(topARefinements.get(domain).get(j));
				}
			}

			if(domain == null) {
				topRefinementsCumulative.put(i, cumulativeRefinements);
			} else {
				if(!topARefinementsCumulative.containsKey(domain))
					topARefinementsCumulative.put(domain, new TreeMap<>());
				topARefinementsCumulative.get(domain).put(i, cumulativeRefinements);
			}
		}

		// register new top refinements length
		if(domain == null)
			topRefinementsLength = maxLength;
		else
			topARefinementsLength.put(domain,maxLength);

		topComputationTimeNs += System.nanoTime() - topComputationTimeStartNs;

//		if(domain == null) {
//			System.out.println("computed top refinements up to length " + topRefinementsLength + ": " + topRefinementsCumulative.get(maxLength));
//		} else {
//			System.out.println("computed top refinements up to length " + topARefinementsLength + ": (domain: "+domain+"): " + topARefinementsCumulative.get(domain).get(maxLength));
//		}
	}

	// compute M_\top
	private void computeM() {
		long mComputationTimeStartNs = System.nanoTime();
		logger.debug(sparql_debug, "computeM");
		// initialise all possible lengths (1 to mMaxLength)
		for(int i=1; i<=mMaxLength; i++) {
			m.put(i, new TreeSet<>());
		}

		/**
		 * @Hua: the first set of M_T has only the top level concepts 
		 */
//		SortedSet<OWLClassExpression> m1 = classHierarchy.getSubClasses(df.getOWLThing(), true);
//		m.get(lengthMetric.classLength).addAll(m1);
		m.get(lengthMetric.classLength).addAll(topAMLRoles);

		if(useNegation) {
			int lc = lengthMetric.objectComplementLength + lengthMetric.classLength;
//			Set<OWLClassExpression> m2tmp = classHierarchy.getSuperClasses(df.getOWLNothing(), true);
			Set<OWLClassExpression> m2tmp = botAMLRoles;
			for(OWLClassExpression c : m2tmp) {
				if(!c.isOWLThing()) {
					m.get(lc).add(df.getOWLObjectComplementOf(c));
				}
			}
		}

		if(useExistsConstructor) {
			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
			int lc_i = lengthMetric.objectSomeValuesLength + lengthMetric.objectInverseLength + lengthMetric.classLength;
			for(OWLObjectProperty r : objectPropertyHierarchy.getMostGeneralRoles()) {
				m.get(lc).add(df.getOWLObjectSomeValuesFrom(r, df.getOWLThing()));

				if(useInverse) {
					m.get(lc_i).add(df.getOWLObjectSomeValuesFrom(r.getInverseProperty(), df.getOWLThing()));
				}
			}
		}

		if(useAllConstructor) {
			// we allow \forall r.\top here because otherwise the operator
			// becomes too difficult to manage due to dependencies between
			// M_A and M_A' where A'=ran(r)
			int lc = lengthMetric.objectAllValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
			int lc_i = lengthMetric.objectAllValuesLength + lengthMetric.objectInverseLength + lengthMetric.classLength;
			for(OWLObjectProperty r : objectPropertyHierarchy.getMostGeneralRoles()) {
				m.get(lc).add(df.getOWLObjectAllValuesFrom(r, df.getOWLThing()));

				if(useInverse) {
					m.get(lc_i).add(df.getOWLObjectAllValuesFrom(r.getInverseProperty(), df.getOWLThing()));
				}
			}
		}

		// boolean datatypes, e.g. testPositive = true
		if(useBooleanDatatypes) {
			Set<OWLDataProperty> booleanDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getBooleanDatatypeProperties());
			logger.debug(sparql_debug, "BOOL DPs:"+booleanDPs);
			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
			for(OWLDataProperty dp : booleanDPs) {
				m.get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(true)));
				m.get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(false)));
			}
		}

		if(useNumericDatatypes) {
			Set<OWLDataProperty> numericDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getNumericDataProperties());
			logger.debug(sparql_debug, "Numeric DPs:"+numericDPs);
			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;
			for(OWLDataProperty dp : numericDPs) {
				addNumericFacetRestrictions(lc, dp);
			}
		}

		if(useTimeDatatypes) {
			Set<OWLDataProperty> dataProperties = dataPropertyHierarchy.getEntities().stream()
					.filter(dp ->
					{
						OWLDatatype datatype = reasoner.getDatatype(dp);
						return (datatype != null && OWLAPIUtils.dtDatatypes.contains(datatype));
					}).collect(Collectors.toSet());
			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;
			for(OWLDataProperty dp : dataProperties) {
				addNumericFacetRestrictions(lc, dp);
			}
		}

		if(useDataHasValueConstructor) {
			Set<OWLDataProperty> stringDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getStringDatatypeProperties());
			logger.debug(sparql_debug, "STRING DPs:"+stringDPs);
			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
			for(OWLDataProperty dp : stringDPs) {
				// loop over frequent values
				Set<OWLLiteral> freqValues = frequentDataValues.get(dp);
				for(OWLLiteral lit : freqValues) {
					m.get(lc).add(df.getOWLDataHasValue(dp, lit));
				}
			}
		}

		if(useHasValueConstructor) {
			int lc = lengthMetric.objectHasValueLength + lengthMetric.objectProperyLength;
			int lc_i = lengthMetric.objectHasValueLength + lengthMetric.objectInverseLength;

			for(OWLObjectProperty p : objectPropertyHierarchy.getMostGeneralRoles()) {
				Set<OWLIndividual> values = frequentValues.get(p);
				values.forEach(val -> m.get(lc).add(df.getOWLObjectHasValue(p, val)));

				if(useInverse) {
					values.forEach(val -> m.get(lc_i).add(df.getOWLObjectHasValue(p.getInverseProperty(), val)));
				}
			}
		}

		if(useCardinalityRestrictions) {
			logger.debug(sparql_debug, "most general properties:");
			int lc = lengthMetric.objectCardinalityLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
			for(OWLObjectProperty r : objectPropertyHierarchy.getMostGeneralRoles()) {
				int maxFillers = maxNrOfFillers.get(r);
				logger.debug(sparql_debug, "`"+r+"="+maxFillers);
				// zero fillers: <= -1 r.C does not make sense
				// one filler: <= 0 r.C is equivalent to NOT EXISTS r.C,
				// but we still keep it, because ALL r.NOT C may be difficult to reach
				if((useNegation && maxFillers > 0) || (!useNegation && maxFillers > 1))
					m.get(lc).add(df.getOWLObjectMaxCardinality(maxFillers-1, r, df.getOWLThing()));

//				m.get(lc).add(df.getOWLObjectExactCardinality(1, r, df.getOWLThing()));
			}
		}

		if(useHasSelf) {
			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.objectHasSelfLength;
			for(OWLObjectProperty p : objectPropertyHierarchy.getMostGeneralRoles()) {
				m.get(lc).add(df.getOWLObjectHasSelf(p));
			}
		}

		logger.debug(sparql_debug, "m: " + m);

		mComputationTimeNs += System.nanoTime() - mComputationTimeStartNs;
	}

	private void addNumericFacetRestrictions(int lc, OWLDataProperty dp) {
		if(splits.get(dp) != null && splits.get(dp).size() > 0) {
			OWLLiteral min = splits.get(dp).get(0);
			OWLLiteral max = splits.get(dp).get(splits.get(dp).size()-1);

				OWLDatatypeRestriction restriction = asDatatypeRestriction(dp, min, OWLFacet.MIN_INCLUSIVE);
				m.get(lc).add(df.getOWLDataSomeValuesFrom(dp, restriction));

				restriction = asDatatypeRestriction(dp, max, OWLFacet.MAX_INCLUSIVE);
				m.get(lc).add(df.getOWLDataSomeValuesFrom(dp, restriction));
		}
	}

	private OWLDatatypeRestriction asDatatypeRestriction(OWLDataProperty dp, OWLLiteral value, OWLFacet facet) {
		OWLDatatype datatype = reasoner.getDatatype(dp);

		OWLDatatypeRestriction restriction = df.getOWLDatatypeRestriction(
				datatype,
				Collections.singleton(df.getOWLFacetRestriction(
						facet,
						value)));
		return restriction;
	}

	// computation of the set M_A
	// a major difference compared to the ILP 2007 \rho operator is that
	// M is finite and contains elements of length (currently) at most 3
	private void computeM(OWLClassExpression nc) {
		long mComputationTimeStartNs = System.nanoTime();

		mA.put(nc, new TreeMap<>());
		// initialise all possible lengths (1 to mMaxLength)
		for(int i=1; i<=mMaxLength; i++) {
			mA.get(nc).put(i, new TreeSet<>());
		}
		
		/**
		 * @Hua: extended top level concept
		 */
		if(nc.equals(amlIE)) {
//			SortedSet<OWLClassExpression> m1 = getClassCandidatesRecursive(index, df.getOWLThing());
			mA.get(nc).get(lengthMetric.classLength).addAll(topAMLRoles);
		}
		else if((nc.equals(amlEI) || nc.equals(amlIL))) {		
			mA.get(nc).get(lengthMetric.classLength).addAll(topAMLInterfaces);
		}
		else {
			/**
			 * @Hua: original M_A for top level concepts
			 */
			// most general classes, which are not disjoint with nc and provide real refinement
			SortedSet<OWLClassExpression> m1 = getClassCandidates(nc);
			mA.get(nc).get(lengthMetric.classLength).addAll(m1);
		}
		

		/**
		 * @Hua: extended negated bottom concepts
		 */
		// most specific negated classes, which are not disjoint with nc
		if(useNegation) {
			SortedSet<OWLClassExpression> m2;
//			/**
//			 * Extend
//			 */
//			if(nc.toString().equals("IE")) {
//				m2 = classHierarchy.getSuperClasses(df.getOWLNothing(), true);
//				SortedSet<OWLClassExpression> botRoles = new TreeSet<OWLClassExpression>();
//				for(OWLClassExpression concept : m2) {
//					for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//	         			if (ap.getValue() instanceof OWLLiteral) {
//	                         OWLLiteral val = (OWLLiteral) ap.getValue();                        
//	                         if (val.getLiteral().equals("RoleClass")) {
//	                        	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//	                        	 	botRoles.add(negatedCandidate);
//	                         }
//	                     }
//	         		}       
//				}
//				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(botRoles);
//			}
//			else if(nc.toString().equals("EI") || nc.toString().equals("IL")) {
//				m2 = classHierarchy.getSuperClasses(df.getOWLNothing(), true);
//				SortedSet<OWLClassExpression> botInterfaces = new TreeSet<OWLClassExpression>();
//				for(OWLClassExpression concept : m2) {
//					for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//	         			if (ap.getValue() instanceof OWLLiteral) {
//	                         OWLLiteral val = (OWLLiteral) ap.getValue();                        
//	                         if (val.getLiteral().equals("InterfaceClass")) {
//	                        	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//	                        	 	botInterfaces.add(negatedCandidate);
//	                         }
//	                     }
//	         		}       
//				}
//				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(botInterfaces);
//			}
//			else {
				/**
				 * @Hua: original M_A for negated bottom concepts
				 */
				m2 = getNegClassCandidates(nc);
				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(m2);		
//			}			
		}

		/**
		 * @Hua: The rest of M is same as M_T for IE, but different for EI/IL,
		 * Still we will use Thing for data properties, but filter out EI/IL in object properties
		 * 
		 */
		// compute applicable properties				
		OWLClassExpression domain = nc;		
		if(nc.toString().equals("IE") || nc.toString().equals("EI") || nc.toString().equals("IL")) {		
			domain = df.getOWLThing();
		}		
				
			
//		computeMg(nc);
		computeMg(domain);

		// boolean datatypes, e.g. testPositive = true
		if(useBooleanDatatypes) {
			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
//			Set<OWLDataProperty> booleanDPs = mgbd.get(nc);
			Set<OWLDataProperty> booleanDPs = mgbd.get(domain);			
			for (OWLDataProperty dp : booleanDPs) {
				mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(true)));
				mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(false)));
			}
		}

		// filter out EI and IL for obj. properties: they do not have nested structures
		if(useExistsConstructor && !nc.equals(amlEI) && !nc.equals(amlIL)) {
			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
//			for(OWLObjectProperty r : mgr.get(nc)) {
			for(OWLObjectProperty r : mgr.get(domain)) {
				mA.get(nc).get(lc).add(df.getOWLObjectSomeValuesFrom(r, df.getOWLThing()));
			}
		}

		// filter out EI and IL for obj. properties: they do not have nested structures
		if(useAllConstructor && !nc.equals(amlEI) && !nc.equals(amlIL)) {
			// we allow \forall r.\top here because otherwise the operator
			// becomes too difficult to manage due to dependencies between
			// M_A and M_A' where A'=ran(r)
			int lc = lengthMetric.objectAllValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
//			for(OWLObjectProperty r : mgr.get(nc)) {
			for(OWLObjectProperty r : mgr.get(domain)) {
				mA.get(nc).get(lc).add(df.getOWLObjectAllValuesFrom(r, df.getOWLThing()));
			}
		}

		if(useNumericDatatypes) {
//			Set<OWLDataProperty> numericDPs = mgNumeric.get(nc);
			Set<OWLDataProperty> numericDPs = mgNumeric.get(domain);
			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;

			for(OWLDataProperty dp : numericDPs) {
				List<OWLLiteral> splitLiterals = splits.get(dp);
				if(splitLiterals != null && splitLiterals.size() > 0) {
					OWLLiteral min = splits.get(dp).get(0);
					OWLLiteral max = splits.get(dp).get(splits.get(dp).size()-1);
					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, min, OWLFacet.MIN_INCLUSIVE)));
					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, max, OWLFacet.MAX_INCLUSIVE)));
				}
			}
		}

		if(useTimeDatatypes) {
//			Set<OWLDataProperty> dtDPs = mgDT.get(nc);
			Set<OWLDataProperty> dtDPs = mgDT.get(domain);
			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;

			for(OWLDataProperty dp : dtDPs) {
				if(splits.get(dp).size() > 0) {
					OWLLiteral min = splits.get(dp).get(0);
					OWLLiteral max = splits.get(dp).get(splits.get(dp).size()-1);
					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, min, OWLFacet.MIN_INCLUSIVE)));
					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, max, OWLFacet.MAX_INCLUSIVE)));
				}
			}
		}

		if(useDataHasValueConstructor) {
//			Set<OWLDataProperty> stringDPs = mgsd.get(nc);
			Set<OWLDataProperty> stringDPs = mgsd.get(domain);
			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
			for(OWLDataProperty dp : stringDPs) {
				// loop over frequent values
				Set<OWLLiteral> freqValues = frequentDataValues.get(dp);
				for(OWLLiteral lit : freqValues) {
					mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, lit));
				}
			}
		}

		if(useHasValueConstructor && !nc.equals(amlEI) && !nc.equals(amlIL)) {
			int lc = lengthMetric.objectHasValueLength + lengthMetric.objectProperyLength;
			int lc_i = lengthMetric.objectHasValueLength + lengthMetric.objectInverseLength;
//
//			m.get(lc).addAll(
//					mgr.get(nc).stream()
//							.flatMap(p -> frequentValues.get(p).stream()
//									.map(val -> df.getOWLObjectHasValue(p, val)))
//							.collect(Collectors.toSet()));
//			for(OWLObjectProperty p : mgr.get(nc)) {
			for(OWLObjectProperty p : mgr.get(domain)) {
				Set<OWLIndividual> values = frequentValues.get(p);
				values.forEach(val -> m.get(lc).add(df.getOWLObjectHasValue(p, val)));

				if(useInverse) {
					values.forEach(val -> m.get(lc_i).add(df.getOWLObjectHasValue(p.getInverseProperty(), val)));
				}
			}
		}

		if(useCardinalityRestrictions) {
			int lc = lengthMetric.objectCardinalityLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
//			for(OWLObjectProperty r : mgr.get(nc)) {
			for(OWLObjectProperty r : mgr.get(domain)) {
				int maxFillers = maxNrOfFillers.get(r);
				// zero fillers: <= -1 r.C does not make sense
				// one filler: <= 0 r.C is equivalent to NOT EXISTS r.C,
				// but we still keep it, because ALL r.NOT C may be difficult to reach
				if((useNegation && maxFillers > 0) || (!useNegation && maxFillers > 1)) {
					mA.get(nc).get(lc).add(df.getOWLObjectMaxCardinality(maxFillers-1, r, df.getOWLThing()));
				}

				// = 1 r.C
//				mA.get(nc).get(lc).add(df.getOWLObjectExactCardinality(1, r, df.getOWLThing()));
			}
		}

		if(useHasSelf) {
			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.objectHasSelfLength;
//			for(OWLObjectProperty p : mgr.get(nc)) {
			for(OWLObjectProperty p : mgr.get(domain)) {
				m.get(lc).add(df.getOWLObjectHasSelf(p));
			}
		}

		logger.debug(sparql_debug, "m for " + nc + ": " + mA.get(nc));

		mComputationTimeNs += System.nanoTime() - mComputationTimeStartNs;
	}

	// get candidates for a refinement of \top restricted to a class B
	public SortedSet<OWLClassExpression> getClassCandidates(OWLClassExpression index) {
		return getClassCandidatesRecursive(index, df.getOWLThing());
	}

	private SortedSet<OWLClassExpression> getClassCandidatesRecursive(OWLClassExpression index, OWLClassExpression upperClass) {
		SortedSet<OWLClassExpression> candidates = new TreeSet<>();

		SortedSet<OWLClassExpression> subClasses = classHierarchy.getSubClasses(upperClass, true);

		if(reasoner instanceof SPARQLReasoner) {
			Collection<? extends OWLClassExpression> meaningfulClasses = ((SPARQLReasoner)reasoner).getMeaningfulClasses(index, subClasses);
			candidates.addAll(meaningfulClasses);
			// recursive call, i.e. go class hierarchy down for non-meaningful classes
//			for (OWLClassExpression cls : Sets.difference(superClasses, meaningfulClasses)) {
//				candidates.addAll(getNegClassCandidatesRecursive(index, cls));
//			}
		} else {

			// we descend the subsumption hierarchy to ensure that we get
			// the most general concepts satisfying the criteria
			for(OWLClassExpression candidate :  subClasses) {
//					System.out.println("testing " + candidate + " ... ");
	//				NamedClass candidate = (OWLClass) d;
					// check disjointness with index (if not no further traversal downwards is necessary)
					if(!isDisjoint(candidate,index)) {
//						System.out.println( " passed disjointness test ... ");
						// check whether the class is meaningful, i.e. adds something to the index
						// to do this, we need to make sure that the class is not a superclass of the
						// index (otherwise we get nothing new) - for instance based disjoints, we
						// make sure that there is at least one individual, which is not already in the
						// upper class
						boolean meaningful;
						if(instanceBasedDisjoints) {
							// bug: tests should be performed against the index, not the upper class
	//						SortedSet<OWLIndividual> tmp = rs.getIndividuals(upperClass);
							SortedSet<OWLIndividual> tmp = reasoner.getIndividuals(index);
							tmp.removeAll(reasoner.getIndividuals(candidate));
	//						System.out.println("  instances of " + index + " and not " + candidate + ": " + tmp.size());
							meaningful = tmp.size() != 0;
						} else {
							meaningful = !isDisjoint(df.getOWLObjectComplementOf(candidate),index);
						}

						if(meaningful) {
							// candidate went successfully through all checks
							candidates.add(candidate);
	//						System.out.println(" real refinement");
						} else {
							// descend subsumption hierarchy to find candidates
	//						System.out.println(" enter recursion");
							candidates.addAll(getClassCandidatesRecursive(index, candidate));
						}
					}
	//				else {
	//					System.out.println(" ruled out, because it is disjoint");
	//				}
			}
		}
//		System.out.println("cc method exit");
		return candidates;
	}

	// get candidates for a refinement of \top restricted to a class B
	public SortedSet<OWLClassExpression> getNegClassCandidates(OWLClassExpression index) {
		return getNegClassCandidatesRecursive(index, df.getOWLNothing(), null);
	}

	private SortedSet<OWLClassExpression> getNegClassCandidatesRecursive(OWLClassExpression index, OWLClassExpression lowerClass, Set<OWLClassExpression> seenClasses) {
		if (seenClasses == null) { seenClasses = new TreeSet<>(); }
		SortedSet<OWLClassExpression> candidates = new TreeSet<>();
//		System.out.println("index " + index + " lower class " + lowerClass);

		SortedSet<OWLClassExpression> superClasses = classHierarchy.getSuperClasses(lowerClass);

		if(reasoner instanceof SPARQLReasoner) {
			Collection<? extends OWLClassExpression> meaningfulClasses = ((SPARQLReasoner)reasoner).getMeaningfulClasses(index, superClasses);
			candidates.addAll(meaningfulClasses);
			// recursive call, i.e. go class hierarchy up for non-meaningful classes
//			for (OWLClassExpression cls : Sets.difference(superClasses, meaningfulClasses)) {
//				candidates.addAll(getNegClassCandidatesRecursive(index, cls));
//			}
		} else {
			for(OWLClassExpression candidate : superClasses) {
				if(!candidate.isOWLThing()) {
					OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(candidate);

					// check disjointness with index/range (should not be disjoint otherwise not useful)
					if(!isDisjoint(negatedCandidate,index)) {
						boolean meaningful;

						if(instanceBasedDisjoints) {
							SortedSet<OWLIndividual> tmp = reasoner.getIndividuals(index);
							tmp.removeAll(reasoner.getIndividuals(negatedCandidate));
							meaningful = tmp.size() != 0;
						} else {
							meaningful = !isDisjoint(candidate,index);
						}

						if(meaningful) {
							candidates.add(negatedCandidate);
						} else if (!seenClasses.contains(candidate)) {
							seenClasses.add(candidate);
							candidates.addAll(getNegClassCandidatesRecursive(index, candidate, seenClasses));
						}
					}
				}
			}
		}

		return candidates;
	}

	private void computeMg(OWLClassExpression domain) {
		// compute the applicable properties if this has not been done yet
		if(appOP.get(domain) == null)
			computeApp(domain);

		// initialise mgr, mgbd, mgdd, mgsd
		mgr.put(domain, new TreeSet<>());
		mgbd.put(domain, new TreeSet<>());
		mgNumeric.put(domain, new TreeSet<>());
		mgsd.put(domain, new TreeSet<>());
		mgDT.put(domain, new TreeSet<>());

		SortedSet<OWLObjectProperty> mostGeneral = objectPropertyHierarchy.getMostGeneralRoles();
		computeMgrRecursive(domain, mostGeneral, mgr.get(domain));
		SortedSet<OWLDataProperty> mostGeneralDP = dataPropertyHierarchy.getMostGeneralRoles();
		// we make the (reasonable) assumption here that all sub and super
		// datatype properties have the same type (e.g. boolean, integer, double)
		Set<OWLDataProperty> mostGeneralBDP = Sets.intersection(mostGeneralDP, reasoner.getBooleanDatatypeProperties());
		Set<OWLDataProperty> mostGeneralNumericDPs = Sets.intersection(mostGeneralDP, reasoner.getNumericDataProperties());
		Set<OWLDataProperty> mostGeneralStringDPs = Sets.intersection(mostGeneralDP, reasoner.getStringDatatypeProperties());
		computeMgbdRecursive(domain, mostGeneralBDP, mgbd.get(domain));
		computeMostGeneralNumericDPRecursive(domain, mostGeneralNumericDPs, mgNumeric.get(domain));
		computeMostGeneralStringDPRecursive(domain, mostGeneralStringDPs, mgsd.get(domain));
	}

	private void computeMgrRecursive(OWLClassExpression domain, Set<OWLObjectProperty> currProperties, Set<OWLObjectProperty> mgrTmp) {
		for(OWLObjectProperty prop : currProperties) {
			if(appOP.get(domain).contains(prop))
				mgrTmp.add(prop);
			else
				computeMgrRecursive(domain, objectPropertyHierarchy.getMoreSpecialRoles(prop), mgrTmp);
		}
	}

	private void computeMgbdRecursive(OWLClassExpression domain, Set<OWLDataProperty> currProperties, Set<OWLDataProperty> mgbdTmp) {
		for(OWLDataProperty prop : currProperties) {
			if(appBD.get(domain).contains(prop))
				mgbdTmp.add(prop);
			else
				computeMgbdRecursive(domain, dataPropertyHierarchy.getMoreSpecialRoles(prop), mgbdTmp);
		}
	}

	private void computeMostGeneralNumericDPRecursive(OWLClassExpression domain, Set<OWLDataProperty> currProperties, Set<OWLDataProperty> mgddTmp) {
		for(OWLDataProperty prop : currProperties) {
			if(appNumeric.get(domain).contains(prop))
				mgddTmp.add(prop);
			else
				computeMostGeneralNumericDPRecursive(domain, dataPropertyHierarchy.getMoreSpecialRoles(prop), mgddTmp);
		}
	}
	private void computeMostGeneralStringDPRecursive(OWLClassExpression domain, Set<OWLDataProperty> currProperties, Set<OWLDataProperty> mgddTmp) {
		for(OWLDataProperty prop : currProperties) {
			if(appSD.get(domain).contains(prop))
				mgddTmp.add(prop);
			else
				computeMostGeneralStringDPRecursive(domain, dataPropertyHierarchy.getMoreSpecialRoles(prop), mgddTmp);
		}
	}

	// computes the set of applicable properties for a given class
	private void computeApp(OWLClassExpression domain) {
		Set<OWLObjectProperty> applicableRoles = new TreeSet<>();
		Set<OWLObjectProperty> mostGeneral = objectPropertyHierarchy.getMostGeneralRoles();
		if (reasoner instanceof SPARQLReasoner) {
			Set<OWLObjectProperty> roleAppRoles = ((SPARQLReasoner) reasoner).getApplicableProperties(domain, mostGeneral);
			applicableRoles.addAll(roleAppRoles);
		} else {
			SortedSet<OWLIndividual> individuals1 = reasoner.getIndividuals(domain);
			// object properties
			for (OWLObjectProperty role : mostGeneral) {
				// TODO: currently we just rely on named classes as roles,
				// instead of computing dom(r) and ran(r)
				OWLClassExpression d = opDomains.get(role);

				Set<OWLIndividual> individuals2 = new HashSet<>();
				for (Entry<OWLIndividual, SortedSet<OWLIndividual>> entry : reasoner.getPropertyMembers(role).entrySet()) {
					OWLIndividual ind = entry.getKey();
					if (!entry.getValue().isEmpty()) {
						individuals2.add(ind);
					}
				}

				boolean disjoint = Sets.intersection(individuals1, individuals2).isEmpty();
//			if(!isDisjoint(domain,d))
				if (!disjoint) {
					applicableRoles.add(role);
				}

			}
		}

		appOP.put(domain, applicableRoles);

		// boolean datatype properties
		Set<OWLDataProperty> allBDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getBooleanDatatypeProperties());
		Set<OWLDataProperty> applicableBDPs = new TreeSet<>();
		for(OWLDataProperty role : allBDPs) {
//			Description d = (OWLClass) rs.getDomain(role);
			OWLClassExpression d = dpDomains.get(role);
			if(!isDisjoint(domain,d))
				applicableBDPs.add(role);
		}
		appBD.put(domain, applicableBDPs);

		// numeric data properties
		Set<OWLDataProperty> allNumericDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getNumericDataProperties());
		Set<OWLDataProperty> applicableNumericDPs = new TreeSet<>();
		for(OWLDataProperty role : allNumericDPs) {
			// get domain of property
			OWLClassExpression d = dpDomains.get(role);
			// check if it's not disjoint with current class expression
			if(!isDisjoint(domain,d))
				applicableNumericDPs.add(role);
		}
		appNumeric.put(domain, applicableNumericDPs);

		// string datatype properties
		Set<OWLDataProperty> allSDPs = Sets.intersection(dataPropertyHierarchy.getEntities(), reasoner.getStringDatatypeProperties());
		Set<OWLDataProperty> applicableSDPs = new TreeSet<>();
		for(OWLDataProperty role : allSDPs) {
//			Description d = (OWLClass) rs.getDomain(role);
			OWLClassExpression d = dpDomains.get(role);
//			System.out.println("domain: " + d);
			if(!isDisjoint(domain,d))
				applicableSDPs.add(role);
		}
		appSD.put(domain, applicableSDPs);

	}

	// returns true if the intersection contains elements disjoint
	// to the given OWLClassExpression (if true adding the OWLClassExpression to
	// the intersection results in a OWLClassExpression equivalent to bottom)
	// e.g. OldPerson AND YoungPerson; Nitrogen-34 AND Tin-113
	// Note: currently we only check named classes in the intersection,
	// it would be interesting to see whether it makes sense to extend this
	// (advantage: less refinements, drawback: operator will need infinitely many
	// reasoner queries in the long run)
	@SuppressWarnings({"unused"})
	private boolean containsDisjoints(OWLObjectIntersectionOf intersection, OWLClassExpression d) {
		List<OWLClassExpression> children = intersection.getOperandsAsList();
		for(OWLClassExpression child : children) {
			if(d.isOWLNothing())
				return true;
			else if(!child.isAnonymous()) {
				if(isDisjoint(child, d))
					return true;
			}
		}
		return false;
	}

	/**
	 * @Hua: changed to private for AML operator 
	 */
	private boolean isDisjoint(OWLClassExpression d1, OWLClassExpression d2) {
		if(d1.isOWLThing() || d2.isOWLThing()) {
			return false;
		}

		if(d1.isOWLNothing() || d2.isOWLNothing()) {
			return true;
		}

		// check whether we have cached this query
		Map<OWLClassExpression,Boolean> tmp = cachedDisjoints.get(d1);
		if(tmp != null && tmp.containsKey(d2)) {
			return tmp.get(d2);
		}

		// compute the disjointness
		Boolean result;
		if (instanceBasedDisjoints) {
			result = isDisjointInstanceBased(d1, d2);
		} else {
			OWLClassExpression d = df.getOWLObjectIntersectionOf(d1, d2);
			result = reasoner.isSuperClassOf(df.getOWLNothing(), d);
		}
		// add the result to the cache (we add it twice such that
		// the order of access does not matter)

		// create new entries if necessary
		if (tmp == null)
			cachedDisjoints.put(d1, new TreeMap<>());
		if (!cachedDisjoints.containsKey(d2))
			cachedDisjoints.put(d2, new TreeMap<>());

		// add result symmetrically in the OWLClassExpression matrix
		cachedDisjoints.get(d1).put(d2, result);
		cachedDisjoints.get(d2).put(d1, result);
		//			System.out.println("---");
		return result;
	}

	private boolean isDisjointInstanceBased(OWLClassExpression d1, OWLClassExpression d2) {
		if(reasoner instanceof SPARQLReasoner) {
			SortedSet<OWLIndividual> individuals = reasoner.getIndividuals(df.getOWLObjectIntersectionOf(d1, d2));
			return individuals.isEmpty();
		} else {
			SortedSet<OWLIndividual> d1Instances = reasoner.getIndividuals(d1);
			SortedSet<OWLIndividual> d2Instances = reasoner.getIndividuals(d2);

			for(OWLIndividual d1Instance : d1Instances) {
				if(d2Instances.contains(d1Instance))
					return false;
			}
			return true;
		}
	}

	/*
	// computes whether two classes are disjoint; this should be computed
	// by the reasoner only ones and otherwise taken from a matrix
	private boolean isDisjoint(OWLClass a, OWLClassExpression d) {
		// we need to test whether A AND B is equivalent to BOTTOM
		Description d2 = new Intersection(a, d);
		return rs.subsumes(new Nothing(), d2);
	}*/

	// we need to test whether NOT A AND B is equivalent to BOTTOM
	@SuppressWarnings("unused")
	private boolean isNotADisjoint(OWLClass a, OWLClass b) {
//		Map<OWLClass,Boolean> tmp = notABDisjoint.get(a);
//		Boolean tmp2 = null;
//		if(tmp != null)
//			tmp2 = tmp.get(b);
//
//		if(tmp2==null) {
		OWLClassExpression notA = df.getOWLObjectComplementOf(a);
		OWLClassExpression d = df.getOWLObjectIntersectionOf(notA, b);
			Boolean result = reasoner.isSuperClassOf(df.getOWLNothing(), d);
			// ... add to cache ...
			return result;
//		} else
//			return tmp2;
	}

	// we need to test whether NOT A AND B = B
	// (if not then NOT A is not meaningful in the sense that it does
	// not semantically add anything to B)
	@SuppressWarnings("unused")
	private boolean isNotAMeaningful(OWLClass a, OWLClass b) {
		OWLClassExpression notA = df.getOWLObjectComplementOf(a);
		OWLClassExpression d = df.getOWLObjectIntersectionOf(notA, b);
		// check b subClassOf b AND NOT A (if yes then it is not meaningful)
		return !reasoner.isSuperClassOf(d, b);
	}

	public int getFrequencyThreshold() {
		return frequencyThreshold;
	}

	public void setFrequencyThreshold(int frequencyThreshold) {
		this.frequencyThreshold = frequencyThreshold;
	}

	public boolean isUseDataHasValueConstructor() {
		return useDataHasValueConstructor;
	}

	public void setUseDataHasValueConstructor(boolean useDataHasValueConstructor) {
		isFinal();
		this.useDataHasValueConstructor = useDataHasValueConstructor;
	}

	public boolean isApplyAllFilter() {
		return applyAllFilter;
	}

	public void setApplyAllFilter(boolean applyAllFilter) {
		this.applyAllFilter = applyAllFilter;
	}

	public boolean isApplyExistsFilter() {
		return applyExistsFilter;
	}

	public void setApplyExistsFilter(boolean applyExistsFilter) {
		this.applyExistsFilter = applyExistsFilter;
	}

	public boolean isUseAllConstructor() {
		return useAllConstructor;
	}

	public void setUseAllConstructor(boolean useAllConstructor) {
		this.useAllConstructor = useAllConstructor;
	}

	public boolean isUseExistsConstructor() {
		return useExistsConstructor;
	}

	public void setUseExistsConstructor(boolean useExistsConstructor) {
		this.useExistsConstructor = useExistsConstructor;
	}

	public boolean isUseHasValueConstructor() {
		return useHasValueConstructor;
	}

	public void setUseHasValueConstructor(boolean useHasValueConstructor) {
		isFinal();
		this.useHasValueConstructor = useHasValueConstructor;
	}

	public boolean isUseCardinalityRestrictions() {
		return useCardinalityRestrictions;
	}

	public void setUseCardinalityRestrictions(boolean useCardinalityRestrictions) {
		isFinal();
		this.useCardinalityRestrictions = useCardinalityRestrictions;
	}

	public boolean isUseHasSelf() {
		return useHasSelf;
	}

	public void setUseHasSelf(boolean useHasSelf) {
		this.useHasSelf = useHasSelf;
	}

	public boolean isUseNegation() {
		return useNegation;
	}

	public void setUseNegation(boolean useNegation) {
		this.useNegation = useNegation;
	}

	public boolean isUseBooleanDatatypes() {
		return useBooleanDatatypes;
	}

	public void setUseBooleanDatatypes(boolean useBooleanDatatypes) {
		this.useBooleanDatatypes = useBooleanDatatypes;
	}

	public boolean isUseStringDatatypes() {
		return useStringDatatypes;
	}

	public void setUseStringDatatypes(boolean useStringDatatypes) {
		this.useStringDatatypes = useStringDatatypes;
	}

	public boolean isInstanceBasedDisjoints() {
		return instanceBasedDisjoints;
	}

	public void setInstanceBasedDisjoints(boolean instanceBasedDisjoints) {
		this.instanceBasedDisjoints = instanceBasedDisjoints;
	}

	public AbstractReasonerComponent getReasoner() {
		return reasoner;
	}

    @Autowired
	public void setReasoner(AbstractReasonerComponent reasoner) {
		this.reasoner = reasoner;
	}

	public ClassHierarchy getSubHierarchy() {
		return classHierarchy;
	}

	public void setSubHierarchy(ClassHierarchy subHierarchy) {
		this.classHierarchy = subHierarchy;
	}

	public OWLClassExpression getStartClass() {
		return startClass;
	}

	@Override
	public void setStartClass(OWLClassExpression startClass) {
		this.startClass = startClass;
	}

	public int getCardinalityLimit() {
		return cardinalityLimit;
	}

	public void setCardinalityLimit(int cardinalityLimit) {
		this.cardinalityLimit = cardinalityLimit;
	}

	public ObjectPropertyHierarchy getObjectPropertyHierarchy() {
		return objectPropertyHierarchy;
	}

	@Override
    public void setObjectPropertyHierarchy(ObjectPropertyHierarchy objectPropertyHierarchy) {
		this.objectPropertyHierarchy = objectPropertyHierarchy;
	}

	public DatatypePropertyHierarchy getDataPropertyHierarchy() {
		return dataPropertyHierarchy;
	}

	@Override
    public void setDataPropertyHierarchy(DatatypePropertyHierarchy dataPropertyHierarchy) {
		this.dataPropertyHierarchy = dataPropertyHierarchy;
	}

	@Override
	public void setReasoner(Reasoner reasoner) {
		this.reasoner = (AbstractReasonerComponent) reasoner;
	}

	@Override @NoConfigOption
	public void setClassHierarchy(ClassHierarchy classHierarchy) {
		this.classHierarchy = classHierarchy;
	}

	/**
	 * @param useObjectValueNegation the useObjectValueNegation to set
	 */
	public void setUseObjectValueNegation(boolean useObjectValueNegation) {
		this.useObjectValueNegation = useObjectValueNegation;
	}

	public boolean isUseNumericDatatypes() {
		return useNumericDatatypes;
	}

	public void setUseNumericDatatypes(boolean useNumericDatatypes) {
		isFinal();
		this.useNumericDatatypes = useNumericDatatypes;
	}

	/**
	 * @param useInverse whether to use inverse properties in property restrictions
	 */
	public void setUseInverse(boolean useInverse) {
		isFinal();
		this.useInverse = useInverse;
	}

	/**
	 * @param useSomeOnly whether to allow universal restrictions on a property r only if there exists already
	 * a existential restriction on the same property in an intersection
	 */
	public void setUseSomeOnly(boolean useSomeOnly) {
		this.useSomeOnly = useSomeOnly;
	}

	/**
	 * @param useTimeDatatypes whether to use data/time literal restrictions
	 */
	public void setUseTimeDatatypes(boolean useTimeDatatypes) {
		isFinal();
		this.useTimeDatatypes = useTimeDatatypes;
	}

	public int getMaxNrOfSplits() {
		return maxNrOfSplits;
	}

	public void setMaxNrOfSplits(int maxNrOfSplits) {
		this.maxNrOfSplits = maxNrOfSplits;
	}

	public boolean isDisjointChecks() {
		return disjointChecks;
	}

	public void setDisjointChecks(boolean disjointChecks) {
		this.disjointChecks = disjointChecks;
	}

	@Override
	public OWLClassExpressionLengthMetric getLengthMetric() {
		return lengthMetric;
	}

	@Override
	public void setLengthMetric(OWLClassExpressionLengthMetric lengthMetric) {
		this.lengthMetric = lengthMetric;

		mMaxLength = max (
				lengthMetric.classLength,
				lengthMetric.objectComplementLength + lengthMetric.classLength,
				lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength,
				lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength + lengthMetric.objectInverseLength,
				lengthMetric.objectAllValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength,
				lengthMetric.objectAllValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength + lengthMetric.objectInverseLength,
				lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength,
				lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1,
				lengthMetric.objectCardinalityLength + lengthMetric.objectProperyLength + lengthMetric.classLength);

		logger.debug("mMaxLength = " + mMaxLength);
	}
	
}