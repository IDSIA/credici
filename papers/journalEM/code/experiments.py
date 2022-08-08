#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments





print(sys.argv)

modelname = "triangolo" # party triangolo
#sizes = len(sys.argv)>1: [int(sys.argv[i]) for i in range(1,len(sys.argv))]
sizes = [1000]
resampling = False
run_em = True

i, j = int(sys.argv[1]), int(sys.argv[2])
if j<i: i,j = j,i 
EM_SEEDS = list(range(i,j))


print("Running experiments.py")
print(modelname)
print(sizes)
print(f"resampling={resampling}")
print(f"runEM={run_em}")
print(EM_SEEDS)

####

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




prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/journalEM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")


jar_file = Path(prj_path, "target/credici-0.1.3-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"


print(prj_path)
print(exp_folder)
print(res_folder)
print(model_folder)

print(jar_file)


def runEM(model, datafile, output, seed=0, maxiter=200):
    args = ""
    args += f"--maxiter {maxiter} " 
    args += f"-w "
    args += f"--seed {seed} "
    args += f"--output {output} "
    args += f"-d {datafile} "
    args += f"{model} "
    
    javafile = Path(code_folder, "RunSingleEM.java")

    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd) 

def sampler(model, datafile, datasize=500, seed=0):

    args = ""
    args += f"-d {datasize} "
    args += f"--seed {seed} "
    args += f"-o {datafile} "
    args += f"{model}"

    javafile = Path(code_folder, "Sampler.java")


    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd) 
    
    
# Check files
model_causal = f"{model_folder}/{modelname}_causal.uai"
model_empirical = f"{model_folder}/{modelname}_empirical.uai"

if(not os.path.isfile(model_causal) or not os.path.isfile(model_empirical)):
    raise ValueError("Model not found")
    
    
# Data sampling

if resampling:
    for datasize in sizes:
        datafile = f"{data_folder}/{modelname}_data_d{datasize}.csv"
        print(datafile)
        sampler(model_empirical, datafile, datasize)
    
# EM
if run_em:
    for s in EM_SEEDS:
        for datasize in sizes:
            output = f"{res_folder}/{modelname}/{datasize}"
            if not os.path.exists(output):
                os.makedirs(output)

            datafile = f"{data_folder}/{modelname}_data_d{datasize}.csv"
            runEM(model_causal, datafile, output, seed=s, maxiter=200)