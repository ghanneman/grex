package ruleLearnerNew;



/**
 * @author ghannema
 * @author mburroug
 *
 */
public class ExtractedRule
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public ExtractedRule(ParseNode lhs1, ParseNode lhs2,
						 ParseNodeRulePart rhs, boolean side1IsSource)
	{
		if (side1IsSource)
		{
			this.srcLHS = lhs1;
			this.tgtLHS = lhs2;
		}
		else
		{
			this.tgtLHS = lhs1;
			this.srcLHS = lhs2;
		}
		
		this.rhs = rhs;
	}

	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public ParseNode getSrcLhsNode()
	{
		return srcLHS;
	}
	
	public ParseNode getTgtLhsNode()
	{
		return tgtLHS;
	}
	
	public ParseNodeRulePart getRhs()
	{
		return rhs;
	}
	
	public boolean isParallelUnary() 
	{
		return rhs.isParallelUnary();
	}
	
	@Override
	public String toString()
	{
		String str = "";
		
		str += rhs.getType() + " ||| ";
		
		str += "[" + srcLHS + "::" + tgtLHS + "]" + DELIM;
		
		str += rhs.toString(DELIM);
		
		str += DELIM + srcLHS.getNodeAlignCategory() + tgtLHS.getNodeAlignCategory();
		
		str += " " + rhs.getAlignTypeString();
		
		return str.trim();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ExtractedRule))
		{
			return false;
		}
		ExtractedRule otherRule = (ExtractedRule) o;
		
		return this.srcLHS.equals(otherRule.srcLHS) &&
		       this.tgtLHS.equals(otherRule.tgtLHS) &&
		       this.rhs.equals(otherRule.rhs);
	}
	
	@Override
	public int hashCode()
	{
		int hashCode = 0;
		
		hashCode = srcLHS.hashCode();
		hashCode = 31*hashCode + tgtLHS.hashCode();
		hashCode = 31*hashCode + rhs.hashCode();
		
		return hashCode;
	}
	
	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	private ParseNode srcLHS;
	private ParseNode tgtLHS;
	private ParseNodeRulePart rhs;

	private static final String DELIM = " ||| ";
}
