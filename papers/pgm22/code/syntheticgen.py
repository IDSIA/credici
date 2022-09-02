#!/usr/bin/python3

import subprocess
import datetime
from datetime import datetime
import sys

from pathlib import Path


#### Parameter experiments

print(sys.argv)
datasize=1000
setname = f"synthetic/{datasize}/set5"
topology = "rand13"
overwrite = False

if len(sys.argv) > 1:
    i, j = int(sys.argv[1]), int(sys.argv[2])
    if j<i: i,j = j,i
    SEEDS = list(range(i,j))
else:
    SEEDS = [0]

print("Running triangololearn.py")
print(setname)
print(SEEDS)


### Global variables
prj_path = Path(str(Path("../../../").resolve()) + "/")
#prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici")
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

def generate(topology, nEndo, markovian=True, datasize=1000, maxdist=2, reduction=1.0, query=True, timeout=120, ptimeout=None, seed = None):
    args = ""
    args += f"-o {output_folder} "
    args += f"-n {nEndo} "
    args += f"-d {datasize} "
    args += f"-m {maxdist} "
    args += f"--mk {0 if markovian else 1} "
    args += f"-r {reduction} "
    args += f"-t {timeout} "
    args += "--debug "

    if seed != None: args += f"-s {seed} "
    if query: args += f"--query "

    args += f"{topology} "

    javafile = Path(code_folder, "ModelDataGenerator.java")

    print(args)
    cmd = f"{java} -Xmx{28*1024}m -cp {jar_file} {javafile} {args}"
    if ptimeout is not None:
        cmd = f"timeout {ptimeout} {cmd}"
    print(cmd)
    exec_bash_print(cmd)

def generated(args):
    filename = args["topology"]
    filename += "_mk0" if args["markovian"] else "_mk1_maxDist3"
    filename += f'_nEndo{args["nEndo"]}'
    filename += f"_k{args['reduction']}".replace(".","")
    filename += f"_{args['seed']}.uai"
    return Path(output_folder,filename).exists()

## main code ##

#generate("chain", nEndo, markovian=markovian, reduction=reduction, seed=seed)
#args = {'nEndo': 7, 'markovian': False, 'reduction': 1.0, 'seed': 1}
#generate("chain", **args)
#print("finished")
#exit()

i = 1

for seed in SEEDS:
    for nEndo in [7,8]:
        for reduction in [0.5, 0.75, 1.0]:
            for markovian in [False, True]:
                args = dict(topology=topology, nEndo=nEndo, markovian=markovian, reduction=reduction, seed=seed, datasize=datasize, ptimeout=40*60)
                #print(f"{i}: args = {args}")
                i += 1
                if overwrite or not generated(args):
                    print(f"{i}: args = {args}")
                    generate(**args)
                    #print("model generated")
                else: print(f"{i}: args = {args} -- generated")

print("finished")



# poly_mk1_maxDist3_nEndo5_k075_1.uai
args


