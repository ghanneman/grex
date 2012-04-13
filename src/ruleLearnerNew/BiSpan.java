package ruleLearnerNew;

/**
 * @author ghannema
 *
 */
public class BiSpan
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public BiSpan(int srcStart, int srcEnd, int tgtStart, int tgtEnd)
	{
		this.srcStart = srcStart;
		this.srcEnd = srcEnd;
		this.tgtStart = tgtStart;
		this.tgtEnd = tgtEnd;
	}


	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public int getSrcStart()
	{
		return srcStart;
	}

	public int getSrcEnd()
	{
		return srcEnd;
	}

	public int getTgtStart()
	{
		return tgtStart;
	}

	public int getTgtEnd()
	{
		return tgtEnd;
	}


	@Override 
	public boolean equals(Object obj)
	{
		// Obvious cases:
		if(obj == this)
			return true;
		if(!(obj instanceof BiSpan))
			return false;
		
		// Now compare values:
		BiSpan objBS = (BiSpan)obj;
		return (objBS.srcStart == this.srcStart &&
				objBS.srcEnd == this.srcEnd &&
				objBS.tgtStart == this.tgtStart &&
				objBS.tgtEnd == this.tgtEnd);
	}

	
	@Override 
	public int hashCode()
	{
		int hash = tgtEnd;
		hash = 97 * hash + tgtStart;
		hash = 97 * hash + srcEnd;
		hash = 97 * hash + srcStart;
		return hash;
	}

	@Override
	public String toString()
	{
		return "(" + srcStart + "," + srcEnd + "),("
				+ tgtStart + "," + tgtEnd + ")";
	}

	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	private int srcStart;
	private int srcEnd;
	private int tgtStart;
	private int tgtEnd;
}
