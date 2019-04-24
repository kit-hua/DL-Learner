package org.dllearner.algorithms.mcts;

import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.AbstractClassExpressionLearningProblem;
import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.Score;
import org.dllearner.core.config.ConfigOption;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.HashMap;
import java.util.Map;


/**
 * UCT ("Upper Confidence Bound applied to Trees") heuristic for ordering nodes as used by Monte Carlo Tree Search.
 * The values for each node are updated in the backpropagation step of MCTS, using the result of the last simulation.
 * The heuristic tries to balance exploitation versus exploration.
 *
 * The heuristic values are computed using a variant of the UCT heuristic, which is also used in the AlphaGo algorithm.
 * (Silver, David, et al. "Mastering the game of go without human knowledge." Nature 550.7676 (2017): 354.)
 *
 * The formula is UCT(n) = Q(n) + c * (sqrt(N))/(1 + N(n))
 * where
 *  N is the total amount of simulations
 *  N(n) is the amount of simulations started from node n or a node below it in the tree
 *  c is an exploration constant
 *  Q(n) is the "mean action value", which in this case is totalScore/N(n) where totalScore is the sum of all
 *  back-propagated accuracy gains. These accuracy gains come from simulations started from the node n or a node
 *  that is below it in the tree. Its meaning is the mean value of further expanding this node.
 *
 * @author Patrick Hegemann
 */

@ComponentAnn(name = "MCTSHeuristic", shortName = "mcts_heuristic", version = 0.1)
public class MCTSHeuristic extends AbstractHeuristic {

    @ConfigOption(defaultValue="1.4", description="exploration parameter")
    private double exploration = 1.4;

    /**
     * Class that holds information about nodes that is relevant to the heuristic.
     * The UCT value is ideally only computed once per iteration of MCTS, after the backpropagation.
     */
    class UCTNode {
        /* Amount of times this node or a node in its subtree has been simulated */
        int countSimulations = 0;
        /* Sum over all added/propagated simulation results */
        double totalScore = 0;
        /* UCT score (which is exactly what this heuristic outputs) */
        double uct;
    }
    private Map<OENode, UCTNode> nodes;

    /* Total amount of simulation results that have been added since the start */
    private int totalSimulations = 0;

    MCTSHeuristic() {
        nodes = new HashMap<>();
    }

    @Override
    public double getNodeScore(OENode node) {
        if (nodes.containsKey(node)) {
            /*
             * This node's UCT score has already been computed
             */
            return nodes.get(node).uct;
        } else {
            /*
             * Node is not cached, then create default one
             */
            UCTNode n = new UCTNode();
            updateUCTScore(n);
            nodes.put(node, n);
            return n.uct;
        }
    }


    /**
     * Updates the information used for UCT of a node according to a result of a simulation.
     * @param node          The affected node
     * @param result        The result of the simulation
     */
    void addSimulationResult(OENode node, double result) {
        /*
         * Update simulation info (simulation count and node's total score)
         */
        UCTNode n = nodes.getOrDefault(node, new UCTNode());
        totalSimulations++;
        n.countSimulations++;
        n.totalScore += result;
        nodes.put(node, n);
    }

    /**
     * Updates the UCT scores of all nodes. Call this method once after backpropagation.
     */
    void updateAllScores() {
        for (UCTNode m : nodes.values()) {
            updateUCTScore(m);
        }
    }

    /**
     * Calculates the new UCT value for a node.
     * @param n     The node whose UCT value should be updated
     */
    private void updateUCTScore(UCTNode n) {
        /* Add the exploration term to the score */
        double score = exploration * Math.sqrt(totalSimulations)/(1 + (double) n.countSimulations);

        /* If this node has previous simulation results, then add "exploitation" term (division by zero otherwise) */
        if (n.countSimulations > 0) {
            score += n.totalScore / n.countSimulations;
        }

        n.uct = score;
    }


    public void setExploration(double exploration) {
        this.exploration = exploration;
    }
}
