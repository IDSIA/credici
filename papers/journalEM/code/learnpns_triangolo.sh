#!/bin/bash

sbatch --exclusive --export=init_seed=0 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=25 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=50 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=75 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=100 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=125 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=150 -n 25 learnpns_triangolo.sbs
sbatch --exclusive --export=init_seed=175 -n 25 learnpns_triangolo.sbs
