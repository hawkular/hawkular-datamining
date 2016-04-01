# Helper functions for exporting data to CSV

DIRECTORY <- 'data/'

export <- function(timeSeries) {
  
  for (ts in timeSeries) {
    exportToCSV(ts)
    exportChart(ts)
    exportModel(ts)
  }
}

# export data frame to csv file
exportToCSV <- function(ts) {
    
    name <- attr(ts, 'name')
    series <- attr(ts, 'ts')
    
    fileName <- fileName(name ,'.csv')
    
    # write to file 
    write.csv(series, file=fileName, quote=FALSE, row.names=FALSE)
    cat(paste('Series ', name, ', exported to ', fileName, '\n'))
}

# export data frame to 
exportModel <- function(ts) {
  name <- attr(ts, 'name')
  model <- attr(ts, 'model')
        
  # add suffix .model and remove white spaces
  fileName <- fileName(name, '.model')
  
  # write to file 
  cat(model, file=fileName)
    
  cat(paste('Model ', name, ', exported to ', fileName, '\n'))
  print(model)
}

exportChart <- function(ts) {
  name <- attr(ts, 'name')
  series <- attr(ts, 'ts')
  
   # add suffix .model and remove white spaces
  fileName <- fileName(name, '.pdf')

  pdf(fileName)
  plot(series, type='l', col='red')
  dev.off()
  
  cat(paste('Chart ', name, ', exported to ', fileName, '\n'))
}

addName <- function(ts, frequency=1) {

  name <- deparse(substitute(ts))
  
  result <- name
  attr(result, 'name') <- name
  attr(result, 'ts') <- ts(ts, frequency=frequency)
  
  result 
}

fileName <- function(name='whiteNoise', extension='.csv') {
    fileName <- paste(DIRECTORY, name, extension)
    fileName <- gsub('[[:blank:]]', '', fileName)
}
