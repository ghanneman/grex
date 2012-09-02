package ruleLearnerNew;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Mapper;

public class RuleLearnerMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
	
	private String getOpt(String opt, Context context)
	{
		String strVal = context.getConfiguration().get(opt);
		
		if (strVal == null) 
		{
			throw new RuntimeException("Required option not set: " + opt);
		}
		
		return strVal;
	}
	
	@Override
	public void setup(final Context context) 
	{
		int maxGrammarRuleSize = Integer.parseInt(getOpt("MAX_G_RULE_SIZE", context));
		
		int maxPhraseRuleSize = Integer.parseInt(getOpt("MAX_P_RULE_SIZE", context));
		
		int maxVirtualNodeComponents = Integer.parseInt(getOpt("MAX_V_NODE_SIZE", context));
		
		allowUnary = Boolean.parseBoolean(getOpt("ALLOW_UNARY", context));

		boolean allowTriangular = 
			Boolean.parseBoolean(getOpt("ALLOW_TRIANGULAR", context));
		
		boolean minimalRulesOnly = 
				Boolean.parseBoolean(getOpt("MINIMAL_RULES_ONLY", context));
		
		outputAligns = Boolean.parseBoolean(getOpt("OUTPUT_ALIGNS", context));

		sentences = context.getCounter("COUNT", "Sentences");
		badRecords = context.getCounter("COUNT", "BadRecords");
		
		nodeAligner = new VamshiNodeAligner(maxVirtualNodeComponents);
		grammarExtractor = new BaseGrammarExtractor(maxGrammarRuleSize, 
													maxPhraseRuleSize, 
													allowTriangular,
													minimalRulesOnly);
	}

	@Override
	public void map(LongWritable dummy, Text value, final Context context) throws IOException,
			InterruptedException {
		
		final String strValue = value.toString().trim();
		System.err.println("Processing record: " + strValue);
		String[] fields = strValue.split(" \\|\\|\\| ");

		if (fields.length != 3) 
		{
			badRecords.increment(1);
		}
		
		String srcTree = fields[0];
		String tgtTree = fields[1];
		String aligns = fields[2];

		// Create parse tree structures and word alignment matrix:
		boolean hadError = false;
		ParseNode srcRoot = null;
		ParseNode tgtRoot = null;
		WordAlignment wordAligns = null;
		
		sentences.increment(1);

		try
		{
			srcRoot = new ParseNode(srcTree);
		}
		catch(MalformedTreeException e)
		{
			System.err.println("ERROR: " + srcTree + ": " + e.getMessage());
			hadError = true;
		}
		try
		{
			tgtRoot = new ParseNode(tgtTree);
		}
		catch(MalformedTreeException e)
		{
			System.err.println("ERROR: " + tgtTree + ": " + e.getMessage());
			hadError = true;
		}
		try
		{
			wordAligns = new WordAlignment(aligns);
		}
		catch(MalformedAlignmentException e)
		{
			System.err.println("ERROR: " + aligns + e.getMessage());
			hadError = true;
		}

			if(!hadError)
			{
				// Align nodes:
				NodeAlignmentList list = 
					nodeAligner.align(srcRoot, tgtRoot, wordAligns);
				if (outputAligns)
				{
					for (String aligned : list.getInterchangeFormatStringList())
					{
						context.write(ALIGN_KEY, new Text(aligned));
					}
				}

				// Extract grammar:
				Collection<ExtractedRule> rules = 
					grammarExtractor.extract(srcRoot, tgtRoot);
				
				if (allowUnary)
				{
					for (ExtractedRule rule : rules)
					{
						context.write(RULE_KEY, new Text(rule.toString()));
					}
				}
				else
				{
					for (ExtractedRule rule : rules)
					{
						if (!rule.isParallelUnary())
						{
							context.write(RULE_KEY, new Text(rule.toString()));
						}
					}
				}
			}

	}
	
	
	private static LongWritable ALIGN_KEY = new LongWritable(1);
	private static LongWritable RULE_KEY = new LongWritable(2);
	
	private NodeAligner nodeAligner;
	private GrammarExtractor grammarExtractor;
	private Counter sentences;
	private Counter badRecords;
	
	private boolean allowUnary;
	private boolean outputAligns;
}
