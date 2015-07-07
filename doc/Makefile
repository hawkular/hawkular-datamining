
NAME=thesis
BIB=bibliography
AUXFILES=$(NAME).{pdf,ps,dvi,log,aux,toc,bbl,blg,out} missfont.log


all: pdf

pdf: $(NAME).tex $(BIB).bib
	pdflatex $<
	bibtex $(NAME)
	pdflatex $<
	pdflatex $<

clean:
	rm -rf $(AUXFILES)

