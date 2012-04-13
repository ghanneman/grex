package ruleLearnerNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeAlignmentList
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public NodeAlignmentList()
	{
		this.listFromSrc = new HashMap<Span, Map<Span, Integer>>();
		this.listFromTgt = new HashMap<Span, Map<Span, Integer>>();
	}


	// PUBLIC MEMBER ATTRIBUTES: ////////////////////////////////////////////

	public static enum GrownType {
		SRC_GROWN (1, "src-grown"),
		TGT_GROWN (2, "tgt-grown");
		
	    private final int typeVal;
	    private final String name;
	    
	    GrownType(int typeVal, String name) {
	        this.typeVal = typeVal;
	        this.name = name;
	    }
	    public int getTypeVal()   { return typeVal; }
	    
	    public String getName() { return name; };
	    
	    
	    /**
	     * 
	     * @param type An int interpreted as a bit array.
	     * @return true if this type's bit is set to 1 in type, else false.
	     */
	    public boolean inType(int type){
	    	return ((type & this.getTypeVal()) == this.getTypeVal());
	    }
	}
	
	public static enum NodeAlignmentType {
		SRC_GROWN (1),
		TGT_GROWN (2),
	    T2T (4),
		T2S (8),
		S2T (16),
		T2TS (32),
		TS2T (64),
		TS2TS (128),
		T2P (256), // Terminal to phrase: Internal use only; DO NOT extract rules.
		P2T (512); // Phrase to terminal
		
	    private final int typeVal;
	    private final int index;
	    
	    NodeAlignmentType(int typeVal) {
	        this.typeVal = typeVal;
	        index = (int)(Math.log(typeVal)/Math.log(2));
	    }
	    
	    public int getTypeVal()   { return typeVal; }
	    public int getIndex() { return index; }
	    
	    /**
	     * 
	     * @param type An int interpreted as a bit array.
	     * @return true if this type's bit is set to 1 in type, else false.
	     */
	    public boolean inType(int type){
	    	return ((type & this.getTypeVal()) == this.getTypeVal());
	    }
	    
	    public NodeAlignmentType flip()
	    {
		    if (this.equals(T2S))
		    {
		    	return S2T;
		    }
		    else if (this.equals(S2T))
		    {
		    	return T2S;
		    }
		    else if (this.equals(T2TS))
		    {
		    	return TS2T;
		    }
		    else if (this.equals(TS2T))
		    {
		    	return T2TS;
		    }
		    else
		    {
		    	return this;
		    }
	    	
	    }

	}

	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public void add(int srcSpanStart, int srcSpanEnd,
					int tgtSpanStart, int tgtSpanEnd,
					boolean fromSrc, int ... alignTypes )
	{
		Span srcSpan = new Span(srcSpanStart, srcSpanEnd);
		Span tgtSpan = new Span(tgtSpanStart, tgtSpanEnd);
	
		if (fromSrc)
		{
			// See if a node alignment already exists in the from-source map:
			if(!listFromSrc.containsKey(srcSpan))
				listFromSrc.put(srcSpan, new HashMap<Span, Integer>());
			if(!listFromSrc.get(srcSpan).containsKey(tgtSpan))
				listFromSrc.get(srcSpan).put(tgtSpan, 0);
	
			// Add the new alignment to the from-source map:
			int types = listFromSrc.get(srcSpan).get(tgtSpan);
			for (int type : alignTypes) 
			{
				types = types | type;
			}
			
			listFromSrc.get(srcSpan).put(tgtSpan, types);
		}
		else
		{
			// See if a node alignment already exists in the from-target map:
			if(!listFromTgt.containsKey(tgtSpan))
				listFromTgt.put(tgtSpan, new HashMap<Span, Integer>());
			if(!listFromTgt.get(tgtSpan).containsKey(srcSpan))
				listFromTgt.get(tgtSpan).put(srcSpan, 0);

			// Add the new alignment to the from-target map:
			int types = listFromTgt.get(tgtSpan).get(srcSpan);
			for (int type : alignTypes) 
			{
				types = types | type;
			}
			
			listFromTgt.get(tgtSpan).put(srcSpan, types);
		}
	}
	
	public Map<Span, Integer> getAlignsForSrc(int srcSpanStart, int srcSpanEnd)
	{
		Span srcSpan = new Span(srcSpanStart, srcSpanEnd);
		if(!listFromSrc.containsKey(srcSpan))
			return new HashMap<Span, Integer>();
		else
			return listFromSrc.get(srcSpan);
	}


	public Map<Span, Integer> GetAlignsForTgt(int tgtSpanStart, int tgtSpanEnd)
	{
		Span tgtSpan = new Span(tgtSpanStart, tgtSpanEnd);
		if(!listFromTgt.containsKey(tgtSpan))
			return new HashMap<Span, Integer>();
		else
			return listFromTgt.get(tgtSpan);
	}


	public Map<BiSpan, Integer> GetAllAligns()
	{
		// We assume the integrity of both maps (since they're private member
		// variables) and just collect entries from the source-based map rather
		// than cross-checking both:
		Map<BiSpan, Integer> result = new HashMap<BiSpan, Integer>();
		int srcStart, srcEnd, tgtStart, tgtEnd;
		for(Span srcSpan : listFromSrc.keySet())
		{
			srcStart = srcSpan.getStart();
			srcEnd = srcSpan.getEnd();
			for(Span tgtSpan : listFromSrc.get(srcSpan).keySet())
			{
				tgtStart = tgtSpan.getStart();
				tgtEnd = tgtSpan.getEnd();
				BiSpan bs = new BiSpan(srcStart, srcEnd, tgtStart, tgtEnd);
				result.put(bs, listFromSrc.get(srcSpan).get(tgtSpan));
			}
		}
		return result;
	}


	public String interchangeFormatString()
	{
		// We assume the integrity of both maps (since they're private member
		// variables) and just print out from the source-based map rather than
		// cross-checking both:
		String result = "";
		for(Span srcSpan : listFromSrc.keySet())
		{
			for(Span tgtSpan : listFromSrc.get(srcSpan).keySet())
			{
				String type =
					decodeTypeField(listFromSrc.get(srcSpan).get(tgtSpan));
				result += (srcSpan.getStart() + "-" + srcSpan.getEnd() + "\t" +
						   tgtSpan.getStart() + "-" + tgtSpan.getEnd() + "\t" +
						   type + "\n");
			}
		}
		return result.trim();
	}

	public Collection<String> getInterchangeFormatStringList()
	{
		List<String> stringList = new ArrayList<String>();
		
		String separator = " ||| ";
		
		for(Span srcSpan : listFromSrc.keySet())
		{
			for(Span tgtSpan : listFromSrc.get(srcSpan).keySet())
			{
				String type =
					decodeTypeField(listFromSrc.get(srcSpan).get(tgtSpan));
				stringList.add(srcSpan.getStart() + "-" + srcSpan.getEnd() + separator +
						   tgtSpan.getStart() + "-" + tgtSpan.getEnd() + separator +
						   type);
			}
		}
		
		return stringList;
	}

	public String decodeTypeField(int type)
	{
		// Start by accumulating encoded alignment types:
		String result = "";
		for (NodeAlignmentType alignType : NodeAlignmentType.values())
		{
			if (alignType.inType(type))
			{
				result += alignType.toString() + " ";
			}
		}
		
		if(result.equals(""))
		{
			result = "UNKNOWN-code" + type;
		}
		
		return result.trim();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof NodeAlignmentList))
		{
			return false;
		}
		
		return ((NodeAlignmentList)o).GetAllAligns().equals(this.GetAllAligns());
	}

	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	// A view of the node alignments given the source span first, as the
	// outer key, then mapping to the target:
	private Map<Span, Map<Span, Integer>> listFromSrc;

	// A view of the node alignments given the target span first, as the
	// outer key, then mapping to the source:
	private Map<Span, Map<Span, Integer>> listFromTgt;
}