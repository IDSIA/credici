#!/bin/bash
## usage: sbatch --export=init_seed=0 -n 50 triangolobiaslearn.sbs
#
#sbatch  --export=init_seed=0 -n 25 triangolobiaslearn.sbs
#sbatch  --export=init_seed=25 -n 25 triangolobiaslearn.sbs
#sbatch  --export=init_seed=50 -n 25 triangolobiaslearn.sbs
#sbatch  --export=init_seed=75 -n 25 triangolobiaslearn.sbs
sbatch  --export=init_seed=100 -n 25 triangolobiaslearn.sbs
sbatch  --export=init_seed=125 -n 25 triangolobiaslearn.sbs
sbatch  --export=init_seed=150 -n 25 triangolobiaslearn.sbs
sbatch  --export=init_seed=175 -n 25 triangolobiaslearn.sbs
