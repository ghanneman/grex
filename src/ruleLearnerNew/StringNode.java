package ruleLearnerNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringNode extends ParseNode {
	
	public StringNode(ParseNode rootNode, int minIndex, int maxIndex)
	{
		super(minIndex, maxIndex);
		
		List<ParseNode> nodes = rootNode.getNodesSpanning(minIndex, maxIndex);
		terminalNodes = new ArrayList<ParseNode>();
		
		for (ParseNode node : nodes)
		{
			getTerminals(node);
		}
		
	}
	
	private void getTerminals(ParseNode node)
	{
		if (node.isTerminal())
		{
			terminalNodes.add(node);
		}
		else
		{
			for (ParseNode childNode : node.getChildren())
			{
				getTerminals(childNode);
			}
		}
	}
	
	@Override 
	public String getCategory()
	{
		return toString();
	}

	@Override
	public boolean isTerminal()
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		String str = "";
		
		for (ParseNode node : terminalNodes)
		{
			str += node.getCategory() + " ";
		}
		
		if (terminalNodes.size() > 0)
		{
			str = str.substring(0,str.length()-1);
		}
		
		return str;
	}
	
	public List<ParseNode> getTerminalComponents()
	{
		return terminalNodes;
	}
	
	public static Collection<ParseNode> flattenStrings(Collection<ParseNode> nodes)
	{
		ArrayList<ParseNode> flattenedList = new ArrayList<ParseNode>();
		
		for (ParseNode node : nodes)
		{
			if (node.isString())
			{
				flattenedList.addAll(((StringNode)node).getTerminalComponents());
			}
			else
			{
				flattenedList.add(node);
			}
		}
		
		return flattenedList;
	}
	
	public static Set<ParseNode> flattenStringsUnique(Collection<ParseNode> nodes)
	{
		Set<ParseNode> flattenedList = new HashSet<ParseNode>();
		
		for (ParseNode node : nodes)
		{
			if (node.isString())
			{
				flattenedList.addAll(((StringNode)node).getTerminalComponents());
			}
			else
			{
				flattenedList.add(node);
			}
		}
		
		return flattenedList;
	}
	
	@Override
	public boolean isString()
	{
		return true;
	}
	
	@Override
	public boolean isLhs()
	{
		return false;
	}

	@Override 
	public String getNodeAlignCategory()
	{
		return "";
	}
	
	List<ParseNode> terminalNodes;
}
