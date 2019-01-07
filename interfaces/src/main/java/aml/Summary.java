package aml;

public class Summary{
	
	private long aml;
	private long rho;
	
	public Summary(long aml, long rho){
		this.aml = aml;
		this.rho = rho;
	}
	
	public long getDiff() {
		return rho-aml;
	}
	
	public double getPercent() {
		return 100 * aml / (double) rho;
	}
	
}