 library(RCurl)
 library(rjson)
 library(fpp)
 library(xts)
 
 tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
 metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
 hours = 1
 buckets= 50
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
  
 times <- lapply(df$start, function(x){
  format(as.POSIXlt(round(x/1000), origin="1970-01-01"), '%H:%M')
  })
 
#Moving Averages smoothing - estimating the trend-cycle of past values.
movingAverageSmoothing5 = ma(df$avg, 5)
movingAverageSmoothing10 = ma(df$avg, 10)

# Exponential smoothing
exponentialSmoothing = ses(df, alpha=0.2, initial="simple", h=3)
 
 
#plot
plot(df$start, df$avg, xlab='time', ylab='non Heap', xaxt='n', type='o', col='red', pch=20)


axis(1, df$start, times)

 # moving average 
lines(df$start, movingAverageSmoothing5, col='blue')
lines(df$start, movingAverageSmoothing10, col='orange')
#lines(df$start, exponentialSmoothing, col='green')


#legend
#legend("topleft", lty=1, col=c(1, "blue","red","green"),  c("data", expression(alpha == 0.2), expression(alpha == 0.6),  expression(alpha == 0.89)), pch=1)

# change timestamp to date
# af[1] = as.POSIXct(as.numeric(as.character(df$start)) / 1000, origin="1970-01-01",tz="GMT")
zoo = zoo(df['avg'], as.POSIXct(as.numeric(as.character(df$start)) / 1000, origin="1970-01-01",tz="GMT"))
series = ts(zoo)

#name of the object
typeof(series)
class(series)
names(df)