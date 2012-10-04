package ruleLearnerNew;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ruleLearnerNew.NodeAlignmentList.NodeAlignmentType;
import ruleLearnerNew.ParseNode.VectorType;


/**
 * @author ghannema
 * @author mburroug
 */
public class VamshiNodeAligner implements NodeAligner
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public VamshiNodeAligner(int maxVirtualNodeComponents)
	{
		this.maxVirtualNodeComponents = maxVirtualNodeComponents;
	}

	
	// FUNCTIONS: ///////////////////////////////////////////////////////////
	
	/**
	 * Top-level function for aligning nodes.
	 *
	 * @param srcRoot Root of the source-language tree being node aligned
	 * @param tgtRoot Root of the target-language tree being node aligned
	 * @param wordAligns Source-to-target word alignments for the sentence
	 * @param sentNum Global sentence number in the corpus
	 *
	 * @return A structure containing a list of the aligned word index spans
	 * for this sentence pair, along with the types of alignment for each one
	 */
	public NodeAlignmentList align(ParseNode srcRoot,
								   ParseNode tgtRoot,
								   WordAlignment wordAligns)
	{
		
		// Compute projected vectors used to check alignment consistency:
		computeProjectedCoverageVector(srcRoot, true, wordAligns);
		computeProjectedCoverageVector(tgtRoot, false, wordAligns);
		computeProjectedComplementVector(srcRoot);
		computeProjectedComplementVector(tgtRoot);

		// Compute vectors of unaligned source and target words:
		BitSet srcUnaligned = new BitSet();
		srcUnaligned.set(0, srcRoot.getSpanEnd() + 1);
		for(int aligned : wordAligns.getAllSrcAlignedWords())
			srcUnaligned.clear(aligned);
		BitSet tgtUnaligned = new BitSet();
		tgtUnaligned.set(0, tgtRoot.getSpanEnd() + 1);
		for(int aligned : wordAligns.getAllTgtAlignedWords())
			tgtUnaligned.clear(aligned);

		// Extract node alignments:
		NodeAlignmentList nodeAligns = new NodeAlignmentList();
		computeNodeAlignments(srcRoot, srcRoot, tgtRoot, srcUnaligned, tgtUnaligned,
							  true, true, nodeAligns);
		
		computeNodeAlignments(tgtRoot, tgtRoot, srcRoot, tgtUnaligned, srcUnaligned,
				  			  false, true, nodeAligns);
		
		computeTs2TsAlignments(srcRoot, tgtRoot, srcUnaligned, tgtUnaligned, nodeAligns);
		
		// Return list of aligned nodes to be sent on to rule extraction:
		return nodeAligns;
	}


	/**
	 * Recursive function that computes projected coverage vectors for nodes
	 * in a tree in a bottom-up manner.  It leaves alone vectors that are not
	 * null, but recomputes others.
	 *
	 * @param srcRoot Node whose projected coverage vector should be computed
	 * @param isSrcNode Indicates whether the given node is or is not on the
	 * source side of the word alignment structure passed in wordAligns
	 * @param wordAligns Source-to-target word alignments for the sentence
	 *
	 * @return Nothing
	 */
	public void computeProjectedCoverageVector(ParseNode srcRoot,
								   			   boolean isSrcNode,
								   			   WordAlignment wordAligns)
	{
		// For a terminal node, the vector comes directly from alignments:
		if(srcRoot.numChildren() == 0 && !srcRoot.isInitialized(VectorType.PROJ_COV))
		{
			// Projected coverage vector is exactly the other-side words
			// aligned to:
			Set<Integer> links = null;
			if(isSrcNode)
				links = wordAligns.getLinksForSrcWord(srcRoot.getSpanStart());
			else
				links = wordAligns.getLinksForTgtWord(srcRoot.getSpanStart());
			srcRoot.setVector(VectorType.PROJ_COV, new BitSet());
			for(Integer link : links)
			{
				srcRoot.setProjCovLink(link);
			}
		}

		// For a non-terminal, it's the union of its children's vectors:
		else if(!srcRoot.isInitialized(VectorType.PROJ_COV))
		{
			srcRoot.setVector(VectorType.PROJ_COV, new BitSet());
			for(ParseNode child : srcRoot.getChildren())
			{
				computeProjectedCoverageVector(child, isSrcNode, wordAligns);
				srcRoot.orVectors(child, VectorType.PROJ_COV, VectorType.PROJ_COV);
			}
		}
	}


	/**
	 * Recursive function that computes projected complement vectors for nodes
	 * in a tree in a top-down manner.  It leaves alone vectors that are not
	 * null, but recomputes others.  Projected coverage vectors must have been
	 * already computed in order to use this function.
	 *
	 * @param node Node whose projected complement vector should be computed
	 *
	 * @return Nothing
	 */
	public void computeProjectedComplementVector(ParseNode node)
	{
		// For the root node, the vector is all 0s:
		if(node.getParent() == null && !node.isInitialized(VectorType.PROJ_COMP))
		{
			node.setVector(VectorType.PROJ_COMP, new BitSet());
		}
		// For a non-root, it is computed from its parent and siblings:
		else if(!node.isInitialized(VectorType.PROJ_COMP))
		{
			// Start out with the parent's projected complement vector:
			node.setVector(VectorType.PROJ_COMP, new BitSet());
			node.orVectors(node.getParent(), 
						   VectorType.PROJ_COMP,
						   VectorType.PROJ_COMP);

			// Now union all siblings' projected coverage vectors:
			for(ParseNode sibling :
				node.getParent().getChildren())
			{
				if(sibling != node)
				{
					node.orVectors(sibling, 
								   VectorType.PROJ_COMP, 
								   VectorType.PROJ_COV);
				}
			}
		}

		// Recursive call: compute vectors for children as well:
		for(ParseNode child : node.getChildren())
			computeProjectedComplementVector(child);
	}


	/**
	 * Currently computes tight and grown tree-to-tree, tree-to-string, and
	 * tree-to-tree/string node alignments between a node in the Language 1
	 * tree and a Language 2 tree.
	 * Recursively calls itself on children of the Language 1 node, so the
	 * final effect is that tree-to-tree and tree-to-string alignments are
	 * found for all nodes in the Language 1 tree.
	 *
	 * @param node1 Node in the Language 1 tree to find tree-to-tree and
	 * tree-to-string node alignments for
	 * @param tree2 The entire Language 2 tree
	 * @param unaligned1 Indicates the word indexes in the Language 1 sentence
	 * that do not have word alignments
	 * @param unaligned2 Indicates the word indexes in the Language 2 sentence
	 * that do not have word alignments
	 * @param side1IsSrc Indicates whether or not Language 1 is the source side
	 * of the parallel corpus being node aligned
	 * @param findExactNodeAligns Indicates whether tree-to-tree alignments
	 * should also be found for the Language 1 tree in the Language 2 tree
	 * @param outputAligns Output parameter: all node alignments found in this
	 * function will be added to this structure
	 *
	 * @return Nothing; results are added to outputAligns
	 */
	public void computeNodeAlignments(ParseNode node1,
									  ParseNode tree1,
									  ParseNode tree2,
									  BitSet unaligned1,
									  BitSet unaligned2,
									  boolean side1IsSrc,
									  boolean findExactNodeAligns,
									  NodeAlignmentList outputAligns)
	{	
		// Run on this node:
		if(isConsistent(node1))
		{
			if(findExactNodeAligns)
			{
				findExactNodeAlignments(node1, tree1, tree2, unaligned1, unaligned2,
										side1IsSrc, outputAligns);
			}
			findStringAlignments(node1, tree2, unaligned1, unaligned2, side1IsSrc,
								 outputAligns);
			findProjectedNodeAlignments(node1, tree1, tree2, unaligned1, unaligned2,
										side1IsSrc, outputAligns);
		}
		else
		{
			if (node1.isTerminal())
			{
				if (node1.alignmentState.hasMultAligns())
				{
					for (ParseNode node : getTerminalAligns(node1, tree2))
					{
						node1.addNodeAlignment(NodeAlignmentType.T2P, node);
						node.addNodeAlignment(NodeAlignmentType.P2T, node1);
					}
				}
				/*else
				{
					for (ParseNode node : getTerminalAligns(node1, tree2))
					{
						node1.addNodeAlignment(NodeAlignmentType.P2T, node);
						node.addNodeAlignment(NodeAlignmentType.T2P, node1);
					}
				}*/
			}
		}

		// And also on its children:
		for(ParseNode child : node1.getChildren())
		{
			computeNodeAlignments(child, tree1, tree2, unaligned1, unaligned2,
								  side1IsSrc, findExactNodeAligns, 
								  outputAligns);
		}
		
//		if (node1.getCategory().equals("is"))
//		{
//			System.out.println("is");
//		}
	}

	
	public Collection<ParseNode> getTerminalAligns(ParseNode node1, ParseNode tree2)
	{
		// Find projected bounds of node1 in tree2:
		int projCov1Min = node1.getMinSetInVector(VectorType.PROJ_COV);
		int projCov1Max = node1.getMaxSetInVector(VectorType.PROJ_COV);

		// Find the tree2 terminals that are in the range of node1's projected coverage:
		List<ParseNode> t2Matches = tree2.getTerminalNodesSpanning(projCov1Min, projCov1Max - 1);
		
		if(t2Matches == null)
			return new LinkedList<ParseNode>();
		
		LinkedList<ParseNode> aligned = new LinkedList<ParseNode>();
		for (ParseNode node : t2Matches)
		{
			if (node1.getVector(VectorType.PROJ_COV).get(node.spanStart) == true)
			{
				aligned.addFirst(node);
			}
		}
		
		return aligned;
	}

	/**
	 * Returns whether or not the given node is aligned consistently to the
	 * other side of the sentence pair.  This decision is based on the node's
	 * projected coverage vector and projected complement vector, which must
	 * have been computed before this function is called.
	 *
	 * @param node Node whose alignment consistency should be checked
	 *
	 * @returns True if node is consistently aligned; false if not
	 */
	public boolean isConsistent(ParseNode node)
	{		
		return areConsistent(node.getVector(VectorType.PROJ_COV), 
							 node.getVector(VectorType.PROJ_COMP));
	}
	
	/**
	 * Returns whether or not the given vectors are consistent.
	 *
	 * @param projCovVector the projected coverage vector
	 * @param projCompVector the projected complement vector
	 *
	 * @returns True if node is consistently aligned; false if not
	 */
	public boolean areConsistent(BitSet projCovVector, BitSet projCompVector)
	{
		// Compute projected coverage *span* of the node:
		BitSet projCovSpan = new BitSet();
		if(projCovVector.isEmpty())
		{
			return false;    // words with no alignments aren't consistent
		}

		projCovSpan.set(projCovVector.nextSetBit(0),
						projCovVector.length());

		// A node is consistently aligned if its projected coverage *span*
		// intersect its projected complement *vector* is 0:	
		projCovSpan.and(projCompVector);
		
		return projCovSpan.isEmpty();
	}


	/**
	 * Checks for a tight tree-to-tree or a grown (on either side) tree-to-
	 * tree alignment between a node in Language 1 and all the nodes in a
	 * Language 2 tree.
	 *
	 * @param node1 Node in the Language 1 tree to find tree-to-tree node
	 * alignments for
	 * @param tree2 The entire Language 2 tree
	 * @param unaligned1 Indicates the word indexes in the Language 1 sentence
	 * that do not have word alignments
	 * @param unaligned2 Indicates the word indexes in the Language 2 sentence
	 * that do not have word alignments
	 * @param side1IsSrc Indicates whether or not Language 1 is the source side
	 * of the parallel corpus being node aligned
	 * @param outputAligns Output parameter: all node alignments found in this
	 * function will be added to this structure
	 *
	 * @return Nothing; results are added to outputAligns
	 */
	public void
	findExactNodeAlignments(ParseNode node1,
							ParseNode tree1,
							ParseNode tree2,
							BitSet unaligned1,
							BitSet unaligned2,
							boolean side1IsSrc,
							NodeAlignmentList outputAligns)
	{	
		// Find projected bounds of node1 in tree2:
		int projCov1Min = node1.getMinSetInVector(VectorType.PROJ_COV);
		int projCov1Max = node1.getMaxSetInVector(VectorType.PROJ_COV);

		// Check consistency of node1: with all unaligned words within its
		// projected coverage span, we must have a solid vector of 1s from
		// the coverage min to the coverage max
		BitSet projCov1Unaligned = unaligned2.get(projCov1Min, projCov1Max);
		
		projCov1Unaligned.or(node1.getVector(VectorType.PROJ_COV));
		
		BitSet check = new BitSet();
		check.set(projCov1Min, projCov1Max);
		check.xor(projCov1Unaligned);
		if(!check.isEmpty())
			return;

		// Find the tree2 nodes that exactly match node1's projected coverage:
		NodeSpanMap t2NodeSpans = new NodeSpanMap(tree2);
		Set<ParseNode> t2Matches = t2NodeSpans.getNodesForSpan(projCov1Min, projCov1Max - 1);
		
		if(t2Matches == null)
			return;

		// We may get more matches by taking higher-up nodes in tree2 that
		// only contain node1's projection plus unaligned words:
		// Iteratively check parents of matching nodes to see:
		Set<ParseNode> toProcess = t2Matches;
		do
		{
			Set<ParseNode> added = new HashSet<ParseNode>();
			for(ParseNode match : toProcess)
			{
				// A tree2 match's parent is also a match if it covers only
				// node1's projection plus unaligned words:
				ParseNode parent = match.getParent();
				if(parent == null)
					continue;
				BitSet projCov1 = new BitSet();
				projCov1.set(parent.getSpanStart(), parent.getSpanEnd() + 1);
				projCov1.and(unaligned2);
				node1.orWith(VectorType.PROJ_COV, projCov1);
				BitSet cov2 = new BitSet();
				cov2.set(parent.getSpanStart(), parent.getSpanEnd() + 1);
				cov2.xor(projCov1);
				if(cov2.isEmpty())
					added.add(parent);
			}
			t2Matches.addAll(added);
			toProcess = new HashSet<ParseNode>();
			toProcess.addAll(added);
		}
		while(toProcess.size() > 0);

		// Each match generates a tree-to-tree alignment of some kind:
		for(ParseNode match : t2Matches)
		{
			addAlignment(node1, match.getSpanStart(), match.getSpanEnd(), 
					     match, unaligned1, unaligned2, 
					     side1IsSrc, outputAligns, NodeAlignmentType.T2T);
		}
	}


	/**
	 * Extracts tight or grown (on either side) tree-to-string alignments
	 * for the given node in a Language 1 tree, treating the Language 2 side
	 * as just a string.
	 *
	 * @param node1 Node in the Language 1 tree to find tree-to-string node
	 * alignments for; assumed to be consistently aligned
	 * @param unaligned1 Indicates the word indexes in the Language 1 sentence
	 * that do not have word alignments
	 * @param unaligned2 Indicates the word indexes in the Language 2 sentence
	 * that do not have word alignments
	 * @param side1IsSrc Indicates whether or not Language 1 is the source side
	 * of the parallel corpus being node aligned
	 * @param outputAligns Output parameter: all node alignments found in this
	 * function will be added to this structure
	 */
	public void findStringAlignments(ParseNode node1,
									 ParseNode tree2,
									 BitSet unaligned1,
									 BitSet unaligned2,
									 boolean side1IsSrc,
									 NodeAlignmentList outputAligns)
	{				
		// Start with tight bounds on node1's projection into Language 2:
		int projCovMin = node1.getMinSetInVector(VectorType.PROJ_COV);
		int projCovMax = node1.getMaxSetInVector(VectorType.PROJ_COV) - 1;

		// Handle one-to-many terminal alignments
		if (node1.isTerminal() && projCovMax > projCovMin)
		{
			List<ParseNode> terminalNodes = 
				tree2.getTerminalNodesSpanning(projCovMin, projCovMax);
			BitSet projCov = node1.getVector(VectorType.PROJ_COV);
			for (ParseNode aligned : terminalNodes)
			{
				if (projCov.get(aligned.spanStart))
				{
					//T2P aligns will be found using string alignments,
					//but this is an annotation to differentiate aligned from
					//unaligned nodes within that string.
					node1.addNodeAlignment(NodeAlignmentType.T2P, aligned);
					aligned.addNodeAlignment(NodeAlignmentType.P2T, node1);
				}
			}
		}
				
		// Grow left and right as long as boundary words are unaligned:
		for(int t2Min = projCovMin;
			t2Min >= 0 && (t2Min == projCovMin || unaligned2.get(t2Min));
			t2Min = t2Min - 1)
		{
			for(int t2Max = projCovMax;
				t2Max == projCovMax || unaligned2.get(t2Max);
				t2Max++)
			{					
				ParseNode alignedNode = new StringNode(tree2, t2Min, t2Max);
			
				addAlignment(node1, t2Min, t2Max, alignedNode, unaligned1, 
						     unaligned2, side1IsSrc, outputAligns, NodeAlignmentType.T2S);
				addAlignment(alignedNode, node1.spanStart, node1.spanEnd, 
						     node1, unaligned2, unaligned1, !side1IsSrc, outputAligns, 
						     NodeAlignmentType.S2T);

			}
		}
	}


	/**
	 * Checks for a tight tree-to-tree/string or a grown (on either side)
	 * tree-to-tree/string alignment between a node in Language 1 and a
	 * Language 2 tree.  "Tree-to-tree/string" is defined in the constrained
	 * sense: the Language 1 node projected into Language 2 must be consistent
	 * with the surrounding structure in the Language 2 tree.
	 *
	 * @param node1 Node in the Language 1 tree to find tree-to-tree/string
	 * node alignments for
	 * @param tree2 The entire Language 2 tree
	 * @param unaligned1 Indicates the word indexes in the Language 1 sentence
	 * that do not have word alignments
	 * @param unaligned2 Indicates the word indexes in the Language 2 sentence
	 * that do not have word alignments
	 * @param side1IsSrc Indicates whether or not Language 1 is the source side
	 * of the parallel corpus being node aligned
	 * @param outputAligns Output parameter: all node alignments found in this
	 * function will be added to this structure
	 *
	 * @return Nothing; results are added to outputAligns
	 */
	public void
	findProjectedNodeAlignments(ParseNode node1,
								ParseNode tree1,
								ParseNode tree2,
								BitSet unaligned1,
								BitSet unaligned2,
								boolean side1IsSrc,
								NodeAlignmentList outputAligns)
	{
		if (!node1.isTAlignable() || !node1.isReal() || node1.isString())
		{
			return;
		}
			
		// Find node1's projected range in tree2: it's node1's projected
		// coverage vector union any unaligned tree2 words within its range:
		int projCov1Min = node1.getMinSetInVector(VectorType.PROJ_COV);
		int projCov1Max = node1.getMaxSetInVector(VectorType.PROJ_COV);
		
		// We can grow the range on the target side, as long as we only pick
		// up unaligned words:
		for(int t2Min = projCov1Min;
			t2Min >= 0 && (t2Min == projCov1Min || unaligned2.get(t2Min));
			t2Min = t2Min - 1)
		{
			for(int t2Max = projCov1Max - 1;
				t2Max == projCov1Max - 1 || unaligned2.get(t2Max);
				t2Max++)
			{
				// Find the closest-to-root collection of target tree nodes
				// covering the current range:
				
				// See if all the nodes have the same parent:
				ParseNode parent = null;
				boolean allSameParent = true;
				boolean firstNode = true;
				List<ParseNode> spanningNodes = tree2.getNodesSpanning(t2Min, t2Max);
				for (ParseNode n : spanningNodes)
				{
					// Use the first node returned to get the parent to compare
					// against all the other nodes' parents:
					if(firstNode)
					{
						parent = n.getParent();
						firstNode = false;
					}
					if(n.getParent() != parent)
					{
						allSameParent = false;
						break;
					}
				}

				// If so, then we can make a constrained projected alignment:
				if(allSameParent)
				{
					ParseNode alignedNode = null;
					// TODO: Do we want to keep track of "T2TS" when 
					// the TS is really just a node?
					if (spanningNodes.size() <= maxVirtualNodeComponents)
					{
						if (spanningNodes.size() > 1)
						{
							if (parent != null)
							{
								alignedNode = parent.getVirtualChild(t2Min, t2Max);
								if (alignedNode == null)
								{
									alignedNode = 
										new VirtualParseNode(
												spanningNodes,
												t2Min,
												t2Max,
												parent);
								}
							}
						}
						else if (spanningNodes.size() > 0)
						{
							alignedNode = spanningNodes.get(0);
						}
					}
					
					if (alignedNode != null)
					{
						addAlignment(node1, t2Min, t2Max, alignedNode,
								     	unaligned1, unaligned2, side1IsSrc, 
								     	outputAligns, NodeAlignmentType.T2TS);
						addAlignment(alignedNode, node1.getSpanStart(), node1.getSpanEnd(),
										 node1, unaligned2, unaligned1, !side1IsSrc, 
										 outputAligns, NodeAlignmentType.TS2T);
					}
				}
			}
		}
	}
	
	/**
	 * Computes all of the TS2TS alignments between the two trees 
	 * rooted at srcRoot and tgtRoot
	 * 
	 * @param srcRoot The root of the source tree
	 * @param tgtRoot The root of the target tree
	 * @param srcUnaligned The vector representing unaligned words in the source
	 * @param tgtUnaligned The vector representing unaligned words in the target
	 */
	public void computeTs2TsAlignments(ParseNode srcRoot, ParseNode tgtRoot, 
									   BitSet srcUnaligned, BitSet tgtUnaligned,
									   NodeAlignmentList nodeAligns)
	{
		List<ParseNode> children = srcRoot.getChildren();
		
		if (children.size() > 2)
		{
			// consider subset of nodes i through j, inclusive
			for (int i = 0; i < children.size()-1; i++)
			{
				for (int j = i + 1; 
					 (j < children.size() - 1 || (j < children.size() && i > 0))
					 && j-i + 1 <= maxVirtualNodeComponents; 
					 j++)
				{
					// Examine.
					BitSet projCovVector = new BitSet();
					
					for (int k = i; k <= j; k++)
					{
						projCovVector.or(children.get(k).getVector(VectorType.PROJ_COV));
					}
					projCovVector.or(tgtUnaligned);
					
					BitSet projCompVector = (BitSet)srcRoot.getVector(VectorType.PROJ_COMP).clone();
					for (int k = 0; k < i; k++)
					{
						projCompVector.or(children.get(k).getVector(VectorType.PROJ_COV));
					}
					for (int k = j+1; k < children.size(); k++)
					{
						projCompVector.or(children.get(k).getVector(VectorType.PROJ_COV));
					}
					
					// Now check, is this consistent?
					
					if (!areConsistent(projCovVector, projCompVector))
					{
						continue;
					}
					
					// If consistent, check if there are sibling nodes that cover it.
					List<ParseNode> spanningNodes = tgtRoot.getNodesSpanning(
															projCovVector.nextSetBit(0), 
															projCovVector.length()-1);
					if (spanningNodes.size() <= 1 
						|| spanningNodes.size() > maxVirtualNodeComponents)
					{
						continue;
					}
					else 
					{
						ParseNode parent = null;
						boolean sameParent = true;
						for (ParseNode child: spanningNodes)
						{
							if (parent == null)
							{
								parent = child.getParent();
							}
							else
							{
								if (!parent.equals(child.getParent()))
								{
									sameParent = false;
									break;
								}
							}
						}
						if (sameParent)
						{
							// this is a TS2TS alignment; create the new nodes.
							int srcStart = children.get(i).getSpanStart();
							int srcEnd = children.get(j).getSpanEnd();
							
							int tgtStart = spanningNodes.get(0).getSpanStart();
							int tgtEnd = spanningNodes.get(spanningNodes.size()-1).getSpanEnd();
							
							List<ParseNode> subset = new ArrayList<ParseNode>();
							for (int k = i; k <= j; k++)
							{
								subset.add(children.get(k));
							}
							
							VirtualParseNode srcNode = srcRoot.getVirtualChild(srcStart, srcEnd);
							if (srcNode == null)
							{
								srcNode = new VirtualParseNode(subset,srcStart,srcEnd,srcRoot);
							}
							
							if (parent == null)
							{
								System.err.println("Null parent for virtual child");
								continue;
							}
							VirtualParseNode tgtNode = parent.getVirtualChild(tgtStart, tgtEnd);
							if (tgtNode == null)
							{
								tgtNode = new VirtualParseNode(spanningNodes,tgtStart,tgtEnd,parent);
							}
							
							//Now actually add in the alignment.
							
							addAlignment(srcNode, tgtStart, tgtEnd, tgtNode,
							     		 srcUnaligned, tgtUnaligned, true, 
							     	     nodeAligns, NodeAlignmentType.TS2TS);
						}
					}
				}
			}
			
		}
		
		for (ParseNode child: children)
		{
			computeTs2TsAlignments(child, tgtRoot, srcUnaligned, tgtUnaligned, nodeAligns);
		}
	}
	
	/**
	 * x
	 */
	public void addAlignment(ParseNode alignee, int start2, int end2,
							 ParseNode alignedNode,
							 BitSet unaligned1, BitSet unaligned2,
							 boolean side1IsSrc,
							 NodeAlignmentList outputAligns, NodeAlignmentType ... types)
	{
		// Figure out the type of the alignment:
		int type = 0;
		
		int start1 = alignee.getSpanStart();
		int end1 = alignee.getSpanEnd();
		
		if (end1 < 0)
		{
			System.err.println("negative");
		}
		
		// Set alignment type(s):
		for (NodeAlignmentType subType : types)
		{
			type |= subType.getTypeVal();
		}

		if(side1IsSrc)
		{
			// Set "grown" flags:
			if(unaligned1.get(start1))
			{
				int max = start1;
				while (unaligned1.get(max+1))
				{
					max++;
				}
				type |= NodeAlignmentList.GrownType.SRC_GROWN.getTypeVal();
				
				// here add the grown alignment each time.
			}
			if (unaligned1.get(end1))
			{
				type |= NodeAlignmentList.GrownType.SRC_GROWN.getTypeVal();
			}
			if(unaligned2.get(start2) || unaligned2.get(end2))
			{
				type |= NodeAlignmentList.GrownType.TGT_GROWN.getTypeVal();
			}

			// Actually add alignment:
			outputAligns.add(start1, end1, start2, end2, true, type);
		}
		else
		{
			// Set "grown" flags:
			if(unaligned1.get(start1) || unaligned1.get(end1))
			{
				type |= NodeAlignmentList.GrownType.TGT_GROWN.getTypeVal();
			}
			if(unaligned2.get(start2) || unaligned2.get(end2))
			{
				type |= NodeAlignmentList.GrownType.SRC_GROWN.getTypeVal();
			}

			// Actually add alignment:
			outputAligns.add(start2, end2, start1, end1, false, type);
		}
		
		if (alignee.isLhs() == alignedNode.isLhs())
		{
			for (NodeAlignmentType alignType : types)
			{
				alignee.addNodeAlignment(alignType, alignedNode);
			}
		}
	}

	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	private int maxVirtualNodeComponents;
}
