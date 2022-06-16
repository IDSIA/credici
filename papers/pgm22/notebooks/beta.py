#%%
import pandas as pd
import numpy as np
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import dblquad
import utils
import re
from functools import partial
import matplotlib.pyplot as plt
import sys


def fit(row, n, eps=0.00001) :
    samples = row[[f"pns_{c}" for c in range(n)]].values.astype(float)
    
    pos = row['pns_l_x'] - eps
    scale = row['pns_u_x'] - row['pns_l_x'] + 2 * eps
    params = beta_dist.fit(samples, floc = pos, fscale = scale)
    
    return params[0:2]



def _p_beta2(x, y, L, alpha, beta, n):
    return (
        (
            ((L + x) ** alpha) *  hyp2f1(alpha, 1 - beta, alpha + 1, (L + x) / (L + x + y)) -
            (x ** alpha) * hyp2f1(alpha, 1 - beta, alpha + 1, x / (L + x + y))
        ) / (
            alpha * ((L + x + y) ** alpha) * beta_func(alpha, beta)  
        )
    ) ** n
    

def p_beta(row, n, eps = None):
    epsabs=1e-30
    import warnings
    try:
        with warnings.catch_warnings(record=True) as w:
            alpha, beta = fit(row, n)
            a = min(row[[f'pns_{i}' for i in range(n)]])
            b = max(row[[f'pns_{i}' for i in range(n)]]) 
            L = b - a

            delta = eps * 2 * L

            f = partial(_p_beta2, L = L, alpha = alpha, beta = beta, n = n)

            num, e1 = dblquad(f, 0, delta / 2, lambda y: 0, lambda y: delta / 2, epsabs=epsabs)

            #f(y1, x1), x1_, x1^, y1_, y1^

            den, e2 = dblquad(f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y, epsabs=epsabs)

            return [num/den, e1, e2, num, den, len(w)]
    except:
        return [np.nan, np.nan, np.nan, np.nan, np.nan, len(w)]

def prob_unif_data(data, n, eps):
    a = data[[f'pns_{i}' for i in range(n)]].min(axis=1)
    b = data[[f'pns_{i}' for i in range(n)]].max(axis=1)
    L = b - a
    #delta = eps * 2 * L
    return (1 + (1 + 2*eps)**(2 - n) - 2 * (1+ eps) ** (2-n)) / (1 - L ** (n - 2) - (n - 2) * (1 - L) * L ** (n - 2))



def filter_data(data):
    # remove identifiable
    data2 = data[(~data.identifiable) & data.selector]
    return data2.copy()

#%%

n = int(sys.argv[1])
eps = float(sys.argv[2])
data_folder = sys.argv[3]
#n=20
#eps =0.01
#data_folder  = "../results/synthetic/1000/set4_aggregated/"

print(n, eps, data_folder)

input = utils.load_data(data_folder, "_x80_*")
data2 = utils.merge_exact(input, "exact_data_based")

data = filter_data(data2)

#%%
data[f'p_unif_{n}_{eps}'] = prob_unif_data(data, n, eps)

#%%
f = partial(p_beta, n=n, eps=eps)
data[f'p_beta_{n}_{eps}'] = data.apply(f, axis=1)

data.to_csv(f"probs_{n}_{eps}.csv")

#ns = [20, 40, 60, 80]
#eps=[ 0.0075, 0.01, 0.025, 0.05, 0.075 ]
#for n in ns:
#    for e in eps:
#        print(n, e)
#        f = partial(p_beta, n=n, eps=e)
#        data[f'p_beta_{n}_{e}'] = data.apply(f, axis=1)
#        data[f'p_unif_{n}_{e}'] = prob_unif_data(data, n, e)

#data.to_csv(f"proabilitis_20_40_60_80_may_eps.csv")

