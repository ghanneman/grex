package ruleLearnerNew;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


/**
 * x
 */
public class NodeSpanMap
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public NodeSpanMap(ParseNode root)
	{
		spanMap = new HashMap<Span, Set<ParseNode>>();
		fillFromTree(root);
	}

	
	// FUNCTIONS: ///////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private void fillFromTree(ParseNode root)
	{
		// Add the root to the map:
		Span rootSpan = new Span(root.getSpanStart(), root.getSpanEnd());
		if(!spanMap.containsKey(rootSpan))
			spanMap.put(rootSpan, new HashSet<ParseNode>());
		spanMap.get(rootSpan).add(root);

		// Recursively add all this node's children:
		for(ParseNode child : root.getChildren()) {
			fillFromTree(child);
		}
	}

	public Set<ParseNode> getNodesForSpan(int start, int end)
	{
		Span mySpan = new Span(start, end);
		return spanMap.get(mySpan);
	}


	public Set<ParseNode> getNodesForSpan(Span span)
	{
		return spanMap.get(span);
	}


	// MEMBER VARIABLES: ////////////////////////////////////////////////////
	
	Map<Span, Set<ParseNode>> spanMap;
}
