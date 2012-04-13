package ruleLearnerNew;

import java.util.Set;
import java.util.HashSet;

/**
 * @author ghannema
 * 
 */

public class AvenueLabeler implements Labeler
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public AvenueLabeler(NodeSpanMap spanMap)
	{
		this.spanMap = spanMap;
	}

	
	// FUNCTIONS: ///////////////////////////////////////////////////////////

	public Set<String> getLabels(int spanStart, int spanEnd)
	{
		// Get list of parse nodes matching the given range:
		Set<String> results = new HashSet<String>();
		Set<ParseNode> nodes = spanMap.getNodesForSpan(spanStart, spanEnd);

		// If none, then the label must be projected from another tree:
		if(nodes == null)
		{
			results.add(noLabel());
			return results;
		}

		// Otherwise, accumulate the labels from each matching node:
		else
		{
			for(ParseNode node : nodes)
				results.add(node.getCategory());
		}
		return results;
	}


	public String noLabel()
	{
		return "-P";
	}


	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	private NodeSpanMap spanMap;

}