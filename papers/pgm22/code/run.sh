#!/bin/bash
#

nohup ./selectbiasexp.py synthetic/1000/set1 0 2 > output0.out 2>&1 &
nohup ./selectbiasexp.py synthetic/1000/set1 2 6 > output1.out 2>&1 &
nohup ./selectbiasexp.py synthetic/1000/set1 6 8 > output3.out 2>&1 &
nohup ./selectbiasexp.py synthetic/1000/set1 8 11 > output4.out 2>&1 &