package ruleLearnerNew;

public class NullParseNode extends ParseNode 
{
	public NullParseNode()
	{
		super(-1,-1);
	}
	
	@Override
	public String toString()
	{
		return "";
	}
	
	@Override
	public boolean isTerminal()
	{
		return true;
	}
}
