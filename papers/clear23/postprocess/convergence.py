import numpy as np
from functools import partial
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import dblquad, nquad, quad



def double_integral(func, a, b, gfun, hfun, args=(), epsabs=1.49e-8, epsrel=1.49e-8):
    ''' 
    custom integral to support larger subdivisions 
    func(y, x) from x = a..b and y = gfun(x)..hfun(x).
    '''
    def temp_ranges(*args):
        return [gfun(args[0]) if callable(gfun) else gfun,
                hfun(args[0]) if callable(hfun) else hfun]

    return nquad(func, [temp_ranges, [a, b]], args=args,
            opts={"epsabs": epsabs, "epsrel": epsrel}) #, 'limit':100



def fit_beta(samples, eps, precision):
    ''' 
    Fit a beta distribution on the rescaled data and 
    return the estimated α and β 
    '''
    L = max(samples) - min(samples)

    pos = max(-precision, min(samples) - eps * L)
    scale = min(L + 2 * eps * L, 1 - pos - precision)
    
    params = beta_dist.fit(samples, floc = pos, fscale = scale)
    return params[0:2]



def _pjoint(x, y, L, alpha, beta, n):
    ''' 
    Helper function for the computation of the convergence probability
    '''
    return (
        (
            ((L + x) ** alpha) *  hyp2f1(alpha, 1 - beta, alpha + 1, (L + x) / (L + x + y)) -
            (x ** alpha) * hyp2f1(alpha, 1 - beta, alpha + 1, x / (L + x + y))
        ) / (
            alpha * ((L + x + y) ** alpha) * beta_func(alpha, beta)  
        )
    ) ** n


def p_eps_convergence(samples, eps):
    """ 
    Evaluation probability that the epsilon reduced true interval is 
    within the interval defined by the provided samples.
    """
    
    k = len(samples)
    a = min(samples)
    b = max(samples) 
    L = b - a
    
    precision = eps *  L * 0.001

    epsL_a = eps * L
    if a - eps * L < 0:
        epsL_a = a 

    epsL_b = eps * L
    if b + eps * L > 1: 
        epsL_b = 1 - b
     
    alpha, beta = fit_beta(samples, eps, precision)

    
    full_f = lambda x, y: _pjoint(x, y, L, alpha, beta, k)

    if (epsL_a < precision) & (epsL_b < precision): # both bounds are degenerate then we are certain that we have the correct value
        return 1, 1, 1, 0, 0

    if epsL_a < precision : # assume a == 0
        f = lambda y: _pjoint(0, y, L, alpha, beta, k)
        num, e1 = quad(f, 0, epsL_b)
        den, e2 = quad(f, 0, 1 - b)

    elif epsL_b < precision : # assume b == 1
        f = lambda x: _pjoint(x, 0, L, alpha, beta, k)
        num, e1 = quad(f, 0, epsL_a)
        den, e2 = quad(f, 0,  a)

    else: 
        #func(y, x) from x = a..b and y = gfun(x)..hfun(x).
        num, e1 = double_integral(full_f, 0, epsL_b, lambda y: 0, lambda y: epsL_a)
        den, e2 = double_integral(full_f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y)
    
    return num / den, num, den, e1, e2


def p_unif_convergence(data, eps):
    a = min(data)
    b = max(data)
    n = len(data)
    L = b - a
    
    num = (1 + (1 + 2*eps)**(2 - n) - 2 * (1+ eps) ** (2-n)) 
    den = (1 - L ** (n - 2) - (n - 2) * (1 - L) * L ** (n - 2))
    return num/den
