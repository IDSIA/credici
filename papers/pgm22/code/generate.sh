#!/bin/bash
#

nohup python3 -u ./syntheticgen.py 0 2 > output0.out 2>&1 &
nohup python3 -u ./syntheticgen.py 2 5 > output1.out 2>&1 &