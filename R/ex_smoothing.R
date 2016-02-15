library(fpp)
library(xts)
library(forecast)

setwd('/home/pavol/projects/hawkular/hawkular-datamining/R')
source('getBuckets')

df <- getBuckets()
ts = ts(as.numeric(df$avg))

horizon=4

# single exponential smoothing
ex = ses(ts, alpha=0.2, initial='optimal', h=horizon)
exHolt = holt(ts, h=horizont, damped=FALSE, exponential=FALSE)

plot(ex, plot.conf=FALSE, main="Exponential smoothing", col='black', fcol=col[1], flwd=2)
lines(exHolt$mean, col=col[2], lwd=2)
lines(exHoltExp$mean, col=col[3], lwd=2)
legend('topleft', lty=1, col=col, legend=c('Exponential smoothing', 'Holt', 'Holt exp'))
accuracy(ex)
accuracy(exHolt)

