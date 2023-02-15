#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments

print(sys.argv)



numnodes = int(sys.argv[1])
seed = int(sys.argv[2])
set = "s3"


print("Running generatemodels.py")
print(f"numnodes={numnodes}")
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


prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/journalEM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output_test")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")


jar_file = Path(prj_path, "target/credici.jar")
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


def generateModel(num_nodes, output, data_size=1000,  data_increment=0.5, min_ratio=0.98, reduction=1.0, rewrite=False, seed=0):
    # -n 6 -d 1000 -di 0.5 -mid 3 -mr 0.98 -r 1.0 --seed 1 -rw
    args = ""
    args += f"-n {num_nodes} "
    args += f"-d {data_size} "
    args += f"-di {data_increment} "
    args += f"-mr {min_ratio} "
    args += f"-r {reduction} "
    if rewrite: args += f"-rw "
    args += f"--seed {seed} "
    args += f"--output {output} "
    args += "--debug "

    
    javafile = Path(code_folder, "GenerateModel.java")
    print(javafile)
    runjava(javafile, args_str=args, heap_gbytes=64)




output = f"{model_folder}/synthetic/{set}/"
if not os.path.exists(output):
    os.makedirs(output)

generateModel(numnodes, output, seed=seed)