package ruleLearnerNew;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

/**
 * @author ghannema
 */
public class GrammarExtractionState
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public GrammarExtractionState()
	{
		alignedTo = new HashMap<Integer, Set<List<ParseNode>>>();
	}

    // FUNCTIONS: ///////////////////////////////////////////////////////////

	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	// What the node is aligned to under each alignment scenario: it is aligned
	// to every node in the List part at once in order to make one valid
	// alignment, while every entry in the Set part represents an alternative
	// valid alignment, such as to each node in a unary chain.
	public Map<Integer, Set<List<ParseNode>>> alignedTo;
}
