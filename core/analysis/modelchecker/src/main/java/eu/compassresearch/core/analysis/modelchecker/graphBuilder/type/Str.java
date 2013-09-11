package eu.compassresearch.core.analysis.modelchecker.graphBuilder.type;

public class Str implements Type {
	
	private String value;
	
	
	public Str(String value) {
		this.value = value;
	}


	@Override
	public String toString() {
		return value;
	}
	
	public String toFormula() {
		return "Str(\""+ value + "\")";
	}

}