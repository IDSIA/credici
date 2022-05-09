commands
==============

mvn clean compile assembly:single

./experiments.py 0 s1a


cd dev/credici/papers/clear22/notebooks/

nohup ./experiments.py 0 s0 > output0.out 2>&1 &
nohup ./experiments.py 0 s1a > output1a.out 2>&1 &
nohup ./experiments.py 0 s1b > output1b.out 2>&1 &


nohup ./experiments.py 0 s0_10 > output0.out 2>&1 &
nohup ./experiments.py 0 s1a_10 > output1a.out 2>&1 &
nohup ./experiments.py 0 s1b_10 > output1b.out 2>&1 &