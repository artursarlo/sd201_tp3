package pagerank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.google.common.io.Files;

/*
 * VERY IMPORTANT 
 * 
 * Each time you need to read/write a file, retrieve the directory path with conf.get 
 * The paths will change during the release tests, so be very carefully, never write the actual path "data/..." 
 * CORRECT:
 * String initialVector = conf.get("initialRankVectorPath");
 * BufferedWriter output = new BufferedWriter(new FileWriter(initialVector + "/vector.txt"));
 * 
 * WRONG
 * BufferedWriter output = new BufferedWriter(new FileWriter(data/initialVector/vector.txt"));
 */

public class PageRank {
	
	public static void createInitialRankVector(String directoryPath, long n) throws IOException 
	{
		System.out.println("Create Initial Rank Vector!");
		File dir = new File(directoryPath);
		if(! dir.exists())
			FileUtils.forceMkdir(dir);
		BufferedWriter output = new BufferedWriter(new FileWriter(directoryPath + "/part-r-00000"));
        for (int k =0; k<n; k++){
        	output.write(Integer.toString(k+1) + " " + Double.toString(1.0/n));
        	if (k != (n-1))
        		output.write("\n");
        }
        output.close();
	}
	
	public static boolean checkConvergence(String initialDirPath, String iterationDirPath, double epsilon) throws IOException
	{
		System.out.println("Check Convergence!");
		BufferedReader ri = new BufferedReader(new FileReader(initialDirPath + "/part-r-00000"));
		BufferedReader rii = new BufferedReader(new FileReader(iterationDirPath + "/part-r-00000"));
		String line_ri = ri.readLine();
		String line_rii = rii.readLine();
		System.out.println("Check Convergence: " +line_ri + " " + line_rii);
		while(line_ri != null){
//			line_ri = line_ri.replaceAll("\n", "");
//			line_rii = line_rii.replaceAll("\n", "");
			line_ri = line_ri.split("\\s+")[1];
			line_rii = line_rii.split("\\s+")[1];
			System.out.println("Check Convergence: " +line_ri + " " + line_rii);
			if (Math.abs(Double.parseDouble(line_rii) - Double.parseDouble(line_ri)) > epsilon){
				ri.close();
				rii.close();
				return false;
			}			
			line_ri = ri.readLine();
			line_rii = rii.readLine();
		}
		ri.close();
		rii.close();
		return true;
	}
	
	public static void avoidSpiderTraps(String vectorDirPath, long nNodes, double beta) throws IOException 
	{
		System.out.println("Avoid Spider Traps!");
		BufferedReader ri = new BufferedReader(new FileReader(vectorDirPath + "/part-r-00000"));
		
		String line_ri = ri.readLine();
		String final_result = "";
		while(line_ri != null){
//			line_ri = line_ri.replaceAll("\n", "");
			String[] list = line_ri.split("\\s+");
			final_result = final_result.concat(list[0] + " ");
			final_result = final_result.concat(Double.toString((Double.parseDouble(list[1])*beta + (1.0/nNodes)*(1.0 -beta))));
			System.out.println("Spider Trap Iteration: " + list[0] + " " + Double.toString((Double.parseDouble(list[1])*beta + (1.0/nNodes)*(1.0 -beta))));
			line_ri = ri.readLine();
			if (line_ri != null)
				final_result = final_result.concat("\n");
			
		}
		ri.close();
		FileUtils.deleteDirectory(new File(vectorDirPath));
		FileUtils.forceMkdir(new File(vectorDirPath));		
		BufferedWriter output = new BufferedWriter(new FileWriter(vectorDirPath + "/part-r-00000"));
		output.write(final_result);
		output.close();
		
		System.out.println("Spider Trap: " + final_result);
//		BufferedWriter rii = new BufferedWriter(new FileWriter(vectorDirPath + "/part-r-00001"));
//		rii.write(final_result);
//		rii.close();
//		Files.move( new File(vectorDirPath + "/part-r-00001"),  new File(vectorDirPath + "/part-r-00000"));
	}
	
	public static void iterativePageRank(Configuration conf) 
			throws IOException, InterruptedException, ClassNotFoundException
	{
		System.out.println("Iterative Page Rank!");
		String initialVector = conf.get("initialVectorPath");
		String currentVector = conf.get("currentVectorPath");
		
		String finalVector = conf.get("finalVectorPath"); 
		/*here the testing system will search for the final rank vector*/
		
		Double epsilon = conf.getDouble("epsilon", 0.1);
		Double beta = conf.getDouble("beta", 0.8);

//		FIRST thing that i have to do
		createInitialRankVector(initialVector, conf.getLong("numNodes", 0));
		FileUtils.deleteDirectory(new File(currentVector));
		GraphToMatrix.job(conf);
		MatrixVectorMult.job(conf);
		avoidSpiderTraps(currentVector, conf.getLong("numNodes", 0), beta);
		
		while (!checkConvergence(initialVector, currentVector, epsilon)){
			System.out.println("Iterative Page Rank: Inside While!");
			FileUtils.deleteDirectory(new File(initialVector));
			FileUtils.copyDirectory(new File(currentVector), new File(initialVector));
			FileUtils.deleteDirectory(new File(currentVector));
			MatrixVectorMult.job(conf);
			avoidSpiderTraps(currentVector, conf.getLong("numNodes", 0), beta);
		}
		
		System.out.println("Iterative Page Rank: After while!");
		FileUtils.copyDirectory(new File(currentVector), new File(finalVector));
		

		// when you finished implementing delete this line
//		throw new UnsupportedOperationException("Implementation missing");
		
	}
}
