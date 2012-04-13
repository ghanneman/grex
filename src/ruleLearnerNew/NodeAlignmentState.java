package ruleLearnerNew;

import java.util.BitSet;

/**
 * @author ghannema
 * @author mburroug
 *
 */
public class NodeAlignmentState
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

	public NodeAlignmentState()
	{
		this.projCovVector = null;
		this.projCompVector = null;
	}


    // FUNCTIONS: ///////////////////////////////////////////////////////////

	public BitSet getProjCovVector() { return projCovVector; }
	
	public BitSet getProjCompVector() { return projCompVector; }
	
	public void setProjCovVector(BitSet vector) 
	{
		projCovVector = vector;
	}
	
	public void setProjCompVector(BitSet vector) 
	{
		projCompVector = vector;
	}
	
	public boolean isUnaligned()
	{
		return projCovVector.isEmpty();
	}
	
	public boolean hasMultAligns()
	{
		return projCovVector.cardinality() > 1;
	}

	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	// Projected coverage vector -- the exact words in the other parse tree
	// covered by this node, calculated via projection through word aligns
	private BitSet projCovVector;

	// Projected complement vector -- the exact words in the other parse tree
	// *not* covered by this node or its descendants
	private BitSet projCompVector;
}
