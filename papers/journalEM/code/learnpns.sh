#!/bin/bash

sbatch --export=init_id=0,seed=0 -n 20 learnpns.sbs
sbatch --export=init_id=0,seed=20 -n 20 learnpns.sbs
sbatch --export=init_id=0,seed=40 -n 20 learnpns.sbs
sbatch --export=init_id=0,seed=60 -n 20 learnpns.sbs
sbatch --export=init_id=0,seed=80 -n 20 learnpns.sbs