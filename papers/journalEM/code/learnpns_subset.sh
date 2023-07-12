#!/bin/bash

sbatch  --export=init_id=0,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=25,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=50,seed=0 -n 25 learnpns_subset.sbs
sbatch  --export=init_id=75,seed=0 -n 25 learnpns_subset.sbs
