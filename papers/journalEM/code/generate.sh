#!/bin/bash

for i in $(seq 5 11); do
  sbatch --export=init_seed=0,dagsize=${i} -n 20 generatemodels.sbs
done
