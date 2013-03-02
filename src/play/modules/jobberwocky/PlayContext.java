package play.modules.jobberwocky;


import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringUtils;

import play.Invoker;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.jobs.Job;

/**
 * Stand along class that initializes Play! environment.
 */
public class PlayContext {

	/** Default classloader */
	private ClassLoader oldClassLoader;
	
	public PlayContext() {
		String applicationPath = System.getProperty("application.path");
		String frameworkPath = System.getProperty("play.path");
		String playid = System.getProperty("play.id");
		bootstrap(frameworkPath, applicationPath, playid);
	}
	
	public PlayContext(final String frameworkPath, final String applicationPath, final String playid) {
		bootstrap(frameworkPath, applicationPath, playid);
	}	
	
	/**
	 * Bootstraps Play! framework
	 * @param frameworkPath
	 * @param applicationPath
	 */
	private void bootstrap(final String frameworkPath, final String applicationPath, final String playid) {
		// Play swap away the default class loader
		oldClassLoader = Thread.currentThread().getContextClassLoader();
		
	    Play.standalonePlayServer = true;
	 
	    // Setup Play Application path
	    if (applicationPath == null) {
	    	System.out.println("Application path is not set. Set using -Dapplication.path JVM argument");
        	throw new RuntimeException("Application path is not set");	    	
	    }
        File root = new File(applicationPath);
        System.out.println("Running Play! application in " + root.getAbsolutePath());
        if (!root.exists()) {
        	System.out.println("Application path does not exists: " + root.getAbsolutePath());
        	throw new RuntimeException("Invalid application path: " + root.getAbsolutePath());
        }
        
        // Setup Play Framework path
	    if (frameworkPath == null) {
	    	System.out.println("Play frameworkPath path is not set. Set using -Dplay.path JVM argument");
        	throw new RuntimeException("Play frameworkPath path is not set");	    	
	    }
        Play.frameworkPath = new File(frameworkPath);
        System.out.println("Play! Framework in " + Play.frameworkPath);
        if (!Play.frameworkPath.exists()) {
        	System.out.println("Play Framework directory does not exists: " + Play.frameworkPath.getAbsolutePath());
        	throw new RuntimeException("Invalid Play framework path: " + Play.frameworkPath.getAbsolutePath());
        }
        
        // Accept precompiled mode
	    File precompiled = new File(applicationPath, "precompiled");
        if (precompiled.exists()) {
        	System.out.println("Using precompiled code: " + precompiled.getAbsolutePath() + " code exists.");
        	Play.usePrecompiled = true;
        }
	    
        // Initialise Play
        Play.init(root, playid);
        try {
        	// Trigger plugins to load by running a dummy job
    		Job boostrapJob = (Job) BootstrapJob.class.newInstance();
    		Invoker.invoke(boostrapJob).get();    	
        }
        catch(Exception e) {
        	Logger.error(e, "Error initialization Play environment");
        }
        finally {
        	Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
	}
	
	/**
	 * Runs a Play job by job name (full package name)
	 * @param jobName
	 * @throws Exception
	 */
	public Object runJob(String jobName) throws Exception {
		Class jobClass = Play.classloader.loadClass( jobName );
		return runJob(jobClass, null);	
	}	
	
	/**
	 * Runs a Play job by job name (full package name)
	 * @param jobName
	 * @throws Exception
	 */
	public Object runJob(String jobName, Map<String,String> params) throws Exception {
		Class jobClass = Play.classloader.loadClass( jobName );
		return runJob(jobClass, params);	
	}		
	
	/**
	 * Runs a Play job and wait for its completion
	 * @param jobClass
	 * @throws Exception
	 */
	public Object runJob(Class jobClass, Map<String,String> params) throws Exception {
		Thread.currentThread().setContextClassLoader(Play.classloader);
		Job job = null;
		if (params != null) {
			Logger.debug("Executing job %s with arguments: %s", jobClass.getName(), params);
			try {
				job = (Job) (jobClass.getConstructor(Map.class)).newInstance(params);
			}
			catch(NoSuchMethodException e) {
				Logger.warn("Command line arguments %s provided but the job does not accept arguments.", params);
				Logger.warn("The job must have a Constructor with java.util.Map to accept arguments.");
				Logger.warn("Running the Job using the default Constructor");
				job = (Job) jobClass.newInstance();
			}
		}
		else {
			Logger.debug("Executing job %s", jobClass.getName());
			job = (Job) jobClass.newInstance();
		}
		Object returnResult = job.now().get();		
		Thread.currentThread().setContextClassLoader(oldClassLoader);
		return returnResult;
	}
	
	/**
	 * This is a dummy Job to force Play to kick-in its initialization process.
	 * The job doesn't have to do anything.
	 */
	static class BootstrapJob extends Job {
	    public void doJob() throws Exception {
	        Logger.info("Boostraping process completed");
	    }
	}
	
}
