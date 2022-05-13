commands
==============


Generate jar:

mvn clean compile assembly:single

cd dev/credici/papers/pgm22/code/


Experiments with defaults (all the models, seed=0, synthetic/1000 folder)

./selectionbiasex.py

Additional arguments are [folder] [initial_idx final_idx] [seed]

./selectionbiasex.py synthetic/1000
./selectionbiasex.py synthetic/1000 0 5
./selectionbiasex.py synthetic/1000 0 5 1234


Run in 2 parallel processes:

nohup ./selectionbiasex.py synthetic/1000 0 5 > output0.out 2>&1 &
nohup ./selectionbiasex.py synthetic/1000 5 11 > output0.out 2>&1 &
