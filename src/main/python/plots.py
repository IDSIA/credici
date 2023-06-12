def boxplots_cols(data, columns):
    import pandas as pd

    df = pd.DataFrame()
    for col in columns:
        vals = data[col]
        lout, bmin, q25, q50, q75, bmax, uout = boxplot(vals)

        new_row = pd.DataFrame({
            "col":col,
            "outliers_low": [lout],
            "min" : bmin, 
            "q25": q25, 
            "q50": q50, 
            "q75": q75,
            "max": bmax, 
            "outliers_high": [uout]
        })
        df = pd.concat([df, new_row], ignore_index=True)
    return df
            


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