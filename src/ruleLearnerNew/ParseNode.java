package ruleLearnerNew;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ruleLearnerNew.NodeAlignmentList.NodeAlignmentType;


/**
 * A ParseNode is a representation of a node in a syntactic structure. 
 * 
 * @author ghannema
 * @author mburroug
 */
public class ParseNode implements Comparable<ParseNode>
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public ParseNode()
	{
		this(-1,-1);
	}

	public ParseNode(int spanStart, int spanEnd)
	{
		this.initialize("", spanStart, spanEnd, null, null, 
				        null, null, null, null, null);
	}

	public ParseNode(String parenString) throws MalformedTreeException
	{
		this(parenString, 0);
	}
	
	/**
	 * Constructor intended to create terminal nodes.
	 * 
	 * @param val The value of this node
	 * @param spanStart
	 * @param spanEnd
	 */
	public ParseNode(String val, int spanStart, int spanEnd){

		this.initialize(val, spanStart, spanEnd, null, null, null, 
				        null, null, null, null);
	}
	
	private ParseNode(String parenString, int startIndex)
	throws MalformedTreeException
	{
		// Break the string into a node label and its yield or subtree:
		parenString = parenString.trim();
		Matcher m = parenCatAndSubtree.matcher(parenString);
		if(m.find())
		{
			String cat = m.group(1);
			String subtree = m.group(2);
			if(!subtree.contains("("))
			{
				// We have a terminal node
				
				this.initialize(cat, startIndex, startIndex, null, 
						       null, null, null, null, null, null);
				this.children.add(new TerminalNode(subtree,startIndex,this));
				return;
			}
			else
			{
				// Break the subtree into child subtrees:
				List<String> childSubtrees = new ArrayList<String>();
				int numParens = 0;
				int currStart = 0;
				for(int i = 0; i < subtree.length(); i++)
				{
					// Update number of parentheses deep we are:
					if(subtree.charAt(i) == '(') numParens++;
					if(subtree.charAt(i) == ')') numParens--;
					
					// A new child node is identified when we're back at zero:
					if((numParens == 0) && (i != currStart))
					{
						childSubtrees.add(subtree.substring(currStart, i + 1));
						currStart = i + 1;
					}
				}
				
				// Create a node, linked to this node, for each child subtree:
				
				int childStart = startIndex;
				
				this.initialize(cat, startIndex, childStart-1, 
						       null, null, null, null, null, null, null);
				
				for(String child : childSubtrees)
				{
					ParseNode n = new ParseNode(child, childStart);
					n.parent = this;
					this.children.add(n);
					childStart = n.spanEnd + 1;
				}
				
				int minChildGeneration = Integer.MAX_VALUE;
				for(ParseNode child : children)
					minChildGeneration = Math.min(minChildGeneration, child.generation);
				
				this.spanEnd = childStart - 1;
				this.generation = minChildGeneration - 1;
				
				return;
			}
		}
		else
		{
			// Error!  String isn't a valid tree structure.
			this.initialize("", -1, -1, null, null, null, null, null, null, null);
			throw new MalformedTreeException("Not a valid tree structure: '" +
											 parenString + "'");
		}
	}

	
	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public void initialize(String category, int spanStart, int spanEnd,
						   List<ParseNode> children, 
						   Map<Integer,Set<VirtualParseNode>> virtualChildren,
						   ParseNode parent, List<ExtractedRule> rules,
						   ArrayList<Set<ParseNode>> alignedTo,
						   GrammarExtractionState extractionState,
						   NodeAlignmentState alignmentState)
	{
		this.category = category;
		this.spanStart = spanStart;
		this.spanEnd = spanEnd;
		this.children = children;
		if (children == null)
		{
			this.children = new ArrayList<ParseNode>();
		}
		this.virtualChildren = virtualChildren;
		if (virtualChildren == null)
		{
			this.virtualChildren = new HashMap<Integer,Set<VirtualParseNode>>();
		}
		this.parent = parent;
		
		if (parent == null)
		{
			this.generation = 0;
		}
		else
		{
			this.generation = parent.generation + 1;
		}
		
		this.rules = rules;
		if (rules == null)
		{
			this.rules = new ArrayList<ExtractedRule>();
		}
		this.alignedTo = alignedTo;
		if (alignedTo == null)
		{
			this.alignedTo = new ArrayList<Set<ParseNode>>(NodeAlignmentType.values().length);
			for (int i = 0; i < NodeAlignmentType.values().length; i++)
			{
				this.alignedTo.add(null);
			}
		}
		this.extractionState = extractionState;
		if (extractionState == null)
		{
			this.extractionState = new GrammarExtractionState();
		}
		this.alignmentState = alignmentState;
		if (alignmentState == null)
		{
			this.alignmentState = new NodeAlignmentState();
		}
		this.alignmentsMinimized = false;
	}

	/**
	 * 
	 * @return The category of this ParseNode. For terminals, this is its string value.
	 */
	public String getCategory()
	{
		return category;
	}
	
	public int getGeneration()
	{
		return generation;
	}


	/**
	 * Set the category of this ParseNode
	 * @param category The new category for this ParseNode
	 */
	public void setCategory(String category)
	{
		this.category = category;
	}

	/**
	 * Get the start of the coverage span of this node.
	 * @return The start of the span as an integer.
	 */
	public int getSpanStart()
	{
		return spanStart;
	}

	/**
	 * Get the end of the coverage span of this node.
	 * @return The end of the span as an integer.
	 */
	public int getSpanEnd()
	{
		return spanEnd;
	}
	
	/**
	 * Compares this span of this node to the input parameter.
	 * @param span A span to compare to
	 * @return True if the span is the same, else false.
	 */
	public boolean hasSameSpan(Span span)
	{
		return span.getEnd() == spanEnd &&
			   span.getStart() == spanStart;
	}

	/**
	 * Get the (real) children of this ParseNode.
	 * @return A list of this ParseNode's children.
	 */
	public List<ParseNode> getChildren()
	{
		return children;
	}

	/**
	 * Gets all virtual children of a ParseNode. There is no 
	 * guarantee about the ordering of these children.
	 * 
	 * @return A list of virtual ParseNode children.
	 */
	public List<ParseNode> getVirtualChildren()
	{
		List<ParseNode> children = new ArrayList<ParseNode>();
		
		if (virtualChildren == null)
		{
			return children;
		}
		
		Collection<Set<VirtualParseNode>> childrenSets = virtualChildren.values();
		
		for (Set<VirtualParseNode> parseNodeSet : childrenSets)
		{
			children.addAll(parseNodeSet);
		}
		
		return children;
	}
	
	/**
	 * Gets the virtual child with the given range.
	 * 
	 * @return The appropriate virtual child, or null if it doesn't exist
	 */
	public VirtualParseNode getVirtualChild(int start, int end)
	{		
		if (virtualChildren == null)
		{
			return null;
		}
		
		Set<VirtualParseNode> childrenSet = virtualChildren.get(start);
		
		if (childrenSet == null)
		{
			return null;
		}
		
		for (VirtualParseNode node : childrenSet)
		{
			if (node.spanEnd == end)
			{
				return node;
			}
		}
		
		return null;
	}
	
	public Set<VirtualParseNode> getVirtualChildren(int start)
	{		
		if (virtualChildren == null)
		{
			return null;
		}
		
		Set<VirtualParseNode> childrenSet = virtualChildren.get(start);
		
		return childrenSet;
	}

	/**
	 * Get the number of real children this node has.
	 * @return The number of children this node has.
	 */
	public int numChildren()
	{
		return children.size();
	}

	
	/**
	 * Removes all of the real children of this ParseNode.
	 * 
	 */
	public void removeChildren()
	{
		for(ParseNode child : children)
			child.parent = null;
		children.clear();
		spanStart = -1;
		spanEnd = -1;
	}
	
	/**
	 * Removes a subset of this node's children and updates the 
	 * state of this ParseNode appropriately.
	 * @param children The subset of children to remove.
	 */
	public void removeChildren(List<ParseNode> children)
	{
		Collections.sort(children);
		
		for (ParseNode child : children)
		{
			if (child.spanStart == spanStart)
			{
				this.spanStart = child.spanEnd + 1;	
			}
			if (child.spanEnd == spanEnd)
			{
				this.spanEnd = child.spanStart - 1;
			}
			children.remove(child);
		}
	}


	/**
	 * Add a new real child to this ParseNode.
	 * @param newChild The new child to add.
	 */
	public void addChild(ParseNode newChild)
	{
		children.add(newChild);
		newChild.parent = this;
		spanStart = Math.min(spanStart, newChild.spanStart);
		spanEnd = Math.max(spanEnd, newChild.spanEnd);
	}
	
	/**
	 * Add a virtual child node. These are additional nodes
	 * consistent with the original tree (i.e. its children are
	 * a proper subset of this node's children); however, 
	 * the children of virtual nodes may have non-zero intersection.
	 * 
	 * @param newChild The virtual child to add.
	 */
	public void addVirtualChild(VirtualParseNode newChild)
	{
		Set<VirtualParseNode> nodeSet = virtualChildren.get(newChild.spanStart);
		
		if (nodeSet == null)
		{
			nodeSet = new HashSet<VirtualParseNode>();
			nodeSet.add(newChild);
			virtualChildren.put(newChild.spanStart, nodeSet);
		}
		else
		{
			nodeSet.add(newChild);
		}
		
		newChild.parent = this;
	}

	/**
	 * Add another rule which has this Node on the LHS.
	 * @param rule The rule to add.
	 */
	public void addRule(ExtractedRule rule)
	{
		rules.add(rule);
	}
	
	/**
	 * Get the collection of rules extracted so far with this ParseNode
	 * on the LHS
	 * @return The extracted rules as a Collection.
	 */
	public Collection<ExtractedRule> getRules()
	{
		return rules;
	}

	public boolean hasNoExtractedRules()
	{
		return rules == null || rules.isEmpty();
	}
	
	/**
	 * Get this node's parent
	 * @return The ParseNode that is the parent of this ParseNode.
	 */
	public ParseNode getParent()
	{
		return parent;
	}


	/**
	 * Checks if this node has an alignment of a given type.
	 * @param type The type of alignment to check for.
	 * @return True if this node has an alignment of the given type, else false.
	 */
	public boolean isNodeAligned(NodeAlignmentType type)
	{
		Set<ParseNode> alignedNodes = alignedTo.get(type.getIndex());
		
		return alignedNodes != null && !alignedNodes.isEmpty();
	}

	/**
	 * Checks if this node has an alignment at all.
	 * @return True if this node has an alignment of any type, else false.
	 */
	public boolean isNodeAligned()
	{
		return isAligned;
	}

	/**
	 * Gets all node alignments from this ParseNode of a particular type.
	 * @param type The type of alignments to retrieve.
	 * @return The set of ParseNodes this ParseNode is aligned to with the
	 * given alignment type
	 */
	public Set<ParseNode> getNodeAlignments(NodeAlignmentType type)
	{
		Set<ParseNode> alignedNodes = alignedTo.get(type.getIndex());
		
		if (alignedNodes == null)
		{
			return new HashSet<ParseNode>();
		}
		else
		{
			return alignedNodes;
		}
	}
	
	/**
	 * Gets all node alignments from this ParseNode of any of a list of types.
	 * @param type A variable number of alignment types to retrieve.
	 * @return The set of ParseNodes this ParseNode is aligned to with any of the
	 * given alignment types as the keys of a map. The value corresponds to the 
	 * or of the types of the alignment.
	 */
	public Set<ParseNode> getNodeAlignments(Collection<NodeAlignmentType> types)
	{
		Set<ParseNode> alignedNodes = new HashSet<ParseNode>();
		
		Set<ParseNode> alignedForType = null;
		for (NodeAlignmentType type : types)
		{
			alignedForType = alignedTo.get(type.getIndex());
			if (alignedForType != null)
			{
				alignedNodes.addAll(alignedForType);
			}
		}
		
		return alignedNodes;
	}

	/**
	 * Gets all node alignments for this ParseNode
	 * @return A set of ParseNodes that this ParseNode is aligned to.
	 */
	public Set<ParseNode> getNodeAlignments()
	{
		Set<ParseNode> allAligns = null;
		
		for (Set<ParseNode> alignSet : alignedTo)
		{
			if (allAligns == null)
			{
				allAligns = alignSet;
			}
			else
			{
				if (alignSet != null)
				{
					allAligns.addAll(alignSet);
				}
			}
		}
		
		if (allAligns == null)
		{
			allAligns = new HashSet<ParseNode>();
		}
		
		return allAligns;
	}

	/**
	 * Add in a new node alignment
	 * @param type The type of alignment.
	 * @param aligned The node this ParseNode is aligned to.
	 */
	public void addNodeAlignment(NodeAlignmentType type, ParseNode aligned)
	{		
		Set<ParseNode> alignedNodes = alignedTo.get(type.getIndex());
		if (alignedNodes == null)
		{
			alignedNodes = new HashSet<ParseNode>();
			alignedTo.set(type.getIndex(), alignedNodes);
		}
		alignedNodes.add(aligned);
		isAligned = true;
	}


	/**
	 * Finds the highest up nodes in the parse tree spanning the given
	 * terminal indices.
	 * @param searchStart The least index to be covered.
	 * @param searchEnd The greatest index to be covered.
	 * @return An ordered list of ParseNodes spanning the desired indices,
	 * if this exists.
	 */
	public List<ParseNode> getNodesSpanning(int searchStart, int searchEnd)
	{
		List<ParseNode> results = new ArrayList<ParseNode>();
		getNodesSpanningHelper(searchStart, searchEnd, results);
		return results;
	}

	/**
	 * Helper for getNodesSpanning.
	 * @param searchStart Start of search.
	 * @param searchEnd End of search.
	 * @param results Accumulator for results so far.
	 * void
	 */
	protected void getNodesSpanningHelper(int searchStart, int searchEnd,
										List<ParseNode> results)
	{
		if(this.spanStart >= searchStart && this.spanEnd <= searchEnd)
		{
			// This node is wholly within the search range; save it:
			results.add(this);
		}
		else if(this.spanEnd < searchStart || this.spanStart > searchEnd)
		{
			// This node is wholly outside the search range; quit:
		}
		else
		{
			// This node is partially within search range; check kids:
			for(ParseNode child : children)
			{
				child.getNodesSpanningHelper(searchStart, searchEnd, results);
			}
		}
	}

	public List<ParseNode> getTerminalNodesSpanning()
	{
		return getTerminalNodesSpanning(this.spanStart, this.spanEnd);
	}
	
	public List<ParseNode> getTerminalNodesSpanning(int searchStart, int searchEnd)
	{
		List<ParseNode> results = new ArrayList<ParseNode>();
		getNodesSpanningHelper(searchStart, searchEnd, results);
		List<ParseNode> terminalResults = new ArrayList<ParseNode>();
		for (ParseNode node : results)
		{
			node.getTerminals(terminalResults);
		}
		return terminalResults;
	}
	
	public void getTerminals(List<ParseNode> results)
	{
		if (isTerminal())
		{
			results.add(this);
		}
		else
		{
			for (ParseNode child : children)
			{
				child.getTerminals(results);
			}
		}
	}
	
	/**
	 * Get all terminals that are descendants of this node,
	 * but that are unaligned.
	 * @return The list of unaligned terminals
	 */
	public List<ParseNode> getUnalignedTerminals()
	{
		List<ParseNode> results = new ArrayList<ParseNode>();
		
		for (ParseNode child : children)
		{
			if (child.isTerminal())
			{
				if (child.isUnaligned())
				{
					results.add(child);
				}
			}
			else
			{
				results.addAll(child.getUnalignedTerminals());
			}
		}
		
		return results;
	}
	
	public boolean isUnaligned()
	{
		boolean unaligned = alignedTo.get(NodeAlignmentType.T2T.getIndex()) == null;
		return unaligned;
	}

	/**
	 * Print out the subtree of which this ParseNode is a root.
	 * @return The string representation of the subtree with parenthetical notation.
	 */
	public String parenString()
	{
		String yield = "";
		
		// Quick case: Terminal nodes can be written out as "(NNS boys)":
		if(children.size() == 0)
			return "(" + category + " " + yield + ")";

		// Otherwise, recursively generate strings for child subtrees:
		else
		{
			String subtree = "";
			for(ParseNode child : children)
				subtree += (child.parenString() + " ");
			subtree = subtree.trim();
			return "(" + category + " " + subtree + ")";
		}
	}
	
	/**
	 * @return The extraction state for this ParseNode
	 */
	public GrammarExtractionState getExtractionState()
	{
		return extractionState;
	}
	
	/**
	 * @param state The new extraction state for this ParseNode
	 */
	public void setExtractionState(GrammarExtractionState state)
	{
		this.extractionState = state;
	}

	/**
	 * @return The state of alignment for this ParseNode
	 */
	public NodeAlignmentState getAlignmentState()
	{
		return alignmentState;
	}

	/**
	 * @param state The new NodeAlignment state for this ParseNode.
	 */
	public void setAlignmentState(NodeAlignmentState state)
	{
		this.alignmentState = state;
	}
	
	/**
	 * @param type The type of vector to check for
	 * @return True if the vector type was initialized for this ParseNode,
	 * else false.
	 */
	public boolean isInitialized(VectorType type) 
	{ 
		BitSet vector = getVector(type);
		return vector != null; 
	}
	
	/**
	 * Sets a particular vector type.
	 * @param type The type of vector to set.
	 * @param vector The new value of the vector.
	 * void
	 */
	public void setVector(VectorType type, BitSet vector)
	{
		if (type.equals(VectorType.PROJ_COMP))
		{
			alignmentState.setProjCompVector(vector);
		}
		else if (type.equals(VectorType.PROJ_COV))
		{
			alignmentState.setProjCovVector(vector);
		}		
	}
	
	/**
	 * Set a link in the ProjCovVector
	 * @param link The link to set.
	 */
	public void setProjCovLink(int link)
	{
		alignmentState.getProjCovVector().set(link);
	}
	
	/**
	 * Modifies this ParseNode's vector of type myType
	 * by or-ing it with the otherType vector of node.
	 * @param node The other ParseNode
	 * @param myType The type of vector in this ParseNode to modify
	 * @param otherType The type of vector in the other ParseNode
	 * to utilize
	 */
	public void orVectors (ParseNode node, VectorType myType, 
							VectorType otherType)
	{
		BitSet myVector = getVector(myType);
		BitSet otherVector = node.getVector(otherType);

		myVector.or(otherVector);

	}
	
	/**
	 * Gets the minimum index set in a vector
	 * @param type The type of vector to check
	 * @return The minimum set index.
	 */
	public int getMinSetInVector(VectorType type)
	{
		BitSet vector = getVector(type);
		return vector.nextSetBit(0);
	}
	
	/**
	 * Gets the maximum index set in a vector
	 * @param type The type of vector to check
	 * @return The maximum set index.
	 */
	public int getMaxSetInVector(VectorType type)
	{
		BitSet vector = getVector(type);
		return vector.length();
	}
	
	public void orWith(VectorType type, BitSet vector)
	{
		BitSet thisVector = getVector(type);
		vector.or(thisVector);
	}
	
	public BitSet getVector(VectorType type)
	{
		BitSet vector;
		
		if (type.equals(VectorType.PROJ_COMP))
		{
			vector = alignmentState.getProjCompVector();
		}
		else if (type.equals(VectorType.PROJ_COV))
		{
			vector = alignmentState.getProjCovVector();
		}
		else
		{
			vector = null;
		}
		
		return vector;
	}
	
	public boolean isVectorEmpty(VectorType type)
	{
		BitSet vector = getVector(type);

		if (vector == null || vector.isEmpty()) 
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isTAlignable() 
	{
		return true;
	}
	
	public boolean isLhs() 
	{
		return true;
	}
	
	@Override 
	public String toString() 
	{
		return category;
	}
	
	@Override
	public int hashCode()
	{
		int hashCode = category.hashCode();
		hashCode = 31 * hashCode + spanStart;
		hashCode = 31 * hashCode + spanEnd;
		hashCode = 31 * hashCode + Boolean.valueOf(isTerminal()).hashCode();
		
		return hashCode;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ParseNode))
		{
			return false;
		}
		
		ParseNode otherNode = (ParseNode) o;
		
		return this.category.equals(otherNode.category) &&
		       this.spanStart == otherNode.spanStart &&
		       this.spanEnd == otherNode.spanEnd &&
		       this.isTerminal() == otherNode.isTerminal() && 
		       this.children == otherNode.children;
	}
	
	public boolean isReal()
	{
		return true;
	}
	
	public boolean isTerminal() 
	{
		return false;
	}

	public boolean isString()
	{
		return false;
	}
	
	public Set<RulePart> getExpansions(int maxSize)
	{
		Set<RulePart> expansions = null;
		Set<RulePart> tmpExpansions = new HashSet<RulePart>();
		
		for (ParseNode child : this.children)
		{
			if (!child.isReal())
			{
				continue;
			}
			
			Set<RulePart> childExpansions = child.getExpansions(maxSize);
			if (expansions == null)
			{
				expansions = childExpansions;
			}
			else 
			{
				for (RulePart currExpansion : expansions)
				{
					for (RulePart childExpansion : childExpansions)
					{
						RulePart combined = currExpansion.combineWith(childExpansion);
						if (combined != null)
						{
							tmpExpansions.add(combined);
						}
					}
				}
				expansions = tmpExpansions;
			}
		}
		
		if (expansions == null)
		{
			expansions= new HashSet<RulePart>();
		}
		
		if (isTerminal())
		{
			expansions.add(new RulePart(this.toString(),1,maxSize));
		}
		
		return expansions;
	}
	
	/* Note that comparison refers to position in the sentence -
	 * i.e. a left to right ordering of the nodes' spans
	 */
	public int compareTo(ParseNode otherNode)
	{
		return (spanStart - otherNode.spanStart);
	}

	
	public static enum VectorType {
		PROJ_COV, //Projected coverage vector
		PROJ_COMP; //Projected complement
	}
	
	public List<List<ParseNode>> getChildrenListsWithVirtualNodes() 
	{
		List<List<ParseNode>> childrenSets = new ArrayList<List<ParseNode>>();
		ArrayList<ParseNode> baseList = new ArrayList<ParseNode>(children.size());
		childrenSets.add(baseList);
		List<List<ParseNode>> tmpList = new ArrayList<List<ParseNode>>();
		
		if (virtualChildren == null || children.isEmpty())
		{
			return new ArrayList<List<ParseNode>>();
		}
		
		for (ParseNode child : children)
		{
			for (List<ParseNode> list : childrenSets)
			{
				// Check to make sure that this index isn't covered already.
				// Add in the child, and add in any of the virtual nodes that
				// start at this index.
				Set<VirtualParseNode> virtualNodes = virtualChildren.get(child.spanStart);
				int maxIndex = -1;
				if (list.size() > 0)
				{
					maxIndex = list.get(list.size() - 1).spanEnd;
				}
				if (maxIndex < child.spanStart)
				{
					ArrayList<ParseNode> childrenNodes = new ArrayList<ParseNode>();
					childrenNodes.addAll(list);
					childrenNodes.add(child);
					tmpList.add(childrenNodes);
					// we want to add in. but wait, might want to do that twice.
					if (virtualNodes != null)
					{
						for (VirtualParseNode node : virtualNodes)
						{
							childrenNodes = new ArrayList<ParseNode>();
							childrenNodes.addAll(list);
							childrenNodes.add(node);
							tmpList.add(childrenNodes);
						}
					}
				}
				else
				{
					tmpList.add(list);
				}
			}
			List<List<ParseNode>> tmp = childrenSets;
			childrenSets = tmpList;
			tmpList = tmp;
			tmpList.clear();
		}
		
		childrenSets.remove(0);
		
		return childrenSets;
	}
	
	public List<ParseNodeRulePart> getRuleAlignments
		(NodeAlignmentType type, int maxGrammar, 
		 int maxPhrase, boolean side1isSrc)
	{
		List<ParseNodeRulePart> aligns = 
			new ArrayList<ParseNodeRulePart>();

		for (ExtractedRule rule : rules)
		{
			ParseNodeRulePart part = rule.getRhs();
			if (part.getMaxPhraseComponents() > 0)
			{
				aligns.add(rule.getRhs());
			}
		}
		
		// Now add in the other things - is this really always needed?
		for (ParseNode leftNode : getNodeAlignments(type))
		{
			aligns.add(new ParseNodeRulePart(this, leftNode, maxGrammar, 
											 maxPhrase, side1isSrc, false));
		}
		
		return aligns;
	}
	
	
	/**
	 * Gets all RHS uses for this node and expansions.
	 * Note that this is not thread safe.
	 * 
	 * @param max
	 * @param side1isSrc
	 * @param types
	 * @return
	 */
	public Collection<ParseNodeRulePart> getRuleAlignments(
			int maxGrammar, int maxPhrase, boolean side1isSrc, 
			Collection<NodeAlignmentType> types, boolean allowLhsNode,
			boolean minimalRulesOnly, ParseNode lhsNode)
	{	
		//if (rhsCollection != null)
		//{
			//return rhsCollection;
		//}
		
		rhsCollection = new HashSet<ParseNodeRulePart>();

		if(!minimalRulesOnly)
		{
			for (ExtractedRule rule : rules)
			{
				ParseNodeRulePart part = rule.getRhs();
				if (part.getMaxPhraseComponents() > 0)
				{
					rhsCollection.add(rule.getRhs());
				}
			}
		}
		
		// Now add in the other things
		Set<ParseNode> alignedNodes = getNodeAlignments(types);
	
		for (ParseNode leftNode : alignedNodes)
		{
			ParseNodeRulePart part =
				new ParseNodeRulePart(this, leftNode, maxGrammar, maxPhrase, side1isSrc, false);
			if (part.getMaxPhraseComponents() == maxPhrase)
			{
				rhsCollection.add(part);
			}
		}
		
		if (rhsCollection.isEmpty() && this.isTerminal())
		{			
			ParseNodeRulePart part =
				new ParseNodeRulePart(this, new NullParseNode(), maxGrammar, 
									  maxPhrase, side1isSrc, false);
			if (part.getMaxPhraseComponents() == maxPhrase)
			{
				rhsCollection.add(part);
			}
		}
		
		return rhsCollection;
	}
	
	/**
	 * Gets all RHS uses for this node and expansions.
	 * Note that this is not thread safe.
	 * 
	 * @param max
	 * @param side1isSrc
	 * @param types
	 * @return
	 */
	public Collection<ParseNodeRulePart> getRuleAlignments(
			int maxGrammar, int maxPhrase, boolean side1isSrc, 
			boolean allowLhsNode)
	{		
//		if (rhsCollection != null)
//		{
//			return rhsCollection;
//		}
//		
		rhsCollection = new HashSet<ParseNodeRulePart>();

		for (ExtractedRule rule : rules)
		{
			ParseNodeRulePart part = rule.getRhs();
			if (part.getMaxPhraseComponents() > 0)
			{
				rhsCollection.add(rule.getRhs());
			}
		}
		
		Collection<ParseNode> p2t = alignedTo.get(NodeAlignmentType.P2T.getIndex());
		Collection<ParseNode> t2p = alignedTo.get(NodeAlignmentType.T2P.getIndex());
		
		// Now add in the other things
		for (ParseNode leftNode : getNodeAlignments())
		{
			if (/*(p2t == null || !p2t.contains(leftNode)) && 
				*/(t2p == null || !t2p.contains(leftNode)))
			{
				ParseNodeRulePart part =
					new ParseNodeRulePart(this, leftNode, maxGrammar, maxPhrase, side1isSrc, false);
				if (part.getMaxPhraseComponents() == maxPhrase)
				{
					rhsCollection.add(part);
				}
			}
		}
		
		if (rhsCollection.isEmpty())
		{			
			ParseNodeRulePart part =
				new ParseNodeRulePart(this, new NullParseNode(), maxGrammar, 
									  maxPhrase, side1isSrc, false);
			if (part.getMaxPhraseComponents() == maxPhrase)
			{
				rhsCollection.add(part);
			}
		}
		
		return rhsCollection;
	}
	
	public void minimizeAlignments()
	{
		for(ParseNode child : this.getChildren())
			child.minimizeAlignments();
		
		Set<ParseNode> allAlignedNodes = this.getNodeAlignments();
		Set<ParseNode> minimalAlignments = this.getMinimalNodeAlignments(allAlignedNodes);
		
		Set<ParseNode> alignedForType = null;
		for(int i = 0; i < alignedTo.size(); i++)
		{
			alignedForType = alignedTo.get(i);
			if (alignedForType != null)
			{
				Set<ParseNode> minimized = new HashSet<ParseNode>();
				for(ParseNode node : alignedForType)
					if(minimalAlignments.contains(node))
						minimized.add(node);
				
				if(minimized.size() > 0)
					alignedTo.set(i, minimized);
				else
					alignedTo.set(i, null);
			}
		}
		
		for(ParseNode aligned : minimalAlignments)
			aligned.alignmentsMinimized = true;
		
		if(minimalAlignments.size() == 0)
			this.isAligned = false;
		
		this.alignmentsMinimized = true;
	}
	
	public Set<ParseNode> getMinimalNodeAlignments(Set<ParseNode> allNodeAlignments)
	{
		// If any node above this one aligns to any node above the aligned Node, take the minimal alignment
		// Otherwise, take the maximal target alignment		
		// But if there's ambiguity in this node, then one aligned node must be a parent of the other(s)
		
		// Force root to align to root, and disallow anything else from aligning to the roots
		if(this.getParent() == null)
		{
			Set<ParseNode> oppositeRoot = new HashSet<ParseNode>();
			for(ParseNode aligned : allNodeAlignments)
				if(aligned.getParent() == null)
					oppositeRoot.add(aligned);
			return oppositeRoot;
		}
		
		int maxSpanSize = -1;
		int minSpanSize = Integer.MAX_VALUE;
		for(ParseNode aligned : allNodeAlignments)
		{
			int alignedSpan = aligned.getSpanEnd() - aligned.getSpanStart() + 1;
			if(!aligned.alignmentsMinimized)
			{
				minSpanSize = Math.min(minSpanSize, alignedSpan);
				maxSpanSize = Math.max(maxSpanSize, alignedSpan);
			}
		}
		
		int minGeneration = Integer.MAX_VALUE;
		int maxGeneration = Integer.MIN_VALUE;
		for(ParseNode aligned : allNodeAlignments)
		{
			if(!aligned.alignmentsMinimized)
			{
				minGeneration = Math.min(minGeneration, aligned.getGeneration());
				maxGeneration = Math.max(maxGeneration, aligned.getGeneration());
			}
		}
		
		Set<ParseNode> minimalNodeAlignments = new HashSet<ParseNode>();
		for(ParseNode aligned : allNodeAlignments)
		{
			int alignedSpan = aligned.getSpanEnd() - aligned.getSpanStart() + 1;
			if(!aligned.alignmentsMinimized && alignedSpan == minSpanSize && aligned.getGeneration() == maxGeneration)
				minimalNodeAlignments.add(aligned);
		}
	
		// If this is not the root, remove any alignments to the opposing root
		for(Iterator<ParseNode> it = minimalNodeAlignments.iterator(); it.hasNext();)
		{
			ParseNode aligned = it.next();
			if(aligned.getParent() == null)
				it.remove();
		}
		return minimalNodeAlignments;
	}
	
	public String getNodeAlignCategory()
	{
		return "O";
	}
	
	// PUBLIC MEMBER ATTRIBUTES: ////////////////////////////////////////////

	public static final Pattern parenCatAndSubtree =
		Pattern.compile("^\\((\\S+) (.*)\\)$");

	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	private Collection<ParseNodeRulePart> rhsCollection = null;
	
	private String category;  // "NP", "VP", "VBN", etc.
	
	protected int spanStart;    // first 0-based word index covered by this node
	protected int spanEnd;    // last 0-based word index covered by this node

	protected List<ParseNode> children;
	protected Map<Integer,Set<VirtualParseNode>> virtualChildren;
	protected ParseNode parent;
	protected List<ExtractedRule> rules;

	// Each entry in the set is a valid and complete node alignment of the
	// type given in the map key:
	private ArrayList<Set<ParseNode>> alignedTo;
	
	private GrammarExtractionState extractionState;      // state of extraction
	protected NodeAlignmentState alignmentState;  // state of extraction
	protected boolean isAligned = false;
	
	protected int generation;
	protected boolean alignmentsMinimized;
}
