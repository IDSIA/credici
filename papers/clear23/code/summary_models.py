#!/usr/bin/python3


import datetime
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path
import networkx as nx

import pandas as pd

#### Parameter experiments

prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")
prj_path = Path(str(Path("../../../").resolve())+"/")
exp_folder = Path(prj_path, "papers/clear23/")
code_folder = Path(exp_folder, "code")
res_folder = Path(exp_folder, "output")
model_folder = Path(exp_folder, "models")
data_folder = Path(exp_folder, "data")



def get_info_table(folder):

    def get_info(r):
        info_file = pd.read_csv(r)["modelPath"][0]
        info_file = info_file.replace("/mnt/beegfs/rcabanas/credici", str(prj_path))
        info_file = info_file.replace(".uai", "_info.csv")
        return pd.read_csv(info_file)

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


folder = Path(res_folder, "synthetic/s1")
info = get_info_table(folder)
summary = get_summary(info)

print(summary)

info.to_csv(Path(folder,"info.csv"))
summary.to_csv(Path(folder,"info_summary.csv"))


