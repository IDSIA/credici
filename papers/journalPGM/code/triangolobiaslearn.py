#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


MODELS = ["triangolo_causal_biassoft_2", "triangolo_causal_biashard_2"]
sizes = [1000]
run_em = True

max_iter, EM_SEEDS, idx_model=[10, [0], 0]
max_iter = int(sys.argv[1])
EM_SEEDS = [int(sys.argv[2])]
idx_model = int(sys.argv[3])

modelname = MODELS[idx_model]

print("Running triangololearn.py")
print(modelname)
print(sizes)
print(f"runEM={run_em}")
print(f"max_iter={max_iter}")
print(f"SEEDS={EM_SEEDS}")


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

prj_path = Path(str(Path("../../../").resolve()) + "/")
exp_folder = Path(prj_path, "papers/journalPGM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output/biased/triangolo/")
model_folder = Path(exp_folder, "models/literature/triangolo")
data_folder = Path(exp_folder, "data")


jar_file = Path(prj_path, "target/credici-0.1.5-dev-SNAPSHOT-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"


print(prj_path)
print(exp_folder)
print(res_folder)
print(model_folder)

print(jar_file)


def runEMbias(model, datafile, output, seed=0, maxiter=200):
    args = ""
    args += f"--maxiter {maxiter} " 
    args += f"-w "
    args += f"--seed {seed} "
    args += f"--output {output} "
    args += f"-d {datafile} "
    args += "--debug "
    args += f"{model} "

    
    javafile = Path(code_folder, "RunSingleEM.java")

    cmd = f"{java} -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd) 



s = EM_SEEDS[0]
datasize = sizes[0]
max_iter=1

# EM
if run_em:
    for s in EM_SEEDS:
        for datasize in sizes:
            output = f"{res_folder}/{modelname}/{datasize}"
            if not os.path.exists(output):
                os.makedirs(output)
            model_causal = f"{model_folder}/{modelname}.uai"

            datafile = f"{data_folder}/triangolo_data_d{datasize}.csv"
            runEMbias(model_causal, datafile, output, seed=s, maxiter=max_iter)