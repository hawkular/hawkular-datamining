# Decompose time seris 
library(RCurl)
library(rjson)
library(fpp)
library(xts)

 tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
 metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
  hours = 1
 buckets= 100
 now = Sys.time()
 startTime = (as.integer(now) - 2 * 3600) * 1000
 
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
 
 plot(df$start, df$avg, col='red', type='b', fcol='green', flty='l')
 dev.new()
 plot(avgForecast, plot.conf=TRUE, shaded=TRUE,  shadecols=NULL, col=1, fcol=4,  pi.col=1, pi.lty=2, ylim=NULL, main=NULL, ylab="", xlab="", type="l")
 
 #fit = stl(ts)