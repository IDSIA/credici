#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


id,seed,k,N=8,0,1,3
id = int(sys.argv[1])
seed = int(sys.argv[2])
k = int(sys.argv[3])
N = int(sys.argv[4])



modelset = "synthetic/s12/"
#modelset = "triangolo/"
modelsetOutput = modelset
#modelsetOutput = "synthetic/s12/"

filterbyid = True
CAUSE_EFFECT = []
heapGB = 64
TH = [0.0]
#TH = [0.0, 0.00000001]
SCRITERIA = ["KL"]



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
exp_folder = Path(prj_path, "papers/journalPGM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output/hybrid/")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")


jar_file = Path(prj_path, "target/credici-0.1.5-dev-SNAPSHOT-jar-with-dependencies.jar")
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

end_pattern = ".uai"
if filterbyid: end_pattern = f"_{id}"+end_pattern
MODELS = [f for f in os.listdir(Path(model_folder, modelset)) if f.endswith(end_pattern)]

print(MODELS)
print(f"{len(MODELS)} models")

MODELS = [MODELS[i] for i in range(len(MODELS)) if i%N==k]


def learnintegrate(model, weighted = True, rewrite = False,
                   executions = 300, max_iter = 500, stop_criteria = "KL", th = 0.0,
                   target_ps=0.25, localparams=True, output = "."):

    if stop_criteria == "LLratio": th = 1 - th

    args = ""
    if weighted: args += f"-w "
    if rewrite: args += f"-rw "
    if localparams: args += "-lp "
    args += f"-x {executions} "
    args += f"-m {max_iter} "
    args += f"-sc {stop_criteria} "
    args += f"-th {th} "
    args += f"--seed {seed} "
    args += f"--output {output} "
    args += f"-tps {target_ps} "
    args += str(model)

    javafile = Path(code_folder, "LearnMultiStudy.java")
    print(javafile)
    runjava(javafile, args_str=args, heap_gbytes=64)

####

for tps in [0.25, 0.75]:
    for m in MODELS:
        modelpath = Path(model_folder, modelset, m)
        outputpath = Path(res_folder, modelsetOutput)
        learnintegrate(modelpath, output=outputpath, target_ps=tps)

