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
            d = pd.concat((d, d1))
    return d.reset_index()