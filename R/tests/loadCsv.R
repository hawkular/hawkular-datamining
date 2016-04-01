# load time series from CSV file

loadCsv <- function(fileName) {

  df <- read.csv(paste(fileName))
  ts <- as.numeric(df$x)
  
  return(ts)
}
