#!/bin/bash

sbatch  --export=init_id=0,seed=0 -n 25 learn_multi_study.sbs
sbatch  --export=init_id=25,seed=0 -n 25 learn_multi_study.sbs
sbatch  --export=init_id=50,seed=0 -n 25 learn_multi_study.sbs
sbatch  --export=init_id=75,seed=0 -n 25 learn_multi_study.sbs
