/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dllearner.algorithms.layerwise;

import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;

/**
 * Search algorithm heuristic for the ontology engineering algorithm. The heuristic
 * has a strong bias towards short descriptions (i.e. the algorithm is likely to be
 * less suitable for learning complex descriptions).
 * 
 * @author Jens Lehmann
 *
 */
@ComponentAnn(name = "CELOE Heuristic with Count", shortName = "celoe_heuristic_count", version = 0.5)
public class UCTHeuristic extends LayerwiseAbstractHeuristic{
	
	
	@ConfigOption(description = "penalty for long descriptions (horizontal expansion) (strong by default)", defaultValue = "0.1")
	private static double expansionPenaltyFactor = 0.1;
	@ConfigOption(description = "bonus for being better than parent node", defaultValue = "0.3")
	private double gainBonusFactor = 0.3;
	@ConfigOption(description = "penalty if a node description has very many refinements since exploring such a node is computationally very expensive",
			defaultValue = "0.0001")
	private double nodeRefinementPenalty = 0.0001;
	
	@ConfigOption(defaultValue="0.1")
	private double startNodeBonus = 0.1;
	
	private static int totalExpansion = 0;
	
	@ConfigOption(defaultValue="1.4", description="exploration factor parameter")
	private double explorationFactor = 1;
	@ConfigOption(defaultValue="alphago", description="exploration formula variant")
	private String explorationVariant = "alphago";
	public static final String EXPLORATION_ALPHAGA = "alphago";
	public static final String EXPLORATION_ORIGINAL = "original";
	
	// the score gain of this node
	private double gain;
	
	// the best score gain of this node and all its descendants
	private double bestGain;
	
	// the score of this node
	private double score;
	
	// the best score of this node and all its descendants
	private double bestScore;
	
	// the accumulated score of this node and all its descendants
	private double accumulatedScore;
	
	// the accumulated gain of this node and all its descendants
	private double accumulatedGain;
	
	// the weighted accumulated score of this node and all its descendants
	// the weight of each score is: 1/expansionCount
	// so the sum is: score1/expansionCount1 + score2/expansionCount2 + ...
	private double weightedAccumulatedScore;
	
	public UCTHeuristic() {

	}
	
	@Override
	public void init() throws ComponentInitException {

		initialized = true;
	}

	@Override
	public double getNodeScore(LayerwiseSearchTreeNode node) {
		// accuracy as baseline
		double score = node.getAccuracy();
//		double score = 0;

		// being better than the parent gives a bonus;
		if(!node.isRoot()) {
			double accuracyGain = node.getAccuracy() - node.getParent().getAccuracy();
			score += accuracyGain * gainBonusFactor;
		// the root node also gets a bonus to possibly spawn useful disjunctions
		} else {
			score += startNodeBonus;
		}

		// penalty for horizontal expansion
//		score -= node.getHorizontalExpansion() * expansionPenaltyFactor;
		
		score -= node.getAccumulatedExpansionCounter() * expansionPenaltyFactor;
//		score -= OWLClassExpressionUtils.getLength(node.getExpression()) * expansionPenaltyFactor;
//		score += getExploration(node);

		// penalty for having many child nodes (stuck prevention)
		score -= node.getRefinementCount() * nodeRefinementPenalty;

		return score;
	}
	
	public double getExploration(LayerwiseSearchTreeNode node) {
		
		if(explorationVariant.equals(EXPLORATION_ALPHAGA)) {
			return explorationFactor * Math.sqrt(totalExpansion)/(1 + (double) node.getAccumulatedExpansionCounter());	
		}
		
		if(explorationVariant.equals(EXPLORATION_ORIGINAL)) {
			if(node.isRoot())
				//TODO: the root has no parent, therefore cannot ln(0) = -inf, therefore cannot apply the UCT
				// do we have better answer than 0 to allow possible expansion of the root?
				// As the exploitation of the root shall be smaller than anyother nodes, can we force the root to expand after certain time?
				return 0;
			
			LayerwiseSearchTreeNode parent = node.getParent();	
			
			// we use the formula from [2012] A Survey of Monte Carlo Tree Search Methods
			// for inner nodes, the formula is: UCT(n) = Q(n) + c * ( 2 * sqrt(log(N(p))) / N(n) )
			// for leaf nodes, return infinite
			if(node.getAccumulatedExpansionCounter() != 0)
				return explorationFactor * Math.sqrt(2 * Math.log(parent.getAccumulatedExpansionCounter()) / ((double) node.getAccumulatedExpansionCounter()));
			else
				return Double.MAX_VALUE;
		}
		
		return Double.NaN;
	}

	
	public static void incTotalExpansion() {
		totalExpansion++;
	}

	public double getExpansionPenaltyFactor() {
		return expansionPenaltyFactor;
	}

	public double getGainBonusFactor() {
		return gainBonusFactor;
	}

	public void setGainBonusFactor(double gainBonusFactor) {
		this.gainBonusFactor = gainBonusFactor;
	}

	public double getNodeRefinementPenalty() {
		return nodeRefinementPenalty;
	}

	public void setNodeRefinementPenalty(double nodeRefinementPenalty) {
		this.nodeRefinementPenalty = nodeRefinementPenalty;
	}

	public void setExpansionPenaltyFactor(double expansionPenaltyFactor) {
		this.expansionPenaltyFactor = expansionPenaltyFactor;
	}

	public double getStartNodeBonus() {
		return startNodeBonus;
	}

	public void setStartNodeBonus(double startNodeBonus) {
		this.startNodeBonus = startNodeBonus;
	}
	
	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @return the accumulatedScore
	 */
	public double getAccumulatedScore() {
		return accumulatedScore;
	}


	/**
	 * @return the gain
	 */
	public double getGain() {
		return gain;
	}

	/**
	 * @return the accumulatedGain
	 */
	public double getAccumulatedGain() {
		return accumulatedGain;
	}

	/**
	 * @return the bestGain
	 */
	public double getBestGain() {
		return bestGain;
	}

	/**
	 * @return the bestScore
	 */
	public double getBestScore() {
		return bestScore;
	}

	/**
	 * @return the weightedAccumulatedScore
	 */
	public double getWeightedAccumulatedScore() {
		return weightedAccumulatedScore;
	}

}