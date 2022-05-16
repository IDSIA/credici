#!/bin/bash
#

nohup ./selectbiasexp.py synthetic/1000/set1 0 5 > output0.out 2>&1 &
nohup ./selectbiasexp.py synthetic/1000/set1 5 11 > output1.out 2>&1 &