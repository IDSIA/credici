#!/usr/bin/python3


import subprocess
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)


nzerorate, zdroprate, seed = 0.8,0.8,1234

nzerorate = float(sys.argv[1])
zdroprate= float(sys.argv[2])
seed = int(sys.argv[3])




print(nzerorate, zdroprate, seed)

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
#prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/zeros_ecsqaru25/")
model_folder = Path(exp_folder, "models/sm1/")
code_folder = Path(exp_folder, "code")


jar_file = Path(prj_path, "target/credici-0.1.5-dev-SNAPSHOT-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"




def runjava(javafile, args_str, heap_gbytes=None):
    cmd = f"{java} "
    if heap_gbytes is not None: cmd +=f"-Xmx{heap_gbytes}g "
    cmd += f"-cp {jar_file} {javafile} {args_str}"
    exec_bash_print(cmd)


# -np 2 -nzr 0.2 --output ./papers/zeros/models -rw -s 1234 -ys 3


args = ""
args += f"-nzr {nzerorate} "
args += f"-zdr {zdroprate} "

args += f"-o {model_folder} "
args += "-rw "
args += f"-s {seed} "



javafile = Path(code_folder, "GenerateSM.java")
print(javafile)
runjava(javafile, args_str=args, heap_gbytes=128)

