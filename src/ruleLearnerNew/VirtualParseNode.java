package ruleLearnerNew;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to represent virtual nodes for T2TS and TS2T
 * alignments.
 * 
 * @author mburroug
 *
 */
public class VirtualParseNode extends ParseNode {

	public VirtualParseNode(List<ParseNode> children, int min, int max, ParseNode parent)
	{
		super(min,max);
		
		String category = "";
		for (int i = 0; i < children.size(); i++)
		{
			category += children.get(i);
			this.addChild(children.get(i));
			if (i < children.size() - 1)
			{
				category += "-";
			}
		}
		
		this.setCategory(category);
		this.generation = children.get(0).generation;

	    this.virtualChildren = new HashMap<Integer, Set<VirtualParseNode>>();
		for (ParseNode node : parent.getVirtualChildren())
		{
			if (node.spanStart >= this.spanStart && node.spanEnd <= this.spanEnd)
			{
				this.addVirtualChild((VirtualParseNode)node);
			}
		}
		
		for (ParseNode node : parent.getVirtualChildren())
		{
			if (this.spanStart >= node.spanStart 
				&& this.spanEnd <= node.spanEnd
				&& !(this.spanStart == node.spanStart 
				     && this.spanEnd == node.spanEnd))
			{
				((VirtualParseNode)node).addVirtualChild(this);
			}
		}
		
		parent.addVirtualChild(this);
	}

	@Override
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
	}
	
	@Override
	public boolean isReal()
	{
		return false;
	}
	
	@Override
	protected void getNodesSpanningHelper(int searchStart, int searchEnd,
			List<ParseNode> results)
	{
		// Do Nothing - This node is only Virtual!
		return;
	}
	
	@Override
	public void addChild(ParseNode newChild)
	{
		children.add(newChild);
		spanStart = Math.min(spanStart, newChild.spanStart);
		spanEnd = Math.max(spanEnd, newChild.spanEnd);
	}
	
	@Override 
	public String getNodeAlignCategory()
	{
		return "V";
	}
}
