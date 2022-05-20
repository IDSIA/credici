#!/usr/bin/python3

import subprocess
import datetime
from datetime import datetime
import sys

from pathlib import Path


#### Parameter experiments

setname = "synthetic/1000/set2"
idx_start = None
idx_end = None
seed = 0
rewrite = False


print(f"args len = {len(sys.argv)}")
if len(sys.argv)>1:
    setname = sys.argv[1]
if len(sys.argv)>3:
    idx_start = int(sys.argv[2])
    idx_end = int(sys.argv[3])

if len(sys.argv)>4:
    seed = int(sys.argv[4])



print("Running experiments.py")
print(setname)



### Global variables
prj_path = Path(str(Path("../../../").resolve()) + "/")
#prj_path = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/"
exp_folder = Path(prj_path, "papers/pgm22/")
code_folder = Path(exp_folder, "code")
output_folder = Path(exp_folder, "results", setname)
#output_folder = Path(exp_folder, "results", "example")

models_folder = Path(exp_folder, "models", setname)


# todo: update if credici version is changed. Note: requires rebuild using: mvn clean compile assembly:single
jar_file = Path(prj_path, "target/credici-0.1.3-jar-with-dependencies.jar")

#todo: set java command depending on location
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"

print(prj_path)
print(exp_folder)
print(output_folder)
print(models_folder)
print(jar_file)
print([idx_start, idx_end] if idx_start is not None and idx_end is not None else [])

#### Auxiliary function for interacting with bash

def gen_exec(cmd, check_return: bool = False, timeout = None):
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



## get in folder
import os


f = os.listdir(output_folder)[0]

PROCESSED = [f[:f.index("_mIter")]+".uai" for f in os.listdir(output_folder) if "_mIter" in f]



def select_model(f):

    if f in PROCESSED and not rewrite:
        return False
    if idx_start is not None and idx_end is not None:
        return any([f.endswith(f"_{i}.uai") for i in range(idx_start, idx_end)])
    return f.endswith(f".uai")

MODELS = [f for f in os.listdir(models_folder) if select_model(f)]

print(f"{len(MODELS)} models to process")

#### Function that interacts with credici


# -m 50 -x 4 -s 0 --debug papers/pgm22/models/synthetic/1000/chain_mk1_maxDist3_nEndo5_k075_3.uai


def run(model, maxiter=300, executions=30, timeout = None):
    args = ""
    args += f"-o {output_folder} "
    args += f"-m {maxiter} "
    args += f"-x {executions} "
    args += f"-s {seed} "
    args += f"{Path(models_folder, model)}"

    javafile = Path(code_folder, "SelectBiasExp.java")

    print(args)
    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    if timeout is not None:
        cmd = f"timeout {timeout} {cmd}"
    print(cmd)
    exec_bash_print(cmd)

i = 1
for m in MODELS:
    print(f"{i}/{len(MODELS)}: {m}")
    run(m, timeout=60*60*2)
    i+=1