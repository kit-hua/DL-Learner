package org.dllearner.algorithms.layerwise;

import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.AbstractSearchTreeNode;

import java.util.*;

public class MCTSUtil {
    static Random rnd = new Random();
    
    /**
     * Samples one node of a given set at uniform random, where only those nodes are considered, that have the best
     * score according to the given heuristic.
     *
     * @param collection
     *      Collection of nodes that one should be sampled from.
     * @param heuristic
     *      The heuristic that is used to determine which of the nodes are considered.
     *
     * @return
     *      The sampled node.
     */
    public static OENode sampleUniformFromBest(Collection<OENode> collection, AbstractHeuristic heuristic) {
        assert !collection.isEmpty();

        /* Create a sorted array from the collection and find out up to which index the score is the best score */
        ArrayList<OENode> arr = new ArrayList<>(collection);
        arr.sort(heuristic);

        double bestRating = heuristic.getNodeScore(arr.get(arr.size() - 1));
        int bestCount = 0;
        for (int i = arr.size()-1; i >= 0; i--) {
            OENode n = arr.get(i);
            if (heuristic.getNodeScore(n) < bestRating) {
                break;
            }
            bestCount++;
        }

        /* Out of those nodes with the best score, choose one at uniform random. */
        int i = rnd.nextInt(bestCount);
        return arr.get(arr.size()-1-i);
    }

    /**
     * Samples one node of a set of nodes, each node weighted by its score in the given heuristic.
     *
     * @param collection
     *      Collection of nodes that one should be sampled from.
     * @param heuristic
     *      The heuristic that is used to determine the weights of the nodes.
     *
     * @return
     *      The sampled node.
     */
    public static OENode sampleWeighted(Collection<OENode> collection, AbstractHeuristic heuristic) {
        assert !collection.isEmpty();

        /* Calculate the total weight of all nodes, so one random number between 0 and the maximum can be used. */
        double total = 0;
        for (OENode n : collection) {
            double weight = Math.max(heuristic.getNodeScore(n), 0);
            total += weight;
        }

        /* Get a number between 0 and the maximum weight and use it to determine which node has been sampled. */
        double r = randomDoubleBetween(0, total);
        for (OENode n : collection) {
            r -= Math.max(heuristic.getNodeScore(n), 0);
            if (r <= 0) {
                return n;
            }
        }

        assert false;
        return Collections.max(collection, heuristic);
    }

    private static double randomDoubleBetween(double min, double max) {
        return min + (max - min) * rnd.nextDouble();
    }
}
