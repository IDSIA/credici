#!/bin/bash

sbatch  --export=np=1,nzr=1.0,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=1,nzr=0.8,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=1,nzr=0.6,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=1,nzr=0.4,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=1,nzr=0.2,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs

sbatch  --export=np=2,nzr=1.0,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=2,nzr=0.8,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=2,nzr=0.6,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=2,nzr=0.4,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=2,nzr=0.2,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs


sbatch  --export=np=3,nzr=1.0,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=3,nzr=0.8,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=3,nzr=0.6,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=3,nzr=0.4,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs
sbatch  --export=np=3,nzr=0.2,zdr=0.5,ys=3,init_seed=0 -n 200 genn.sbs



#sbatch  --export=np=2,nzr=1.0,zdr=0.5,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.8,zdr=0.5,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.6,zdr=0.5,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.4,zdr=0.5,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.2,zdr=0.5,ys=2,init_seed=0 -n 50 genn.sbs

#sbatch  --export=np=2,nzr=1.0,zdr=1.0,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.8,zdr=1.0,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.6,zdr=1.0,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.4,zdr=1.0,ys=2,init_seed=0 -n 50 genn.sbs
#sbatch  --export=np=2,nzr=0.2,zdr=1.0,ys=2,init_seed=0 -n 50 genn.sbs

#sbatch  --export=np=3,nzr=1.0,zdr=0.5,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.8,zdr=0.5,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.6,zdr=0.5,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.4,zdr=0.5,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.2,zdr=0.5,ys=2,init_seed=0 -n 100 genn.sbs

#sbatch  --export=np=3,nzr=1.0,zdr=1.0,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.8,zdr=1.0,ys=2,init_seed=0 -n 100 genn.sbs
##sbatch  --export=np=3,nzr=0.6,zdr=1.0,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.4,zdr=1.0,ys=2,init_seed=0 -n 100 genn.sbs
#sbatch  --export=np=3,nzr=0.2,zdr=1.0,ys=2,init_seed=0 -n 100 genn.sbs


