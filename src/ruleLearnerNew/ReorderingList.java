package ruleLearnerNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ReorderingList {
	
	public ReorderingList()
	{
		reorderList = new ArrayList<List<Integer>>();
	}
	
	public ReorderingList(List<ParseNode> srcNodes, 
            			  List<ParseNode> tgtNodes,
            			  boolean fromSource) 
	{	
		List<ParseNode> alignedNodes;
		List<ParseNode> oppositeNodes;
		
		if (fromSource)
		{
			alignedNodes = srcNodes;
			oppositeNodes = tgtNodes;
		}
		else
		{
			alignedNodes = tgtNodes;
			oppositeNodes = srcNodes;
		}
		
		reorderList = new ArrayList<List<Integer>>(alignedNodes.size());
		
		for (int i = 0; i < srcNodes.size(); i++)
		{
			reorderList.add(i, null);
		}
		
		for (int i = 0; i < alignedNodes.size(); i++)
		{
			ParseNode node = alignedNodes.get(i);
			Collection<ParseNode> aligns = node.getNodeAlignments();
		
			for (int j = 0; j < oppositeNodes.size(); j++)
			{
				if (aligns.contains(oppositeNodes.get(j)))
				{
					if (fromSource)
					{
						insertReorder(i, j);
					}
					else
					{
						insertReorder(j, i);
					}
				}
			}
		}
	}

	public void insertReorder(int firstIndex, int secondIndex) 
	{
		List<Integer> list = reorderList.get(firstIndex); 
		if (list == null)
		{
			list = new LinkedList<Integer>();
			reorderList.set(firstIndex, list);
		}
		list.add(secondIndex);
	}
	
	public List<Integer> fromSource(int firstIndex) 
	{
		return reorderList.get(firstIndex);
	}

	@Override
	public String toString() 
	{
		String str = "";
		
		for (int i = 0; i < reorderList.size(); i++)
		{
			if (reorderList.get(i) != null)
			{
				for (Integer aligned : reorderList.get(i))
				{
					str += i + "-" + aligned + " ";
				}
			}
		}

		if (str.length() > 0)
		{
			str = str.substring(0, str.length()-1);
		}
		
		return str;
	}

	private ArrayList<List<Integer>> reorderList;
}
