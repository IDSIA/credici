#!/bin/bash
#

nohup python3 -u ./syntheticgen.py 0 1 > output0.out 2>&1 &
nohup python3 -u ./syntheticgen.py 1 2 > output1.out 2>&1 &
nohup python3 -u ./syntheticgen.py 2 3 > output2.out 2>&1 &
nohup python3 -u ./syntheticgen.py 3 4 > output3.out 2>&1 &
nohup python3 -u ./syntheticgen.py 4 5 > output4.out 2>&1 &
nohup python3 -u ./syntheticgen.py 5 6 > output4.out 2>&1 &
