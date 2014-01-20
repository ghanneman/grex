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
		
		if(genReorderList || true)
			this.reorderList = new ReorderingList(srcPart, tgtPart, fromSource);
	}

	private void intialize(ParseNode source, ParseNode target, 
						   boolean fromSource) {
		coveredSrcWords = new HashSet<Integer>();
		for (int i = source.getSpanStart(); i <= source.getSpanEnd(); ++i)
			coveredSrcWords.add(i);

		coveredTgtWords = new HashSet<Integer>();
		for (int i = target.getSpanStart(); i <= target.getSpanEnd() && i >= 0; ++i)
			coveredTgtWords.add(i);

		
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
		
		sanityCheck();
	}
	
	public boolean notOk()
	{
		if (tgtPart.size() > maxGrammarComponents || tgtPart.size() > maxPhraseComponents)
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
	
	public void sanityCheck()
	{
		List<ParseNode> sourceNonTerminals = new ArrayList<ParseNode>();
		List<ParseNode> targetNonTerminals = new ArrayList<ParseNode>();
		
		for(ParseNode node : srcPart)
			if(!node.isTerminal())
				sourceNonTerminals.add(node);
		
		for(ParseNode node : tgtPart)
			if(!node.isTerminal())
				targetNonTerminals.add(node);
		
		if(sourceNonTerminals.size() != targetNonTerminals.size())
			throw new RuntimeException();
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
		
		coveredSrcWords = new HashSet<Integer>();
		for (int i = 0; i < srcPart.size(); ++i) {
			for (int j = srcPart.get(i).getSpanStart(); j <= srcPart.get(i).getSpanEnd(); ++j)
				coveredSrcWords.add(j);
		}

		coveredTgtWords = new HashSet<Integer>();
		for (int i = 0; i < tgtPart.size(); ++i) {
			for (int j = tgtPart.get(i).getSpanStart(); j <= tgtPart.get(i).getSpanEnd() && j >= 0; ++j)
				coveredTgtWords.add(j);
		}

		this.srcPart = srcPart;
		this.tgtPart = tgtPart;
	
		this.minSrcGeneration = minSrcGeneration;
		this.minTgtGeneration = minTgtGeneration;
		
		sanityCheck();
		if (genReorderList || true)
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
	
	public Set<Integer> getTgtCoverage()
	{
		return coveredTgtWords;
	}
	
	public Set<Integer> getSrcCoverage()
	{
		return coveredSrcWords;
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

		HashSet<Integer> srcCoverage = new HashSet<Integer>();
		for (int i = src.getSpanStart(); i <= src.getSpanEnd(); ++i)
			srcCoverage.add(i);

		HashSet<Integer> tgtCoverage = new HashSet<Integer>();
		for (int i = tgt.getSpanStart(); i <= tgt.getSpanEnd(); ++i)
			tgtCoverage.add(i);

		return coveredSrcWords.equals(srcCoverage) &&
			coveredTgtWords.equals(tgtCoverage);	
	}

	public static ParseNodeRulePart combineParts(ParseNodeRulePart left, 
									ParseNodeRulePart right, 
									List<ParseNode> extraAligned,
									boolean alignedToSrc,
									boolean genReorderList)
	{
		removeNulls(left);
		removeNulls(right);

		for (int i : left.coveredTgtWords) {
			if (right.coveredTgtWords.contains(i)) {
				return null;
			}
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
		
		ParseNodeRulePart result = new ParseNodeRulePart(
				newSrcRepresentation, newTgtRepresentation,
				left.maxGrammarComponents, left.maxPhraseComponents,
				left.fromSource, isPhrase, minSrcGen, minTgtGen, genReorderList);
		return result;
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
		part.coveredSrcWords = this.coveredSrcWords;
		part.coveredTgtWords = this.coveredTgtWords;
		
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
		
		for (ParseNode extra : extraAligned) {
			for (int i = extra.getSpanStart(); i <= extra.getSpanEnd() && i >= 0; ++i)
				part.coveredTgtWords.add(i);
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
	
	public String partToString(List<ParseNode> nodeSet, List<ParseNode> src, List<ParseNode> tgt, 
			List<Integer> srcIndices, List<Integer> tgtIndices, List<Integer> mapIndices)
	{
		StringBuilder part = new StringBuilder();
		
		for (int i = 0; i < nodeSet.size(); i++)
		{
			ParseNode node = nodeSet.get(i);
			
			if (node.isTerminal())
			{
				part.append(node.getCategory());
				part.append(" ");
			}
			else if (!(node instanceof NullParseNode))
			{
				part.append('[');
				
				int node1Index = (srcIndices == null) ? i : srcIndices.get(i);
				int node2Index = (tgtIndices == null) ? i : tgtIndices.get(i);
				ParseNode node1 = (node1Index != -1) ? src.get(node1Index) : null;
				ParseNode node2 = (node2Index != -1) ? tgt.get(node2Index) : null;
				
				part.append(node1);
				part.append("::");
				part.append(node2);
				part.append(',');
				part.append(mapIndices.get(i));
				part.append("] ");
			}
		}
		return part.toString();
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
		List<ParseNode> src, tgt;
		src = srcPart;
		tgt = tgtPart;
		
		ArrayList<Integer> mapIndex = new ArrayList<Integer>(src.size()); // src index to cnt
		ArrayList<Integer> backwardsMapIndex = new ArrayList<Integer>(tgt.size()); // tgt index to cnt
		ArrayList<Integer> mapNodeIndex = new ArrayList<Integer>(src.size()); // src index to tgt index
		ArrayList<Integer> backwardsMapNodeIndex = new ArrayList<Integer>(tgt.size()); // tgt index to src index
		
		for (int i = 0; i < src.size(); i++)
		{
			mapIndex.add(-1);
			mapNodeIndex.add(-1); 
		}
		
		for (int i = 0; i < tgt.size(); i++)
		{
			backwardsMapIndex.add(-1);
			backwardsMapNodeIndex.add(-1);
		}
	
		for (int i = 0; i < src.size(); i++)
		{
			ParseNode node = src.get(i);
			if(!node.isTerminal() && !(node instanceof NullParseNode))
			{
				Integer node2Index = reorderList.fromSource(i).get(0);
				mapNodeIndex.set(i, node2Index);
				backwardsMapNodeIndex.set(node2Index, i);
			}
		}
		
		int cnt = 1; 
		List<ParseNode> nodeSet = (sourceFirst) ? src : tgt;
 		for (int i = 0; i < nodeSet.size(); i++)
		{
			ParseNode node = nodeSet.get(i);
			if(!node.isTerminal() && !(node instanceof NullParseNode))
			{
				Integer node1Index = (sourceFirst) ? i : backwardsMapNodeIndex.get(i);
				Integer node2Index = (sourceFirst) ? mapNodeIndex.get(i) : i;
				mapIndex.set(node1Index, cnt);
				backwardsMapIndex.set(node2Index, cnt);
				cnt++;
			}
		}
		
		StringBuilder part1, part2;
		if(sourceFirst)
		{
			part1 = new StringBuilder(this.partToString(src, src, tgt, null, mapNodeIndex, mapIndex));
			part2 = new StringBuilder(this.partToString(tgt, src, tgt, backwardsMapNodeIndex, null, backwardsMapIndex));
		}
		else
		{
			// TODO: Must re-sort backwards/forwards map index to ensure part1 is well ordered
			part1 = new StringBuilder(this.partToString(tgt, tgt, src, null, backwardsMapNodeIndex, backwardsMapIndex));
			part2 = new StringBuilder(this.partToString(src, tgt, src, mapNodeIndex, null, mapIndex));
		}
	
		String result = part1.toString().trim() + delim 
		                + part2.toString().trim() + delim
		                + reorderList.toString(sourceFirst);

		return result;
	}
	
	 public String toStringOld(String delim)
     {
             int cnt = 1; 
             StringBuilder part1 = new StringBuilder();
             
             List<ParseNode> src, tgt;
             src = srcPart;
             tgt = tgtPart;
             
             int size = Math.max(tgt.size(), src.size());
             ArrayList<Integer> backwardsMapIndex = new ArrayList<Integer>(size);
             ArrayList<Integer> backwardsMapNodeIndex = new ArrayList<Integer>(size);
             
             for (int i = 0; i < size; i++)
             {
                     backwardsMapIndex.add(-1);
                     backwardsMapNodeIndex.add(-1);
             }
             
             for (int i = 0; i < src.size(); i++)
             {
                     ParseNode node = src.get(i);
                     if (node.isTerminal())
                     {
                             part1.append(node.getCategory());
                             part1.append(" ");
                     }
                     else if (!(node instanceof NullParseNode))
                     {
                             part1.append('[');
                             
                             Integer node2Index = reorderList.fromSource(i).get(0);
                             
                             ParseNode node2 = tgt.get(node2Index);
                             
                             part1.append(src.get(i));
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
             
             for (int i = 0; i < tgt.size(); i++)
             {
                     ParseNode node = tgt.get(i);
                     
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
                                     node1 = src.get(node1Index);
                             }
                             catch(Exception e)
                             {
                                     System.err.println("exception");
                             }
                             part2.append(node1);
                             part2.append("::");
                             part2.append(tgt.get(i));
                             part2.append(",");
                             part2.append(backwardsMapIndex.get(i));
                             part2.append("] ");
                             cnt++;
                     }
             }
     
             String result = part1.toString().trim() + delim 
                             + part2.toString().trim() + delim
                             + reorderList;

             return result;
     }
	 
	 public String toShortString()
	 {
		 return this.srcPart.toString() + " ||| " + this.tgtPart.toString();
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
			ParseNode node = src.get(i);
			if (!node.isTerminal() && 
			    !(node instanceof NullParseNode))
			{
				Integer node2Index = reorderList.fromSource(i).get(0);
				ParseNode node2 = tgt.get(node2Index);
				
				if ((node.isReal() && sourceFirst) || (node2.isReal() && !sourceFirst))
					str+= "O";
				else
					str+= "V";
				
				if ((node2.isReal() && sourceFirst) || (node.isReal() && !sourceFirst))
					str+= "O";
				else
					str+= "V";
				str += " ";
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
				&& this.coveredSrcWords.equals(rulePart.coveredSrcWords)
				&& this.coveredTgtWords.equals(rulePart.coveredTgtWords);
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
		hashCode = 31*hashCode + coveredSrcWords.hashCode();
		hashCode = 31*hashCode + coveredTgtWords.hashCode();
		
		return hashCode;
	}
	
	private List<ParseNode> srcPart;
	private List<ParseNode> tgtPart;
	private ReorderingList reorderList;
	
	private int maxGrammarComponents;
	private int maxPhraseComponents;
	private boolean fromSource;

	private HashSet<Integer> coveredSrcWords;
	private HashSet<Integer> coveredTgtWords;	
	
	private boolean isPhrase;
	
	private int minSrcGeneration;
	private int minTgtGeneration;
}

