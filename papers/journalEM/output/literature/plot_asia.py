

import pandas as pd
import os
from pathlib import Path


wdir = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")


res_folder = Path(wdir, "./papers/journalEM/output/literature/")



files = [Path(res_folder, f) for f in os.listdir(res_folder) if f.endswith(".csv")]


data = pd.concat([pd.read_csv(f) for f in files])


cause = "tub"
method = "EMCC"
r = 200

def get_interval(cause,method,r=200):
    cols = [f"pns_{i}" for i in range(0,r if method=="EMCC" else 2)]
    pns_values = data.loc[(data.cause==cause) & (data.method==method)][cols]
    if len(pns_values)>0:
        return round(pns_values.min(axis=1)[0],3), round(pns_values.max(axis=1)[0],3)
    return None,None


methods = data.method.unique()
causes = data.cause.unique()


varnames = dict(bronc="Bronchitis", lung="Lung Cancer", tub="Tuberculosis", smoke="Smoker", asia="Asia")
causes = ["bronc", "lung", "tub", "smoke", "asia"]
methods = ["CCVE", "CCALP", "EMCC"]


tablestr = ""
for c in causes:
    tablestr += f"{varnames[c]}\t\t"
    for m in methods:
        l,u = get_interval(c,m)
        tablestr +="\t\t& "
        if None not in (l,u):
            tablestr += f"$[{l},{u}]$"
        else:
            tablestr += " - "
    tablestr += "\\\\ \n"

print(tablestr)

data = data.reset_index(drop=True)
t = data.iloc[3]

def filter_pns_bounds(t):
    if t.method=="EMCC":
        max_llk = t["max_llk"]
        pns = [t[k.replace("llk","pns")] for k in t.keys() if k.startswith("llk") and max_llk/t[k]>0.99999]
        t.pns_l, t.pns_u = min(pns),max(pns)
    return t

data =  data.apply(filter_pns_bounds, axis=1)

df = data[["cause", "method", "pns_l", "pns_u"]].sort_values(by="cause")

df.loc[df.method=="CCALP"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)

df.loc[df.method=="CCVE"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)
df.loc[df.method=="EMCC"][["cause","pns_l", "pns_u"]].sort_values(by=["pns_l","pns_u"], ascending=False)




data.columns

data[[f"llk_{i}" for i in range(200)]].max().max()

data[[f"llk_{i}" for i in range(200)]].min().min()








data



'''
Bronchitis   & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ &  \\
Lung Cancer  & -         & $[xx,xx]$ & $[xx,xx]$ &  \\
Tuberculosis & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ &  \\
Smoker       & -         & $[xx,xx]$ & $[xx,xx]$ &  \\
Asia         & $[xx,xx]$ & $[xx,xx]$ & $[xx,xx]$ & 
'''