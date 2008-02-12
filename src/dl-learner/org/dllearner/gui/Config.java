package org.dllearner.gui;

/**
 * Copyright (C) 2007-2008, Jens Lehmann
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
 *
 */

import org.dllearner.core.ComponentManager;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.LearningProblem;
import org.dllearner.core.ReasoningService;
import org.dllearner.core.LearningAlgorithm;
import org.dllearner.core.ReasonerComponent;

/**
 * Config save all together used variables: ComponentManager, KnowledgeSource,
 * Reasoner, ReasoningService, LearningProblem, LearningAlgorithm; also inits of
 * these components
 * 
 * @author Tilo Hielscher
 */
public class Config {
    private ComponentManager cm = ComponentManager.getInstance();
    private KnowledgeSource source;
    private String uri;
    private ReasonerComponent reasoner;
    private ReasoningService rs;
    private LearningProblem lp;
    private LearningAlgorithm la;
    private boolean[] isInit = new boolean[4];

    /**
     * Get ComponentManager.
     * 
     * @return ComponentManager
     */
    public ComponentManager getComponentManager() {
	return this.cm;
    }

    /**
     * It is necessary for init KnowledgeSource.
     * 
     * @return URI
     */
    public String getURI() {
	return this.uri;
    }

    /**
     * Set an URI.
     * 
     * @param uri
     *                it's a Link like "http://example.com" or "file://myfile"
     */
    public void setURI(String uri) {
	this.uri = uri;
    }

    /**
     * Get Reasoner.
     * 
     * @return reasoner
     */
    public ReasonerComponent getReasoner() {
	return this.reasoner;
    }

    /**
     * Set Reasoner.
     * 
     * @param reasoner
     */
    public void setReasoner(ReasonerComponent reasoner) {
	this.reasoner = reasoner;
    }

    /**
     * Get ReasoningService.
     * 
     * @return ReasoningService
     */
    public ReasoningService getReasoningService() {
	return this.rs;
    }

    /**
     * Set ReasoningService.
     * 
     * @param reasoningService
     */
    public void setReasoningService(ReasoningService reasoningService) {
	this.rs = reasoningService;
    }

    /**
     * Get KnowledgeSource.
     * 
     * @return KnowledgeSource
     */
    public KnowledgeSource getKnowledgeSource() {
	return this.source;
    }

    /**
     * Set KnowledgeSource.
     * 
     * @param knowledgeSource
     */
    public void setKnowledgeSource(KnowledgeSource knowledgeSource) {
	this.source = knowledgeSource;
    }

    /**
     * Set LearningProblem.
     * 
     * @param learningProblem
     */
    public void setLearningProblem(LearningProblem learningProblem) {
	this.lp = learningProblem;
    }

    /**
     * Get LearningProblem.
     * 
     * @return learningProblem
     */
    public LearningProblem getLearningProblem() {
	return this.lp;
    }

    /**
     * Set LearningAlgorithm.
     * 
     * @param learningAlgorithm
     */
    public void setLearningAlgorithm(LearningAlgorithm learningAlgorithm) {
	this.la = learningAlgorithm;
    }

    /**
     * Get LearningAlgorithm.
     * 
     * @return LearningAlgorithm
     */
    public LearningAlgorithm getLearningAlgorithm() {
	return this.la;
    }

    /**
     * KnowledgeSource.init has run?
     * 
     * @return true, if init was made, false if not
     */
    public boolean isInitKnowledgeSource() {
	return isInit[0];
    }

    /**
     * Set true if you run KnowwledgeSource.init. The inits from other tabs
     * behind will automatic set to false.
     */
    public void setInitKnowledgeSource(Boolean is) {
	isInit[0] = is;
	for (int i = 1; i < 4; i++)
	    isInit[i] = false;
    }

    /**
     * Reasoner.init has run?
     * 
     * @return true, if init was made, false if not
     */
    public boolean isInitReasoner() {
	return isInit[1];
    }

    /**
     * Set true if you run Reasoner.init. The inits from other tabs behind will
     * automatic set to false.
     */
    public void setInitReasoner(Boolean is) {
	isInit[1] = is;
	for (int i = 2; i < 4; i++)
	    isInit[i] = false;
    }

    /**
     * LearningProblem.init has run?
     * 
     * @return true, if init was made, false if not
     */
    public boolean isInitLearningProblem() {
	return isInit[2];
    }

    /**
     * Set true if you run LearningProblem.init. The inits from other tabs
     * behind will automatic set to false.
     */
    public void setInitLearningProblem(Boolean is) {
	isInit[2] = is;
	for (int i = 3; i < 4; i++)
	    isInit[i] = false;
    }

    /**
     * LearningAlgorithm.init() has run?
     * 
     * @return true, if init was made, false if not
     */
    public boolean isInitLearningAlgorithm() {
	return isInit[3];
    }

    /**
     * set true if you run LearningAlgorithm.init
     */
    public void setInitLearningAlgorithm(Boolean is) {
	isInit[3] = is;
    }

}
