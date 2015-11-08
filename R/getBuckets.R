library(RCurl)
library(rjson)

getBuckets <- function(metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used', hours = 1, buckets = 100, tenant = '28026b36-8fe4-4332-84c8-524e173a68bf') {
  now <- Sys.time()
  startTime <- (as.integer(now) - hours * 3600) * 1000
  
  url = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=', toString(startTime),'&buckets=', toString(buckets)), collapse='')
  header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
  data = getURL(url, httpheader=header, httpauth=1)
  
  #convert JSON to list of vectors
  json = fromJSON(paste(data, collapse=''))
  
  #convert data points into data frame
  df <- data.frame(do.call(rbind, json))
  
  times <- lapply(df$start, function(x){
    format(as.POSIXlt(round(x), origin="1970-01-01"), '%H:%M')
  })
  df$times = times
  
  return(df)
}