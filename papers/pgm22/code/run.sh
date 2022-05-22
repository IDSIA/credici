#!/bin/bash
#

nohup python -u ./selectbiasexp.py synthetic/1000/set2 0 2 > output0.out 2>&1 &
nohup python -u ./selectbiasexp.py synthetic/1000/set2 2 6 > output1.out 2>&1 &
nohup python -u ./selectbiasexp.py synthetic/1000/set2 6 8 > output2.out 2>&1 &
nohup python -u ./selectbiasexp.py synthetic/1000/set2 8 11 > output3.out 2>&1 &