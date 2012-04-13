package ruleLearnerNew;

import java.util.ArrayList;

public class RulePart {
	
	public RulePart(String rep, int cnt, int max)
	{
		representation = rep;
		componentCount = cnt;
		maxComponents = max;
		cumulativeSubcomponentCount = new ArrayList<Integer>();
		cumulativeSubcomponentCount.add(cnt);
	}
	
	public RulePart(String rep, int cnt, int max, ArrayList<Integer> counts)
	{
		representation = rep;
		componentCount = cnt;
		maxComponents = max;
		cumulativeSubcomponentCount = counts;
	}
	
	
	/**
	 * Combines a second rule part with this rule part.
	 * Either the entire part is added, or nothing at all.
	 * This ensures that the rulePart maintains its integrity
	 * (i.e. nothing is "missing").
	 * 
	 * @param rulePart The part to append to this RulePart
	 * @return true if the part is added, else false.
	 */
	public RulePart combineWith(RulePart rulePart)
	{
		if (this.componentCount + rulePart.componentCount > maxComponents)
		{
			return null;
		}
		
		String newRepresentation = this.representation + " " + rulePart.representation;
		int newCount = this.componentCount + rulePart.componentCount;
		
		ArrayList<Integer> counts = new ArrayList<Integer>();
		counts.addAll(this.cumulativeSubcomponentCount);
		counts.add(this.getCumulativeSubcomponentCount(this.componentCount - 1) + rulePart.componentCount);
		
		return new RulePart(newRepresentation, newCount, maxComponents, counts);
	}
	
	public static RulePart combineParts(RulePart left, RulePart right)
	{
		if (left.componentCount + right.componentCount > left.maxComponents)
		{
			return null;
		}
		
		String newRepresentation = left.representation + " " + right.representation;
		int newCount = left.componentCount + right.componentCount;
		
		ArrayList<Integer> counts = new ArrayList<Integer>();
		counts.addAll(left.cumulativeSubcomponentCount);
		counts.add(newCount);
		
		return new RulePart(newRepresentation, newCount, left.maxComponents, counts);
	}
	
	@Override
	public String toString()
	{
		return representation;
	}
	
	/**
	 * Get the number of rules before this index in the component
	 * @param index
	 * @return
	 */
	public int getCumulativeSubcomponentCount(int index) {
		if (index >= cumulativeSubcomponentCount.size() || index < 0)
		{
			return 0;
		}
		
		return cumulativeSubcomponentCount.get(index);
	}
	
	private String representation;
	private int componentCount;
	private int maxComponents;
	private ArrayList<Integer> cumulativeSubcomponentCount;

}
