package ruleLearnerNew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseNodeRulePart {
	
	public ParseNodeRulePart()
	{
		maxGrammarComponents = 0;
		isPhrase = true;
		minSrcGeneration = Integer.MAX_VALUE;
		minTgtGeneration = Integer.MAX_VALUE;
	}
	
	public ParseNodeRulePart(ParseNode part1, ParseNode part2, 
			 				 int maxGrammarSize, int maxPhraseSize,
			 				 boolean fromSource, boolean genReorderList)
	{	
		maxGrammarComponents = maxGrammarSize;
		maxPhraseComponents = maxPhraseSize;
		
		this.fromSource = fromSource;

		this.srcPart = new ArrayList<ParseNode>();
		this.tgtPart = new ArrayList<ParseNode>();
		
		this.isPhrase = (part1.isTerminal() || part1.isString())
		                && (part2.isTerminal() || part2.isString());
		
		if (fromSource)
		{
			intialize(part1, part2, fromSource);
		}
		else
		{
			intialize(part2, part1, fromSource);
		}
		
		this.reorderList = new ReorderingList(srcPart, tgtPart, fromSource);
	}

	private void intialize(ParseNode source, ParseNode target, 
						   boolean fromSource) 
	{
		srcSpan = new Span(source.getSpanStart(),source.getSpanEnd());
		tgtSpan = new Span(target.getSpanStart(),target.getSpanEnd());
		
		if (source.isString())
		{
			srcPart.addAll(((StringNode)source).getTerminalComponents());
			if (srcPart.size() > maxPhraseComponents)
			{
				maxPhraseComponents = -1;
				return;
			}
		}
		else
		{
			srcPart.add(source);
		}
		
		if (target.isString())
		{
			tgtPart.addAll(((StringNode)target).getTerminalComponents());
			if (tgtPart.size() > maxPhraseComponents)
			{
				maxPhraseComponents = -1;
				return;
			}
		}
		else
		{
			tgtPart.add(target);
		}
		
		
		minSrcGeneration = source.getGeneration();
		minTgtGeneration = target.getGeneration();
	}
	
	public boolean notOk()
	{
		if (tgtPart.size() > maxGrammarComponents || tgtPart.size()> maxPhraseComponents)
		{
			return true;
		}
		
		int cnt = 0;
		for (ParseNode part : tgtPart)
		{
			if (part.isString())
			{
				cnt += part.numChildren();
			}
			else
			{
				cnt++;
			}
		}
		
		if (cnt > maxGrammarComponents || cnt > maxPhraseComponents)
		{
			return true;
		}
		
		return false;
	}

	public ParseNodeRulePart(List<ParseNode> srcPart, List<ParseNode> tgtPart, 
							 int maxGrammarSize, int maxPhraseSize,
							 boolean fromSource, boolean isPhrase,
							 int minSrcGeneration, int minTgtGeneration,
							 boolean genReorderList)
	{
		maxGrammarComponents = maxGrammarSize;
		maxPhraseComponents = maxPhraseSize;
		
		this.isPhrase = isPhrase;
		
		Collections.sort(srcPart);
		Collections.sort(tgtPart);
		
		this.fromSource = fromSource;
		
		srcSpan = new Span(srcPart.get(0).getSpanStart(),
				   srcPart.get(srcPart.size()-1).getSpanEnd());
		tgtSpan = new Span(tgtPart.get(0).getSpanStart(),
		   		   tgtPart.get(tgtPart.size()-1).getSpanEnd());
		this.srcPart = srcPart;
		this.tgtPart = tgtPart;
		
		this.minSrcGeneration = minSrcGeneration;
		this.minTgtGeneration = minTgtGeneration;
		
		if (genReorderList)
		{
			this.reorderList = new ReorderingList(srcPart, tgtPart, fromSource);
		}
	}
	
	public ParseNodeRulePart combineWith(ParseNodeRulePart rulePart,
										 List<ParseNode> extraAligns,
										 boolean alignsForSrc,
										 boolean genReorderList)
	{
		return combineParts(this, rulePart, extraAligns, alignsForSrc, genReorderList);
	}
	
	/**
	 * Combines a second rule part with this rule part.
	 * Either the entire part is added, or nothing at all.
	 * This ensures that the rulePart maintains its integrity
	 * (i.e. nothing is "missing").
	 * 
	 * @param rulePart The part to append to this RulePart
	 * @return true if the part is added, else false.
	 */
	public ParseNodeRulePart combineWith(ParseNodeRulePart rulePart, 
			                             boolean genReorderList)
	{
		return combineParts(this, rulePart, genReorderList);
	}
	
	public int getMaxPhraseComponents()
	{
		return maxPhraseComponents;
	}
	
	public Span getTgtSpan()
	{
		return tgtSpan;
	}
	
	public Span getSrcSpan()
	{
		return srcSpan;
	}
	
	public int getMinSrcGeneration()
	{
		return minSrcGeneration;
	}
	
	public int getMinTgtGeneration()
	{
		return minTgtGeneration;
	}
	
	public boolean spansMatch(ParseNode node1, ParseNode aligned)
	{
		ParseNode src;
		ParseNode tgt;
		
		if (fromSource)
		{
			src = node1;
			tgt = aligned;
		}
		else
		{
			src = aligned;
			tgt = node1;
		}

		// null node will always be first.
		// if there is only the null node, well, then it's wrong!
		// so figure something out with that.
		return tgtSpan.getStart() == tgt.getSpanStart() &&
			   tgtSpan.getEnd() == tgt.getSpanEnd() &&
			   srcSpan.getStart() == src.getSpanStart() &&
			   srcSpan.getEnd() == src.getSpanEnd();
			
	}

	public static ParseNodeRulePart combineParts(ParseNodeRulePart left, 
									ParseNodeRulePart right, 
									List<ParseNode> extraAligned,
									boolean alignedToSrc,
									boolean genReorderList)
	{
		removeNulls(left);
		removeNulls(right);

		if (!(left.tgtSpan.isNonOverlapping(right.tgtSpan)))
		{
			return null;
		}
		
		ArrayList<ParseNode> newSrcRepresentation = new ArrayList<ParseNode>();
		newSrcRepresentation.addAll(left.srcPart);
		newSrcRepresentation.addAll(right.srcPart);
		
		if (alignedToSrc)
		{
			addUncoveredTerminals(extraAligned, newSrcRepresentation);
		}

		if (newSrcRepresentation.size() == 0)
		{
			newSrcRepresentation.add(new NullParseNode());
		}

		ArrayList<ParseNode> newTgtRepresentation = new ArrayList<ParseNode>();
		newTgtRepresentation.addAll(left.tgtPart);
		newTgtRepresentation.addAll(right.tgtPart);
		
		if (!alignedToSrc)
		{
			addUncoveredTerminals(extraAligned, newTgtRepresentation);
		}

		if (newTgtRepresentation.size() == 0)
		{
			newTgtRepresentation.add(new NullParseNode());
		}
		
		boolean isPhrase = right.isPhrase && left.isPhrase;
		int maxSize;
		if (isPhrase)
		{
			maxSize = Math.max(left.maxGrammarComponents, left.maxPhraseComponents);
		}
		else
		{
			maxSize = left.maxGrammarComponents;
		}
	
		if ( newSrcRepresentation.size() > maxSize 
		     || newTgtRepresentation.size() > maxSize)
		{
			return null;
		}

		int minSrcGen = Math.min(right.minSrcGeneration, left.minSrcGeneration);
		int minTgtGen = Math.min(right.minTgtGeneration, left.minTgtGeneration);
		
		return new ParseNodeRulePart(
				newSrcRepresentation, newTgtRepresentation,
				left.maxGrammarComponents, left.maxPhraseComponents,
				left.fromSource, isPhrase, minSrcGen, minTgtGen, genReorderList);
	}
	
	public ParseNodeRulePart getWithUnalignedAdded(List<ParseNode> extraAligned)
	{
		ParseNodeRulePart part = new ParseNodeRulePart();
		
		part.fromSource = this.fromSource;
		part.isPhrase = this.isPhrase;
		part.maxGrammarComponents = this.maxGrammarComponents;
		part.maxPhraseComponents = this.maxPhraseComponents;
		part.minSrcGeneration = this.minSrcGeneration;
		part.minTgtGeneration = this.minTgtGeneration;
		part.srcPart = new ArrayList<ParseNode>();
		part.srcPart.addAll(this.srcPart);
		part.tgtPart = new ArrayList<ParseNode>();
		part.tgtPart.addAll(this.tgtPart);
		
		addUncoveredTerminals(extraAligned, part.tgtPart);
		int maxSize;
		if (part.isPhrase)
		{
			maxSize = maxPhraseComponents;
		}
		else
		{
			maxSize = maxGrammarComponents;
		}
		
		if (part.tgtPart.size() > maxSize)
		{
			return null;
		}
		
		Collections.sort(part.tgtPart);
		

		part.srcSpan = this.srcSpan;
		if (part.tgtPart.size() > 0)
		{
			part.tgtSpan = new Span(part.tgtPart.get(0).spanStart, 
				part.tgtPart.get(part.tgtPart.size() -1).spanEnd);
		}
		else
		{
			part.tgtSpan = this.tgtSpan;
		}
			
		// get at the End.
		part.reorderList = new ReorderingList(part.srcPart, part.tgtPart, true);
		
		return part;
	}

	private static void addUncoveredTerminals(List<ParseNode> extraAligned,
			List<ParseNode> newRepresentation) 
	{
		if (extraAligned != null)
		{
			Set<Integer> coveredPositions = new HashSet<Integer>();
			
			for (ParseNode node : newRepresentation)
			{
				for (int i = node.spanStart; i <= node.spanEnd; i++)
				{
					coveredPositions.add(i);
				}
			}
			for (ParseNode node : extraAligned)
			{
				if (!coveredPositions.contains(node.spanStart))
				{
					newRepresentation.add(node);
				}
			}
			
		}
	}

	public boolean contains(ParseNode node, boolean searchInTgt)
	{
		if (searchInTgt)
		{
			return tgtPart.contains(node);
		}
		else
		{
			return srcPart.contains(node);
		}
	}
	
	public static ParseNodeRulePart combineParts(ParseNodeRulePart left, 
			                                     ParseNodeRulePart right,
			                                     boolean genReorderList)
	{
		return combineParts(left, right, null, true, genReorderList);
	}
	
	
	private static void removeNulls(ParseNodeRulePart part)
	{
		while (part.srcPart.size() > 0 && 
			   part.srcPart.get(0) instanceof NullParseNode)
		{
			part.srcPart.remove(0);
		}
		while (part.tgtPart.size() > 0 && 
			   part.tgtPart.get(0) instanceof NullParseNode)
		{
			part.tgtPart.remove(0);
		}
	}

	/**
	 * Get the type of this rule part
	 * @return "P" if only has terminals; else "G"
	 */
	public String getType()
	{
		if (isPhrase)
		{
			return "P";
		}
		
		return "G";
	}
	
	@Override
	public String toString()
	{
		return toString("|||", true);
	}
	
	public String toString(boolean sourceFirst)
	{
		return toString( "|||", sourceFirst);
	}
	public String toString(String delim)
	{
		return toString(delim, true);
	}
	
	public String toString(String delim, boolean sourceFirst)
	{
		int cnt = 1; 
		StringBuilder part1 = new StringBuilder();
		
		List<ParseNode> first, second;
		if(sourceFirst)
		{
			first = srcPart;
			second = tgtPart;
		}
		else
		{
			first = tgtPart;
			second = srcPart;
		}
		
		int size = Math.max(second.size(), first.size());
		ArrayList<Integer> backwardsMapIndex = new ArrayList<Integer>(size);
		ArrayList<Integer> backwardsMapNodeIndex = new ArrayList<Integer>(size);
		
		for (int i = 0; i < size; i++)
		{
			backwardsMapIndex.add(-1);
			backwardsMapNodeIndex.add(-1);
		}
		
		for (int i = 0; i < first.size(); i++)
		{
			ParseNode node = first.get(i);
			if (node.isTerminal())
			{
				part1.append(node.getCategory());
				part1.append(" ");
			}
			else if (!(node instanceof NullParseNode))
			{
				part1.append('[');
				
				Integer node2Index = reorderList.fromSource(i).get(0);
				
				ParseNode node2 = second.get(node2Index);
				
				part1.append(first.get(i));
				part1.append("::");
				part1.append(node2);
				part1.append(',');
				part1.append(cnt);
				part1.append("] ");
				
				backwardsMapIndex.set(node2Index, cnt);
				backwardsMapNodeIndex.set(node2Index, i);
				cnt++;
			}
		}
		
		StringBuilder part2 = new StringBuilder();
		
		for (int i = 0; i < second.size(); i++)
		{
			ParseNode node = second.get(i);
			
			if (node.isTerminal())
			{
				part2.append(node.getCategory());
				part2.append(' ');
			}
			else if (!(node instanceof NullParseNode))
			{
				part2.append('[');
				int node1Index = backwardsMapNodeIndex.get(i);
				ParseNode node1 = new NullParseNode();
				try
				{
					node1 = first.get(node1Index);
				}
				catch(Exception e)
				{
					System.out.println("exception");
				}
				part2.append(node1);
				part2.append("::");
				part2.append(second.get(i));
				part2.append(",");
				part2.append(backwardsMapIndex.get(i));
				part2.append("] ");
				cnt++;
			}
		}
	
		String result = part1.toString().trim() + delim 
		                + part2.toString().trim() + delim
		                + reorderList.toString(sourceFirst);

		return result;
	}
	
	
	public String getAlignTypeString()
	{
		return getAlignTypeString(true);
	}
	
	public String getAlignTypeString(boolean sourceFirst) 
	{
		String str = "";
		
		List<ParseNode> src = srcPart;
		List<ParseNode> tgt = tgtPart;
		
		for (int i = 0; i < src.size(); i++)
		{
			Integer node2Index = reorderList.fromSource(i).get(0);
			ParseNode node = sourceFirst ? src.get(i) : tgt.get(node2Index);
			if (!node.isTerminal() && 
			    !(node instanceof NullParseNode))
			{
				if (node.isReal())
				{
					str+= "O";
				}
				else
				{
					str+= "V";
				}
			
				ParseNode node2 = sourceFirst ? tgt.get(node2Index) : src.get(i);
				
				if (node2.isReal())
				{
					str+= "O ";
				}
				else
				{
					str+= "V ";
				}
			}
		}
	
		return str.trim();
	}
	
	public boolean isParallelUnary() 
	{
		return srcPart.size() == 1
		       && tgtPart.size() == 1
		       && !srcPart.get(0).isTerminal()
		       && !tgtPart.get(0).isTerminal();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ParseNodeRulePart) {
			ParseNodeRulePart rulePart = 
				(ParseNodeRulePart) o;
			boolean equals = srcPart.equals(rulePart.srcPart);
			if (!equals)
			{
				return false;
			}
			equals = tgtPart.equals(rulePart.tgtPart);
			return equals 
			       && this.srcSpan.equals(rulePart.srcSpan)
			       && this.tgtSpan.equals(rulePart.tgtSpan);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int hashCode = 0;
		
		hashCode += srcPart.hashCode();
		hashCode = 31*hashCode + tgtPart.hashCode();
		hashCode = 31*hashCode + srcSpan.hashCode();
		hashCode = 31*hashCode + tgtSpan.hashCode();
		
		return hashCode;
	}
	
	private List<ParseNode> srcPart;
	private List<ParseNode> tgtPart;
	private ReorderingList reorderList;
	
	private int maxGrammarComponents;
	private int maxPhraseComponents;
	private boolean fromSource;
	
	private Span srcSpan;
	private Span tgtSpan;
	
	private boolean isPhrase;
	
	private int minSrcGeneration;
	private int minTgtGeneration;
}

