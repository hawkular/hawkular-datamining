# Decompose time seris 
library(RCurl)
library(rjson)
library(fpp)
library(xts)

 tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
 metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
 
 hours = 3
 buckets= 100
 now = Sys.time()
 startTime = (as.integer(now) - hours * 3600) * 1000
 
 #get data
 url = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=', toString(startTime),'&buckets=', toString(buckets)), collapse='')
 header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
 data = getURL(url, httpheader=header, httpauth=1)
  
 #convert JSON to list of vectors
 json = fromJSON(paste(data, collapse=''))
 #convert data points into data frame
 df <- data.frame(do.call(rbind, json))
 
 # time seris 
 ts = ts(df$avg, frequency=1)
 
 #mean
 mean = mean(as.numeric(unlist(df[4])))
 avgForecast = meanf(as.numeric(ts))
 naive = naive(as.numeric(ts))
 drift = rwf(as.numeric(ts),drift=TRUE)
 averageSmoothing = ma(ts, order=4)
 exponentialSmoothing = ses(as.numeric(df$avg), initial='simple', h=4)
 exponentialWithTrend_Holt = holt(as.numeric(df$avg), h=20)
 exponentialWithTrend_Holt_exp = holt(as.numeric(df$avg), h=20, expnetial=TRUE)
 #holt_seasonal = hw(ts, seasonal="additive")
 
 #Linear regression
 x = as.numeric(df$start)
 y = as.numeric(df$avg)
 reg = lm(y ~ x)
 summary(regression)
 reg_forecast = forecast(regression, newdata=data.frame(x=c(as.integer(Sys.time()) * 1000)))
 plot(forecast)
 
 #plot(df$start, df$avg, col='red', type='b', fcol='green', flty='l')
 #dev.new()
 #plot(avgForecast, plot.conf=TRUE, shaded=TRUE,  shadecols=NULL, col=1, fcol=4,  pi.col=1, pi.lty=2, ylim=NULL, main=NULL, ylab="", xlab="", type="l")
