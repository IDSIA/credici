#!/usr/bin/python3

import subprocess
import datetime
import os

import pandas as pd
from datetime import datetime
import timeout_decorator

import sys


print(f"{sys.argv[1]}{sys.argv[2]}") # 0 set1

######### 
## Modify if needed:
start = int(sys.argv[1])
modelset= str(sys.argv[2])
datasize = 1000
executions = 20
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"
#########

@timeout_decorator.timeout(20*60) # seconds
def gen_exec(cmd, check_return: bool = False):
    popen = subprocess.Popen(cmd, stdout=subprocess.PIPE, universal_newlines=True)
    for stdout_line in iter(popen.stdout.readline, ""):
        yield stdout_line
    popen.stdout.close()
    return_code = popen.wait()
    if return_code and check_return:
        raise subprocess.CalledProcessError(return_code, cmd)


def exec_bash(cmd: str, check_return: bool = False):
    return [s for s in gen_exec(cmd.split(), check_return)]
    
def exec_bash_print(cmd: str, check_return: bool = False):
    for path in gen_exec(cmd.split(), check_return):
        print(path, end="")
        
def strtime():
    return datetime.now().strftime("%y%m%d_%H%M%S")



from pathlib import Path

prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/neurips21/")
res_folder = Path(exp_folder, "output")
model_folder = Path(exp_folder, "models")

jar_file = Path(prj_path, "target/credici-0.1.3-SNAPSHOT-jar-with-dependencies.jar")
javafile = Path(exp_folder, "Experiments.java")



print(prj_path)
print(exp_folder)
print(res_folder)
print(model_folder)

print(jar_file)

def run(model, datasize = 1000, executions=20, logfile=None, output = None, datafile=None, seed = 0, timeout = 600, simpleOutput = False):
    logfile = logfile or Path(res_folder, f"{strtime()}_log.txt")
    output = output or res_folder
    output.mkdir(parents=True, exist_ok=True)
    #print(model)
    modelfile = Path(model_folder, model)
    params = f"--executions {executions} --datasize {datasize} --policy LAST --output {output} "\
        f"--logfile {logfile} --timeout {timeout} -q --seed {seed}"
    
    if datafile is not None:
        params += f"-f {datafile}"
        
    if simpleOutput:
        params += " --simpleOutput"
    
    params += f" {modelfile}"
    
    print(params)
    cmd = f"{java} -cp {jar_file} {javafile} {params}"
    print(cmd)
    output = exec_bash(cmd)  
    print(output)
    exec(output[0])
    return locals()["results"]


####### 

stime = strtime()

# Log file and output folder
logfile = Path(res_folder, f"{stime}_log.txt")
output_folder = Path(res_folder, stime)

# Get the models
models = [Path(modelset, file) for file in os.listdir(Path(model_folder,modelset)) if file.endswith(".uai")]
datafiles = [Path(modelset, file) for file in os.listdir(Path(model_folder,modelset)) if file.endswith(".csv")]

print(logfile)
print(output_folder)
print(len(models))
print(len(datafiles))

##########

blacklist = []
res_dicts = []


for i,m in enumerate(models[start:]):
    print(f"{i+start}/{len(models)}: {m}")
    #print(i not in blacklist)
    if i+start not in blacklist:
        try:            
            kwargs = dict(model = m, datasize=datasize,
                          logfile=logfile,
                          output=output_folder,
                          executions=executions,
                          simpleOutput=True)
            df = Path(str(m).replace(".uai",".csv"))
            
            
            if df in datafiles:
                kwargs["datafile"] = Path(model_folder, df)  
            if int(str(m)[str(m).find("twExo")+5])>1:
                kwargs["executions"] = kwargs["executions"]+10
                kwargs["timeout"] = 1200
                #kwargs["simpleOutput"] = True

            res_dicts.append(run(**kwargs))
        except timeout_decorator.TimeoutError:
            print("timeout")
print("finished")