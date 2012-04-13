package ruleLearnerNew;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class HadoopRuleLearner extends Configured implements Tool 
{
	public static void main(String[] args) throws Exception {
		int returnValue = ToolRunner.run(new Configuration(), 
										 new HadoopRuleLearner(), 
										 args);
		System.exit(returnValue);
	}

	public int run(String[] args) throws Exception {
	    Configuration conf =  getConf();

		// Check usage:
		// TODO: Very dumb for now; add proper options, verification, etc.
	    int minOpts = 8;
	    int maxOpts = 9;
	    
		if(args.length < minOpts || args.length > maxOpts)
		{
			System.err.println("Usage:");
			System.err.println("java RuleLearner <inputs> "
							   + "<max-grammar-rule-size> "
							   + "<max-phrase-rule-size>"
							   + "<allow-unary> "
					           + "<allow-triangular> "
					           + "<max-virtual-node-size>"
					           + "<output-dir>"
					           + "<output-aligns>"
					           );
			return 1;
		}
		
		String inputDir = args[0];
		
		conf.set("MAX_G_RULE_SIZE", args[1]);
		conf.set("MAX_P_RULE_SIZE", args[2]);
		conf.set("ALLOW_UNARY", args[3]);
		conf.set("ALLOW_TRIANGULAR", args[4]);
		conf.set("MAX_V_NODE_SIZE", args[5]);
		
		String outputDir = null;
		outputDir = args[6];
		
		conf.set("OUTPUT_ALIGNS", args[7]);
		
		Path inDir = new Path(inputDir);
		Path outDir = new Path(outputDir);
		
		PrintWriter countersOut = new PrintWriter("ruleExtraction.counters.txt");
		
		String name = "Rule extraction";
		Job job = new Job(getConf(), name);
		System.err.println("Starting:" + name);

		FileInputFormat.setInputPaths(job, inDir);
		FileOutputFormat.setOutputPath(job, outDir);

		Class<? extends Mapper<LongWritable, Text, LongWritable, Text>> mapperClass 
				= RuleLearnerMapper.class;
		job.setJarByClass(mapperClass);
		job.setMapperClass(mapperClass);
		job.setNumReduceTasks(0);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		runJob(countersOut, "", job);
		
		System.err.println("Done: " + name);
		System.err.println("Wrote alignment output to directory: " + outDir);
		
		countersOut.close();
		
		return 0;
	}
	
	// This is the code used by phrase dozer, may want to change based on what 
	// we need.
	private void runJob(PrintWriter countersOut, String prefix, Job job) throws IOException,
	InterruptedException, ClassNotFoundException 
	{

		long startTime = System.currentTimeMillis();
		boolean result = job.waitForCompletion(true);
		if (result == false) {
			throw new RuntimeException("Job failed.");
		}
		long finishTime = System.currentTimeMillis();
		
		Collection<String> groups = job.getCounters().getGroupNames();
		for (String group : groups) {
			CounterGroup counterGroup = job.getCounters().getGroup(group);
			for (Counter counter : counterGroup) {
				String key =
						prefix + ".counter." + counterGroup.getDisplayName().trim() + "."
								+ counter.getDisplayName().trim();
				key = key.replace(" ", "_");
				String value = counter.getValue() + "";
				countersOut.println(key + "\t" + value);
			}
		}
		
		// collect the amount of time that each of the tasks took in serial
		int minTaskSeconds = Integer.MAX_VALUE;
		int maxTaskSeconds = 0;
		int sumTaskSeconds = 0;
		int nTasks = 0;
		int nMaps = 0;
		int nReduces = 0;
		for (TaskCompletionEvent task : job.getTaskCompletionEvents(0)) {
			if (task.isMapTask()) {
				nMaps++;
			} else {
				nReduces++;
			}
			int taskSeconds = task.getTaskRunTime() / 1000;
			sumTaskSeconds += taskSeconds;
			minTaskSeconds = Math.min(minTaskSeconds, taskSeconds);
			maxTaskSeconds = Math.max(maxTaskSeconds, taskSeconds);
			nTasks += 1;
		}
		
		countersOut.println(prefix + ".NumMaps\t" + nMaps);
		countersOut.println(prefix + ".NumReduces\t" + nReduces);
		countersOut.println(prefix + ".Time.MinTaskSeconds\t" + minTaskSeconds);
		countersOut.println(prefix + ".Time.MaxTaskSeconds\t" + maxTaskSeconds);
		countersOut.println(prefix + ".Time.AvgTaskSeconds\t" + sumTaskSeconds / (float) nTasks);
		countersOut.println(prefix + ".Time.SumTaskSeconds\t" + sumTaskSeconds);
		
		long secondsElapsed = (finishTime - startTime) / 1000;
		countersOut.println(prefix + ".Time.StartTime\t" + (startTime / 1000));
		countersOut.println(prefix + ".Time.FinishTime\t" + (finishTime / 1000));
		countersOut.println(prefix + ".Time.SecondsElapsed\t" + secondsElapsed);
		
		System.err.println("Job took " + secondsElapsed + " seconds");
	}
}
