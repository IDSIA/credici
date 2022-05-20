from unittest import case


def load_data(folders):
    import pandas as pd
    import glob
    import numpy as np

    d = pd.DataFrame()
    for folder in np.array([folders]).flatten():
        files = glob.glob(f"{folder}/*.csv")
        for f in files:
            d1 = pd.read_csv(f)
            d1["file"] = f
            add_info(d1, f)
            d = pd.concat((d, d1))


    return d.reset_index()


def add_info(data, filename:str):
    parts = filename.split("/")[-1].split("_")

    data["network_type"] = parts[0]
    col = 2
    if parts[1] == "mk0":
        data["markovianity"] = "markovian" 
        data["max_distance"] = None
    else:
        data["markovianity"] = "quasi-markovian"
        data["max_distance"] = parts[col][len("maxDist"):]
        col += 1

    data["num_endogenous"] = parts[col][len("nEndo"):]
    col += 1

    val = parts[col]
    val = float(val[1] + "." + val[2:])
    data["reduction_k"] = val
    col += 1

    # ignore variante number
    col += 1

    data["max_iter"] = int(parts[col][len("mIter"):])
    col += 1

    # ignore w
    col += 1

    data["s_parents"] = int(parts[col][len("sparents"):])
    col += 1




