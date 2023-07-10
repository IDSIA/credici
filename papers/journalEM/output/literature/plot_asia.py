

import pandas as pd
import os
from pathlib import Path

pd.set_option('display.max_columns', None)

wdir = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")


res_folder = Path(wdir, "./papers/journalEM/output/literature/")


llk_th = 0.999999
miter = 1000


files = [Path(res_folder, f) for f in os.listdir(res_folder) if f.endswith(".csv")]

files = [f for f in files if any([s in str(f) for s in ["CCVE", "CCALP", f"EMCC_mIter{miter}"]])]



data = pd.concat([pd.read_csv(f) for f in files])


cause = "tub"
method = "EMCC"
r = 200

'''def get_interval(cause,method,r=400):
    cols = [f"pns_{i}" for i in range(0,r if method=="EMCC" else 2)]
    pns_values = data.loc[(data.cause==cause) & (data.method==method)][cols]
    if len(pns_values)>0:
        return round(pns_values.min(axis=1)[0],3), round(pns_values.max(axis=1)[0],3)
    return None,None
'''
def get_interval(cause,method,dec=3):
    cols = [f"pns_l","pns_u"]
    pns_values = data.loc[(data.cause==cause) & (data.method==method)][cols]
    if len(pns_values)>0:
        return round(pns_values.min(axis=1)[0],dec), round(pns_values.max(axis=1)[0],dec)
    return None,None

methods = data.method.unique()
causes = data.cause.unique()


varnames = dict(bronc="Bronchitis", lung="Lung Cancer", tub="Tuberculosis", smoke="Smoker", asia="Asia")
causes = ["bronc", "lung", "tub", "smoke", "asia"]
methods = ["CCVE", "CCALP", "EMCC"]



def filter_pns_bounds(t):
    if t.method=="EMCC":
        max_llk = t["max_llk"]
        pns = [t[k.replace("llk","pns")] for k in t.keys() if k.startswith("llk") and max_llk/t[k]>llk_th]
        print(f"cause: {len(pns)}")
        t.pns_l, t.pns_u = min(pns),max(pns)
    return t
data =  data.apply(filter_pns_bounds, axis=1)


tablestr = ""
for c in causes:
    tablestr += f"{varnames[c]}\t\t"
    for m in methods:
        l,u = get_interval(c,m,dec=2)
        tablestr +="\t\t& "
        if None not in (l,u):
            tablestr += f"$[{l},{u}]$"
        else:
            tablestr += " - "
    tablestr += "\\\\ \n"

print(tablestr)

data = data.reset_index(drop=True)
t = data.iloc[3]





df = data[["cause", "method", "pns_l", "pns_u"]].sort_values(by="cause")


ccalp = df.loc[df.method=="CCALP"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)
ccve = df.loc[df.method=="CCVE"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)
emcc = df.loc[df.method=="EMCC"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)


pd.concat([ccve, emcc], axis=1, keys=["cause"])


merged = ccve.merge(emcc, on ="cause", suffixes=("_ccve", "_emcc"),)


merged["low_incl"] = merged.pns_l_ccve<= merged.pns_l_emcc
merged["upp_incl"] = merged.pns_u_ccve>= merged.pns_u_emcc


merged["low_diff"] = merged.pns_l_emcc - merged.pns_l_ccve
merged["upp_diff"] = merged.pns_u_ccve - merged.pns_u_emcc


#merged.loc[merged.low_diff>0, "low_diff"] = 0
#merged.loc[merged.upp_diff>0, "upp_diff"] = 0


'''
Bronchitis   & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ &  \\
Lung Cancer  & -         & $[xx,xx]$ & $[xx,xx]$ &  \\
Tuberculosis & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ &  \\
Smoker       & -         & $[xx,xx]$ & $[xx,xx]$ &  \\
Asia         & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ & 
'''

print(merged)
data["time"] = data.time_inf + data.time_learn

data[data.method.isin(["CCVE", "CCALP"])]


data[data.method.isin(["CCVE", "CCALP"])].sort_values(by = "time")[["method", "cause", "time"]]