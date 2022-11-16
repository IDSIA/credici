import numpy as np
from functools import partial
from scipy.stats import beta as beta_dist
from scipy.special import hyp2f1
from scipy.special import beta as beta_func
from scipy.integrate import nquad, quad
from scipy.integrate import dblquad as double_integral


def double_integral2(func, a, b, gfun, hfun, args=(), epsabs=1.49e-8, epsrel=1.49e-8):
    ''' 
    custom integral to support larger subdivisions 
    func(y, x) from x = a..b and y = gfun(x)..hfun(x).
    '''
    def temp_ranges(*args):
        return [gfun(args[0]) if callable(gfun) else gfun,
                hfun(args[0]) if callable(hfun) else hfun]

    return nquad(func, [temp_ranges, [a, b]], args=args,
            opts={"epsabs": epsabs, "epsrel": epsrel, 'limit':100}) #, 'limit':100



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


def p_eps_convergence(samples, eps, eps2):
    """ 
    Evaluation probability that the epsilon reduced true interval is 
    within the interval defined by the provided samples.
    """
    
    k = len(samples)
    a = min(samples)
    b = max(samples) 
    L = b - a

    if (a - eps * L < 0) & (b + eps * L > 1): # both bounds are degenerate then we are certain that we have the correct value
        return 1, 1, 1, 0, 0, -1, -1

    if a - eps * L < 0 : # we are sure that a* is within 0 and a
        alpha, beta = beta_dist.fit(samples, floc = 0, fscale = b + L*eps2 )[:2]
        f = lambda y: _pjoint(0, y, L, alpha, beta, k)
        print(1)
        num, e1 = quad(f, 0, eps*L)
        print(2)
        den, e2 = quad(f, 0, 1 - b)

    elif b + eps * L > 1:
        alpha, beta = beta_dist.fit(samples, floc = a - eps2 * L, fscale = 1 - (a - L*eps2))[:2]
        f = lambda x: _pjoint(x, 0, L, alpha, beta, k)
        print(3)
        num, e1 = quad(f, 0, eps*L)
        print(4)
        den, e2 = quad(f, 0, a)

    else:
        alpha, beta = beta_dist.fit(samples, floc = a - eps2 * L, fscale = L + 2*L*eps2)[:2]
        f = lambda x,y: _pjoint(x,y, L, alpha, beta, k)
        print(5)
        num, e1 = double_integral(f, 0, eps*L, lambda y: 0, lambda y: eps*L)
        print(6)
        den, e2 = double_integral(f, 0,  a + (1 - b), lambda y: 0, lambda y: a + (1 - b) - y)
    
    return num / den, num, den, e1, e2, alpha, beta


def p_unif_convergence(data, eps):
    a = min(data)
    b = max(data)
    n = len(data)
    L = b - a
    
    num = (1 + (1 + 2*eps)**(2 - n) - 2 * (1+ eps) ** (2-n)) 
    den = (1 - L ** (n - 2) - (n - 2) * (1 - L) * L ** (n - 2))
    return num/den
