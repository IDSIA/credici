#!/usr/bin/python3
import sys
from pathlib import Path
import os
import shutil


is_interactive = hasattr(sys, 'ps1')
prj_path = Path(".").resolve() if is_interactive else Path(str(Path("../../../").resolve()) + "/")

print(prj_path)


res_folder = Path(prj_path, "papers/pgm22/results/synthetic/1000/")
orig_folder = Path(prj_path, "papers/pgm22/models/synthetic/1000/")
dest_folder = Path(prj_path, "papers/pgm22/repo/")

set_names = ["set4_aggregated", "set5"]


rfiles = [f for f in sum([[str(Path(s, f)) for f in os.listdir(Path(res_folder,s))]  for s in set_names], []) if f.endswith(".csv")]


print("copying models")
for f in rfiles:
    model = (f.split("_mIter")[0]+".uai").replace("_aggregated","")
    model_orig = Path(orig_folder, model).resolve()
    model_dest = Path(dest_folder, "models/synthetic/", model.split("/")[-1]).resolve()
    print(f"{model_orig}->{model_dest}")
    shutil.copyfile(str(model_orig), str(model_dest))



print("copying queries")
for f in rfiles:
    model = (f.split("_mIter")[0]+"_queries.csv").replace("_aggregated","")
    model_orig = Path(orig_folder, model).resolve()
    model_dest = Path(dest_folder, "models/synthetic/", model.split("/")[-1]).resolve()
    print(f"{model_orig}->{model_dest}")
    shutil.copyfile(str(model_orig), str(model_dest))

print("copying datasets")
for f in rfiles:
    data = (f.split("_mIter")[0]+".csv").replace("_aggregated","")
    data_orig = Path(orig_folder, data).resolve()
    data_dest = Path(dest_folder, "models/synthetic/", data.split("/")[-1]).resolve()
    print(f"{data_orig}->{data_dest}")
    shutil.copyfile(str(data_orig), str(data_dest))