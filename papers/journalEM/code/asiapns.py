#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


cause,alg="asia","CCALP"
cause = str(sys.argv[1])
alg = str(sys.argv[2])
heapGB = 64


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

# -w
# -x 100
# -m 500 -sc KL -th 0.00001 -a EMCC -rw --seed 0 --output ./papers/journalEM/output/synthetic/sample_files/ ./papers/journalEM/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai

# -x 5 -m 10 --debug -a CCVE --cause asia -rw -o /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/output/literature --seed 0
# /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/models/literature/


def asiapns(method, cause,
             rewrite = True,
             executions = 300,
             max_iter = 500,
             output = ".", seed = 0, debug=False):


    args = ""
    if rewrite: args += f"-rw "
    if debug: args += f"--debug "

    args += f"-x {executions} "
    args += f"-m {max_iter} "
    args += f"-a {method} "
    args += f"--seed {seed} "
    args += f"--output {output} "
    if cause is not None: args += f"--cause {cause} "
    args += f"{Path(model_folder, './literature/')}"

    javafile = Path(code_folder, "AsiaPNS.java")
    print(javafile)
    runjava(javafile, args_str=args, heap_gbytes=heapGB)

####

asiapns(alg, cause, output=Path(res_folder, "./literature"), debug=True)