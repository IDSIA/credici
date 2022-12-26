#!/bin/bash

sbatch  --export=init_id=100,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=125,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=150,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=175,seed=0 -n 25 learnpns_subset.sbs
