package ruleLearnerTest;

import ruleLearnerNew.ExtractedRule;

public class MockExtractedRule
{

	public MockExtractedRule(String type, String srcLhs, String tgtLhs, String srcRhs,
			                 String tgtRhs, String alignIndices, String alignTypes)
	{
		this.type = type;
		this.srcLhs = srcLhs;
		this.tgtLhs = tgtLhs;
		this.srcRhs = srcRhs;
		this.tgtRhs = tgtRhs;
		this.alignIndices = alignIndices;
		this.alignTypes = alignTypes;
	}
	
	
	public boolean equivalentRule(ExtractedRule rule)
	{
		String[] ruleParts = rule.toString().split("\\|\\|\\|");
		String newLhs = "[" + this.srcLhs + "::" + this.tgtLhs + "]";
	    return this.type.equals(ruleParts[0].trim())
	    	   && newLhs.equals(ruleParts[1].trim())
	           && this.srcRhs.equals(ruleParts[2].trim())
	           && this.tgtRhs.equals(ruleParts[3].trim())
	           && this.alignIndices.equals(ruleParts[4].trim())
	           && this.alignTypes.equals(ruleParts[5].trim());
	}
	
	public String toString()
	{
		return type + "||| [" + srcLhs  + ":" + tgtLhs + "|||" + srcRhs + "|||" 
			   + tgtRhs + "|||" + alignIndices + "|||" + alignTypes;
	}
	
	private String type;
	private String srcLhs;
	private String tgtLhs;
	private String srcRhs;
	private String tgtRhs;
	private String alignIndices;
	private String alignTypes;
}
