#!/bin/sh

# script to aggregate the total RSS memory used by the processes in MB.
# the script includes itself so that adds about 2mb extra in total memory.
# the script is a bit hackish and improvements is welcome.
ps -o rss -o command | grep $1 | awk '{print $1}' | head -n 100 | awk '{sum+=$1/1024}END{print sum}'
