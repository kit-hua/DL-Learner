package org.dllearner.algorithms.rrch;

import org.dllearner.algorithms.celoe.OENode;
import org.dllearner.core.AbstractHeuristic;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.config.ConfigOption;

/**
 * This is a copy of org.dllearner.algorithms.celoe.OEHeuristicRuntime for the sake of layerwise traversing of the search treee
 * TODO: shall be refactored with a new architecture instead of duplicating the code
 * 
 * @author Yingbing Hua
 *
 */
@ComponentAnn(name = "LayerwiseOEHeuristic", shortName = "celoe_heuristic_lw", version = 0.5)
public class LayerwiseOEHeuristicRuntime extends LayerwiseAbstractHeuristic{
	
	
	@ConfigOption(description = "penalty for long descriptions (horizontal expansion) (strong by default)", defaultValue = "0.1")
	protected double expansionPenaltyFactor = 0.1;
	@ConfigOption(description = "bonus for being better than parent node", defaultValue = "0.3")
	protected double gainBonusFactor = 0.3;
	@ConfigOption(description = "penalty if a node description has very many refinements since exploring such a node is computationally very expensive",
			defaultValue = "0.0001")
	protected double nodeRefinementPenalty = 0.0001;
	
	@ConfigOption(defaultValue="0.1")
	protected double startNodeBonus = 0.1;
	
	public LayerwiseOEHeuristicRuntime() {

	}
	
	@Override
	public void init() throws ComponentInitException {

		initialized = true;
	}
	
	@Override
	public double getNodeScore(LayerwiseSearchTreeNode node) {
		
		// only compute the score if it is necessary
		if(!node.getCeloeScore().equals(Double.NaN)) {
			return node.getCeloeScore();
		}

		// accuracy as baseline
		double score = node.getAccuracy();

		// being better than the parent gives a bonus;
		if(!node.isRoot()) {
			double accuracyGain = node.getAccuracy() - node.getParent().getAccuracy();
			score += accuracyGain * gainBonusFactor;
		// the root node also gets a bonus to possibly spawn useful disjunctions
		} else {
			score += startNodeBonus;
		}

		// penalty for horizontal expansion
		score -= node.getHorizontalExpansion() * expansionPenaltyFactor;

		// penalty for having many child nodes (stuck prevention)
		score -= node.getRefinementCount() * nodeRefinementPenalty;
		
		node.setCeloeScore(score);
		
		return score;
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
	
	public String toString () {
		String s = " - heuristic: " + "celoe_lw" + "\n";
		s += "\t- expansionPenalty: " + expansionPenaltyFactor + "\n";
		s += "\t- refinementPenalty: " + nodeRefinementPenalty + "\n";
		s += "\t- gainBonus: " + gainBonusFactor + "\n";
		s += "\t- startBonus: " + startNodeBonus + "\n";
		
		return s;		
	}

}