# Decompose time seris 
library(RCurl)
library(rjson)
library(fpp)
library(xts)
library(fUnitRoots)
library(gamlss)
library(forecast)

tenant = '28026b36-8fe4-4332-84c8-524e173a68bf'
metricId = 'MI~R~%5Bdhcp130-144~Local~~%5D~MT~WildFly%20Memory%20Metrics~Heap%20Used'
 
setwd('/home/pavol/projects/hawkular/hawkular-datamining/R')
source('getBuckets')

df <- getBuckets(hours=3, buckets=300)
ts = ts(unlist(df$avg), start=unlist(df$start[1]) / 1000)


#AR prediction
ar = ar(as.numeric(df$avg))
pred = predict(ar, n.ahead=15)
plot(forecast(ar))
plot(df$start, df$avg, col='red', type='l', fcol='green', flty='l')


