package play.modules.jobberwocky;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.Play;


public class RunJob  {
	
	/**
	 * Usage:
	 * <pre>
	 *  java ... -Dapplication.path=... -Dplay.path=... -Dplay.id=... jobberwocky.RunJob [Job Class] [Arguments]
	 * </pre>
	 * 
	 * <p>
	 * The classpath must include:
     * </p>
	 * 1. Application path and all its JAR dependencies
	 * 2. Application conf for any properties that ned to be loaded by classloader.
	 * 3. Play Framework Jar and all its JAR dependencies
	 * 
	 * @param args - The first argument is the Job class name
	 *             - Any remaining arguments will be passed into the Job {@link Map} constructor, if one exists.
	 */
	public static void main(String[] args) {
	
		if (args.length == 0) {
			System.err.println("Requires Job class name");
			usage();
			System.exit(1);
		}
		
		int    exitCondition = 0;
		String jobName = args[0];
		try {	
			PlayContext context = new PlayContext();
			Object returnResult = context.runJob(jobName, getJobArguments(args)); 
			Logger.info("Job %s has finished normally. Returns result: %s", jobName, returnResult);
			
			// Set the exit condition if the returnResult is a number
			if (returnResult instanceof Number) {
				exitCondition = ((Number) returnResult).intValue();
			}
		}
		catch(Exception e) {
			Logger.error(e, "Error in running Job %s. The job application will stop", jobName);	
			exitCondition = 1;
		}
		finally {
			Play.stop();
			System.out.println("Job " + jobName + " has stopped");
		}
		System.exit(exitCondition);
	}

	/**
	 * Convert command line arguments into Map of Strings.
	 * @param args
	 * @return
	 */
	private static Map<String,String> getJobArguments(String[] args) {
		if (args.length < 2) return null;
		Map<String,String> arguments = new HashMap();
		for (int i=1;i<args.length; i++) {
			if (args[i].contains("=")) {
				String[] nvp = args[i].split("=");
				arguments.put(nvp[0], nvp[1]);
			}
			else {
				arguments.put(String.valueOf(i), args[i]);
			}
		}
		return arguments;
	}
	
	private static void usage() {
		System.out.println("Usage: ");
		System.out.println("   java ... -Dapplication.path=... -Dplay.path=... -Dplay.id=... jobberwocky.RunJob [Job Class] [Arguments]");
		System.out.println();
		System.out.println("     Job Class       Play job class name");
		System.out.println("     Arguments       Optional. Arguments to pass into Job constructor as Map entries");
	}

}
