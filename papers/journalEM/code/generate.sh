#!/bin/bash

#for i in $(seq 12 15); do
for i in $(seq 5 11); do
  sbatch --export=init_seed=0,dagsize=${i} -n 100 generatemodels.sbs
done
