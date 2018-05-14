//package org.dllearner.refinementoperators;
//
//import static java.util.stream.Collectors.summingInt;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.SortedSet;
//import java.util.TreeMap;
//import java.util.TreeSet;
//import java.util.Map.Entry;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import org.dllearner.core.AbstractReasonerComponent;
//import org.dllearner.core.AnnComponentManager;
//import org.dllearner.core.ComponentAnn;
//import org.dllearner.core.ComponentInitException;
//import org.dllearner.core.annotations.NoConfigOption;
//import org.dllearner.core.config.ConfigOption;
//import org.dllearner.core.options.CommonConfigOptions;
//import org.dllearner.core.owl.ClassHierarchy;
//import org.dllearner.core.owl.DatatypePropertyHierarchy;
//import org.dllearner.core.owl.OWLObjectIntersectionOfImplExt;
//import org.dllearner.core.owl.OWLObjectUnionOfImplExt;
//import org.dllearner.core.owl.ObjectPropertyHierarchy;
//import org.dllearner.reasoning.SPARQLReasoner;
//import org.dllearner.utilities.OWLAPIUtils;
//import org.dllearner.utilities.owl.ConceptTransformation;
//import org.dllearner.utilities.owl.OWLClassExpressionLengthMetric;
//import org.dllearner.utilities.owl.OWLClassExpressionUtils;
//import org.dllearner.utilities.split.DefaultDateTimeValuesSplitter;
//import org.dllearner.utilities.split.DefaultNumericValuesSplitter;
//import org.dllearner.utilities.split.ValuesSplitter;
//import org.semanticweb.owlapi.model.IRI;
//import org.semanticweb.owlapi.model.OWLAnnotation;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLDataHasValue;
//import org.semanticweb.owlapi.model.OWLDataProperty;
//import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
//import org.semanticweb.owlapi.model.OWLDataRange;
//import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
//import org.semanticweb.owlapi.model.OWLDatatype;
//import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
//import org.semanticweb.owlapi.model.OWLFacetRestriction;
//import org.semanticweb.owlapi.model.OWLIndividual;
//import org.semanticweb.owlapi.model.OWLLiteral;
//import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
//import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
//import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
//import org.semanticweb.owlapi.model.OWLObjectComplementOf;
//import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
//import org.semanticweb.owlapi.model.OWLObjectHasValue;
//import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
//import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
//import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
//import org.semanticweb.owlapi.model.OWLObjectProperty;
//import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
//import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
//import org.semanticweb.owlapi.model.OWLObjectUnionOf;
//import org.semanticweb.owlapi.vocab.OWLFacet;
//
//import com.google.common.collect.HashMultimap;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Multimap;
//
//@ComponentAnn(name = "rho refinement operator aml", shortName = "rhoAML", version = 0.8)
//public class RhoDRDownAML extends RhoDRDown{
//	
//	/**
//	 * @Hua: change following config parameters to protected for AML operator
//	 */
//	@ConfigOption(description = "the reasoner to use")	
//	protected AbstractReasonerComponent reasoner;
//
//
//	// limit for cardinality restrictions (this makes sense if we e.g. have compounds with up to
//	// more than 200 atoms but we are only interested in atoms with certain characteristics and do
//	// not want something like e.g. >= 204 hasAtom.NOT Carbon-87; which blows up the search space
//	@ConfigOption(defaultValue = "5", description = "limit for cardinality restrictions (this makes sense if we e.g. have compounds with too many atoms)")
//	protected int cardinalityLimit = 5;
//
//	// start concept (can be used to start from an arbitrary concept, needs
//	// to be Thing or NamedClass), note that when you use e.g. Compound as
//	// start class, then the algorithm should start the search with class
//	// Compound (and not with Thing), because otherwise concepts like
//	// NOT Carbon-87 will be returned which itself is not a subclass of Compound
//	@ConfigOption(
//			defaultValue = "owl:Thing",
//			description = "You can specify a start class for the algorithm")
//	protected OWLClassExpression startClass = OWL_THING;
//
//	@ConfigOption(description = "the number of generated split intervals for numeric types", defaultValue = "12")
//	protected int maxNrOfSplits = 12;
//
//	// data structure for a simple frequent pattern matching preprocessing phase
//	@ConfigOption(defaultValue = "3", description = "minimum number an individual or literal has to be seen in the " +
//			"knowledge base before considering it for inclusion in concepts")
//	protected int frequencyThreshold = CommonConfigOptions.valueFrequencyThresholdDefault;
//
//	@ConfigOption(description = "whether to use hasValue on frequently occuring strings", defaultValue = "false")
//	protected boolean useDataHasValueConstructor = false;
//
//
//	@ConfigOption(defaultValue="true")
//	protected boolean applyAllFilter = true;
//
//	@ConfigOption(defaultValue="true", description = "throwing out all refinements with " +
//			"duplicate \u2203 r for any r")
//	protected boolean applyExistsFilter = true;
//
//	@ConfigOption(description="support of universal restrictions (owl:allValuesFrom), e.g. \u2200 r.C ", defaultValue="true")
//	protected boolean useAllConstructor = true;
//
//	@ConfigOption(description="support of existential restrictions (owl:someValuesFrom), e.g. \u2203 r.C ", defaultValue="true")
//	protected boolean useExistsConstructor = true;
//
//	@ConfigOption(description="support of has value constructor (owl:hasValue), e.g. \u2203 r.{a} ", defaultValue="false")
//	protected boolean useHasValueConstructor = false;
//
//	@ConfigOption(description="support of qualified cardinality restrictions (owl:minCardinality, owl:maxCardinality, owl:exactCardinality), e.g. \u2265 3 r.C ", defaultValue="true")
//	protected boolean useCardinalityRestrictions = true;
//
//	@ConfigOption(description="support of local reflexivity of an object property expression (owl:hasSelf), e.g. \u2203 loves.Self for a narcissistic", defaultValue="false")
//	protected boolean useHasSelf = false;
//
//	@ConfigOption(description="support of negation (owl:complementOf), e.g. \u00AC C ", defaultValue="true")
//	protected boolean useNegation = true;
//
//	@ConfigOption(description="support of inverse object properties (owl:inverseOf), e.g. r\u207B.C ", defaultValue="false")
//	protected boolean useInverse = false;
//
//	@ConfigOption(description="support of boolean datatypes (xsd:boolean), e.g. \u2203 r.{true} ", defaultValue="true")
//	protected boolean useBooleanDatatypes = true;
//
//	@ConfigOption(description="support of numeric datatypes (xsd:int, xsd:double, ...), e.g. \u2203 r.{true} ", defaultValue="true")
//	protected boolean useNumericDatatypes = true;
//
//	@ConfigOption(defaultValue="true")
//	protected boolean useTimeDatatypes = true;
//
//	@ConfigOption(description="support of string datatypes (xsd:string), e.g. \u2203 r.{\"SOME_STRING\"} ",defaultValue="false")
//	protected boolean useStringDatatypes = false;
//
//	/**
//	 * @Hua: changed to protected for AML operator 
//	 */
//	@ConfigOption(defaultValue="true", description = "skip combination of intersection between disjoint classes")	
//	protected boolean disjointChecks = true;
//
//	@ConfigOption(defaultValue="true")
//	protected boolean instanceBasedDisjoints = true;
//
//	@ConfigOption(defaultValue="false", description = "if enabled, generalise by removing parts of a disjunction")
//	protected boolean dropDisjuncts = false;
//
//	/**
//	 * @Hua: changed to protected for AML operator 
//	 */
//	@ConfigOption(description="universal restrictions on a property r are only used when there is already a cardinality and/or existential restriction on r",
//			defaultValue="true")
//	protected boolean useSomeOnly = true;
//
//	@ConfigOption(description = "whether to generate object complement while refining", defaultValue = "false")
//	protected boolean useObjectValueNegation = false;
//
//	@ConfigOption(description = "class expression length metric (should match learning algorithm usage)", defaultValue = "default cel_metric")
//	protected OWLClassExpressionLengthMetric lengthMetric = OWLClassExpressionLengthMetric.getDefaultMetric();
//	
//	
//	/**
//	 * @Hua: preprocess AML roles and interfaces
//	 */
//	SortedSet<OWLClassExpression> topAMLRoles = new TreeSet<OWLClassExpression>();
//	SortedSet<OWLClassExpression> topAMLInterfaces = new TreeSet<OWLClassExpression>();
//	SortedSet<OWLClassExpression> botAMLRoles = new TreeSet<OWLClassExpression>();
//	SortedSet<OWLClassExpression> botAMLInterfaces = new TreeSet<OWLClassExpression>();
//	
//	public RhoDRDownAML(){
//		System.out.println("initializing aml operator.");
//	}
//	
//	public RhoDRDownAML(RhoDRDownAML op) {
//		setApplyAllFilter(op.applyAllFilter);
//		setCardinalityLimit(op.cardinalityLimit);
//		setClassHierarchy(op.classHierarchy);
//		setDataPropertyHierarchy(op.dataPropertyHierarchy);
//		setDropDisjuncts(op.dropDisjuncts);
//		setFrequencyThreshold(op.frequencyThreshold);
//		setInstanceBasedDisjoints(op.instanceBasedDisjoints);
//		setObjectPropertyHierarchy(op.objectPropertyHierarchy);
//		setReasoner(op.reasoner);
//		setStartClass(op.startClass);
//		setSubHierarchy(op.classHierarchy);
//		setUseAllConstructor(op.useAllConstructor);
//		setUseBooleanDatatypes(op.useBooleanDatatypes);
//		setUseCardinalityRestrictions(op.useCardinalityRestrictions);
//		setUseDataHasValueConstructor(op.useDataHasValueConstructor);
//		setUseExistsConstructor(op.useExistsConstructor);
//		setUseHasValueConstructor(op.useHasValueConstructor);
//		setUseNegation(op.useNegation);
//		setUseObjectValueNegation(op.useObjectValueNegation);
//		setUseStringDatatypes(op.useStringDatatypes);
//		setUseNumericDatatypes(op.useNumericDatatypes);
//		initialized = false;
//	}
//	
//	public void init() throws ComponentInitException {
//		/*
//		if(initialized) {
//			throw new ComponentInitException("Refinement operator cannot be initialised twice.");
//		}
//		*/
//
//		if (classHierarchy == null) classHierarchy = reasoner.getClassHierarchy();
//		if (dataPropertyHierarchy == null) dataPropertyHierarchy = reasoner.getDatatypePropertyHierarchy();
//		if (objectPropertyHierarchy == null) objectPropertyHierarchy = reasoner.getObjectPropertyHierarchy();
//
//		logger.debug("classHierarchy: " + classHierarchy);
//		logger.debug("object properties: " + reasoner.getObjectProperties());
//
//		// query reasoner for domains and ranges
//		// (because they are used often in the operator)
//		opDomains = reasoner.getObjectPropertyDomains();
//		opRanges = reasoner.getObjectPropertyRanges();
//		dpDomains = reasoner.getDataPropertyDomains();
//
//		if (useHasValueConstructor) {
//			for (OWLObjectProperty op : objectPropertyHierarchy.getEntities()) {
//				// sets ordered by corresponding individual (which we ignore)
//				Map<OWLIndividual, SortedSet<OWLIndividual>> propertyMembers = reasoner.getPropertyMembers(op);
//
//				Collection<SortedSet<OWLIndividual>> fillerSets = propertyMembers.values();
//
//				// compute frequency of individuals used as object
//				Map<OWLIndividual, Integer> ind2Frequency = fillerSets.stream()
//						.flatMap(Collection::stream)
//						.collect(Collectors.groupingBy(Function.identity(), TreeMap::new, summingInt(s -> 1))); // (ind -> freqency)
//
//				// keep track of this
//				valueFrequency.put(op, ind2Frequency);
//
//				// keep only individuals with frequency > threshold
//				Set<OWLIndividual> frequentInds = ind2Frequency.entrySet().stream()
//						.filter(e -> e.getValue() >= frequencyThreshold) // frequency >= threshold
//						.map(Map.Entry::getKey)
//						.collect(Collectors.toCollection(TreeSet::new));
//				frequentValues.put(op, frequentInds);
//
//				if(useInverse) {
//					Map<OWLIndividual, Integer> opMap = new TreeMap<>();
//					valueFrequency.put(op.getInverseProperty(), opMap);
//
//					frequentInds = new TreeSet<>();
//
//					for (Entry<OWLIndividual, SortedSet<OWLIndividual>> entry : propertyMembers
//							.entrySet()) {
//						OWLIndividual subject = entry.getKey();
//						SortedSet<OWLIndividual> values = entry.getValue();
//
//						opMap.put(subject, values.size());
//
//						if (values.size() >= frequencyThreshold) {
//							frequentInds.add(subject);
//						}
//					}
//					frequentValues.put(op.getInverseProperty(), frequentInds);
//				}
//			}
//		}
//
//		if(useDataHasValueConstructor) {
//			for(OWLDataProperty dp : dataPropertyHierarchy.getEntities()) {
//				Map<OWLLiteral, Integer> dpMap = new TreeMap<>();
//				dataValueFrequency.put(dp, dpMap);
//
////				long s1 = System.currentTimeMillis();
////				ConcurrentMap<OWLLiteral, Integer> lit2frequency = reasoner.getDatatypeMembers(dp).values()
////						.parallelStream()
////						.map(set -> set.stream().collect(Collectors.toList()))
////						.flatMap(list -> list.stream())
////						.collect(Collectors.toConcurrentMap(
////								Function.identity(), lit -> 1, Integer::sum));
////				long s2 = System.currentTimeMillis();
////				System.out.println(s2 - s1);
//
//				// sets ordered by corresponding individual (which we ignore)
////				s1 = System.currentTimeMillis();
//				Collection<SortedSet<OWLLiteral>> fillerSets = reasoner.getDatatypeMembers(dp).values();
//				for(SortedSet<OWLLiteral> fillerSet : fillerSets) {
//					for(OWLLiteral lit : fillerSet) {
//						Integer frequency = dpMap.get(lit);
//
//						if(frequency != null) {
//							dpMap.put(lit, frequency+1);
//						} else {
//							dpMap.put(lit, 1);
//						}
//					}
//				}
////				s2 = System.currentTimeMillis();
////				System.out.println(s2 - s1);
//
//				// keep only frequent patterns
//				Set<OWLLiteral> frequentInds = new TreeSet<>();
//				for(OWLLiteral i : dpMap.keySet()) {
//					if(dpMap.get(i) >= frequencyThreshold) {
//						logger.trace("adding value "+i+", because "+dpMap.get(i) +">="+frequencyThreshold);
//						frequentInds.add(i);
//					}
//				}
//				frequentDataValues.put(dp, frequentInds);
//			}
//		}
//
//		// we do not need the temporary set anymore and let the
//		// garbage collector take care of it
//		valueFrequency = null;
//		dataValueFrequency.clear();// = null;
//
//		// compute splits for numeric data properties
//		if(useNumericDatatypes) {
//			if(reasoner instanceof SPARQLReasoner
//					&& !((SPARQLReasoner)reasoner).isUseGenericSplitsCode()) {
//				// TODO SPARQL support for splits
//				logger.warn("Numeric Facet restrictions are not (yet) implemented for " + AnnComponentManager.getName(reasoner) + ", option ignored");
//			} else {
//				ValuesSplitter splitter = new DefaultNumericValuesSplitter(reasoner, df, maxNrOfSplits);
//				splits.putAll(splitter.computeSplits());
//				if (logger.isDebugEnabled()) {
//					logger.debug( sparql_debug, "Numeric Splits: {}", splits);
//				}
//			}
//		}
//
//		// compute splits for time data properties
//		if (useTimeDatatypes) {
//			if(reasoner instanceof SPARQLReasoner
//					&& !((SPARQLReasoner)reasoner).isUseGenericSplitsCode()) {
//				// TODO SPARQL support for splits
//				logger.warn("Time based Facet restrictions are not (yet) implemented for " + AnnComponentManager.getName(reasoner) + ", option ignored");
//			} else {
//				ValuesSplitter splitter = new DefaultDateTimeValuesSplitter(reasoner, df, maxNrOfSplits);
//				splits.putAll(splitter.computeSplits());
//			}
//		}
//
//		// determine the maximum number of fillers for each role
//		// (up to a specified cardinality maximum)
//		if(useCardinalityRestrictions) {
//			if(reasoner instanceof SPARQLReasoner) {
//				logger.warn("Cardinality restrictions in Sparql not fully implemented, defaulting to 10.");
//			}
//			for(OWLObjectProperty op : objectPropertyHierarchy.getEntities()) {
//				if(reasoner instanceof SPARQLReasoner) {
//					// TODO SPARQL support for cardinalities
//					maxNrOfFillers.put(op, 10);
//				} else {
//					int maxFillers = Math.min(cardinalityLimit,
//							reasoner.getPropertyMembers(op).entrySet().stream()
//									.mapToInt(entry -> entry.getValue().size())
//									.max().orElse(0));
//					maxNrOfFillers.put(op, maxFillers);
//
////					int percentile95 = (int) new Percentile().evaluate(
////							reasoner.getPropertyMembers(op).entrySet().stream()
////							.mapToDouble(entry -> (double)entry.getValue().size())
////							.toArray(), 95);
////					System.out.println("Prop " + op);
////					System.out.println("max: " + maxFillers);
////					System.out.println("95th: " + percentile95);
//
//					// handle inverse properties
//					if(useInverse) {
//						maxFillers = 0;
//	
//						Multimap<OWLIndividual, OWLIndividual> map = HashMultimap.create();
//	
//						for (Entry<OWLIndividual, SortedSet<OWLIndividual>> entry : reasoner.getPropertyMembers(op).entrySet()) {
//							OWLIndividual subject = entry.getKey();
//							SortedSet<OWLIndividual> objects = entry.getValue();
//	
//							for (OWLIndividual obj : objects) {
//								map.put(obj, subject);
//							}
//						}
//	
//						for (Entry<OWLIndividual, Collection<OWLIndividual>> entry : map.asMap().entrySet()) {
//							Collection<OWLIndividual> inds = entry.getValue();
//							if (inds.size() > maxFillers)
//								maxFillers = inds.size();
//							if (maxFillers >= cardinalityLimit) {
//								maxFillers = cardinalityLimit;
//								break;
//							}
//						}
//						maxNrOfFillers.put(op.getInverseProperty(), maxFillers);
//					}
//				}
//			}
//		}
//
//		startClass = OWLAPIUtils.classExpressionPropertyExpanderChecked(startClass, reasoner, df, logger);
//
//		if(classHierarchy == null) {
//			classHierarchy = reasoner.getClassHierarchy();
//		}
//		if(objectPropertyHierarchy == null) {
//			objectPropertyHierarchy = reasoner.getObjectPropertyHierarchy();
//		}
//		if(dataPropertyHierarchy == null) {
//			dataPropertyHierarchy = reasoner.getDatatypePropertyHierarchy();
//		}			
//		
//		initialized = true;		
//		
//		SortedSet<OWLClassExpression> topConcepts = getClassCandidates(df.getOWLThing());
//		SortedSet<OWLClassExpression> botConcepts = classHierarchy.getSuperClasses(df.getOWLNothing(), true);
//		for(OWLClassExpression concept : topConcepts) {
//			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//     			if (ap.getValue() instanceof OWLLiteral) {
//                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
//                     if (val.getLiteral().equals("RoleClass")) {
//                    	 	topAMLRoles.add(concept);
//                     }
//                 }
//     		}       
//		}		
//		for(OWLClassExpression concept : topConcepts) {
//			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//     			if (ap.getValue() instanceof OWLLiteral) {
//                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
//                     if (val.getLiteral().equals("InterfaceClass")) {
//                    	 	topAMLInterfaces.add(concept);
//                     }
//                 }
//     		}       
//		}
//				
//		for(OWLClassExpression concept : botConcepts) {
//			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//     			if (ap.getValue() instanceof OWLLiteral) {
//                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
//                     if (val.getLiteral().equals("RoleClass")) {
//                    	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//                    	 	botAMLRoles.add(negatedCandidate);
//                     }
//                 }
//     		}       
//		}
//		for(OWLClassExpression concept : botConcepts) {
//			for(OWLAnnotation ap : reasoner.getAnnotations(concept)) {
//     			if (ap.getValue() instanceof OWLLiteral) {
//                     OWLLiteral val = (OWLLiteral) ap.getValue();                        
//                     if (val.getLiteral().equals("InterfaceClass")) {
//                    	 	OWLObjectComplementOf negatedCandidate = df.getOWLObjectComplementOf(concept);
//                    	 	botAMLInterfaces.add(negatedCandidate);
//                     }
//                 }
//     		}       
//		}
//	}
//	
//	public Set<OWLClassExpression> refine(OWLClassExpression description, int maxLength,
//			List<OWLClassExpression> knownRefinements, OWLClassExpression currDomain) {	
//		
////		if(!currDomain.isOWLThing())
////			System.out.println("|- " + description + " " + currDomain + " " + maxLength);
////		System.out.println("description [" + description + "] with domain [" + currDomain + "] " + maxLength);
//
//		// actions needing to be performed if this is the first time the
//		// current domain is used
//		if(!currDomain.isOWLThing() && !topARefinementsLength.containsKey(currDomain)){
//			topARefinementsLength.put(currDomain, 0);
//		}
//
//		// check whether using list or set makes more sense
//		// here; and whether HashSet or TreeSet should be used
//		// => TreeSet because duplicates are possible
//		Set<OWLClassExpression> refinements = new TreeSet<>();
//
//		// used as temporary variable
//		Set<OWLClassExpression> tmp;
//
//		if(description.isOWLThing()) {
//			/**
//			 * @Hua: escape refinements of role and interfaces
//			 * 	- we can test with roles and interfaces which are not of type AutomationMLBaseRole/AutomationMLBaseInterface
//			 */
//			// extends top refinements if necessary
//			if(currDomain.isOWLThing()) {
//				if(maxLength>topRefinementsLength)
//					computeTopRefinements(maxLength);
//				refinements = (TreeSet<OWLClassExpression>) topRefinementsCumulative.get(maxLength).clone();
//			} else {
//				if(maxLength>topARefinementsLength.get(currDomain)) {
//					computeTopRefinements(maxLength, currDomain);
//				}
//				refinements = (TreeSet<OWLClassExpression>) topARefinementsCumulative.get(currDomain).get(maxLength).clone();
//			}
////			refinements.addAll(classHierarchy.getMoreSpecialConcepts(description));
//		} else if(description.isOWLNothing()) {
//			// cannot be further refined
//		} else if(!description.isAnonymous()) {
//			refinements.addAll(classHierarchy.getSubClasses(description, true));
//			refinements.remove(df.getOWLNothing());
//		} else if (description instanceof OWLObjectComplementOf) {
//			OWLClassExpression operand = ((OWLObjectComplementOf) description).getOperand();
//			if(!operand.isAnonymous()){
//				tmp = classHierarchy.getSuperClasses(operand, true);
//
//				for(OWLClassExpression c : tmp) {
//					if(!c.isOWLThing()){
//						refinements.add(df.getOWLObjectComplementOf(c));
//					}
//				}
//			}
//		} else if (description instanceof OWLObjectIntersectionOf) {
//			List<OWLClassExpression> operands = ((OWLObjectIntersectionOf) description).getOperandsAsList();
//			// refine one of the elements
//			for(OWLClassExpression child : operands) {
//				// refine the child; the new max length is the current max length minus
//				// the currently considered concept plus the length of the child
//				// TODO: add better explanation
//				int length = OWLClassExpressionUtils.getLength(description, lengthMetric);
//				int childLength = OWLClassExpressionUtils.getLength(child, lengthMetric);
//				tmp = refine(child, maxLength - length + childLength, null, currDomain);
//
//				// create new intersection
//				for(OWLClassExpression c : tmp) {
//					if(!useSomeOnly || isCombinable(description, c)) {
//						List<OWLClassExpression> newChildren = new ArrayList<>(operands);
//						newChildren.add(c);
//						newChildren.remove(child);
//						Collections.sort(newChildren);
//						OWLClassExpression mc = new OWLObjectIntersectionOfImplExt(newChildren);
//
//						// clean concept and transform it to ordered negation normal form
//						// (non-recursive variant because only depth 1 was modified)
//						mc = ConceptTransformation.cleanConceptNonRecursive(mc);
//						mc = ConceptTransformation.nnf(mc);
//
//						// check whether the intersection is OK (sanity checks), then add it
//						if(checkIntersection((OWLObjectIntersectionOf) mc))
//							refinements.add(mc);
//					}
//				}
//
//			}
//
//		} else if (description instanceof OWLObjectUnionOf) {
//			// refine one of the elements
//			List<OWLClassExpression> operands = ((OWLObjectUnionOf) description).getOperandsAsList();
//			for(OWLClassExpression child : operands) {
////				System.out.println("union child: " + child + " " + maxLength + " " + description.getLength() + " " + child.getLength());
//
//				// refine child
//				int length = OWLClassExpressionUtils.getLength(description, lengthMetric);
//				int childLength = OWLClassExpressionUtils.getLength(child, lengthMetric);
//				tmp = refine(child, maxLength - length + childLength, null, currDomain);
//
//				// construct union (see above)
//				for(OWLClassExpression c : tmp) {
//					List<OWLClassExpression> newChildren = new ArrayList<>(operands);
//					newChildren.remove(child);
//					newChildren.add(c);
//					Collections.sort(newChildren);
//					OWLClassExpression md = new OWLObjectUnionOfImplExt(newChildren);
//
//					// transform to ordered negation normal form
//					md = ConceptTransformation.nnf(md);
//					// note that we do not have to call clean here because a disjunction will
//					// never be nested in another disjunction in this operator
//
//					refinements.add(md);
//				}
//			}
//
//			// if enabled, we can remove elements of the disjunction
//			if(dropDisjuncts) {
//				// A1 OR A2 => {A1,A2}
//				if(operands.size() == 2) {
//					refinements.add(operands.get(0));
//					refinements.add(operands.get(1));
//				} else {
//					// copy children list and remove a different element in each turn
//					for(int i=0; i<operands.size(); i++) {
//						List<OWLClassExpression> newChildren = new LinkedList<>(operands);
//						newChildren.remove(i);
//						OWLObjectUnionOf md = new OWLObjectUnionOfImplExt(newChildren);
//						refinements.add(md);
//					}
//				}
//			}
//
//		} else if (description instanceof OWLObjectSomeValuesFrom) {
//			OWLObjectPropertyExpression role = ((OWLObjectSomeValuesFrom) description).getProperty();
//			OWLClassExpression filler = ((OWLObjectSomeValuesFrom) description).getFiller();
//
////			System.out.println("refining: [" + description + "] with filler [" + filler + "]");
//			/**
//			 * @Hua: extend object property refinements
//			 *   - if filler is thing:
//			 *   	- if role = hasIE: use role class to refine instead of checking range
//			 *   	- if role = hasEI: use interface class to refine instead of checking range
//			 *   - else: as now, since we have already chosen one role/interface and we want to specialize it
//			 * we can test with sucs with multiple IEs of specific role classes: hasIE.Robot and hasIE.IO and hasIE.XX ....
//			 */		
//
//			if(role.toString().equals("internalElement")) {
//				OWLClassExpression ie = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#IE"));
//				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, ie);
//			}
//			else if(role.toString().equals("externalInterface")) {
//				OWLClassExpression ei = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#EI"));
//				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, ei);		
//			}
//			else if(role.toString().equals("links")) {
//				OWLClassExpression li = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#LI"));
//				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, li);		
//			}
//			else {
//				OWLClassExpression domain = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
//				// rule 1: EXISTS r.D => EXISTS r.E
//				tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, domain);	
//			}
//						
//			for(OWLClassExpression c : tmp){
//				refinements.add(df.getOWLObjectSomeValuesFrom(role, c));
//			}
//
//			// rule 2: EXISTS r.D => EXISTS s.D or EXISTS r^-1.D => EXISTS s^-1.D
//			Set<OWLObjectProperty> moreSpecialRoles = objectPropertyHierarchy.getMoreSpecialRoles(role.getNamedProperty());
//
//			for (OWLObjectProperty moreSpecialRole : moreSpecialRoles) {
//				refinements.add(df.getOWLObjectSomeValuesFrom(moreSpecialRole, filler));
//			}
//
//			// rule 3: EXISTS r.D => >= 2 r.D
//			// (length increases by 1 so we have to check whether max length is sufficient)
//			if(useCardinalityRestrictions) {// && !role.isAnonymous()) {
//				if(maxLength > OWLClassExpressionUtils.getLength(description, lengthMetric) && maxNrOfFillers.get(role) > 1) {
//					OWLObjectMinCardinality min = df.getOWLObjectMinCardinality(2,role,filler);
//					refinements.add(min);
//				}
//			}
//
//			// rule 4: EXISTS r.TOP => EXISTS r.{value}
//			if(useHasValueConstructor && filler.isOWLThing()){ // && !role.isAnonymous()) {
//				// watch out for frequent patterns
//				Set<OWLIndividual> frequentInds = frequentValues.get(role);
//				if(frequentInds != null) {
//					for(OWLIndividual ind : frequentInds) {
//						OWLObjectHasValue ovr = df.getOWLObjectHasValue(role, ind);
//						refinements.add(ovr);
//						if(useObjectValueNegation ){
//							refinements.add(df.getOWLObjectComplementOf(ovr));
//						}
//
//					}
//				}
//			}
//
//		} else if (description instanceof OWLObjectAllValuesFrom) {
//			refinements.addAll(refine((OWLObjectAllValuesFrom) description, maxLength));
//		} else if (description instanceof OWLObjectCardinalityRestriction) {
//			OWLObjectPropertyExpression role = ((OWLObjectCardinalityRestriction) description).getProperty();
//			OWLClassExpression filler = ((OWLObjectCardinalityRestriction) description).getFiller();
//			OWLClassExpression range = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
//			int cardinality = ((OWLObjectCardinalityRestriction) description).getCardinality();
//			if(description instanceof OWLObjectMaxCardinality) {
//				// rule 1: <= x r.C =>  <= x r.D
//				if(useNegation || cardinality > 0){
//					tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);
//
//					for(OWLClassExpression d : tmp) {
//						refinements.add(df.getOWLObjectMaxCardinality(cardinality,role,d));
//					}
//				}
//
//				// rule 2: <= x r.C  =>  <= (x-1) r.C
////				int number = max.getNumber();
//				if((useNegation && cardinality > 1) || (!useNegation && cardinality > 2)){
//					refinements.add(df.getOWLObjectMaxCardinality(cardinality-1,role,filler));
//				}
//
//			} else if(description instanceof OWLObjectMinCardinality) {
//				tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);
//
//				for(OWLClassExpression d : tmp) {
//					refinements.add(df.getOWLObjectMinCardinality(cardinality,role,d));
//				}
//
//				// >= x r.C  =>  >= (x+1) r.C
////				int number = min.getNumber();
//				if(cardinality < maxNrOfFillers.get(role)){
//					refinements.add(df.getOWLObjectMinCardinality(cardinality+1,role,filler));
//				}
//			} else if(description instanceof OWLObjectExactCardinality) {
//				tmp = refine(filler, maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength, null, range);
//
//				for(OWLClassExpression d : tmp) {
//					refinements.add(df.getOWLObjectExactCardinality(cardinality,role,d));
//				}
//
//				// >= x r.C  =>  >= (x+1) r.C
////				int number = min.getNumber();
//				if(cardinality < maxNrOfFillers.get(role)){
//					refinements.add(df.getOWLObjectExactCardinality(cardinality+1,role,filler));
//				}
//			}
//		} else if (description instanceof OWLDataSomeValuesFrom) {
//			OWLDataProperty dp = ((OWLDataSomeValuesFrom) description).getProperty().asOWLDataProperty();
//			OWLDataRange dr = ((OWLDataSomeValuesFrom) description).getFiller();
//			if(dr instanceof OWLDatatypeRestriction){
//				OWLDatatype datatype = ((OWLDatatypeRestriction) dr).getDatatype();
//				Set<OWLFacetRestriction> facetRestrictions = ((OWLDatatypeRestriction) dr).getFacetRestrictions();
//
//				OWLDatatypeRestriction newDatatypeRestriction = null;
//				if(OWLAPIUtils.isNumericDatatype(datatype) || OWLAPIUtils.dtDatatypes.contains(datatype)){
//					for (OWLFacetRestriction facetRestriction : facetRestrictions) {
//						OWLFacet facet = facetRestriction.getFacet();
//
//						OWLLiteral value =  facetRestriction.getFacetValue();
//
//						if(facet == OWLFacet.MAX_INCLUSIVE){
//							// find out which split value was used
//							int splitIndex = splits.get(dp).lastIndexOf(value);
//							if(splitIndex == -1)
//								throw new Error("split error");
//							int newSplitIndex = splitIndex - 1;
//							if(newSplitIndex >= 0) {
//								OWLLiteral newValue = splits.get(dp).get(newSplitIndex);
//								newDatatypeRestriction = asDatatypeRestriction(dp, newValue, facet);
//							}
//						} else if(facet == OWLFacet.MIN_INCLUSIVE){
//							// find out which split value was used
//							int splitIndex = splits.get(dp).lastIndexOf(value);
//							if(splitIndex == -1)
//								throw new Error("split error");
//							int newSplitIndex = splitIndex + 1;
//							if(newSplitIndex < splits.get(dp).size()) {
//								OWLLiteral newValue = splits.get(dp).get(newSplitIndex);
//								newDatatypeRestriction = asDatatypeRestriction(dp, newValue, facet);
//							}
//						}
//					}
//				}
//				if(newDatatypeRestriction != null){
//					refinements.add(df.getOWLDataSomeValuesFrom(dp, newDatatypeRestriction));
//				}
//			}
//
//		} else if (description instanceof OWLDataHasValue) {
//			OWLDataPropertyExpression dp = ((OWLDataHasValue) description).getProperty();
//			OWLLiteral value = ((OWLDataHasValue) description).getFiller();
//
//			if(!dp.isAnonymous()){
//				Set<OWLDataProperty> subDPs = dataPropertyHierarchy.getMoreSpecialRoles(dp.asOWLDataProperty());
//				for(OWLDataProperty subDP : subDPs) {
//					refinements.add(df.getOWLDataHasValue(subDP, value));
//				}
//			}
//		}
//
//		// if a refinement is not Bottom, Top, ALL r.Bottom a refinement of top can be appended
//		if(!description.isOWLThing() && !description.isOWLNothing()
//				&& !(description instanceof OWLObjectAllValuesFrom && ((OWLObjectAllValuesFrom)description).getFiller().isOWLNothing())) {
//			// -1 because of the AND symbol which is appended
//			int topRefLength = maxLength - OWLClassExpressionUtils.getLength(description, lengthMetric) - 1;
//
//			// maybe we have to compute new top refinements here
//			if(currDomain.isOWLThing()) {
//				if(topRefLength > topRefinementsLength)
//					computeTopRefinements(topRefLength);
//			} else if(topRefLength > topARefinementsLength.get(currDomain))
//				computeTopRefinements(topRefLength, currDomain);
//
//			if(topRefLength>0) {
//				Set<OWLClassExpression> topRefs;
//				if(currDomain.isOWLThing())
//					topRefs = topRefinementsCumulative.get(topRefLength);
//				else
//					topRefs = topARefinementsCumulative.get(currDomain).get(topRefLength);
//
//				for(OWLClassExpression c : topRefs) {
//					// true if refinement should be skipped due to filters,
//					// false otherwise
//					boolean skip = false;
//
//					// if a refinement of of the form ALL r, we check whether ALL r
//					// does not occur already
//					if(applyAllFilter) {
//						if(c instanceof OWLObjectAllValuesFrom) {
//							if(description instanceof OWLNaryBooleanClassExpression){
//								for(OWLClassExpression child : ((OWLNaryBooleanClassExpression) description).getOperands()) {
//									if(child instanceof OWLObjectAllValuesFrom) {
//										OWLObjectPropertyExpression r1 = ((OWLObjectAllValuesFrom) c).getProperty();
//										OWLObjectPropertyExpression r2 = ((OWLObjectAllValuesFrom) child).getProperty();
//										if(r1.equals(r2)){
//											skip = true;
//											break;
//										}
//									}
//								}
//							}
//						}
//					}
//
//					// we only add \forall r.C to an intersection if there is
//					// already some existential restriction \exists r.C
//					if(useSomeOnly) {
//						skip = !isCombinable(description, c);
//					}
//
//					// check for double datatype properties
//					/*
//					if(c instanceof DatatypeSomeRestriction &&
//							description instanceof DatatypeSomeRestriction) {
//						DataRange dr = ((DatatypeSomeRestriction)c).getDataRange();
//						DataRange dr2 = ((DatatypeSomeRestriction)description).getDataRange();
//						// it does not make sense to have statements like height >= 1.8 AND height >= 1.7
//						if((dr instanceof DoubleMaxValue && dr2 instanceof DoubleMaxValue)
//							||(dr instanceof DoubleMinValue && dr2 instanceof DoubleMinValue))
//							skip = true;
//					}*/
//
//					// perform a disjointness check when named classes are added;
//					// this can avoid a lot of superfluous computation in the algorithm e.g.
//					// when A1 looks good, so many refinements of the form (A1 OR (A2 AND A3))
//					// are generated which are all equal to A1 due to disjointness of A2 and A3
//					if(disjointChecks && !c.isAnonymous() && !description.isAnonymous() && isDisjoint(description, c)) {
//						skip = true;
////						System.out.println(c + " ignored when refining " + description);
//					}
//
//					if(!skip) {
//						List<OWLClassExpression> operands = Lists.newArrayList(description, c);
//						Collections.sort(operands);
//						OWLObjectIntersectionOf mc = new OWLObjectIntersectionOfImplExt(operands);
//
//						// clean and transform to ordered negation normal form
//						mc = (OWLObjectIntersectionOf) ConceptTransformation.cleanConceptNonRecursive(mc);
//						mc = (OWLObjectIntersectionOf) ConceptTransformation.nnf(mc);
//
//						// last check before intersection is added
//						if(checkIntersection(mc))
//							refinements.add(mc);
//					}
//				}
//			}
//		}
//		return refinements;
//	}
//	
//	
//	protected Set<OWLClassExpression> refine(OWLObjectAllValuesFrom ce, int maxLength) {
//		Set<OWLClassExpression> refinements = new HashSet<>();
//
//		OWLObjectPropertyExpression role = ce.getProperty();
//		OWLClassExpression filler = ce.getFiller();		
//
//		/**
//		 * @Hua: extend the rule 1 
//		 */
//		Set<OWLClassExpression> tmp;
//		if(role.toString().equals("internalElement") ) {
//			OWLClassExpression ie = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#IE"));
//			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, ie);			
//		}
//		else if(role.toString().equals("externalInterface")) {
//			OWLClassExpression ei = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#EI"));
//			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, ei);		
//		}
//		else if(role.toString().equals("links")) {
//			OWLClassExpression li = df.getOWLClass(IRI.create("http://www.ipr.kit.edu/aml_importer#LI"));
//			tmp = refine(filler, maxLength-lengthMetric.objectSomeValuesLength-lengthMetric.objectProperyLength, null, li);		
//		}
//		else {
//			// rule 1: ALL r.D => ALL r.E
//			OWLClassExpression range = role.isAnonymous() ? opDomains.get(role.getNamedProperty()) : opRanges.get(role);
//			tmp = refine(filler, maxLength-lengthMetric.objectAllValuesLength-lengthMetric.objectProperyLength, null, range);
//		}
//	
//		for(OWLClassExpression c : tmp) {
//			refinements.add(df.getOWLObjectAllValuesFrom(role, c));
//		}
//
//		// rule 2: ALL r.D => ALL r.BOTTOM if D is a most specific atomic concept
//		if(!filler.isOWLNothing() && !filler.isAnonymous() && tmp.size()==0) {
//			refinements.add(df.getOWLObjectAllValuesFrom(role, df.getOWLNothing()));
//		}
//
//		// rule 3: ALL r.D => ALL s.D or ALL r^-1.D => ALL s^-1.D
//		Set<OWLObjectProperty> subProperties = objectPropertyHierarchy.getMoreSpecialRoles(role.getNamedProperty());
//
//		for (OWLObjectProperty subProperty : subProperties) {
//			refinements.add(df.getOWLObjectAllValuesFrom(subProperty, filler));
//		}
//
//		// rule 4: ALL r.D => <= (maxFillers-1) r.D
//		// (length increases by 1 so we have to check whether max length is sufficient)
//		// => commented out because this is actually not a downward refinement
////		if(useCardinalityRestrictions) {
////			if(maxLength > ce.getLength() && maxNrOfFillers.get(ar)>1) {
////				ObjectMaxCardinalityRestriction max = new ObjectMaxCardinalityRestriction(maxNrOfFillers.get(ar)-1,role,description.getChild(0));
////				refinements.add(max);
////			}
////		}
//
//		return refinements;
//	}
//	
//	protected void computeM(OWLClassExpression nc) {
//		long mComputationTimeStartNs = System.nanoTime();
//
//		mA.put(nc, new TreeMap<>());
//		// initialise all possible lengths (1 to mMaxLength)
//		for(int i=1; i<=mMaxLength; i++) {
//			mA.get(nc).put(i, new TreeSet<>());
//		}
//		
//		/**
//		 * @Hua: extended top level concept
//		 */
//		if(aml && nc.toString().equals("IE")) {
//			mA.get(nc).get(lengthMetric.classLength).addAll(topAMLRoles);
//		}
//		else if(aml && (nc.toString().equals("EI") || nc.toString().equals("IL"))) {
//			mA.get(nc).get(lengthMetric.classLength).addAll(topAMLInterfaces);
//		}
//		else {
//			/**
//			 * @Hua: original M_A for top level concepts
//			 */
//			// most general classes, which are not disjoint with nc and provide real refinement
//			SortedSet<OWLClassExpression> m1 = getClassCandidates(nc);
//			mA.get(nc).get(lengthMetric.classLength).addAll(m1);
//		}
//		
//
//		/**
//		 * @Hua: extended negated bottom concepts
//		 */
//		// most specific negated classes, which are not disjoint with nc
//		if(useNegation) {
//			SortedSet<OWLClassExpression> m2;
//			/**
//			 * Extend
//			 */
//			if(nc.toString().equals("IE")) {				
//				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(botAMLRoles);
//			}
//			else if(nc.toString().equals("EI") || nc.toString().equals("IL")) {					
//				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(botAMLInterfaces);
//			}
//			else {
//				/**
//				 * @Hua: original M_A for negated bottom concepts
//				 */
//				m2 = getNegClassCandidates(nc);
//				mA.get(nc).get(lengthMetric.classLength + lengthMetric.objectComplementLength).addAll(m2);		
//			}			
//		}
//
//		/**
//		 * @Hua: The rest of M is same as M_T
//		 */
//		// compute applicable properties				
//		OWLClassExpression domain = nc;		
//		if(nc.toString().equals("IE") || nc.toString().equals("EI") || nc.toString().equals("IL")) {
//			domain = df.getOWLThing();
//		}
//			
////		computeMg(nc);
//		computeMg(domain);
//
//		// boolean datatypes, e.g. testPositive = true
//		if(useBooleanDatatypes) {
//			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
////			Set<OWLDataProperty> booleanDPs = mgbd.get(nc);
//			Set<OWLDataProperty> booleanDPs = mgbd.get(domain);			
//			for (OWLDataProperty dp : booleanDPs) {
//				mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(true)));
//				mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, df.getOWLLiteral(false)));
//			}
//		}
//
//		if(useExistsConstructor) {
//			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
////			for(OWLObjectProperty r : mgr.get(nc)) {
//			for(OWLObjectProperty r : mgr.get(domain)) {
//				mA.get(nc).get(lc).add(df.getOWLObjectSomeValuesFrom(r, df.getOWLThing()));
//			}
//		}
//
//		if(useAllConstructor) {
//			// we allow \forall r.\top here because otherwise the operator
//			// becomes too difficult to manage due to dependencies between
//			// M_A and M_A' where A'=ran(r)
//			int lc = lengthMetric.objectAllValuesLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
////			for(OWLObjectProperty r : mgr.get(nc)) {
//			for(OWLObjectProperty r : mgr.get(domain)) {
//				mA.get(nc).get(lc).add(df.getOWLObjectAllValuesFrom(r, df.getOWLThing()));
//			}
//		}
//
//		if(useNumericDatatypes) {
////			Set<OWLDataProperty> numericDPs = mgNumeric.get(nc);
//			Set<OWLDataProperty> numericDPs = mgNumeric.get(domain);
//			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;
//
//			for(OWLDataProperty dp : numericDPs) {
//				List<OWLLiteral> splitLiterals = splits.get(dp);
//				if(splitLiterals != null && splitLiterals.size() > 0) {
//					OWLLiteral min = splits.get(dp).get(0);
//					OWLLiteral max = splits.get(dp).get(splits.get(dp).size()-1);
//					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, min, OWLFacet.MIN_INCLUSIVE)));
//					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, max, OWLFacet.MAX_INCLUSIVE)));
//				}
//			}
//		}
//
//		if(useTimeDatatypes) {
////			Set<OWLDataProperty> dtDPs = mgDT.get(nc);
//			Set<OWLDataProperty> dtDPs = mgDT.get(domain);
//			int lc = lengthMetric.dataSomeValuesLength + lengthMetric.dataProperyLength + 1;
//
//			for(OWLDataProperty dp : dtDPs) {
//				if(splits.get(dp).size() > 0) {
//					OWLLiteral min = splits.get(dp).get(0);
//					OWLLiteral max = splits.get(dp).get(splits.get(dp).size()-1);
//					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, min, OWLFacet.MIN_INCLUSIVE)));
//					mA.get(nc).get(lc).add(df.getOWLDataSomeValuesFrom(dp, asDatatypeRestriction(dp, max, OWLFacet.MAX_INCLUSIVE)));
//				}
//			}
//		}
//
//		if(useDataHasValueConstructor) {
////			Set<OWLDataProperty> stringDPs = mgsd.get(nc);
//			Set<OWLDataProperty> stringDPs = mgsd.get(domain);
//			int lc = lengthMetric.dataHasValueLength + lengthMetric.dataProperyLength;
//			for(OWLDataProperty dp : stringDPs) {
//				// loop over frequent values
//				Set<OWLLiteral> freqValues = frequentDataValues.get(dp);
//				for(OWLLiteral lit : freqValues) {
//					mA.get(nc).get(lc).add(df.getOWLDataHasValue(dp, lit));
//				}
//			}
//		}
//
//		if(useHasValueConstructor) {
//			int lc = lengthMetric.objectHasValueLength + lengthMetric.objectProperyLength;
//			int lc_i = lengthMetric.objectHasValueLength + lengthMetric.objectInverseLength;
////
////			m.get(lc).addAll(
////					mgr.get(nc).stream()
////							.flatMap(p -> frequentValues.get(p).stream()
////									.map(val -> df.getOWLObjectHasValue(p, val)))
////							.collect(Collectors.toSet()));
////			for(OWLObjectProperty p : mgr.get(nc)) {
//			for(OWLObjectProperty p : mgr.get(domain)) {
//				Set<OWLIndividual> values = frequentValues.get(p);
//				values.forEach(val -> m.get(lc).add(df.getOWLObjectHasValue(p, val)));
//
//				if(useInverse) {
//					values.forEach(val -> m.get(lc_i).add(df.getOWLObjectHasValue(p.getInverseProperty(), val)));
//				}
//			}
//		}
//
//		if(useCardinalityRestrictions) {
//			int lc = lengthMetric.objectCardinalityLength + lengthMetric.objectProperyLength + lengthMetric.classLength;
////			for(OWLObjectProperty r : mgr.get(nc)) {
//			for(OWLObjectProperty r : mgr.get(domain)) {
//				int maxFillers = maxNrOfFillers.get(r);
//				// zero fillers: <= -1 r.C does not make sense
//				// one filler: <= 0 r.C is equivalent to NOT EXISTS r.C,
//				// but we still keep it, because ALL r.NOT C may be difficult to reach
//				if((useNegation && maxFillers > 0) || (!useNegation && maxFillers > 1)) {
//					mA.get(nc).get(lc).add(df.getOWLObjectMaxCardinality(maxFillers-1, r, df.getOWLThing()));
//				}
//
//				// = 1 r.C
////				mA.get(nc).get(lc).add(df.getOWLObjectExactCardinality(1, r, df.getOWLThing()));
//			}
//		}
//
//		if(useHasSelf) {
//			int lc = lengthMetric.objectSomeValuesLength + lengthMetric.objectProperyLength + lengthMetric.objectHasSelfLength;
////			for(OWLObjectProperty p : mgr.get(nc)) {
//			for(OWLObjectProperty p : mgr.get(domain)) {
//				m.get(lc).add(df.getOWLObjectHasSelf(p));
//			}
//		}
//
//		logger.debug(sparql_debug, "m for " + nc + ": " + mA.get(nc));
//
//		mComputationTimeNs += System.nanoTime() - mComputationTimeStartNs;
//	}
//
//}
