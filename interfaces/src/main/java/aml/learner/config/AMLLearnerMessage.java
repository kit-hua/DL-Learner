package aml.learner.config;

import java.util.concurrent.BlockingQueue;

public class AMLLearnerMessage {
	
	private BlockingQueue<Boolean> stopQueue;
	private BlockingQueue<String> logQueue;	
	private BlockingQueue<AMLLearnerSolution> solutionQueue;
	
	public AMLLearnerMessage(BlockingQueue<Boolean> stopQueue, BlockingQueue<String> logQueue,
			BlockingQueue<AMLLearnerSolution> solutionQueue) {
		this.stopQueue = stopQueue;
		this.logQueue = logQueue;
		this.solutionQueue = solutionQueue;
	}
	
	
	/**
	 * @return the stopQueue
	 */
	public BlockingQueue<Boolean> getStopQueue() {
		return stopQueue;
	}
	/**
	 * @param stopQueue the stopQueue to set
	 */
	public void setStopQueue(BlockingQueue<Boolean> stopQueue) {
		this.stopQueue = stopQueue;
	}
	/**
	 * @return the logQueue
	 */
	public BlockingQueue<String> getLogQueue() {
		return logQueue;
	}
	/**
	 * @param logQueue the logQueue to set
	 */
	public void setLogQueue(BlockingQueue<String> logQueue) {
		this.logQueue = logQueue;
	}
	/**
	 * @return the solutionQueue
	 */
	public BlockingQueue<AMLLearnerSolution> getSolutionQueue() {
		return solutionQueue;
	}
	/**
	 * @param solutionQueue the solutionQueue to set
	 */
	public void setSolutionQueue(BlockingQueue<AMLLearnerSolution> solutionQueue) {
		this.solutionQueue = solutionQueue;
	}
	
	

}
