# Here you can create play commands that are specific to the module, and extend existing commands
import subprocess
import os
import sys
import platform

MODULE = 'jobberwocky'

# Commands that are specific to your module

COMMANDS = ['jobberwocky:runjob','jobberwocky:posixscript','jobberwocky:windowscript']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "jobberwocky:posixscript":
		make_posixscript(app,env,args)
		
    if command == "jobberwocky:windowscript":
		make_windowscript(app,env,args)	

    if command == "jobberwocky:runjob":
	    runjob(app,env,args)


# This will be executed before any command (new, run...)
def before(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")


# This will be executed after any command (new, run...)
def after(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "new":
        pass

#################################################################################################################
# Run a Job immediately
#################################################################################################################

def runjob(app, env, args):
    cp = classpath(app, env, args)
    java_cmd = app.java_cmd(["-Dplay.path=" + env["basedir"] ], cp, "play.modules.jobberwocky.RunJob", args)
    try:
        subprocess.call(java_cmd, env=os.environ)
    except OSError:
        print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
        sys.exit(-1)
            
#################################################################################################################
# Generate Java classpath, including the jobberwockymodule jar
#################################################################################################################
            
def classpath(app, env, args):
    separator = class_separator();
    jobberwockymodule = os.path.realpath([module for module in app.modules() if module.endswith('jobberwocky')][0])
    cp_args = app.cp_args() + separator + os.path.normpath(os.path.join(app.path,'tmp', 'classes')) + separator + os.path.normpath(os.path.join(app.path,'precompiled', 'java')) + separator + os.path.join(jobberwockymodule,"lib","play-jobberwocky.jar")
    return cp_args
    
def class_separator():
    if (os.name == "posix"): return(":")
    if (platform.system() == "Windows"): return(";")    
    
#################################################################################################################
# Generate Java command line for executing Play environment
#################################################################################################################    
    
def javacommand(app, env, args):
    cmd = "java -Dplay.id={id} -Dapplication.path=\"{apppath}\" -Dplay.path=\"{playpath}\" -Djava.endorsed.dirs=\"{endorsed}\" -javaagent:\"{playjar}\" -Dfile.encoding=UTF-8"
    cmd = cmd.format(id=env["id"], apppath=app.path, playpath=env["basedir"], endorsed=os.path.join(env["basedir"],"framework","endorsed"), playjar=app.agent_path())
    return cmd

#################################################################################################################
# Write POSIX script
#################################################################################################################        
    
def make_posixscript(app, env, args):
    if (os.name != "posix"):
    	print "~ FAILED: Requires a POSIX platform. This platform is " + os.name
    	return;

    scriptname = args[0].split(".")[-1] + ".sh"
    scriptpath = app.path + "/bin"
    if not os.path.exists(scriptpath): os.makedirs(scriptpath)
    filename = os.path.join(scriptpath,scriptname)
    f = open(os.path.join(scriptpath,scriptname), "w")
    cp = classpath(app, env, args).replace(":",":\\\n");
    script = javacommand(app, env, args) + " -classpath \\\n" + cp + "\\\n play.modules.jobberwocky.RunJob " + args[0] + " \"$@\""
    try:
        f.writelines("#!/bin/bash\n\n")
        f.writelines(script) 
        f.write("\n")
        print "~ The job start up script is written to " + filename
    finally:
        f.close()
        
#################################################################################################################
# Write Windows .bat file
#################################################################################################################        
        
def make_windowscript(app, env, args):
    if (platform.system() != "Windows"):
    	print "~ FAILED: Requires a Windows platform. This platform is " + platform.system()
    	return;
    	
    scriptname = args[0].split(".")[-1] + ".bat"
    scriptpath = app.path + "/bin"
    if not os.path.exists(scriptpath): os.makedirs(scriptpath)
    filename = os.path.join(scriptpath,scriptname)
    f = open(filename, "w")
    cp = classpath(app, env, args).replace(";",";^\n")
    script = javacommand(app, env, args) + " -classpath  ^\n" + cp + " ^\n play.modules.jobberwocky.RunJob " + args[0] + " %*"
    try:
        f.write(script) 
        f.write("\n")
        print "~ The job start bat file written to " + filename
    finally:
        f.close()        
    
            
                    
            
        