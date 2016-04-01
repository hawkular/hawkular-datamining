#find periods of data
#source: http://robjhyndman.com/hyndsight/tscharacteristics/
# version 2.1 http://stats.stackexchange.com/questions/1207/period-detection-of-a-generic-time-series

findFreq <- function(x)
{
    n <- length(x)
    spec <- spec.ar(c(x),plot=FALSE)
    if(max(spec$spec)>10) # Arbitrary threshold chosen by trial and error.
    {
        period <- round(1/spec$freq[which.max(spec$spec)])
        if(period==Inf) # Find next local maximum
        {
            j <- which(diff(spec$spec)>0)
            if(length(j)>0)
            {
                nextmax <- j[1] + which.max(spec$spec[j[1]:500])
                period <- round(1/spec$freq[nextmax])
            }
            else
                period <- 1
        }
    }
    else
        period <- 1
    return(period)
}

findFreq2 <- function(x)
{
  n <- length(x)
  spec <- spec.ar(c(na.contiguous(x)),plot=FALSE)
  if(max(spec$spec)>10) # Arbitrary threshold chosen by trial and error.
  {
    period <- round(1/spec$freq[which.max(spec$spec)])
    if(period==Inf) # Find next local maximum
    {
      j <- which(diff(spec$spec)>0)
      if(length(j)>0)
      {
        nextmax <- j[1] + which.max(spec$spec[j[1]:500])
        if(nextmax <= length(spec$freq))
          period <- round(1/spec$freq[nextmax])
        else
          period <- 1
      }
      else
        period <- 1
    }
  }
  else
    period <- 1
 
  return(period)
}

specAr() <- function (x, n.freq, order = NULL, plot = TRUE, na.action = na.fail, 
    method = "yule-walker", ...) 
{
    if (!is.list(x)) {
        series <- deparse(substitute(x))
        x <- na.action(as.ts(x))
        xfreq <- frequency(x)
        nser <- NCOL(x)
        x <- ar(x, is.null(order), order, na.action = na.action, 
            method = method)
    }
    else {
        cn <- match(c("ar", "var.pred", "order"), names(x))
        if (anyNA(cn)) 
            stop("'x' must be a time series or an ar() fit")
        series <- x$series
        xfreq <- x$frequency
        if (is.array(x$ar)) 
            nser <- dim(x$ar)[2L]
        else nser <- 1
    }
    order <- x$order
    if (missing(n.freq)) 
        n.freq <- 500
    freq <- seq.int(0, 0.5, length.out = n.freq)
    if (nser == 1) {
        coh <- phase <- NULL
        var.p <- as.vector(x$var.pred)
        spec <- if (order >= 1) {
            cs <- outer(freq, 1L:order, function(x, y) cos(2 * 
                pi * x * y)) %*% x$ar
            sn <- outer(freq, 1L:order, function(x, y) sin(2 * 
                pi * x * y)) %*% x$ar
            var.p/(xfreq * ((1 - cs)^2 + sn^2))
        }
        else rep.int(var.p/xfreq, length(freq))
    }
    else .NotYetImplemented()
    spg.out <- list(freq = freq * xfreq, spec = spec, coh = coh, 
        phase = phase, n.used = nrow(x), series = series, method = paste0("AR (", 
            order, ") spectrum "))
    class(spg.out) <- "spec"
    if (plot) {
        plot(spg.out, ci = 0, ...)
        invisible(spg.out)
    }
    else spg.out
}