#!/bin/bash
#

nohup ./selectionbiasexp.py synthetic/1000/set1 0 5 > output0.out 2>&1 &
nohup ./selectionbiasexp.py synthetic/1000/set1 5 11 > output0.out 2>&1 &