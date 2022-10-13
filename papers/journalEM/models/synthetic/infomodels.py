#!/usr/bin/python3


import subprocess
import datetime
import os
import sys


from datetime import datetime
from pathlib import Path
import pandas as pd

#### Parameter experiments


print(sys.argv)
folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/models/synthetic/s1/"
folder = sys.argv[1]


info = pd.concat([pd.read_csv(Path(folder, f)) for f in os.listdir(folder) if f.endswith("info.csv")])


print("Markovianity:")
print(info.markovianity.groupby(info.markovianity).count())


info.iloc[0].dag

def get_num_nodes(dag:str):
    return dag.split("]  [")[0].count("  ")+1

info["num_vars"] = info.apply(lambda t: get_num_nodes(t["dag"]), axis=1)
info["num_endo_vars"] = info.apply(lambda t: get_num_nodes(t["endo_dag"]), axis=1)

assert all(info.num_endo_vars + info.num_exo_vars == info.num_vars)



info[["num_vars", "num_endo_vars", "num_exo_vars", "markovianity"]].astype('object').describe()

info.num_vars.max()



list(info.columns)
len(info)
print(info.markovianity.groupby(info.markovianity).count())

print(info.num_vars.groupby(info.num_vars).count())
print(info.num_endo_vars.groupby(info.num_endo_vars).count())