#!/bin/bash
#

nohup python3 -u ./selectbiasexp.py synthetic/1000/set4 0 1 > output0.out 2>&1 &
nohup python3 -u ./selectbiasexp.py synthetic/1000/set4 1 2 > output1.out 2>&1 &
nohup python3 -u ./selectbiasexp.py synthetic/1000/set4 2 4 > output2.out 2>&1 &
nohup python3 -u ./selectbiasexp.py synthetic/1000/set4 4 6 > output3.out 2>&1 &