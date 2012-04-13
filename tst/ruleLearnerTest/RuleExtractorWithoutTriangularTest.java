package ruleLearnerTest;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ruleLearnerNew.BaseGrammarExtractor;
import ruleLearnerNew.ExtractedRule;
import ruleLearnerNew.MalformedAlignmentException;
import ruleLearnerNew.MalformedTreeException;
import ruleLearnerNew.ParseNode;
import ruleLearnerNew.VamshiNodeAligner;
import ruleLearnerNew.WordAlignment;

public class RuleExtractorWithoutTriangularTest {
	
	@Before
	public void setUp()
	{		
		aligner = new VamshiNodeAligner(MAX_VIRTUAL_COMPONENTS);
		extractor = new BaseGrammarExtractor(MAX_RULE_SIZE, MAX_RULE_SIZE, false);
		goodRulesList = new ArrayList<Set<MockExtractedRule>>();
		
		Set<MockExtractedRule> ruleSet = new HashSet<MockExtractedRule>();
		ruleSet.add(new MockExtractedRule("P", "B", "Q", "c d", "q", "1-0", "OO"));
		ruleSet.add(new MockExtractedRule("P", "B", "R", "c d", "q p", "1-0", "OO"));
		ruleSet.add(new MockExtractedRule("P", "D", "Q", "d", "q", "0-0", "OO"));
		ruleSet.add(new MockExtractedRule("P", "D", "R", "d", "q p", "0-0", "OO"));
		ruleSet.add(new MockExtractedRule("G", "B", "R", "c [D" + DELIM +  "Q,1]", "[D" + DELIM + "Q,1] p", "1-0", "OO OO"));
		
		goodRulesList.add(ruleSet);
		
		ruleSet = new HashSet<MockExtractedRule>();
		ruleSet.add(new MockExtractedRule("P", "A", "Z", "a", "y x", "0-0 0-1", "OO"));
		goodRulesList.add(ruleSet);
		
		ruleSet = new HashSet<MockExtractedRule>();
		
		ruleSet.add(new MockExtractedRule("G", "A","Z","b [C" + DELIM + "V,1] d","[C" + DELIM + "V,1] w x","0-1 1-0 2-2","OO OO"));
		ruleSet.add(new MockExtractedRule("G", "B-C","V-W","b [C" + DELIM + "V,1]","[C" + DELIM + "V,1] w","0-1 1-0","VV OO"));
		ruleSet.add(new MockExtractedRule("P", "D","X","d","x","0-0","OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","b [C" + DELIM + "V,1] [D" + DELIM + "X,2]","[C" + DELIM + "V,1] w [D" + DELIM + "X,2]","0-1 1-0 2-2","OO OO OO"));
		ruleSet.add(new MockExtractedRule("P", "B-C","V-W","b c","v w","0-1 1-0","VV"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B-C" + DELIM + "V-W,1] d","[B-C" + DELIM + "V-W,1] x","0-0 1-1","OO VV"));
		ruleSet.add(new MockExtractedRule("G", "B-C","V-W","[B" + DELIM + "W,1] c","v [B" + DELIM + "W,1]","0-1 1-0","VV OO"));
		ruleSet.add(new MockExtractedRule("P", "C","V","c","v","0-0","OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B" + DELIM + "W,1] [C" + DELIM + "V,2] d","[C" + DELIM + "V,2] [B" + DELIM + "W,1] x","0-1 1-0 2-2","OO OO OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B" + DELIM + "W,1] c [D" + DELIM + "X,2]","v [B" + DELIM + "W,1] [D" + DELIM + "X,2]","0-1 1-0 2-2","OO OO OO"));
		ruleSet.add(new MockExtractedRule("P", "A","Z","b c d","v w x","0-1 1-0 2-2","OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B-C" + DELIM + "V-W,1] [D" + DELIM + "X,2]","[B-C" + DELIM + "V-W,1] [D" + DELIM + "X,2]","0-0 1-1","OO VV OO"));
		ruleSet.add(new MockExtractedRule("G", "B-C","V-W","[B" + DELIM + "W,1] [C" + DELIM + "V,2]","[C" + DELIM + "V,2] [B" + DELIM + "W,1]","0-1 1-0","VV OO OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","b c [D" + DELIM + "X,1]","v w [D" + DELIM + "X,1]","0-1 1-0 2-2","OO OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B" + DELIM + "W,1] c d","v [B" + DELIM + "W,1] x","0-1 1-0 2-2","OO OO"));
		ruleSet.add(new MockExtractedRule("P", "B","W","b","w","0-0","OO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[B" + DELIM + "W,1] [C" + DELIM + "V,2] [D" + DELIM + "X,3]","[C" + DELIM + "V,2] [B" + DELIM + "W,1] [D" + DELIM + "X,3]","0-1 1-0 2-2","OO OO OO OO"));

		goodRulesList.add(ruleSet);
		
		ruleSet = new HashSet<MockExtractedRule>();
		
		ruleSet.add(new MockExtractedRule("G", "A","Z","[D" + DELIM + "X-W,1] c e","[D" + DELIM + "X-W,1] y","0-0 1-1 2-1","OO OV"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","[D" + DELIM + "X-W,1] [C-E" + DELIM + "Y,2]","[D" + DELIM + "X-W,1] [C-E" + DELIM + "Y,2]","0-0 1-1","OO OV VO"));
		ruleSet.add(new MockExtractedRule("P", "C-E","Y","c e","y","0-0 1-0","VO"));
		ruleSet.add(new MockExtractedRule("G", "A","Z","d [C-E" + DELIM + "Y,1]","x w [C-E" + DELIM + "Y,1]","0-0 0-1 1-2","OO VO"));
		ruleSet.add(new MockExtractedRule("P", "D","X-W","d","x w","0-0 0-1","OV"));

		goodRulesList.add(ruleSet);
	}
	
	@Test
	public void TestAligns() 
	{
		String[] srcTrees = {"(B (C c) (D d))", "(A a)", 
							 "(A (B b) (C c) (D d))", "(A (D d) (C c) (E e))"};
		String[] tgtTrees = {"(R (Q q) (P p))", "(Z (Y y) (X x))", 
				 			 "(Z (V v) (W w) (X x))", "(Z (X x) (W w) (Y y))"};
		String[] aligns = {"1-0", "0-0 0-1", "0-1 1-0 2-2", "0-0 0-1 1-2 2-2"};
		
		for (int i = 0; i < srcTrees.length; i++)
		{
			System.out.println("Test " + i);
			testCase(srcTrees[i], tgtTrees[i], aligns[i], i);
		}
		
	}
	
	private static void testCase(String srcTree, String tgtTree, 
								 String aligns, int index)
	{
		WordAlignment wordAligns = null;
		
		try {
			wordAligns = new WordAlignment(aligns);
		}
		catch (MalformedAlignmentException e)
		{
			assertTrue("malformed alignment", false);
		}
		
		ParseNode srcNode = null;
		ParseNode tgtNode = null;
		try 
		{
			srcNode = new ParseNode(srcTree);
			tgtNode = new ParseNode(tgtTree);
		}
		catch (MalformedTreeException e)
		{
			assertTrue("malformed tree", false);
		}
		
		aligner.align(srcNode, tgtNode, wordAligns);
		Set<ExtractedRule> rules = extractor.extract(srcNode, tgtNode);
		
		Set<MockExtractedRule> goodRules = goodRulesList.get(index);
		assertTrue(goodRules.size() == rules.size());
		
		for (ExtractedRule rule : rules)
		{
			MockExtractedRule foundRule = null;
			for (MockExtractedRule expectedRule : goodRules)
			{
				if (expectedRule.equivalentRule(rule))
				{
					foundRule = expectedRule;
					break;
				}
			}
			
			assertTrue("Missing rule: " + rule, foundRule!=null);
			goodRules.remove(foundRule);
		}
		
		assertTrue(goodRules.isEmpty());
	}
	
	private static VamshiNodeAligner aligner;
	private static BaseGrammarExtractor extractor;
	private static List<Set<MockExtractedRule>> goodRulesList;
	
	private static final int MAX_RULE_SIZE = 4;
	private static final int MAX_VIRTUAL_COMPONENTS = 4;
	private static final String DELIM = "::";
	
}
