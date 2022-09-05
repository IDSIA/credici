from unittest import case


def load_data(folders, filter="", exact=""):
    import pandas as pd
    import glob
    import numpy as np

    d = pd.DataFrame()
    for folder in np.array([folders]).flatten():
        files = glob.glob(f"{folder}/*{filter}.csv")
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



def merge_exact(data, exact_method, eps=0.00001):
    exact = data[data['method'] == exact_method].copy()
    exact["identifiable"] = (exact.pns_u - exact.pns_l).abs() < eps
    
    merged = data.merge(exact[["file", "identifiable", "pns_l", "pns_u"]], on="file")
    
    merged["pns_l_exact"] = merged["pns_l_y"]
    merged["pns_u_exact"] = merged["pns_u_y"]
    merged['idx']= merged['index']
    return merged[(merged.method=="EMCC")]




def boxplots(data, value, by, hue=None):
    import pandas as pd
    import numpy as np 
    
    df = pd.DataFrame()
    for group in data[by].unique():
        sub=data[data[by]==group]

        hues = data[hue].unique() if hue is not None else [None]
        for h in hues:
            dta = sub[sub[hue] == h] if h is not None else sub
            vals = dta[value]
            lout, bmin, q25, q50, q75, bmax, uout = boxplot(vals)

            new_row = pd.DataFrame({
                by:group,
                "outliers_low": [lout],
                "min" : bmin, 
                "q25": q25, 
                "q50": q50, 
                "q75": q75,
                "max": bmax, 
                "outliers_high": [uout]
            })
            if hue is not None:
                new_row[hue] = h

            df = pd.concat([df, new_row], ignore_index=True)
    return df
            
        
def boxplot(data):
    import numpy as np
    data = data[~np.isnan(data)]
    q25, q50, q75 = np.quantile(data, q=[0.25,0.5, 0.75])
    iqr = q75 - q25
    up = q50 + 1.5 * iqr
    lo = q50 - 1.5 * iqr
    bmax = data[data <= up].max()
    bmin = data[data >= lo].min()
    lout = np.array(data[data < bmin])
    uout = np.array(data[data > bmax])
    return (lout, bmin, q25, q50, q75, bmax, uout)