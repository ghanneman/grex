package ruleLearnerNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ruleLearnerNew.NodeAlignmentList.NodeAlignmentType;


/**
 * @author ghannema
 *
 */
public class BaseGrammarExtractor implements GrammarExtractor
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public BaseGrammarExtractor(int maxGrammarRuleSize, 
								int maxPhraseRuleSize,
			 					boolean allowTriangularRules,
			 					boolean minimalRulesOnly)
	{
		this.maxGrammarRuleSize = maxGrammarRuleSize;
		this.maxPhraseRuleSize = maxPhraseRuleSize;
		this.allowTriangularRules = allowTriangularRules;
		this.minimalRulesOnly = minimalRulesOnly;
	}

	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public Set<ExtractedRule> extract(ParseNode srcRoot, ParseNode tgtRoot)
	{
		Set<ExtractedRule> rules = new HashSet<ExtractedRule>();
		
		ArrayList<NodeAlignmentType> types = new ArrayList<NodeAlignmentType>();
		
		NodeAlignmentType[] typeArray = {NodeAlignmentType.T2T,NodeAlignmentType.TS2T,
										 NodeAlignmentType.T2TS,NodeAlignmentType.SRC_GROWN,
										 NodeAlignmentType.TGT_GROWN,NodeAlignmentType.TS2TS, 
										 NodeAlignmentType.T2S, NodeAlignmentType.S2T};
		
		for (NodeAlignmentType type : typeArray)
		{
			types.add(type);
		}
		
		extractRulesForNode(srcRoot, true, rules, types, types);
		
		Set<ExtractedRule> oldRules = new HashSet<ExtractedRule>();
		oldRules.addAll(rules);
		
		if (allowTriangularRules)
		{
			ArrayList<NodeAlignmentType> tgtTypes = new ArrayList<NodeAlignmentType>();
			tgtTypes.add(NodeAlignmentType.T2TS);
			tgtTypes.add(NodeAlignmentType.T2T);
			
			extractRulesForNode(tgtRoot, false, rules, tgtTypes, types);
		}
		
		return rules;
	}

	// Bottom-up recursive:
	/**
	 * @param node1 The node to extract to
	 * @param side1IsSrc Whether or not node1 is a src side node
	 * @param type The type of alignment allowed on the LEFT side
	 * @param rules A collection in which to store the extracted rules
	 * @param types The types of alignments allowed on the RIGHT side
	 */
	public void extractRulesForNode(ParseNode node1,
									boolean side1IsSrc,
									Set<ExtractedRule> rules,
									Collection<NodeAlignmentType> lhsTypes,
									Collection<NodeAlignmentType> rhsTypes)
	{	
		// Extract grammar rules from all this node's children:
		
		if (!node1.hasNoExtractedRules())
		{
			return;
		}
		
		for (ParseNode child : node1.getVirtualChildren())
		{
			extractRulesForNode(child, side1IsSrc, rules, lhsTypes, rhsTypes);
		}
		
		for(ParseNode child : node1.getChildren())
		{
			if (node1.isLhs())
			{
				extractRulesForNode(child, side1IsSrc, rules, lhsTypes, rhsTypes);
			}
		}
		
		// Then extract grammar from the node itself: ---
		Collection<List<ParseNode>> childrenLists = null;
		
		if (!node1.isTerminal())
		{
			childrenLists = getDecompPoints(node1, node1.getChildren());
		}
		
		if (childrenLists == null)
		{
			return;
		}
		
		Set<ParseNode> nodeAlignments = node1.getNodeAlignments(lhsTypes);
		for(ParseNode aligned : nodeAlignments)
		{		
			// Now we have to build the right hand-side of the rule,
			// but this is really just the composition of all rules
			// that already exist its children's rules.

			if (!node1.isLhs())
			{
				ExtractedRule rule = new ExtractedRule(node1, 
													   aligned, 
													   new ParseNodeRulePart(),
													   side1IsSrc);
				node1.addRule(rule);
			}
			else
			{	
				for (List<ParseNode> children : childrenLists)
				{
					// TODO: We can check for many-to-one alignments here.
					// Simply check each slice of children
					// and see if their alignments map to a single target node.
					
					ExtractedRule rule;	
					Collection<ParseNodeRulePart> rhsSides = 
						 getRhs(children, aligned, side1IsSrc, rhsTypes);

					for (ParseNodeRulePart rhs : rhsSides)
					{	
						if (rhs.spansMatch(node1, aligned) 
							&& (( side1IsSrc 
								 && node1.getGeneration() <= rhs.getMinSrcGeneration()
								 && aligned.getGeneration() <= rhs.getMinTgtGeneration())
						    || (!side1IsSrc 
						    	  && node1.getGeneration() <= rhs.getMinTgtGeneration()
						    	  && aligned.getGeneration() <= rhs.getMinSrcGeneration())))
						{
							rule = new ExtractedRule(node1, aligned, rhs, side1IsSrc);
							
							node1.addRule(rule);
							
							//Don't add in the T2S mappings to original rule.
							if (aligned.isLhs())
							{ 
								if (rules.contains(rule) == false)
								{
									rules.add(rule);
								}
							}
						}
					 }
				}				
					
			}
		}	
	}
	
	
	/**
	 * Generates RHS from a set of nodes.
	 * @param maxLength The max length of a RHS half allowed.
	 * @param nodes The nodes to extract RHS parts from
	 * @param alignedNode The parent node for the opposite side of the rules
	 * @param side1isSrc True if nodes are source nodes, else false.
	 * @param types Variable number of allowed alignment types
	 * @return A Collection of ParseNodeRuleParts (i.e. right hand sides)
	 */
	public Collection<ParseNodeRulePart> getRhs
		   (Collection<ParseNode> nodes, 
		    ParseNode alignedNode, boolean side1isSrc, 
		    Collection<NodeAlignmentType> types)
	{
		List<ParseNode> unalignedTerminals = 
			alignedNode.getUnalignedTerminals();
		
		Collection<ParseNodeRulePart> expansions = null;
		Collection<ParseNodeRulePart> subExpansions;
		
		int remaining = nodes.size();
		
		if (remaining > Math.max(maxGrammarRuleSize, maxPhraseRuleSize))
		{
			return new LinkedList<ParseNodeRulePart>();
		}
		
		for(ParseNode child1 : nodes) 
		{
			// In this case all previous expansions were too long
			// There are no short enough RHS, so return an empty list
			if (expansions != null && expansions.isEmpty())
			{
				return new LinkedList<ParseNodeRulePart>();
			}
			
			subExpansions = child1.getRuleAlignments(maxGrammarRuleSize,
					 maxPhraseRuleSize,
                     side1isSrc, 
                     types,
                     allowTriangularRules,
                     minimalRulesOnly,
                     alignedNode);
			
			ParseNode nextGen = child1;
			while (nextGen.numChildren() == 1 && !minimalRulesOnly)
			{
				nextGen = nextGen.getChildren().get(0);
				subExpansions.addAll(nextGen.getRuleAlignments(
									maxGrammarRuleSize,
									maxPhraseRuleSize,
									side1isSrc, 
									allowTriangularRules));
			}
			
			Collection<ParseNodeRulePart> parts = 
				new ArrayList<ParseNodeRulePart>();
			
			if (expansions == null)
			{
				if (allowTriangularRules)
				{
					expansions = subExpansions;
				}
				else
				{
					expansions = new ArrayList<ParseNodeRulePart>(subExpansions.size());
					
					for (ParseNodeRulePart part : subExpansions)
					{ 
						if(!part.contains(alignedNode, side1isSrc))
						{
							ParseNodeRulePart expandedPart = part;
							if(remaining == 1)
								expandedPart = part.getWithUnalignedAdded(unalignedTerminals);
							if (expandedPart != null)
							{
								expansions.add(expandedPart);
							}
						}
					}
				}
			}
			else 
			{
				for (ParseNodeRulePart base : expansions)
				{
					for (ParseNodeRulePart expansion : subExpansions)
					{
						ParseNodeRulePart combined;
						
						if (allowTriangularRules || 
							!expansion.contains(alignedNode, side1isSrc))
						{
							if (remaining == 1)
							{
								combined = base.combineWith(expansion, 
															unalignedTerminals, 
															!side1isSrc,
															true);
							}
							else
								combined = base.combineWith(expansion, false);
							
							if (combined != null)
								parts.add(combined);
						}
					}
				}
				expansions = parts;
			}
			remaining --;
		}
		
		if (expansions == null)
		{
			expansions = new HashSet<ParseNodeRulePart>();
		}
		
		return expansions;
	}	

	/** Given a starting node, returns a list of descendant nodes that are the
	 *  next decomposition points down in the tree for a certain type of node 
	 *  alignment.
	 *  
	 * @param children The children to decompose
	 * @param types Variable number of alignment types which nodes may have
	 * to be included in the decomposition
	 * @return A List of the children appropriately decomposed.
	 */
	public Collection<List<ParseNode>> getDecompPoints(ParseNode parent,
										   List<ParseNode> children)
	{				
		int spanLength = parent.spanEnd - parent.spanStart + 1;
		
		int maxSize;
		
		if (spanLength > maxPhraseRuleSize)
		{
			maxSize = maxGrammarRuleSize;
		}
		else
		{
			maxSize = Math.max(maxGrammarRuleSize, maxPhraseRuleSize);
		}
		
		List<Map<ParseNode,Integer>> virtualResults = 
			new ArrayList<Map<ParseNode,Integer>>();
		
		for (int i = 0; i < spanLength; i++)
		{
			Map<ParseNode, Integer> skipMap = new HashMap<ParseNode,Integer>();
			virtualResults.add(skipMap);
		}
		
		for (ParseNode child : children)
		{
			//getDecompPointsHelper(child, virtualResults, types, parent.spanStart);
			getDecompPointsHelper(child, virtualResults, parent.spanStart);
		}
		
		for (ParseNode child : parent.getVirtualChildren())
		{
			//getDecompPointsHelper(child, virtualResults, types, parent.spanStart);
			getDecompPointsHelper(child, virtualResults, parent.spanStart);
		}
		
		//Now reconstruct.
		List<List<List<ParseNode>>> lists = new ArrayList<List<List<ParseNode>>>();
		
		for (int i = 0; i <= spanLength; i++)
		{
			List<List<ParseNode>> listOfLists = new ArrayList<List<ParseNode>>();
			lists.add(listOfLists);
		}
		
		List<ParseNode> firstList = new ArrayList<ParseNode>();
		lists.get(0).add(firstList);
		
		for (int i = 0; i < spanLength; i++)
		{
			List<List<ParseNode>> partialLists = lists.get(i);
				//removeRedundancies(lists.get(i));
			Set<ParseNode> keySet = virtualResults.get(i).keySet();
			for (ParseNode node : keySet)
			{
				for (List<ParseNode> partialList : partialLists)
				{
					if (partialList.size() < maxSize)
					{
						List<ParseNode> newList = 
							new ArrayList<ParseNode>(partialList.size());
						newList.addAll(partialList);
						newList.add(node);
						List<List<ParseNode>> list = lists.get(virtualResults.get(i).get(node));
						list.add(newList);
					}
				}

			}
			if (i < spanLength-1)
			{
				lists.get(i).clear();
			}
		}
		
		List<List<ParseNode>> results = lists.get(spanLength);
		
		return results;
	}
	
	private List<List<ParseNode>> removeRedundancies(List<List<ParseNode>> lists)
	{
		for (int i = 0; i < lists.size(); i++)
		{
			List<ParseNode> listToCheck = lists.get(i);
			if (listToCheck != null)
			{
				for (int j = 0; j < lists.size(); j++)
				{
					if (j == i || lists.get(j) == null)
					{
						continue;
					}
					
					List<ParseNode> listToCompare = lists.get(j);
					boolean nonCrossing = true;
					int checkNode = 0;
					int compareNode = 0;
					while (nonCrossing && checkNode < listToCheck.size() 
							&& compareNode < listToCompare.size())
					{
						int checkEndSpan = listToCheck.get(checkNode).spanEnd;
						int compareStartSpan = listToCompare.get(compareNode).spanStart;
						int compareEndSpan = listToCompare.get(compareNode).spanEnd;
						if (checkEndSpan > compareStartSpan)
						{
							if (checkEndSpan < compareEndSpan)
							{
								nonCrossing = false;
							}
							else
							{
								compareNode++;
							}
						}
						else
						{
							checkNode++;
						}
					}
					if (nonCrossing)
					{
						if (listToCheck.size() > listToCompare.size())
						{
							lists.set(i, null);
						}
						else
						{
							lists.set(j, null);
						}
					}
				}
			}
			
		}
		
		int cnt = 0;
		while (cnt < lists.size())
		{
			if (lists.get(cnt) == null)
			{
				lists.remove(cnt);
			}
			else
			{
				cnt++;
			}
		}
		return lists;
	}
	
	
	/**
	 * Helper for getDecompPoints
	 * @param node The node to decompose.
	 * @param results The decomposition.
	 * @param types The types allowed for decomposition.
	 * void
	 */
	public void getDecompPointsHelper(ParseNode node,
									  List<Map<ParseNode, Integer>> results,
							  		  Collection<NodeAlignmentType> types,
							  		  int spanStart)
	{
		
		// If this node is an unaligned dead end,
		if(node.isTerminal())
		{
			// want the terminal as a string node
			results.get(node.spanStart-spanStart).put(node,(node.spanEnd+1-spanStart));
			return;
		}

		// If this node has an alignment of the right type, return it as part
		// of the results:
		for (NodeAlignmentType alignType : types)
		{
			if(node.isNodeAligned(alignType))
			{
				results.get(node.spanStart-spanStart).put(node,(node.spanEnd+1-spanStart));
				return;
			}
		}

		// Otherwise, keep searching this node's children:
		for(ParseNode child : node.getChildren())
		{
			getDecompPointsHelper(child, results, types, spanStart);
		}
		
		for(ParseNode child : node.getVirtualChildren())
		{
			getDecompPointsHelper(child, results, types, spanStart);
		}
	}
	
	/**
	 * Helper for getDecompPoints
	 * @param node The node to decompose.
	 * @param results The decomposition.
	 * @param types The types allowed for decomposition.
	 * void
	 */
	public void getDecompPointsHelper(ParseNode node,
									  List<Map<ParseNode, Integer>> results,
							  		  int spanStart)
	{
		
		// If this node is an unaligned dead end,
		if(node.isTerminal())
		{
			// want the terminal as a string node
			results.get(node.spanStart-spanStart).put(node,(node.spanEnd+1-spanStart));
			return;
		}

		// If this node has an alignment of the right type, return it as part
		// of the results:
		if(node.isNodeAligned())
		{
			results.get(node.spanStart-spanStart).put(node,(node.spanEnd+1-spanStart));
			return;
		}

		// Otherwise, keep searching this node's children:
		for(ParseNode child : node.getChildren())
		{
			getDecompPointsHelper(child, results, spanStart);
		}
		
		for(ParseNode child : node.getVirtualChildren())
		{
			getDecompPointsHelper(child, results, spanStart);
		}
	}
	
	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	private int maxGrammarRuleSize;
	private int maxPhraseRuleSize;
	private boolean allowTriangularRules;
	private boolean minimalRulesOnly;
}
