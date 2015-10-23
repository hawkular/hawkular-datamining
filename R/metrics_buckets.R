 library(RCurl)
 library(rjson)
 library(fpp)
 
 tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
 metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
 hours = 2
 buckets= 30
 now = Sys.time()
 startTime = (as.integer(now) - 2 * 3600) * 1000
 
#get data
 urlBucket = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=', toString(startTime),'&buckets=', toString(buckets)), collapse='')
 header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
 data = getURL(urlBucket, httpheader=header, httpauth=1)
 
 
 #convert JSON to list of vectors
 json = fromJSON(paste(data, collapse=''))
 #convert data points into data frame
 df <- data.frame(do.call(rbind, json))
  
 times <- lapply(df$start, function(x){
  format(as.POSIXlt(round(x/1000), origin="1970-01-01"), '%H:%M')
  })
 
 #plot
 plot(df$start, df$avg, xlab='time', ylab='non Heap', xaxt='n', type='o', col='red', pch=20)
 axis(1, df$start, times)

 # moving average 
 lines(df$start, ma(df$avg,5), col='blue')
