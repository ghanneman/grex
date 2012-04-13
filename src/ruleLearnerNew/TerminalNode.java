package ruleLearnerNew;

import java.util.HashSet;
import java.util.Set;

/**
 * A node type for only terminal nodes.
 * @author mburroug
 *
 */
public class TerminalNode extends ParseNode {

	public TerminalNode(String cat, int position, ParseNode parent) {
		super(cat, position, position);
		this.parent = parent;
		this.generation = Integer.MAX_VALUE;
	}

	@Override
	public boolean isTAlignable()
	{
		return false;
	}
	
	@Override
	public boolean isLhs()
	{
		return false;
	}
	
	@Override
	public boolean isTerminal() 
	{
		return true;
	}
	
	@Override
	public Set<RulePart> getExpansions(int max)
	{
		Set<RulePart> set = new HashSet<RulePart>();
		RulePart terminal = new RulePart(this.getCategory(),1,max);
		set.add(terminal);
		return set;
	}
	
	@Override 
	public String getNodeAlignCategory()
	{
		return "";
	}
}
