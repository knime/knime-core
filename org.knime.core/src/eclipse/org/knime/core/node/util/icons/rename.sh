#!/bin/bash
# append -icon
rename 's/\.svg/-icon.svg/' *.svg
rename 's/\.ai/-icon.ai/' *.ai
# remove 1_
rename 's/1\_//' *.ai
