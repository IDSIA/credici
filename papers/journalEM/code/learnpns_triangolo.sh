#!/bin/bash

sbatch --exclusive --export=init_seed=0 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=25 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=50 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=75 -n 25 learnpns_triangolo.sbs
