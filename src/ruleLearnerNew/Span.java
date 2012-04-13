package ruleLearnerNew;

import java.util.ArrayList;
import java.util.List;


public class Span
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public Span(int start, int end)
	{
		this.start = start;
		this.end = end;
		includedNodes = new ArrayList<ParseNode>();
	}

	public Span(int start, int end, List<ParseNode> nodes)
	{
		this.start = start;
		this.end = end;
		includedNodes = nodes;
	}
	
	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public int getStart()
	{
		return start;
	}


	public int getEnd()
	{
		return end;
	}

	public List<ParseNode> getIncludedNodes()
	{
		return includedNodes;
	}
	
	@Override 
	public boolean equals(Object obj)
	{
		// Obvious cases:
		if(obj == this)
			return true;
		if(!(obj instanceof Span))
			return false;
		
		// Now compare values:
		Span objS = (Span)obj;
		return (objS.start == this.start &&
				objS.end == this.end);
	}
	
	public boolean isNonOverlapping(Span comparedSpan)
	{
		return this.end < comparedSpan.start 
		       || this.start > comparedSpan.end;
	}
	
	@Override 
	public int hashCode()
	{
		int hash = end;
		hash = 97 * hash + start;
		return hash;
	}

	@Override 
	public String toString()
	{
		return "(" + start + "," + end + ")";
	}
	
	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	private int start;
	private int end;
	
	private List<ParseNode> includedNodes;
}
