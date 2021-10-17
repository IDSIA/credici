commands
==============

mvn clean compile assembly:single

./experiments.py 0 s1a


cd dev/credici/papers/clear22/notebooks/

./experiments.py 0 s0 > output0.out 2>&1 &
./experiments.py 0 s1a > output1a.out 2>&1 &
./experiments.py 0 s1b > output1b.out 2>&1 &
