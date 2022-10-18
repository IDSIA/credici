#!/bin/bash

sbatch --exclusive --export=init_id=0,seed=0 -n 25 learnpns.sbs
sbatch --exclusive --export=init_id=0,seed=25 -n 25 learnpns.sbs
sbatch --exclusive --export=init_id=0,seed=50 -n 25 learnpns.sbs
sbatch --exclusive --export=init_id=0,seed=75 -n 25 learnpns.sbs
