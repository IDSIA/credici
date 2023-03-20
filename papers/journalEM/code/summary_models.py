#!/usr/bin/python3


import datetime
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path
import networkx as nx

import pandas as pd


gen_info = True

#### Parameter experiments

import os

def is_pycharm():
    return os.getenv("PYCHARM_HOSTED") != None
if is_pycharm():
    prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")
else:
    prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/journalEM/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")



def get_info_table(folder):

    def get_info(r):

        model_file = pd.read_csv(r)["modelPath"][0]

        model_file = model_file.replace("/mnt/beegfs/rcabanas/dev/credici", str(prj_path))
        info_file = model_file.replace(".uai", "_info.csv")
        print(info_file)
        df =  pd.read_csv(info_file)
        df["model_file"] = model_file.split("/")[-1]
        return df

    res_files = [Path(folder, f) for f in os.listdir(folder) if f.endswith(".csv") and  "info" not in f]
    info = pd.concat([get_info(r) for r in res_files])


    def get_num_nodes(dag:str):
        return dag.split("]  [")[0].count("  ")+1

    info["num_vars"] = info.apply(lambda t: get_num_nodes(t["dag"]), axis=1)
    info["num_endo_vars"] = info.apply(lambda t: get_num_nodes(t["endo_dag"]), axis=1)

    assert all(info.num_endo_vars + info.num_exo_vars == info.num_vars)


    def get_max_indegree(dag):
        lines = dag.split("]  [")[-1][1:-3].split(")  (")
        nodes = nx.parse_edgelist(lines, nodetype = int)
        dag = nx.DiGraph(nodes)
        return max(dict(dag.in_degree).values())


    info["max_indegree"] = info.apply(lambda t: get_max_indegree(t["dag"]), axis=1)

    info = info.rename(dict(datasize="datasize_obs"),axis=1 )


    return info

def get_summary(info):
    cols = ["num_vars", "num_endo_vars", "num_exo_vars", "avg_exo_card", "max_exo_card", "tw", "avg_endo_indegree", "avg_indegree", "max_indegree", "datasize_obs"]
    summary = info[cols].describe().loc[["min", "max", "mean"]]
    return summary



folder = Path(res_folder, "synthetic/s12")
info_path = Path(folder,"info.csv")


if gen_info:
    info = get_info_table(folder)
    info.to_csv(info_path)
else:
    info = pd.read_csv(info_path)


info.columns
info["model_file"]

info = info.groupby("model_file").mean()

summary = get_summary(info)

print(summary)

summary.to_csv(Path(folder,"info_summary.csv"))
'''
Index(['Unnamed: 0', 'avg_exo_card', 'tw', 'exo_dag', 'avg_indegree', 'dag',
       'num_exo_vars', 'exo_cc', 'endo_dag', 'exo_tw', 'endo_tw', 'endo_cc',
       'avg_endo_indegree', 'datasize_obs', 'max_exo_card', 'markovianity',
       'ratio', 'num_vars', 'num_endo_vars', 'max_indegree'],
      dtype='object')

'''

s = summary[["num_exo_vars","num_endo_vars", "num_vars", "max_exo_card", "tw"]]

## For the latex
print(" minimum &   "+"\t&\t".join([str(v) for v in s.loc["min"].values.astype(int)])+" \\\\\\hline")
print(" maximum &   "+"\t&\t".join([str(v) for v in s.loc["max"].values.astype(int)])+" \\\\\\hline")
print(" average &   "+"\t&\t".join(["{:.2f}".format(round(v, 2)) for v in s.loc["mean"].values])+" \\\\\\hline")

num_models = len(info)
num_qm = len(info[info.markovianity==1])
num_nqm = len(info[info.markovianity==2])

print(f"{num_models} models. QM: {num_qm}. NQM: {num_nqm}")



len(info)