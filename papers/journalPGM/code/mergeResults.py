#!/usr/bin/python3

''''
 python mergeResults.py 0 10

 where 0.10 is the range of seeds

 nohup ./mergeResults.py 0 10 &
 nohup ./mergeResults.py 0 10 > output01.out 2>&1 &

'''

import subprocess
import datetime
import os
from datetime import datetime


import sys

MODELS = ["triangolo_causal_biassoft_2", "triangolo_causal_biashard_2"]
modelname = "triangolo_causal_biassoft_2" # party triangolo
sizes = 1000


CAUSES = [3,9,7]
#CAUSES = list(range(1,11))


print("Merging precise models to bound causal queries.py")
print(modelname)
print(sizes)

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



from pathlib import Path
prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")

#prj_path = Path(str(Path("../../../../").resolve()) + "/")
exp_folder = Path(prj_path, "papers/journalPGM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output/biased/triangolo")



jar_file = Path(prj_path, "target/credici-0.1.5-dev-SNAPSHOT-jar-with-dependencies.jar")
#java = "/Library/Java/JavaVirtualMachines/openjdk-12.0.1.jdk/Contents/Home/bin/java"
java = "java"


print(prj_path)
print(exp_folder)
print(res_folder)
print(jar_file)


def merge(minsize, maxsize, input, output, cause, effect, seed=0, debug=False, descr=""):
    args = ""
    if debug: args+="--debug "
    args += f"-m {minsize} "
    args += f"-M {maxsize} "
    args += f"--input {input} "
    args += f"--output {output} "
    args += f"--seed {seed} "
    args += f"--descr {descr} "
    args += f"{cause} {effect} "


    javafile = Path(code_folder, "mergeAndRun.java")

    cmd = f"{java} -Xmx30G -cp {jar_file} {javafile} {args}"
    print(cmd)
    exec_bash_print(cmd)


c = CAUSES[2]

varnames = ["Death", "Symptoms", " PPreference", " FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"]

for modelname in MODELS:
    for c in CAUSES:
        io_folder = f"{res_folder}/{modelname}/{sizes}"
        if not os.path.exists(io_folder):
            os.makedirs(io_folder)

        # 0 = Death, 3 = FAwareness, 9 = PAwareness 7 =Triangolo

        merge(200,200, io_folder, io_folder, cause=c, effect=0, debug=False, descr=f"{modelname}_{c}")
