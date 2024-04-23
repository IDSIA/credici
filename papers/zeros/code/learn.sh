#!/bin/bash

sbatch  --export=maxiter=1000,init_seed=0 -n 50 learn.sbs
sbatch  --export=maxiter=1000,init_seed=50 -n 50 learn.sbs
sbatch  --export=maxiter=1000,init_seed=100 -n 50 learn.sbs
sbatch  --export=maxiter=1000,init_seed=150 -n 50 learn.sbs