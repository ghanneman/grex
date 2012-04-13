package ruleLearnerNew;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;


public class WordAlignment
{
	// CONSTRUCTORS: ////////////////////////////////////////////////////////

    public WordAlignment()
    {
        // Initialize empty alignment structures:
        fromSrc = new HashMap<Integer, Set<Integer>>();
        fromTgt = new HashMap<Integer, Set<Integer>>();
        maxSrcIndex = -1;
        maxTgtIndex = -1;
    }

    // From a Moses-format Viterbi alignment string, e.g. "0-0 1-2 1-3":
    public WordAlignment(String mosesAligns) throws MalformedAlignmentException
    {
        // Initialize empty alignment structures:
        fromSrc = new HashMap<Integer, Set<Integer>>();
        fromTgt = new HashMap<Integer, Set<Integer>>();
        maxSrcIndex = -1;
        maxTgtIndex = -1;

        // Fill in the structures:
        mosesAligns = mosesAligns.trim();
        String[] aligns = mosesAligns.split("\\s+");
        for(String s : aligns)
        {
            // Add representation for this word-to-word link:
            String[] srcTgt = s.split("-");
            if(srcTgt.length != 2)
            {
            	throw new MalformedAlignmentException("Not a valid alignment: "
            										  + s);
                //System.err.println("WARNING: Alignment \"" + s +
                //                   "\" is malformed.  Skipping it!");
                //continue;
            }
            if((srcTgt[0].length() > 0) && (srcTgt[1].length() > 0))
            {
                int src = Integer.parseInt(srcTgt[0]);
                int tgt = Integer.parseInt(srcTgt[1]);
                addLink(src, tgt);
            }
			else
			{
				throw new MalformedAlignmentException("Not a valid alignment: "
													  + s);
                //System.err.println("WARNING: Alignment \"" + s +
                //                   "\" is malformed.  Skipping it!");
                //continue;
			}
        }
    }

    
    // FUNCTIONS: ///////////////////////////////////////////////////////////

    // Add an alignment link to this sentence pair between the specified words.
    // The indexes are 0-based.
    public void addLink(int srcIndex, int tgtIndex)
    {
        // Insert bidirectional link into lists of links:
        if(!fromSrc.containsKey(srcIndex))
            fromSrc.put(srcIndex, new TreeSet<Integer>());
        if(!fromTgt.containsKey(tgtIndex))
            fromTgt.put(tgtIndex, new TreeSet<Integer>());
        fromSrc.get(srcIndex).add(tgtIndex);
        fromTgt.get(tgtIndex).add(srcIndex);

        // Update maximum aligned indexes:
        if(srcIndex > maxSrcIndex)
            maxSrcIndex = srcIndex;
        if(tgtIndex > maxTgtIndex)
            maxTgtIndex = tgtIndex;
	}


    // Returns the list of target indexes linked to by the specified source
    // word index.  The indexes are 0-based.
    public Set<Integer> getLinksForSrcWord(int srcIndex)
    {
        Set<Integer> result = fromSrc.get(srcIndex);
        if(result == null)
            result = new TreeSet<Integer>();
        return result;
    }


    // Returns the list of source indexes linked to by the specified target
    // word index.  The indexes are 0-based.
    public Set<Integer> getLinksForTgtWord(int tgtIndex)
    {
        Set<Integer> result = fromTgt.get(tgtIndex);
        if(result == null)
            result = new TreeSet<Integer>();
        return result;
    }


	// Returns a list of all source word indexes that are aligned.  The indexes
	// are 0-based.
	public Set<Integer> getAllSrcAlignedWords()
	{
		return fromSrc.keySet();
	}


	// Returns a list of all target word indexes that are aligned.  The indexes
	// are 0-based.
	public Set<Integer> getAllTgtAlignedWords()
	{
		return fromTgt.keySet();
	}

    // Returns the set of links as a Moses-format Viterbi alignment string.
	// Use sparingly because Jon says this sort of string-building in Java
	// is really slow.
    public String getMosesString()
    {
        // Moses "0-0 2-1 3-1", etc. strings are in order by target index,
        // then by source index:
        String out = "";
        Integer[] sortedTargets = new Integer[fromTgt.keySet().size()];
        sortedTargets = fromTgt.keySet().toArray(sortedTargets);
        Arrays.sort(sortedTargets);
        for(Integer t : sortedTargets)
        {
            Integer[] sortedSources = new Integer[fromTgt.get(t).size()];
            sortedSources = fromTgt.get(t).toArray(sortedSources);
            Arrays.sort(sortedSources);
            for(int s : sortedSources)
                out += (s + "-" + t + " ");
        }
		return out.trim();
    }

    
	// MEMBER VARIABLES: ////////////////////////////////////////////////////

	// Maps from a {src, tgt} word to its {tgt, src} alignments:
    private Map<Integer, Set<Integer>> fromSrc;
    private Map<Integer, Set<Integer>> fromTgt;

    // The maximum source and target indexes that have alignments:
    private int maxSrcIndex;
    private int maxTgtIndex;
}
