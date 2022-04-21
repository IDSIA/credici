#!/usr/bin/python3


import subprocess
import datetime
import os
import pandas as pd
from datetime import datetime

#### Parameter experiments


import sys


import sys

print(sys.argv)
setname = "synthetic"


if len(sys.argv) > 1:
    i, j = int(sys.argv[1]), int(sys.argv[2])
    if j<i: i,j = j,i
    SEEDS = list(range(i,j))
else:
    SEEDS = [0]

print("Running experiments.py")
print(setname)
print(SEEDS)

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



from pathlib import Path

prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/journalEM/")
code_folder = Path(exp_folder, "code")
output_folder = Path(exp_folder, "models", setname)


jar_file = Path(prj_path, "target/credici-0.1.3-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"


print(prj_path)
print(exp_folder)
print(output_folder)


print(jar_file)
'''
	-n nEndo
	-d datasize
	-m maxDist
	-t twExo
	-r reduction
	chain

'''

def generate(topology, nEndo, markovian=True, datasize=1000, maxdist=3, reduction=1.0, seed = None):
    args = ""
    args += f"-o {output_folder} "
    args += f"-n {nEndo} "
    args += f"-d {datasize} "
    args += f"-m {maxdist} "
    args += f"-t {0 if markovian else 1} "
    args += f"-r {reduction} "

    if seed != None:
        args += f"-s {seed} "


    args += f"{topology} "

    javafile = Path(code_folder, "ModelDataGenerator.java")

    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd) 





for seed in SEEDS:
    for nEndo in [5,7,10]:
        for reduction in [0.5, 0.75, 1.0]:
            for markovian in [False, True]:
                generate("chain", nEndo, markovian=markovian, reduction=reduction, seed=seed)