#!/bin/bash

sbatch  --export=init_id=0,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=10,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=20,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=30,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=40,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=50,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=60,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=70,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=80,seed=0 -n 10 learn_multi_study.sbs
sbatch  --export=init_id=90,seed=0 -n 10 learn_multi_study.sbs


