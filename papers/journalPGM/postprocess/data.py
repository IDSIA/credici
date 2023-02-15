import pandas as pd
import glob
import numpy as np


def load_data(folders, filter=""):
    d = pd.DataFrame()
    for folder in np.array([folders]).flatten():
        files = glob.glob(f"{folder}/*{filter}.csv")
        for f in files:
            d1 = pd.read_csv(f)
            d1["file"] = f
            add_info(d1, f)
            d = pd.concat((d, d1))
    
    d = d.copy()
    #d["modelFile"] = d["modelPath"].str.split("/").str[-1]
    return d.reset_index()

def add_info(data, filename:str):
    # random_mc2_n11_mid3_d1000_05_mr098_r10_74_uai_emcc_llratio_th09999_mIter500_wtrue_x100_0

    parts = filename.split("/")[-1].split(".")[0].split("_")

    # model generation info
    data["network_type"] = parts[0]
    data["mc"] = int(parts[1][2:]) #mc2
    data["n"] = int(parts[2][1:]) #n
    data["mid"] = int(parts[3][3:]) # mid
    data["d"] = int(parts[4][1:]) # d

    inftype =  parts[10]
    data["inf"] = inftype # emcc
    
    #inference info
    if inftype == "emcc" : 
        data['crit'] = parts[11]
        data["th"] = float(parts[12][2] + "." + parts[12][3:])
        data["max_iter"] = int(parts[13][len("mIter"):])
        data["r_max"] = int(parts[15][1:])


def postprocess(d) :
    d["modelFile"] = d["modelPath"].str.split("/").str[-1]
    return d[d.inf == "emcc"].merge(d[d.inf=="ccve"][["modelFile", "pns_l", "pns_u"]], how="left", on='modelFile', suffixes=("_emcc", "_ccve"))