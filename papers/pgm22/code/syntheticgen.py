#!/usr/bin/python3

import subprocess
import datetime
from datetime import datetime
import sys

from pathlib import Path


#### Parameter experiments

print(sys.argv)
setname = "synthetic/1000"

if len(sys.argv) > 1:
    i, j = int(sys.argv[1]), int(sys.argv[2])
    if j<i: i,j = j,i
    SEEDS = list(range(i,j))
else:
    SEEDS = [0]

print("Running experiments.py")
print(setname)
print(SEEDS)


### Global variables
prj_path = Path(str(Path("../../../").resolve()) + "/")
exp_folder = Path(prj_path, "papers/pgm22/")
code_folder = Path(exp_folder, "code")
output_folder = Path(exp_folder, "models", setname)

# todo: update if credici version is changed. Note: requires rebuild using: mvn clean compile assembly:single
jar_file = Path(prj_path, "target/credici-0.1.3-jar-with-dependencies.jar")

#todo: set java command depending on location
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"

print(prj_path)
print(exp_folder)
print(output_folder)
print(jar_file)

#### Auxiliary function for interacting with bash

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


#### Function that interacts with credici

def generate(topology, nEndo, markovian=True, datasize=1000, maxdist=3, reduction=1.0, query=True, timeout=30, seed = None):
    args = ""
    args += f"-o {output_folder} "
    args += f"-n {nEndo} "
    args += f"-d {datasize} "
    args += f"-m {maxdist} "
    args += f"--mk {0 if markovian else 1} "
    args += f"-r {reduction} "
    args += f"-t {timeout} "

    if seed != None: args += f"-s {seed} "
    if query: args += f"--query "

    args += f"{topology} "

    javafile = Path(code_folder, "ModelDataGenerator.java")

    print(args)
    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd) 



## main code ##

#generate("chain", nEndo, markovian=markovian, reduction=reduction, seed=seed)
#args = {'nEndo': 7, 'markovian': False, 'reduction': 1.0, 'seed': 1}
#generate("chain", **args)
#print("finished")
#exit()
for seed in SEEDS:
    for nEndo in [5,7,10]:
        for reduction in [0.5, 0.75, 1.0]:
            for markovian in [False, True]:
                args = dict(nEndo=nEndo, markovian=markovian, reduction=reduction, seed=seed)
                print(f"args = {args}")
                generate("chain", **args)
                print("model generated")
print("finished")