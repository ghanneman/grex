package ruleLearnerNew;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ghannema
 *
 */
public class RuleLearner
{
	public static void main(String[] args)
	{
		// Check usage:
		// TODO: Very dumb for now; add proper options, verification, etc.
		// NOTE: Minimal rules only is single decomposition when max-virtual-node-components
		// is set to one. Otherwise, multiple decompositions are possible!
		int minArgs = 9;
		int maxArgs = 12;
		if(args.length < minArgs || args.length > maxArgs)
		{
			System.err.println("Usage:");
			System.err.println("java RuleLearner <src-trees> <tgt-trees> "
					           + "<word-aligns>"
					           + "<max-grammar-rule-size>"
					           + "<max-phrase-rule-size>"
					           + "<allow-unary>"
					           + "<allow-triangular> "
					           + "<minimal-rules-only> "
					           + "<max-virtual-node-components>"
					           + "[<align-output-file>] " 
					           + "[<startsent>] [<endsent>]");
			System.exit(1);
		}

		// Open input files:
		BufferedReader srcReader;
		try 
		{
			srcReader = new BufferedReader(new FileReader(args[0]));
		} catch (FileNotFoundException e1) 
		{
			System.err.println("Source tree file not found: " + e1.getMessage());
			System.err.println("Exiting...");
			return;
		}
		BufferedReader tgtReader;
		try 
		{
			tgtReader = new BufferedReader(new FileReader(args[1]));
		} catch (FileNotFoundException e1) 
		{
			System.err.println("Target tree file not found: " + e1.getMessage());
			System.err.println("Exiting...");
			return;
		}
		BufferedReader alignReader;
		try 
		{
			alignReader = new BufferedReader(new FileReader(args[2]));
		} catch (FileNotFoundException e1) {
			System.err.println("Alignments file not found: " + e1.getMessage());
			System.err.println("Exiting...");
			return;
		}
		
		int maxGrammarRuleSize = Integer.parseInt(args[3]);
		int maxPhraseRuleSize = Integer.parseInt(args[4]);
		boolean allowUnary = Boolean.parseBoolean(args[5]);
		boolean allowTriangular = Boolean.parseBoolean(args[6]);
		boolean minimalRulesOnly = Boolean.parseBoolean(args[7]);
		int maxVirtualNodeComponents = Integer.parseInt(args[8]);
		
		String alignOutputFile = null;
		
		if (args.length > minArgs)
		{
			alignOutputFile = args[minArgs];
		}

		Integer startSent = 1;
		Integer endSent = Integer.MAX_VALUE;
		
		if (args.length > minArgs + 1)
		{
			startSent = Integer.parseInt(args[minArgs + 1]);
			if (args.length > minArgs + 2)
				endSent = Integer.parseInt(args[minArgs + 2]);
		}
		// Read first lines of files:
		int sentNum = 1;
		String srcLine = null;
		String tgtLine = null;
		String alignLine = null;
		try
		{
			srcLine = srcReader.readLine();
			tgtLine = tgtReader.readLine();
			alignLine = alignReader.readLine();
		}
		catch(IOException e)
		{
			if(srcLine == null)
				System.err.println("ERROR: Can't read from file " + args[0]);
			if(tgtLine == null)
				System.err.println("ERROR: Can't read from file " + args[1]);
			if(alignLine == null)
				System.err.println("ERROR: Can't read from file " + args[2]);
			e.printStackTrace();
			System.exit(1);
		}

		// Run node alignment and grammar extraction on each sentence:
		VamshiNodeAligner nodeAligner = 
			new VamshiNodeAligner(maxVirtualNodeComponents);
		BaseGrammarExtractor grammarExtractor = 
			new BaseGrammarExtractor(maxGrammarRuleSize,
									 maxPhraseRuleSize,
									 allowTriangular,
									 minimalRulesOnly);
		while((srcLine != null) && (tgtLine != null) && (alignLine != null))
		{
			if ((startSent == null || startSent <= sentNum) && (endSent == null || endSent >= sentNum))
			{
				System.out.println("Sentence " + sentNum);
				
				// Create parse tree structures and word alignment matrix:
				boolean hadError = false;
				ParseNode srcRoot = null;
				ParseNode tgtRoot = null;
				WordAlignment wordAligns = null;
				WordAlignment reversedWordAligns = null;
				try
				{
					srcRoot = new ParseNode(srcLine);
				}
				catch(MalformedTreeException e)
				{
					System.err.println("ERROR: " + args[0] + " at line " +
									   sentNum + ": " + e.getMessage());
					hadError = true;
				}
				try
				{
					tgtRoot = new ParseNode(tgtLine);
				}
				catch(MalformedTreeException e)
				{
					System.err.println("ERROR: " + args[1] + " at line " +
									   sentNum + ": " + e.getMessage());
					hadError = true;
				}
				try
				{
					wordAligns = new WordAlignment(alignLine);
					reversedWordAligns = new WordAlignment(alignLine, false);
				}
				catch(MalformedAlignmentException e)
				{
					System.err.println("ERROR: " + args[2] + " at line " +
									   sentNum + ": " + e.getMessage());
					hadError = true;
				}
	
				// Continue only if trees and word aligns OK; otherwise skip
				// this sentence and go to the next one:
				if(!hadError)
				{
					try
					{
						// ************ ACTUAL MAIN WORK STARTS HERE ****************
						List<String> forwardRules = extractFromSentencePair(allowUnary, alignOutputFile,
								sentNum, nodeAligner, grammarExtractor, srcRoot,
								tgtRoot, wordAligns, true, startSent);
						
						// If we extract backwards, we have to dedup but this is difficult
						// because the same rule can be extracted more than once from within one sentence.
						//HashSet<String> backwardRules = extractFromSentencePair(allowUnary, null,
						//		sentNum, nodeAligner, grammarExtractor, tgtRoot,
						//		srcRoot, reversedWordAligns, false, startSent);
						
						List<String> allRules = new ArrayList<String>();
						allRules.addAll(forwardRules);
						//allRules.addAll(backwardRules);
						
						for(String rule : allRules)
							System.out.println(rule);
												
						// **********************************************************
					}
					catch( Exception e )
					{
						System.err.println( "Error extracting rules from sentence #" + sentNum );
						System.err.println( srcLine );
						System.err.println( tgtLine );
						System.err.println( alignLine );
						throw new RuntimeException( e );
					}
				}
			}
			
			// Get ready for next iteration:
			sentNum++;
			try
			{
				srcLine = srcReader.readLine();
				tgtLine = tgtReader.readLine();
				alignLine = alignReader.readLine();
			}
			catch(IOException e)
			{
				if(srcLine == null)
				{
					System.err.println("ERROR: Can't read from file " +
									   args[0] + " at line " + sentNum);
				}
				
				if(tgtLine == null)
				{
					System.err.println("ERROR: Can't read from file " +
									   args[1] + " at line " + sentNum);
				}
				
				if(alignLine == null)
				{
					System.err.println("ERROR: Can't read from file "
									   + args[2] + " at line " + sentNum);
				}
				
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static List<String> extractFromSentencePair(boolean allowUnary,
			String alignOutputFile, int sentNum, VamshiNodeAligner nodeAligner,
			BaseGrammarExtractor grammarExtractor, ParseNode srcRoot,
			ParseNode tgtRoot, WordAlignment wordAligns, boolean sourceFirst, int startSent) {
		// Align nodes:
		NodeAlignmentList alignedNodes = 
			nodeAligner.align(srcRoot, tgtRoot, wordAligns);

		if (alignOutputFile != null)
		{
			// Output
			PrintWriter out;
			try 
			{
				out = new PrintWriter(new FileWriter(alignOutputFile, (sentNum > startSent)));
				out.println(String.format("Sentence %d", sentNum));
				for (String aligned : alignedNodes.getInterchangeFormatStringList())
					out.println(aligned);
				out.close();
			} catch (IOException e) {
				System.err.println("Align output failed: " + e.getMessage());
			}

		}
		// Extract grammar:
		Set<ExtractedRule> rules = 
			grammarExtractor.extract(srcRoot, tgtRoot);
		List<String> ruleStrings = new ArrayList<String>();
		
		for (ExtractedRule rule : rules)
		{
			if(allowUnary || !rule.isParallelUnary())
			{
				if(sourceFirst)
					ruleStrings.add(rule.toString());
				else
					ruleStrings.add(rule.toReversedString());
			}
		}
		
		return ruleStrings;
	}
}
