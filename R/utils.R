library(xts)

# Create time series
flat0 <- xts(rnorm(200), Sys.Date() - 200:1)
flat20 <- xts(rnorm(200), Sys.Date() - 200:1) + 20

trend0 <- flat0 + (row(flat0) * 0.1)
trend20 <- flat0 + (row(flat0) * 0.1) + 20


ndiff <- ndiffs(trend20)
trend20D <- diff(trend20, differences=1)
trend20Orig <- diffinv(trend20D, xi=trend20[1], differences=1)

#trend20D <- na.omit(trend20D)


plot(predict(ar, n.ahead=20)$pred)
dev.new()
plot(forecast(ar(trend20, method='ols'), h=20))

Acf(trend20, type='correlation')

typeof(flat0)
class(flat0)
names(flat0)
str(flat0)