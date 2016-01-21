library(RCurl)
library(rjson)

getBuckets <- function(metricId = paste(c('MI~R~%5B', getFeedId(), '%2FLocal~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used')), hours = 10, buckets = 100, tenant = '28026b36-8fe4-4332-84c8-524e173a68bf') {
  now <- Sys.time()
  startTime <- (as.integer(now) - hours * 3600) * 1000
  
  url = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=', toString(startTime),'&buckets=', toString(buckets)), collapse='')
  header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
  data <- getURL(url, httpheader=header, httpauth=1)
  
  df <- do.call('parseResponse', list(data));
  
  dfWithoutEmty <- df[df$empty == FALSE,]
  
  return(dfWithoutEmty);
}

getPoints <- function(metricId = paste(c('MI~R~%5B', getFeedId(), '%2FLocal~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used')), tenant = '28026b36-8fe4-4332-84c8-524e173a68bf') {
  now <- Sys.time()
  startTime <- (as.integer(now) - 72*3600) * 1000
  
  url = paste(c('http://jdoe:password@localhost:8080/hawkular/metrics/gauges/', metricId, '/data', '?start=', toString(startTime)), collapse='')
  header = c(Accept='application/json', 'Hawkular-Tenant'= tenant)
  data <- getURL(url, httpheader=header, httpauth=1)
 
  df <- parseResponse(data);
  
  return(df);
}

getFeedId <- function() {
  url = paste(c('http://jdoe:password@localhost:8080/hawkular/inventory/feeds'), collapse='')
  header = c(Accept='application/json')
  data = getURL(url, httpheader=header, httpauth=1)

  json = fromJSON(paste(data, collapse=''))
  df <- data.frame(do.call(rbind, json))
  
  return(df$id)
}

parseResponse <- function(data='') {
 #convert JSON to list of vectors
  json <- fromJSON(paste(data, collapse=''))
  
  #convert data points into data frame
  df <- data.frame(do.call(rbind, json))
  
  times <- lapply(df$start, function(x){
    format(as.POSIXlt(round(x), origin="1970-01-01"), '%H:%M')
  })
  
  return(df);
}