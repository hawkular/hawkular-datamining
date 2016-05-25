# Pavol Loffay, p.loffay@gmail.com
# Makefile for building documentation (master's thesis)

# make - produces thesis.pdf based on template fithesis2
# make new - produces thesis3.pdf based on template fithesis3

NAME=thesis
NAME_FITHESIS3=thesis3
BIB=bibliography

AUXFILES=$(NAME).{pdf,ps,dvi,log,aux,toc,bbl,blg,out} missfont.log
AUXFILES+=$(NAME_FITHESIS3).{pdf,ps,dvi,log,aux,toc,bbl,blg,out,run.xml,idx,bcx,lot,bcf,lof}

all: pdf

pdf: $(NAME).tex $(BIB).bib convert_images
	pdflatex $<
	bibtex $(NAME)
	pdflatex $<
	pdflatex $<

new: $(NAME_FITHESIS3).tex $(BIB).bib convert_images
	pdflatex $<
	bibtex $(NAME_FITHESIS3)
	pdflatex $<
	pdflatex $<

convert_images:
	dia -t eps img/src/*.dia -O img/src
	mogrify -format pdf -- img/src/*.{eps}
	rsvg-convert -f pdf -o img/src/results-simple-mse.pdf img/src/results-simple-mse.svg
	rsvg-convert -f pdf -o img/src/results-simple-r.pdf img/src/results-simple-r.svg
	rsvg-convert -f pdf -o img/src/results-double-mse.pdf img/src/results-double-mse.svg
	rsvg-convert -f pdf -o img/src/results-double-r.pdf img/src/results-double-r.svg
	rsvg-convert -f pdf -o img/src/results-triple-mse.pdf img/src/results-triple-mse.svg
	rsvg-convert -f pdf -o img/src/results-triple-r.pdf img/src/results-triple-r.svg
	rsvg-convert -f pdf -o img/src/results-overall-mse.pdf img/src/results-overall-mse.svg
	rsvg-convert -f pdf -o img/src/results-overall-r.pdf img/src/results-overall-r.svg

clean:
	rm -rf $(AUXFILES) img/src/*.{eps,pdf,png}

.PHONY: pdf new convert_images

