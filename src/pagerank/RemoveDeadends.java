package pagerank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import pagerank.MatrixVectorMult.CombinerForSecondMap;
import pagerank.MatrixVectorMult.FirstMap;
import pagerank.MatrixVectorMult.FirstReduce;
import pagerank.MatrixVectorMult.SecondMap;
import pagerank.MatrixVectorMult.SecondReduce;

public class RemoveDeadends {

	enum myCounters{ 
		NUMNODES;
	}

	static class Map extends Mapper<LongWritable, Text, Text, Text> {
		
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
			{
			String[] values = value.toString().split("\\s+");
			context.write(new Text(values[0]), new Text("1 " + values[1]));
			context.write(new Text(values[1]), new Text("0 " + values[0]));			
//			System.out.println("RemoveDeadEndsMAP: " + values[0] + " 1 " + values[1]);
//			System.out.println("RemoveDeadEndsMAP: " + values[1] + " 0 " + values[0]);
			}
		}
	

	static class Reduce extends Reducer<Text, Text, Text, Text>  {
		
		protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
			
			List<String> link_list = new ArrayList<String>();
			int has_successor = 0;
			for (Text val : values){
				String[] link = val.toString().split("\\s+");
//				System.out.println("RemoveDeadEndsREDUCE: "+ key.toString() + " " + link[0] + " " +link[1] + " " +(link[0].compareTo("1") == 0));
				if (link[0].compareTo("1") == 0)
					has_successor = 1;
				else
					link_list.add(link[1]);
			}
//			System.out.println("\n");
			if (has_successor == 1){
			for (String link : link_list){
					context.write(new Text(link), key);
//					System.out.println("RemoveDeadEndsREDUCE: "+ link + " " + key.toString());
				}
			}
//			System.out.println("\n");
			if (has_successor == 1){
				context.getCounter(myCounters.NUMNODES).increment(1);
			}
		}		
}

	public static void job(Configuration conf) throws IOException, ClassNotFoundException, InterruptedException{
		
		
		boolean existDeadends = true;
		
		/* You don't need to use or create other folders besides the two listed below.
		 * In the beginning, the initial graph is copied in the processedGraph. After this, the working directories are processedGraphPath and intermediaryResultPath.
		 * The final output should be in processedGraphPath. 
		 */
		
		FileUtils.copyDirectory(new File(conf.get("graphPath")), new File(conf.get("processedGraphPath")));
		String intermediaryDir = conf.get("intermediaryResultPath");
		String currentInput = conf.get("processedGraphPath");
		
		long nNodes = conf.getLong("numNodes", 0);
		
			
		while(existDeadends)
		{
			Job job = Job.getInstance(conf);
			job.setJobName("deadends job");
			/* TO DO : configure job and move in the best manner the output for each iteration
			 * you have to update the number of nodes in the graph after each iteration,
			 * use conf.setLong("numNodes", nNodes);
			*/
			
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);

			job.setMapperClass(Map.class);
			job.setReducerClass(Reduce.class);

			job.setInputFormatClass(TextInputFormat.class);
			job.setOutputFormatClass(TextOutputFormat.class);

			FileInputFormat.setInputPaths(job, new Path(currentInput));
			FileOutputFormat.setOutputPath(job, new Path(intermediaryDir));

			job.waitForCompletion(true);
			
			System.out.println("RemoveDeadEndsREDUCE: END OF ONE REDUCE - " + job.getCounters().findCounter(myCounters.NUMNODES).getValue());

			if(nNodes == job.getCounters().findCounter(myCounters.NUMNODES).getValue()){
				existDeadends = false;
			}	
			else{
				nNodes = job.getCounters().findCounter(myCounters.NUMNODES).getValue();
				conf.setLong("numNodes", nNodes);
			}
			
			FileUtils.deleteDirectory(new File(currentInput));
			FileUtils.copyDirectory(new File(intermediaryDir), new File(currentInput));
			FileUtils.deleteDirectory(new File(intermediaryDir));
		}
//		nNodes = RemoveDeadends.myCounters.NUMNODES.ordinal();
		System.out.println("DEAD ENDS FINAL COUNT: " + nNodes);
//		conf.setLong("numNodes", nNodes);
		// when you finished implementing delete this line
//		throw new UnsupportedOperationException("Implementation missing");
		
		
	}
	
}