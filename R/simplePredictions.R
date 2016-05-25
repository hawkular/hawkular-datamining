library(fpp)
library(xts)
library(fUnitRoots)
library(lsmeans)

setwd('/home/pavol/projects/hawkular/hawkular-datamining/R')
source('hawkularClient.R')

df <- getBuckets()
ts = ts(unlist(df$avg), start=unlist(df$start[1]) / 1000)
horizont <- 8
col = c('red', 'blue', 'green', 'orange')

#Simple predictions
mean = meanf(ts, h=horizont)
naive = naive(ts, h=horizont)
drift = rwf(ts, drift=TRUE, h=horizont)
plot(mean, plot.conf=FALSE, main="Simple forecasts", col='black', fcol=col[1], flwd=2)
lines(naive$mean, col=col[2], lwd=2)
lines(drift$mean, col=col[3], lwd=2)
legend('topleft', lty=1, col=col, legend=c('Mean method','Naive method', 'Drift method'))
dev.new()
accuracy(mean)
accuracy(naive)
accuracy(drift)

#Moving Averages smoothing - estimating the trend-cycle of past values.
movingAverageSmoothing5 = ma(df$avg, 5)
movingAverageSmoothing10 = ma(df$avg, 10)

# ts linear regression
p = getPoints()
df = data.frame('timestamp' = as.numeric(p$timestamp), 'value'= as.numeric(p$value))
t <- ts(df)
reg <- tslm(t ~ trend)

#Linear regression
x = as.numeric(df$start)
y = as.numeric(df$avg)
reg = lm(y ~ x)
summary(regression)
reg_fore = forecast(regression, newdata=data.frame(x=c(as.integer(Sys.time()) * 1000)))
 
# stationarity test, null hiposthesis is that series is non-stationarity => if p-value is high series is non-stationary
#adfTest(as.numeric(ts), lags=0, type='nc')
 
