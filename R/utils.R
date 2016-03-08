library(xts)

N_SAMPLES = 200;
# Create time series
flat0 <- xts(rnorm(N_SAMPLES), Sys.Date() - N_SAMPLES:1)
flat20 <- xts(rnorm(N_SAMPLES), Sys.Date() - N_SAMPLES:1) + 20

trend0 <- flat0 + (row(flat0) * 0.1)
trend20 <- flat0 + (row(flat0) * 0.1) + 20


ndiff <- ndiffs(trend20)
trend20D <- diff(trend20, differences=1)
trend20Orig <- diffinv(trend20D, xi=trend20[1], differences=1)

#trend20D <- na.omit(trend20D)


#plot(predict(ar, n.ahead=20)$pred)
#dev.new()
#plot(forecast(ar(trend20, method='ols'), h=20))

Acf(trend20, type='correlation')

typeof(flat0)
class(flat0)
names(flat0)
str(flat0)
# function source code
#getAnywhere("predict.ar")

lms <- function(x) {
  alfa <- 0.000000000000000000000000001
  L <- smoothingLength(x) * 0.8
  N <- 2
  # rows, columns
  w = matrix(0, N, L)
  w[, 3] = c(3, -1);
  
  for(n in 4:L-1) {
    # scalar error
    xp =  + (w[,n] %*% as.numeric(x[c(n-1, n-N)]))
    xp = as.numeric(xp)
    e = as.numeric(x[n]) - xp;
    
    print(e)
    
    w[ ,n+1] = w[,n] - alfa*e*as.numeric(x[c(n-1, n-N)])
  }
  
  return(w);
}

w <- lms(trend20)
dim(w)
