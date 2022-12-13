import numpy as np
from functools import partial
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import nquad, quad
#from scipy.integrate import dblquad as double_integral


def double_integral(func, a, b, gfun, hfun, args=(), epsabs=1.49e-8, epsrel=1.49e-8):
    ''' 
    custom integral to support larger subdivisions 
    func(y, x) from x = a..b and y = gfun(x)..hfun(x).
    '''
    def temp_ranges(*args):
        return [gfun(args[0]) if callable(gfun) else gfun,
                hfun(args[0]) if callable(hfun) else hfun]

    return nquad(func, [temp_ranges, [a, b]], args=args,
            opts={"epsabs": epsabs, "epsrel": epsrel, 'limit':500}) #, 'limit':100



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


def p_eps_convergence(samples, eps, eps2, abs, rel, method="single", n = -1):
    """ 
    Evaluation probability that the epsilon reduced true interval is 
    within the interval defined by the provided samples.
    """
    k = len(samples) if n < 1 else n
    a = min(samples)
    b = max(samples) 
    L = b - a

    if method != "single":
        den1 = np.nan
        den2 = np.nan
        den3 = np.nan
        
        e21 = np.nan
        e22 = np.nan
        e23 = np.nan
    
    if (a - eps * L < 0) & (b + eps * L > 1): # both bounds are degenerate then we are certain that we have the correct value
        if method == "single":
            return 1, 1, 1, 0, 0, -1, -1, ""
        else:
            return 1, 1, 1, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0,""

    if a - eps * L < 0 : # we are sure that a* is within 0 and a
        itype = "y"
        alpha, beta = beta_dist.fit(samples, floc = 0, fscale = b + L*eps2 )[:2]
        f = lambda y: _pjoint(0, y, L, alpha, beta, k)
        num, e1 = quad(f, 0, eps*L, epsabs=abs, epsrel=rel)
        if method != "single":
            den1, e2 = quad(f, eps*L, 1 - b, epsabs=abs, epsrel=rel)
            den = den1 + num
        else:
            den, e2 = quad(f, 0, 1 - b, epsabs=abs, epsrel=rel)

    elif b + eps * L > 1: # we are sure that b* is within b and 1
        itype = "x"
        alpha, beta = beta_dist.fit(samples, floc = a - eps2 * L, fscale = 1 - (a - L*eps2))[:2]
        f = lambda x: _pjoint(x, 0, L, alpha, beta, k)
        num, e1 = quad(f, 0, eps*L, epsabs=abs, epsrel=rel)
        if method != "single":
            den1, e2 = quad(f, eps*L, a, epsabs=abs, epsrel=rel)
            den = den1 + num
        else:
            den, e2 = quad(f, 0, a, epsabs=abs, epsrel=rel)

    else:
        itype="xy"
        alpha, beta = beta_dist.fit(samples, floc = a - eps2 * L, fscale = L + 2*L*eps2)[:2]
        f = lambda x,y: _pjoint(x,y, L, alpha, beta, k)
        num, e1 = double_integral(f, 0, eps*L, lambda y: 0, lambda y: eps*L, epsabs=abs, epsrel=rel)

        if method != "single":
            den1, e21 = double_integral(f, 0, eps*L, lambda y: eps*L, lambda y: a + (1 - b) - y, epsabs=abs, epsrel=rel)
            den2, e22 = double_integral(f, eps*L,  a + (1 - b), lambda y: 0, lambda y: min(eps*L, a + (1 - b) - y), epsabs=abs, epsrel=rel)
            den3, e23 = double_integral(f, eps*L,  a + (1 - b), lambda y: eps*L, lambda y: a + (1 - b) - y, epsabs=abs, epsrel=rel)
            den = den1 + num
            e2 = np.nan
        else:
            den, e2 = double_integral(f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y, epsabs=abs, epsrel=rel)

    res = [ num / den, num, den, e1, e2, alpha, beta ]
    if method != "single": res+=[den1, den2, den3, e21, e22, e23]
    res.append(itype)

    return res


def p_unif_convergence(data, eps):
    a = min(data)
    b = max(data)
    n = len(data)
    L = b - a
    
    num = (1 + (1 + 2*eps)**(2 - n) - 2 * (1+ eps) ** (2-n)) 
    den = (1 - L ** (n - 2) - (n - 2) * (1 - L) * L ** (n - 2))
    return num/den
