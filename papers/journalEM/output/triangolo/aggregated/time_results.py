import os
import pandas as pd
from pathlib import Path


prj_path = Path("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/")

exp_folder = Path(prj_path, "papers/journalEM/")
folder = Path(exp_folder, "output/triangolo/aggregated/")


for rf in ["triangolo_causal_uai_emcc_kl_th00_mIter500_wtrue_x1_c3_e0.csv",
           "triangolo_causal_uai_emcc_kl_th00_mIter500_wtrue_x1_c7_e0.csv",
           "triangolo_causal_uai_emcc_kl_th00_mIter500_wtrue_x1_c9_e0.csv"]:
    res_file = Path(folder, rf)
    df = pd.read_csv(res_file)
    t_cols = [c for c in df.columns if c.startswith("time_learn")]
    avg_ms = df[t_cols].mean(axis=1)

    print(rf)
    print(f"EMruns: {len(t_cols)}")
    print(f"Time per EMrun {float(avg_ms/(1000*60))}")

