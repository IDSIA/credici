#!/bin/bash

sbatch --export=init_seed=0 -n 10 learnpns_triangolo.sbs
sbatch --export=init_seed=10 -n 10 learnpns_triangolo.sbs
sbatch --export=init_seed=20 -n 10 learnpns_triangolo.sbs
sbatch --export=init_seed=30 -n 10 learnpns_triangolo.sbs
sbatch --export=init_seed=40 -n 10 learnpns_triangolo.sbs
