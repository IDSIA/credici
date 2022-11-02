import os
import pandas as pd
from pathlib import Path


prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")

exp_folder = Path(prj_path, "papers/journalEM/")
source_folder = Path(exp_folder, "output/triangolo/")
target_folder = Path(exp_folder, "output/triangolo/aggregated/")


def get_aggregated_df(c):
    dframes = [pd.read_csv(Path(source_folder, f)) for f in files if f.startswith(c)]
    # rename "iter_max", "time_learn", "time_pns"
    def get_individual_info(df):
        seed = [c for c in df.columns if c.startswith("ratio")][0].split("_")[-1]
        df = df[[f"ll_{seed}", f"iter_{seed}", f"ratio_{seed}", f"pns_{seed}", "iter_max", "time_learn", "time_pns"]]
        df = df.rename(dict(iter_max=f"iter_max_{seed}", time_learn=f"time_learn_{seed}", time_pns=f"time_pns_{seed}"), axis="columns")
        return df

    individual_info = pd.concat([get_individual_info(df) for df in dframes], axis=1)

    shared_cols = ['trueState', 'method', 'modelID',
                   'modelPath', 'cause', 'infoPath', 'threshold',
                   'falseState', 'effect',
                   'stop_criteria', 'datasize']

    return pd.concat([dframes[0][shared_cols], individual_info], axis=1)


files = [f for f in os.listdir(source_folder) if f.endswith(".csv")]
confs = list(set(["_".join(f.split("_")[:-1]) for f in files]))


for c in confs:
    print(c)
    df_agg = get_aggregated_df(c)
    df_agg.to_csv(Path(target_folder, c+".csv"))

