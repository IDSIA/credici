#!/bin/bash

for i in $(seq 5 11); do
  sbatch --export=init_seed=40,dagsize=${i} -n 60 generatemodels.sbs
done
