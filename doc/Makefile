# Autor: Pavol Loffay
# Date: 20.2.2015
#
# vlna -v KkSsVvZzOoUuAIai obsah.tex -o obsah.tex
#

NAME=thesis

all: pdf

pdf:
#	latex $(NAME).tex
#	bibtex $(NAME).aux
#	latex $(NAME).tex
#	latex $(NAME).tex
#	dvips -t a4 $(NAME).dvi
#	ps2pdf $(NAME).ps

	pdflatex $(NAME)
	bibtex $(NAME) 
	pdflatex $(NAME)
	pdflatex $(NAME)

clean:
	rm -rf $(NAME).{pdf,ps,dvi,log,aux,toc,bbl,blg} missfont.log

