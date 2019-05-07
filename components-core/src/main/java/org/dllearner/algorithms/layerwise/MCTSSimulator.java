package org.dllearner.algorithms.layerwise;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.jena.base.Sys;
import org.dllearner.algorithms.celoe.OEHeuristicRuntime;
import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.AbstractClassExpressionLearningProblem;
import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.refinementoperators.LengthLimitedRefinementOperator;
import org.dllearner.refinementoperators.RhoDRDown;
import org.dllearner.utilities.datastructures.SearchTree;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.*;


@ComponentAnn(name="MCTSSimulator", shortName="mcts_sim", version=0.1, description="Simulation for MCTS")
public class MCTSSimulator  {

    // ========== Configuration Options ==========

    @ConfigOption(defaultValue="celoe_heuristic")
    private AbstractHeuristic heuristic = new OEHeuristicRuntime();

    @ConfigOption(defaultValue="2", description="maximum depth of refinement during single step of simulation")
    private int maxRefinementLength = 2;

    @ConfigOption(defaultValue = "0.2", description = "noise")
    private double noise = 0.2;

    @ConfigOption(defaultValue = "1", description = "amount of consecutive simulation steps where an accuracy change of 0 is tolerated")
    private int maxNoChangeSteps = 1;

    /* Specialized Refinement Operator for Simulation */
    private RhoDRDownMCTS operator;

    /* The learning problem we're concerned with. Used for measuring the accuracy of concepts. */
    private AbstractClassExpressionLearningProblem<? extends Score> learningProblem;

    /* Maximum depth that concepts may have. This should be consistent with the setting used in the algorithm. */
    private double maxConceptDepth;


    // ========== Simulation ==========

    /* "Log" of the last simulation's steps. */
    private List<OENode> lastSimulation;

    /* A cache for the accuracies of concepts that were already considered during simulation */
    private HashMap<OWLClassExpression, Double> accuracyCache = new HashMap<>();

    /* Amount of simulation steps for which the accuracy has not been improved */
    private int noChangeSteps = 0;

    /* Total simulation time during the whole execution time in milliseconds */
    private long totalSimulationTime = 0;

    private double lastAccuracy;
    private double currentAccuracy;


    /**
     * Carries out a simulation from a given leaf node. During the simulation, a specialized version of the RhoDRDown
     * refinement operator is used to sample refinements. A selection of refinements is measured for their accuracy.
     * Among those concepts, one is selected at random weighted by their CELOE score.
     *
     * @param leaf The leaf node that the simulation will be started from.
     * @return
     *      The last concept at the end of the simulation.
     */
    public OWLClassExpression simulate(OENode leaf) {
        long simulationStartTime = java.lang.System.currentTimeMillis();

        /* Get this descriptions accuracy to determine later whether it has improved. */
        double accuracy = leaf.getAccuracy();
        OWLClassExpression leafDescription = leaf.getDescription();
        currentAccuracy = accuracy;
        lastAccuracy = currentAccuracy;

        /* Initialize the simulation log */
        lastSimulation = new LinkedList<>();
        OENode leafNode = new OENode(leafDescription, accuracy);
        lastSimulation.add(leafNode);


        /* Do simulation steps until stop condition is met */
        OENode lastNode;
        OENode currentNode = leafNode;
        do {
            /* Do one step and update the scores */
            lastNode = currentNode;
            lastAccuracy = currentAccuracy;
            currentNode = simulationStep(lastNode);
            if (currentNode != null) {
                currentAccuracy = currentNode.getAccuracy();
                lastSimulation.add(currentNode);
            } else {
                break;
            }
        } while (!stopSimulation());

        /* Output most accurate expression that we got during simulation (i.e. the last or second-to-last one) */
        if (currentAccuracy > lastAccuracy) {
            totalSimulationTime += java.lang.System.currentTimeMillis() - simulationStartTime;
            return currentNode.getDescription();
        } else {
            totalSimulationTime += java.lang.System.currentTimeMillis() - simulationStartTime;
            return lastNode.getDescription();
        }
    }

    private OENode simulationStep(OENode node) {
        Monitor mon = MonitorFactory.start("simulation step");

        /* Calculate/Sample new refinements using the modified RhoDRDown operator.
         * An additional length of 2 will be sufficient to get all possible refinements in one or more simulation steps.
         */
        OWLClassExpression expression = node.getDescription();
        int length = OWLClassExpressionUtils.getLength(expression);
        Set<OWLClassExpression> refinements = this.operator.refineSimulation(expression, length + maxRefinementLength);

        /* Evaluate new concepts */
        for (OWLClassExpression e : refinements) {
            /* Ignore the concept if it is too deep */
            if (OWLClassExpressionUtils.getDepth(e) > maxConceptDepth) continue;

            /* Measure and store the concept's accuracy */
            double acc;
            if (accuracyCache.containsKey(e)) {
                acc = accuracyCache.get(e);
            } else {
                acc = learningProblem.getAccuracyOrTooWeak(e, noise);
            }
            accuracyCache.put(e, acc);
            if (acc == -1) continue;    // ignore concept if it's too weak


            OENode newChild = new OENode(e, acc);
            node.addChild(newChild);
        }

        /*
         * Select one refinement/concept at random. The concepts are weighted with their CELOE score.
         */
        OENode selection;
        if (node.getChildren().isEmpty()) {
            selection = null;
        } else {
            selection = MCTSUtil.sampleWeighted(node.getChildren(), heuristic);

        }

        mon.stop();
        return selection;
    }

    private boolean stopSimulation() {
        /* Simulation made accuracy worse -> stop */
        if (lastAccuracy > currentAccuracy) {
            return true;
        }

        /* Accuracy has stayed the same. If this happens for a number (maxNoChangeSteps) of consecutive steps,
         * the simulation will be terminated. Otherwise it continues.
         */
        if (lastAccuracy == currentAccuracy) {
            noChangeSteps++;
            if (noChangeSteps > maxNoChangeSteps) {
                return true;
            }
        }
        /* Reset the number of consecutive steps where the accuracy hasn't changed. */
        if (lastAccuracy < currentAccuracy) {
            noChangeSteps = 0;
        }

        /* Reached best accuracy possible */
        if (currentAccuracy >= 1) {
            return true;
        }

        return false;
    }


    // ========== Setters and getters ==========

    public void setRefinementOperator(LengthLimitedRefinementOperator operator) {
        this.operator = (RhoDRDownMCTS) operator;
    }

    public void setLearningProblem(AbstractClassExpressionLearningProblem<? extends Score> learningProblem) {
        this.learningProblem = learningProblem;
    }

    public double getFinalAccuracy() {
        return Math.max(currentAccuracy, lastAccuracy);
    }

    public List<OENode> getLastSimulation() {
        return lastSimulation;
    }

    public long getTotalSimulationTime() {
        return totalSimulationTime;
    }

    public void setMaxConceptDepth(double maxDepth) {
        maxConceptDepth = maxDepth;
    }
}
