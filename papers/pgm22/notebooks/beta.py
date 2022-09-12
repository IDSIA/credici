#%%
import pandas as pd
import numpy as np
import logging
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import dblquad
from functools import partial
import utils
import re
import matplotlib.pyplot as plt
import sys
import os
import warnings
import argparse
from multiprocessing import Pool

def fit_old(row, n, eps=0.00001) :
    samples = row[[f"pns_{c}" for c in range(n)]].values.astype(float)
    pos = row['pns_l_x'] - eps
    scale = row['pns_u_x'] - row['pns_l_x'] + 2 * eps
    params = beta_dist.fit(samples, floc = pos, fscale = scale)
    
    return params[0:2]

def fit_new(rho, eps):
    samples = rho #rho[[f"pns_{c}" for c in range(n)]].values.astype(float)  
    
    L = max(samples) - min(samples)

    pos = min(samples) - eps * L 
    if pos < 0: 
        pos = 0
        print("pos is 0")

    
    scale = max(samples) - min(samples) + 2 * eps * L
    
    if pos + scale > 1:
        scale = 1 - pos
        print("shifting")

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

def p_beta_new(row, n, eps, epsabs=1e-30):
    postfix = ""#f"_{n}_{eps}"
    try:
        with warnings.catch_warnings(record=True) as w:
            rho = row[[f"pns_{c}" for c in range(n)]].values.astype(float)
            k = len(rho)
            a = min(rho)
            b = max(rho) 
            L = b - a
    
            delta = eps * 2 * L


            alpha, beta = fit_new(rho,eps)
            f = partial(_p_beta2, L = L, alpha = alpha, beta = beta, n = k)

            if a - eps * L < 0:
                a = eps*L 
            if b + eps*L > 1: 
                b = 1-eps*L

            num, e1 = dblquad(f, 0, delta / 2, lambda y: 0, lambda y: delta / 2, epsabs=epsabs)
            den, e2 = dblquad(f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y, epsabs=epsabs)

            row[f'p_beta{postfix}'] = num/den
            row[f'num_p_beta{postfix}'] = num
            row[f'den_p_beta{postfix}'] = den
            row[f'enum_p_beta{postfix}'] = e1
            row[f'eden_p_beta{postfix}'] = e2
            row[f'warn_p_beta{postfix}'] = len(w)
            row[f'exception{postfix}']=""
            return row

    except Exception as err:
        print(row['index'],n, eps, "exception: ", err)
        row[f'p_beta{postfix}'] = np.nan
        row[f'num_p_beta{postfix}'] = np.nan
        row[f'den_p_beta{postfix}'] = np.nan
        row[f'enum_p_beta{postfix}'] = np.nan
        row[f'eden_p_beta{postfix}'] = np.nan
        row[f'warn_p_beta{postfix}'] = -1
        row[f'exception{postfix}']=err
        return row


#log(x+y) = log(x) + log(1 + exp(log(y) - log(x)))
def prob_unif_data(data, n, eps):
    a = data[[f'pns_{i}' for i in range(n)]].min(axis=1)
    b = data[[f'pns_{i}' for i in range(n)]].max(axis=1)
    L = b - a
    
    num = (1 + (1 + 2*eps)**(2 - n) - 2 * (1+ eps) ** (2-n)) 
    den = (1 - L ** (n - 2) - (n - 2) * (1 - L) * L ** (n - 2))
    return [num, den, num/den]



def filter_data(data):
    # remove identifiable
    data2 = data[(~data.identifiable) & data.selector]
    return data2.copy()


def process(x, data):
    n,eps = x
    n = int(n)
    print(n, eps)
    data['n']=n
    data['eps'] = eps

    data[f'p_unif_num'], data[f'p_unif_den'], data[f'p_unif']  = prob_unif_data(data, n, eps)

    f = partial(p_beta_new, n=n, eps=eps)
    data  = data.apply(f, axis=1)
    return data

#%% Main

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Beta eval.')
    
    #vals = [0.012189092,	0.000836,	0.000708,	0.0000134,	0.003683158,	0.012313264,	0.014506693,	0.014560816,	0.011660093,	0.014383987,	0.014540713,	0.01095759,	0.000172,	0.012471503,	0.014549885,	0.00358868,	0.003005411,	0.008731469,	0.008432049,	0.014563335,	0.002133413,	0.001672443,	0.006216613,	0.00306785,	0.014485338,	0.002993782,	0.014114919,	0.014218961,	0.014508285,	0.004951977,	0.011576424,	0.009330315,	0.014560351,	0.003345492,	0.014562799,	0.014563212,	0.014558314,	0.011756225,	0.005631821,	0.014563261,	0.006217812,	0.002446918,	0.014516566,	0.014402356,	0.014563356,	0.014439132,	0.0003,	0.014308892,	0.013254674,	0.002987762,	0.014548334,	0.014390936,	0.014498479,	0.009713626,	0.013863024,	0.014563367,	0.014563167,	0.006825383,	0.002103993,	0.014563119,	0.002343343,	0.0000787,	0.014563342,	0.006454483,	0.014561299,	0.009397756,	0.003571348,	0.012654194,	0.002987136,	0.014196857,	0.010354826,	0.014474465,	0.014562533,	0.014521865,	0.008030334,	0.014563182,	0.00895547,	0.01152541,	0.014548613,	0.014541243,	0.014561206,	0.012069181,	0.01220905,	0.011818605,	0.014493132,	0.000219,	0.000000335,	0.002569901,	0.014563287,	0.000919,	0.008538002,	0.010274634,	0.003064117,	0.010341366,	0.014561517,	0.002992252,	0.013987906,	0.014168899,	0.01451498,	0.011577997,	0.001800333,	0.013575334,	0.0000185,	0.014563339,	0.012206023,	0.005304583,	0.014563362,	0.00472693,	0.01151021,	0.012843856,	0.007760848,	0.000815,	0.008404639,	0.0000251,	0.014561052,	0.002087466,	0.011578828,	0.0000755,	0.002740682,	0.013586152,	0.013085444,	0.003189073,	0.014563119,	0.011322467,	0.011576489,	0.011424139,	0.008016134,	0.007007776,	0.011270593,	0.014378912,	0.01441797,	0.007898851,	0.010172003,	0.014511191,	0.01456023,	0.011394382,	0.01456332,	0.002913245,	0.00071,	0.003040186,	0.01420521,	0.010495542,	0.011991869,	0.004481137,	0.014383781,	0.014114903,	0.005636875,	0.011651188,	0.005962354,	0.014563361,	0.014553987,	0.014563364,	0.01445646,	0.01455803,	0.013996998,	0.011214427,	0.003378323,	0.010632521,	0.014563038,	0.013288408,	0.014446417,	0.014563351,	0.011576827,	0.011789202,	0.001081422,	0.012300185,	0.014563367,	0.014207422,	0.014563335,	0.007098768,	0.01179491,	0.014510934,	0.014167271,	0.014563356,	0.011658909,	0.01456313,	0.014540652,	0.014561897,	0.014550003,	0.008498368,	0.0113924,	0.0000000819,	0.013091385,	0.003046782,	0.0000109,	0.002987341,	0.000142,	0.012235321,	0.011576769,	0.0000418,	0.003022424,	0.005446526,	0.0000238,	0.003791199,	0.013473333,	0.014409561,	0.014582059,	0.014459575,	0.014239706,	0.014563217]
    #data = pd.DataFrame([vals], columns=[ f"pns_{i}" for i in range(len(vals))])

    filename = "../results/synthetic/1000/set4_aggregated"
    ns = [30,50]
    epss= [0.02,0.04,0.06,0.08,0.1]
    out = "testdata.csv"

    #%%

    if sys.argv[1] == "-l":
        ns = map(int, sys.argv[2].split(",")) 
        epss = map(float, sys.argv[3].split(",")) 
        at_param = 4
        
    else:
        ns = [ int(sys.argv[1]) ] 
        epss = [ float(sys.argv[2]) ]
        at_param = 3


    data_files = sys.argv[at_param]
    out = sys.argv[at_param + 1]
    filename = sys.argv[at_param]

    #%%

    data = utils.load_data(filename)
    print(data.shape)
    data = utils.merge_exact(data, "exact_data_based")
    data = data[data['index'] == 2]

    #%%
    print(data.shape)
    #%%
#    for n in ns:
#        for eps in epss:
#            data[f'p_unif_num'], data[f'p_unif_den'], data[f'p_unif']  = prob_unif_data(data, n, eps)

#            f = partial(p_beta_new, n=n, eps=eps)
#            data  = data.apply(f, axis=1)

    
    #%% using a pool to parallelize
    from functools import partial
        
    configs = np.transpose(np.meshgrid(ns, epss)).reshape(-1, 2)
    pool = Pool()
    xx = pool.map(partial(process, data=data.copy()), configs)
    pool.close()

    #%% Data save

    data['astar'] = data[[f'pns_{i}' for i in range(80)]].min(axis=1)
    data['bstar'] = data[[f'pns_{i}' for i in range(80)]].max(axis=1)

   # data.to_csv(out)



    #%% Data Post Processing

    n = 10
    eps = 0.02

    def plot_scatter_p_error2(d, n, eps, id,  ax, pmin=-1e300, pmax=1e300,):
        col = f"p_beta_{n}_{eps}"
        d['p'] = d[col] #np.log(d[col])

        mmn = d['astar'] 
        mmx = d['bstar'] 

        mn = d[[f'pns_{i}' for i in range(n)]].min(axis=1)
        mx = d[[f'pns_{i}' for i in range(n)]].max(axis=1)

        d['err'] =  np.sqrt(((mmx - mx) ** 2 + (mmn - mn)**2)/(2*(mmx - mmn))**2) #+   #d["pns_l_exact"]
        #mdr = np.min(d['err'])

        #d['err'] = np.log(d['err'] + mdr)
        #print(d[(d['p'] < 1.0)  & (d['index'] == id) & (d.sample_size == n) & (d.eps == eps)].ps1)
        d[
        #    (d['p'] <= np.log(pmax)) &
        #    (d['p'] >= np.log(pmin)) &
            (d['index'] == id)
        ][['err', "p"]].plot.scatter("p", "err", ax = ax)


    import matplotlib.pyplot as plt
    fig,axs = plt.subplots(len(ns),len(epss), figsize=(14,10))
    r = 0 
    c = 0
    for eps in epss:
        r = 0
        for n in ns:
            print(n, eps)
            plot_scatter_p_error2(data, n, eps, 2, axs[r,c])
            r = r + 1
        c = c+1
    plt.tight_layout()
    # %%
