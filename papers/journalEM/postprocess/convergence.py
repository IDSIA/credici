import numpy as np
from functools import partial
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import dblquad


def fit_beta(samples, eps):
    ''' Fit a beta distribution on the rescaled data and 
        return the estimated α and β 
    '''
    L = max(samples) - min(samples)

    pos = max(0, min(samples) - eps * L)
    scale = min(L + 2 * eps * L, 1 - pos)
    
    params = beta_dist.fit(samples, floc = pos, fscale = scale)
    return params[0:2]


def _pjoint(x, y, L, alpha, beta, n):
    ''' Helper function for the computation of the convergence probability
    '''
    return (
        (
            ((L + x) ** alpha) *  hyp2f1(alpha, 1 - beta, alpha + 1, (L + x) / (L + x + y)) -
            (x ** alpha) * hyp2f1(alpha, 1 - beta, alpha + 1, x / (L + x + y))
        ) / (
            alpha * ((L + x + y) ** alpha) * beta_func(alpha, beta)  
        )
    ) ** n


def p_eps_convergence(samples, eps, approx = 1e-30):
    """ 
    Evaluation probability that the epsilon reduced true interval is 
    within the interval defined by the provided samples.
    """
    k = len(samples)
    a = min(samples)
    b = max(samples) 
    L = b - a
    
    delta = eps * 2 * L

    if a - eps * L < 0:
        a = eps*L 

    if b + eps*L > 1: 
        b = 1 - eps*L

    alpha, beta = fit_beta(samples, eps)
    f = partial(_pjoint, L = L, alpha = alpha, beta = beta, n = k)

    num, e1 = dblquad(f, 0, delta / 2, lambda y: 0, lambda y: delta / 2, epsabs=approx)
    den, e2 = dblquad(f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y, epsabs=approx)
    
    return num / den, num, den, e1, e2