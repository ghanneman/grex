package ruleLearnerNew;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
		int minArgs = 8;
		int maxArgs = 11;
		if(args.length < minArgs || args.length > maxArgs)
		{
			System.err.println("Usage:");
			System.err.println("java RuleLearner <src-trees> <tgt-trees> "
					           + "<word-aligns>"
					           + "<max-grammar-rule-size>"
					           + "<max-phrase-rule-size>"
					           + "<allow-unary>"
					           + "<allow-triangular> "
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
		int maxVirtualNodeComponents = Integer.parseInt(args[7]);
		
		String alignOutputFile = null;
		
		if (args.length > minArgs)
		{
			alignOutputFile = args[minArgs];
		}

		Integer startSent = null;
		Integer endSent = null;
		
		if (args.length > minArgs + 1)
		{
			startSent = Integer.parseInt(args[minArgs + 1]);
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
									 allowTriangular);
		while((srcLine != null) && (tgtLine != null) && (alignLine != null))
		{
			if (startSent == null || startSent <= sentNum && endSent >= sentNum)
			{
				System.out.println("Sentence " + sentNum);
				
				// Create parse tree structures and word alignment matrix:
				boolean hadError = false;
				ParseNode srcRoot = null;
				ParseNode tgtRoot = null;
				WordAlignment wordAligns = null;
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
					// ************ ACTUAL MAIN WORK STARTS HERE ****************
					// Align nodes:
					NodeAlignmentList alignedNodes = 
						nodeAligner.align(srcRoot, tgtRoot, wordAligns);
	
					if (alignOutputFile != null)
					{
						// Output
						PrintWriter out;
						try 
						{
							out = new PrintWriter(new FileWriter(alignOutputFile));
							for (String aligned : alignedNodes.getInterchangeFormatStringList())
							{
								out.println(aligned);
							}
							out.close();
						} catch (IOException e) {
							System.err.println("Align output failed: " + e.getMessage());
						}
	
					}
					// Extract grammar:
					Set<ExtractedRule> rules = 
						grammarExtractor.extract(srcRoot, tgtRoot);
					
					if (allowUnary)
					{
						for (ExtractedRule rule : rules)
						{
							System.out.println(rule.toString());
						}
					}
					else
					{
						for (ExtractedRule rule : rules)
						{
							if (!rule.isParallelUnary())
							{
								System.out.println(rule);
							}
						}
					}
											
					// **********************************************************
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
}