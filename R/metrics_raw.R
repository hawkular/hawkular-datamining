 library(RCurl)
 library(rjson)
 
 tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
 metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
 hours <- 1
 now = Sys.time()
 startTime = (as.integer(now) - 2 * 3600) * 1000
 
 # get data
 url = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=1'), collapse='')
 header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
 data = getURL(url, httpheader=header, httpauth=1)
 
 #convert JSON to list of vectors
 json = fromJSON(paste(data, collapse=''))
 #convert data points into data frame
 df <- data.frame(do.call(rbind, json))
  
 times <- lapply(df$timestamp, function(x){
  format(as.POSIXlt(round(x/1000), origin="1970-01-01"), '%H:%M')
  })
 
 #plot
 plot(df$timestamp, df$value, xlab='time', ylab='non Heap', xaxt='n', type='l', col='red', pch=18)
 axis(1, df$timestamp, times)