clear ; close all; clc

rand('twister', sum(100 * clock))
ni = rand(1, 10000) - 0.5;
a = [1 1.75 0.8745];
b = 1;
x = filter(b, a, ni);
x = x(:);


alfa = 0.005;
L = 150;
N = 2;
# rows, columns
w = zeros(N, L);

w(:, 3) = [3, -1];

printf('w = %d\n', size(w));

for n = 3:L-1
	# w(:,1) - first column, 
	# x is vector of two points
	# xp is scalar
	# e is error 
	xp = -w (:, n)'  * x(n-1:-1:n-N);
	e = x(n) - xp;
	w(:, n+1) = w(:, n) - alfa * e * x (n-1: -1: n-N);  	
	
	#plot(filter(w(1, n),  w(2, n), ni));
	#printf("xp= %d\n", size(xp));
	#size(xp)
	 #x(n-1:-1:n-N)
	#-w (:, n)'
	alfa * e * x (n-1: -1: n-N)
	#x(n-1:-1:n-N)
	printf("e = %f\n", xp);
	w(:, n)
	#printf("x =" );
	#x(n-1: -1: n-N)
end

figure(1);
plot(w(1,3:end), 'b'), hold on;
plot(w(2,3:end), 'g'),

xlabel(n);
ylabel('w1, w2');
title(['alfa=' num2str(alfa)]);

x2 = filter(b, [1, w(1, n), w(2, n) ], ni)(:);
rmse = [x, x2];
