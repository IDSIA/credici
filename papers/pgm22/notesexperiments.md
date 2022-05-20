commands
==============


Generate jar:
cd ~/dev/credici/
mvn clean compile assembly:single

cd ./papers/pgm22/code/


Experiments with defaults (all the models, seed=0, synthetic/1000/set1 folder)

./selectbiasexp.py

Additional arguments are [folder] [initial_idx final_idx] [seed]

./selectbiasexp.py synthetic/1000/set1
./selectbiasexp.py synthetic/1000/set1 0 5
./selectbiasexp.py synthetic/1000/set1 0 5 1234


Run in 2 parallel processes:

nohup ./selectbiasexp.py synthetic/1000/set1 0 5 > output0.out 2>&1 &
nohup ./selectbiasexp.py synthetic/1000/set1 5 11 > output1.out 2>&1 &
