#!/usr/bin/env gnuplot
reset

set title "Regression result"
set grid
set datafile separator ","
# don't show legend
set key reverse Left outside
unset key

set terminal png

set xlabel "Time"
set ylabel "Value"

# hack to calculate max values
set output "a.png"
plot 'in' using 1:2 with linespoints ls 1
MAX_X=GPVAL_X_MAX
MAX_Y=GPVAL_Y_MAX
set xrange [0:MAX_X+2]
set yrange [0:MAX_Y+2]

# line
set style line 1 lc rgb '#0060ad' lt 1 lw 2 pt 1 ps 1.5   # --- blue

set grid ytics lt 0 lw 1 lc rgb "#bbbbbb"
set grid xtics lt 0 lw 1 lc rgb "#bbbbbb"

set output "graph.png"
plot 'in' using 1:2 with linespoints  ls 1, \
      '' using 1:2:(sprintf("(%d,%.1f)",$1, $2)) with labels offset 4, 0 , 0, 0

