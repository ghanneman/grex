package ruleLearnerTest;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ruleLearnerNew.BiSpan;
import ruleLearnerNew.MalformedAlignmentException;
import ruleLearnerNew.MalformedTreeException;
import ruleLearnerNew.NodeAlignmentList;
import ruleLearnerNew.ParseNode;
import ruleLearnerNew.VamshiNodeAligner;
import ruleLearnerNew.WordAlignment;
import ruleLearnerNew.NodeAlignmentList.NodeAlignmentType;

public class VamshiNodeAlignerTest {

	@Before
	public void setUp()
	{
		goodLists = new ArrayList<NodeAlignmentList>();
		T2T = NodeAlignmentList.NodeAlignmentType.T2T.getTypeVal();
		T2TS = NodeAlignmentList.NodeAlignmentType.T2TS.getTypeVal();
		TS2T = NodeAlignmentList.NodeAlignmentType.TS2T.getTypeVal();
		T2S = NodeAlignmentList.NodeAlignmentType.T2S.getTypeVal();
		S2T = NodeAlignmentList.NodeAlignmentType.S2T.getTypeVal();
		TS2TS = NodeAlignmentList.NodeAlignmentType.TS2TS.getTypeVal();
		
		tgtGrown = NodeAlignmentList.GrownType.TGT_GROWN.getTypeVal();
		srcGrown = NodeAlignmentList.GrownType.SRC_GROWN.getTypeVal();
		
		NodeAlignmentList goodList1 = new NodeAlignmentList();
		goodList1.add(0,1,4,4,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList1.add(0,1,4,5,true, T2T,T2TS,TS2T,T2S,S2T,tgtGrown);
		goodList1.add(0,2,4,6,true, S2T);
		goodList1.add(0,5,0,6,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList1.add(2,2,5,6,true, T2S,tgtGrown);
		goodList1.add(2,2,6,6,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList1.add(3,3,0,0,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList1.add(3,3,0,1,true, T2TS,T2S,tgtGrown);
		goodList1.add(3,4,0,0,true, S2T,srcGrown);
		goodList1.add(3,5,0,3,true, S2T);
		goodList1.add(4,5,1,3,true, T2TS,T2S,srcGrown,tgtGrown);
		goodList1.add(4,5,2,3,true, T2T,T2TS,TS2T,T2S,S2T,srcGrown);
		goodList1.add(5,5,1,3,true, T2TS,T2S,tgtGrown);
		goodList1.add(5,5,2,3,true, T2T,T2TS,TS2T,T2S,S2T);
		
		goodLists.add(goodList1);
		
		NodeAlignmentList goodList2 = new NodeAlignmentList();
		goodList2.add(0,1,1,1,true, S2T,srcGrown);
		goodList2.add(0,2,0,3,true, TS2T,S2T,srcGrown,tgtGrown);
		goodList2.add(0,2,1,2,true, TS2T,S2T,srcGrown);
		goodList2.add(0,3,0,2,true, T2TS,T2S,srcGrown,tgtGrown);
		goodList2.add(0,3,0,3,true, T2T,T2TS,TS2T,T2S,S2T,srcGrown,tgtGrown);
		goodList2.add(0,3,1,2,true, T2T,T2TS,TS2T,T2S,S2T,srcGrown);
		goodList2.add(0,3,1,3,true, T2TS,T2S,srcGrown,tgtGrown);
		goodList2.add(1,1,0,1,true, T2S,tgtGrown);
		goodList2.add(1,1,1,1,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList2.add(1,2,0,2,true, T2TS,T2S,tgtGrown);
		goodList2.add(1,2,0,3,true, T2T,T2TS,TS2T,T2S,S2T,tgtGrown);
		goodList2.add(1,2,1,2,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList2.add(1,2,1,3,true, T2TS,T2S,tgtGrown);
		goodList2.add(1,3,0,3,true, TS2T,S2T,srcGrown,tgtGrown);
		goodList2.add(1,3,1,2,true, TS2T,S2T,srcGrown);
		goodList2.add(2,2,2,2,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList2.add(2,2,2,3,true, T2S,tgtGrown);
		goodList2.add(2,3,2,2,true, S2T,srcGrown);
		
		goodLists.add(goodList2);
		
		NodeAlignmentList goodList3 = new NodeAlignmentList();
		goodList3.add(0,0,3,3,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList3.add(0,1,2,3,true, T2TS,T2S);
		goodList3.add(0,3,0,3,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList3.add(1,1,2,2,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList3.add(2,2,1,1,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList3.add(2,3,0,1,true, T2TS,T2S);
		goodList3.add(3,3,0,0,true, T2T,T2TS,TS2T,T2S,S2T);
		
		goodLists.add(goodList3);
		
		NodeAlignmentList goodList4 = new NodeAlignmentList();
		goodList4.add(0,0,4,5,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList4.add(0,1,2,5,true, T2S,tgtGrown);
		goodList4.add(0,1,3,5,true, T2S);
		goodList4.add(0,3,0,5,true, S2T);
		goodList4.add(0,4,0,5,true, T2T,T2TS,TS2T,T2S,S2T,srcGrown);
		goodList4.add(1,1,2,3,true, T2TS,T2S,tgtGrown);
		goodList4.add(1,1,3,3,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList4.add(1,3,0,3,true, S2T);
		goodList4.add(1,4,0,3,true, S2T,srcGrown);
		goodList4.add(2,2,1,1,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList4.add(2,2,1,2,true, T2TS,T2S,tgtGrown);
		goodList4.add(2,4,0,1,true, T2TS,T2S,srcGrown);
		goodList4.add(2,4,0,2,true, T2TS,T2S,TS2TS,srcGrown,tgtGrown);
		goodList4.add(3,3,0,0,true, T2T,T2TS,TS2T,T2S,S2T);
		goodList4.add(3,4,0,0,true, TS2T,S2T,srcGrown);
		goodList4.add(2,3,0,2,true, TS2TS, tgtGrown);
		
		goodLists.add(goodList4);
		
		aligner = new VamshiNodeAligner(MAX_VIRTUAL_COMPONENTS);
		
	}
	
	@Test
	public void TestAligns() 
	{
		String[] srcTrees = {"(A (B (C c) (D d)) (E (F (G g) (H h)) (I (J j) (K k))))",
							 "(A (B b) (C (D d) (E e)) (F f))",
							 "(A (B (D d) (E e)) (C (F f) (G g)))",
							 "(A (B (D d) (E e)) (C (F f) (G g) (H h)))"};
		String[] tgtTrees = {"(Z (Y (X x) (W w) (V (U u) (T t))) (S (R (Q q) (P p)) (O o)))",
							 "(Z (Y y) (X (W w) (V v)) (T t))",
							 "(Z (Y y) (X x) (W w) (V v))",
							 "(R (Z (Y y) (X x) (W w) (V v)) (S (T t) (U u)))"};
		String[] aligns = {"0-4 1-4 2-6 3-0 5-2 5-3", "1-1 2-2", "0-3 1-2 2-1 3-0",
						   "0-4 0-5 1-3 2-1 3-0"};
		
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
		
		NodeAlignmentList list = null;
		
		try 
		{
			list = aligner.align(new ParseNode(srcTree), new ParseNode(tgtTree), wordAligns);
		}
		catch (MalformedTreeException e)
		{
			assertTrue("malformed tree", false);
		}
		
		Map<BiSpan, Integer> goodAligns = (goodLists.get(index)).GetAllAligns();
		Map<BiSpan, Integer> testAligns = list.GetAllAligns();
		
		for (BiSpan span : goodAligns.keySet())
		{
			Integer good = goodAligns.get(span);
			Integer test = testAligns.get(span);
			
			assertTrue("Good: " + span + ", " + decomposeInt(good) 
						+ " Test: " + decomposeInt(test), good.equals(test));
		}
		
		for (BiSpan span : testAligns.keySet())
		{
			Integer good = goodAligns.get(span);
			Integer test = testAligns.get(span);
			
			assertTrue("Good: " + span + ", " + decomposeInt(good) 
						+ " Test: " + decomposeInt(test), test.equals(good));
		}		
	}
	
	private static String decomposeInt(Integer typeVal)
	{
		String types = "";
		
		if (typeVal == null)
		{
			return "";
		}
		
		for (NodeAlignmentType type : NodeAlignmentType.values())
		{
			if (type.inType(typeVal))
			{
				types += type.name() + " ";
			}
		}
		
		types.trim();
		return types;
	}
	
	private static VamshiNodeAligner aligner;
	private static ArrayList<NodeAlignmentList> goodLists;
	

	private static final int MAX_VIRTUAL_COMPONENTS = 4;
	
	private static int T2T;
	private static int T2TS;
	private static int TS2T;
	private static int T2S;
	private static int S2T;
	private static int TS2TS;
	
	private static int tgtGrown;
	private static int srcGrown;
}
