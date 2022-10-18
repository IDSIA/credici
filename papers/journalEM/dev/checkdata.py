import pandas as pd

from pathlib import Path

wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici"
dataset = "papers/journalEM/data/triangolo_causal.csv"

data = pd.read_csv(Path(wdir,dataset), index_col=0)


varnames = ["Death", "Symptoms", "PPreference", "FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"]


idx = {v:str(varnames.index(v)) for v in ("Karnofsky", "Symptoms")}

X, Y = idx.values()


info = data.groupby([X,Y]).size().reset_index(name='counts')

info["prob"] = info["counts"]/len(data)


print(idx)
print(info)