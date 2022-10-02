At cluster:

sbatch --export=init_seed=0,dagsize=5 -n 20 generatemodels.sbs
sbatch --export=init_seed=0,dagsize=6 -n 20 generatemodels.sbs
sbatch --export=init_seed=0,dagsize=6 -n 20 generatemodels.sbs
sbatch --export=init_seed=0,dagsize=6 -n 20 generatemodels.sbs
sbatch --export=init_seed=0,dagsize=6 -n 20 generatemodels.sbs
sbatch --export=init_seed=0,dagsize=6 -n 20 generatemodels.sbs

