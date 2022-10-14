import convergence

def _compute_array(row, r, eps) :
    import numpy as np
    import logging 

    rho = [row[f"pns_{i}"] for i in range(r)]
    logging.info(f"running on row {row.modelFile}")
    # num / den, num, den, e1, e2
    try:
        row[f"P_{r}_{eps}"],row[f"num_{r}_{eps}"],row[f"den_{r}_{eps}"],_,_ = convergence.p_eps_convergence(rho, eps)
    except:
        logging.error(f"exception cought {row.modelFile}")
        row[f"P_{r}_{eps}"],row[f"num_{r}_{eps}"],row[f"den_{r}_{eps}"]=(np.nan,np.nan,np.nan)
    return row

def compute(data, r, eps) :
    from functools import partial

    fun = partial(_compute_array, r = r, eps= eps)
    return data.apply(fun, axis=1)

if __name__ == "__main__":
    import argparse
    import logging 
    import glob
    import pandas as pd
    
    args = argparse.ArgumentParser("beta postprocess")
    args.add_argument("--eps","-e", type=float)
    args.add_argument("-r", type=int)
    args.add_argument("--log-level", default=logging.INFO,type=lambda x: getattr(logging, x), help="Configure the logging level.")    
    args.add_argument('-f', action='store_true', help="overwrite output")
    args.add_argument("datafile")
    args.add_argument("outfile")

    ns = args.parse_args()
    logging.basicConfig(level=ns.log_level)
    logger = logging

    files_present = glob.glob(ns.outfile)
    if ns.f or not files_present:
        logger.info(f"saving to {ns.outfile}")
    else:
        logger.error(f"file exists {ns.outfile}")
        exit(-1)

    logger.info("compute")
    r = ns.r
    eps = ns.eps

    logger.info(f"will process {r} {eps}")
    logger.info(f"loading {ns.datafile}")
    d = pd.read_csv(ns.datafile)
    logger.info(f"loaded")
    
    logger.info(f"computing {r} {eps}")
    res = compute(d, r=r, eps=eps)
    
    
    if ns.f or not files_present:
        logger.info(f"saving to {ns.outfile}")
        res.to_csv(ns.outfile)
    else:
        logger.error(f"file exists {ns.outfile}")
        exit(-1)
