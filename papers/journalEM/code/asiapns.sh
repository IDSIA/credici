#!/bin/bash


# asia, tub, smoke, bronc, lung
sbatch  --export=cause="asia",method="CCALP" -n 1 asiapns.sbs
sbatch  --export=cause="tub",method="CCALP" -n 1 asiapns.sbs
sbatch  --export=cause="smoke",method="CCALP" -n 1 asiapns.sbs
sbatch  --export=cause="bronc",method="CCALP" -n 1 asiapns.sbs
sbatch  --export=cause="lung",method="CCALP" -n 1 asiapns.sbs

#sbatch  --export=cause="asia",method="CCVE" -n 1 asiapns.sbs
#sbatch  --export=cause="tub",method="CCVE" -n 1 asiapns.sbs
#sbatch  --export=cause="smoke",method="CCVE" -n 1 asiapns.sbs
#sbatch  --export=cause="bronc",method="CCVE" -n 1 asiapns.sbs
#sbatch  --export=cause="lung",method="CCVE" -n 1 asiapns.sbs

#sbatch  --export=cause="asia",method="EMCC" -n 1 asiapns.sbs
#sbatch  --export=cause="tub",method="EMCC" -n 1 asiapns.sbs
#sbatch  --export=cause="smoke",method="EMCC" -n 1 asiapns.sbs
#sbatch  --export=cause="bronc",method="EMCC" -n 1 asiapns.sbs
#sbatch  --export=cause="lung",method="EMCC" -n 1 asiapns.sbs

