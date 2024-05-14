#!/usr/bin/python3


import subprocess
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


maxiter, seed = 1,0
maxiter = int(sys.argv[1])
seed = int(sys.argv[2])

modelname = "simple_learner_4Q"


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
exp_folder = Path(prj_path, "papers/causal_flavour/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "learntmodels/")
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
    cmd = f"{java} "
    if heap_gbytes is not None: cmd +=f"-Xmx{heap_gbytes}g "
    cmd += f"-cp {jar_file} {javafile} {args_str}"
    exec_bash_print(cmd)



args = ""
args += f"-m {maxiter} "
args += f"-o {Path(res_folder, f'miter{maxiter}')} "
args += f"--data {Path(data_folder, f'{modelname}_data.csv')} "
args += "-rw "
args += f"-s {seed} "
args += f"{Path(model_folder, f'{modelname}.uai')} "



javafile = Path(code_folder, "LearnCF.java")
print(javafile)
runjava(javafile, args_str=args, heap_gbytes=64)

