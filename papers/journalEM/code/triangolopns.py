#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path

#### Parameter experiments


print(sys.argv)


print("Running triangolopns.py")



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
res_folder = Path(exp_folder, "output")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")


jar_file = Path(prj_path, "target/credici-0.1.3-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"
#java = "/Library/Java/JavaVirtualMachines/adoptopenjdk-12.jdk/Contents/Home/bin/java"

print(prj_path)
print(exp_folder)
print(res_folder)
print(model_folder)

print(jar_file)


def calculatePNS(path, cause, effect, description):
    args = ""
    args += f"--cause {cause} "
    args += f"--effect {effect} "
    args += f"--descr {description} "
    args += "--debug "
    args += f"{path} "

    
    javafile = Path(code_folder, "CalculatePNS.java")

    cmd = f"{java} -Xmx128g -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd)


#where Y is "Death" (0) and X is Awareness_Patient (9) or Awareness_Famility (3) or Triangolo (7)
calculatePNS("../output/triangolo/1000/", cause=9, effect=0, description="triangolo_PAwareness")
calculatePNS("../output/triangolo/1000/", cause=3, effect=0, description="triangolo_FAwareness")
calculatePNS("../output/triangolo/1000/", cause=7, effect=0, description="triangolo_Triangolo")

calculatePNS("../output/triangolo_biassoft/1000/", cause=9, effect=0, description="triangolo_biassoft_PAwareness")
calculatePNS("../output/triangolo_biassoft/1000/", cause=3, effect=0, description="triangolo_biassoft_FAwareness")
calculatePNS("../output/triangolo_biassoft/1000/", cause=7, effect=0, description="triangolo_biassoft_Triangolo")

calculatePNS("../output/triangolo_biashard/1000/", cause=9, effect=0, description="triangolo_biashard_PAwareness")
calculatePNS("../output/triangolo_biashard/1000/", cause=3, effect=0, description="triangolo_biashard_FAwareness")
calculatePNS("../output/triangolo_biashard/1000/", cause=7, effect=0, description="triangolo_biashard_Triangolo")