#!/bin/bash

for i in $(seq 5 15); do
#for i in $(seq 5 11); do
  sbatch --export=init_seed=100,dagsize=${i} -n 100 generatemodels.sbs
done
