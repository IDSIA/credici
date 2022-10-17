#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


# id,seed=8,0
id = int(sys.argv[1])
seed = int(sys.argv[2])
modelset = "synthetic/s1/"
modelsetOutput = "synthetic/s1b/"
#TH = [0.0, 0.00000001, 0.0000001, 0.000001, 0.00001, 0.0001, 0.001, 0.01]
TH = [0.0, 0.00000001]
SCRITERIA = ["LLratio", "KL"]



print("Running generatemodels.py")
print(f"id={id}")
print(f"seed={seed}")


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

prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")
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


def runjava(javafile, args_str, heap_gbytes=None):
    print("run java")
    cmd = f"{java} "
    if heap_gbytes is not None: cmd +=f"-Xmx{heap_gbytes}g "
    cmd += f"-cp {jar_file} {javafile} {args_str}"
    print(cmd)
    exec_bash_print(cmd)

## Get models

MODELS = [f for f in os.listdir(Path(model_folder, modelset)) if f.endswith(f"_{id}.uai")]

print(MODELS)
print(f"{len(MODELS)} models")


# -w
# -x 100
# -m 500 -sc KL -th 0.00001 -a EMCC -rw --seed 0 --output ./papers/journalEM/output/synthetic/sample_files/ ./papers/journalEM/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai

def learnpns(method, model, weighted = True, rewrite = False, executions = 100, max_iter = 500, stop_criteria = "KL", th = 0.00001, output = "."):

    if stop_criteria == "LLratio": th = 1 - th

    args = ""
    if weighted: args += f"-w "
    if rewrite: args += f"-rw "
    args += f"-x {executions} "
    args += f"-m {max_iter} "
    args += f"-sc {stop_criteria} "
    args += f"-th {th} "
    args += f"-a {method} "
    args += f"--seed {seed} "
    args += f"--output {output} "
    args += str(model)

    javafile = Path(code_folder, "LearnAndCalculatePNS.java")
    print(javafile)
    runjava(javafile, args_str=args, heap_gbytes=64)

####

for m in MODELS:
    modelpath = Path(model_folder, modelset, m)
    outputpath = Path(res_folder, modelsetOutput)

    #learnpns("CCVE", modelpath, output=outputpath)
    #learnpns("CCALP", modelpath, output=outputpath)


for th in TH:
        for criteria in SCRITERIA:
            learnpns("EMCC", modelpath, stop_criteria=criteria, th=th, output=outputpath)