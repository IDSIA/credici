#!/bin/bash

#sbatch --export=init_id=0,seed=0 -n 10 learnintegrate.sbs
#sbatch --export=init_id=10,seed=0 -n 20 learnintegrate.sbs
#sbatch --export=init_id=50,seed=0 -n 25 learnintegrate.sbs
#sbatch --export=init_id=75,seed=0 -n 25 learnintegrate.sbs

sbatch --export=init_id=0,seed=0 -n 20 learnintegrate.sbs
sbatch --export=init_id=0,seed=20 -n 20 learnintegrate.sbs
sbatch --export=init_id=0,seed=40 -n 20 learnintegrate.sbs
sbatch --export=init_id=0,seed=60 -n 20 learnintegrate.sbs
sbatch --export=init_id=0,seed=80 -n 20 learnintegrate.sbs
