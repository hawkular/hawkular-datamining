#!/bin/bash

# Dark Lord xpavlov@fi.muni.cz

VERSION="1.3"

if [ $# -le 0 ]; then
	echo "usage: install.sh version [texdir]";
	echo "example: install.sh 0.2.9";
	echo "example: install.sh 0.2.9 /opt/texlive";
	exit 1;
fi

TMPDIR=$PWD/fithesis-$1
TEXMFDIR=texmf
TEXDIR=`which tex | sed "s/\/bin.*\/tex$//"`

if [ ! -d $TEXDIR/$TEXMFDIR ]; then
	TEXDIR=`echo $TEXDIR | sed "s/\/$TEXMFDIR$//"`;
fi

echo "fithesis installer version: $VERSION"
echo ""

if [ ! $2 ]; then
	if [ -d $TEXDIR/$TEXMFDIR ]; then
		echo "TeX directory found in $TEXDIR";
		echo "is it correct? [Y/n]";
		read ANW;
		if [ ! $ANW ]; then ANW=y; fi
		if [ $ANW = "n" ]; then
			echo "select your TeX directory:"
			read TEXDIR;
		fi
	else
		echo "select your TeX directory:"
		read TEXDIR;
	fi
else 
	TEXDIR=$2;
fi

echo "checking TeX directory: $TEXDIR"
if [ ! -d $TEXDIR/$TEXMFDIR ]; then
	echo "directory $TEXDIR failed! ... exiting";
	exit 1;
fi

echo "untarring fithesis distribution"
tar zxvf fithesis-$1.tar.gz 1>/dev/null

echo "creating fithesis directories"
if [ ! -d $TEXDIR/$TEXMFDIR/fonts ]; then mkdir $TEXDIR/$TEXMFDIR/fonts; fi
if [ ! -d $TEXDIR/$TEXMFDIR/fonts/tfm ]; then mkdir $TEXDIR/$TEXMFDIR/fonts/tfm; fi
if [ ! -d $TEXDIR/$TEXMFDIR/fonts/tfm/fi-logo ]; then mkdir $TEXDIR/$TEXMFDIR/fonts/tfm/fi-logo; fi

if [ ! -d $TEXDIR/$TEXMFDIR/fonts/pk ]; then mkdir $TEXDIR/$TEXMFDIR/fonts/pk; fi
if [ ! -d $TEXDIR/$TEXMFDIR/fonts/pk/fi-logo ]; then mkdir $TEXDIR/$TEXMFDIR/fonts/pk/fi-logo; fi

if [ ! -d $TEXDIR/$TEXMFDIR/tex ]; then mkdir $TEXDIR/$TEXMFDIR/tex; fi
if [ ! -d $TEXDIR/$TEXMFDIR/tex/fi-logo ]; then mkdir $TEXDIR/$TEXMFDIR/tex/fi-logo; fi

if [ ! -d $TEXDIR/$TEXMFDIR/tex/latex ]; then mkdir $TEXDIR/$TEXMFDIR/tex/latex; fi
if [ ! -d $TEXDIR/$TEXMFDIR/tex/latex/fithesis ]; then mkdir $TEXDIR/$TEXMFDIR/tex/latex/fithesis; fi

if [ -f $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fithesis.cls ]; then rm $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fithesis.cls; fi
if [ -f $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit10.clo ]; then rm $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit10.clo; fi
if [ -f $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit11.clo ]; then rm $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit11.clo; fi
if [ -f $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit12.clo ]; then rm $TEXDIR/$TEXMFDIR/tex/latex/fithesis/fit12.clo; fi

echo "copying fithesis files"
cp $TMPDIR/loga/fi-logo.tfm $TEXDIR/$TEXMFDIR/fonts/tfm/fi-logo
cp $TMPDIR/loga/fi-logo.514pk $TEXDIR/$TEXMFDIR/fonts/pk/fi-logo
cp $TMPDIR/loga/filogo.sty $TEXDIR/$TEXMFDIR/tex/fi-logo
cp $TMPDIR/loga/filogo.tex $TEXDIR/$TEXMFDIR/tex/fi-logo
cp $TMPDIR/loga/showfilogo.tex $TEXDIR/$TEXMFDIR/tex/fi-logo
cp $TMPDIR/fithesis.dtx $TEXDIR/$TEXMFDIR/tex/latex/fithesis
cp $TMPDIR/fithesis.ins $TEXDIR/$TEXMFDIR/tex/latex/fithesis
cp $TMPDIR/csquot.sty $TEXDIR/$TEXMFDIR/tex/latex/fithesis

echo "installing fithesis"
cd $TEXDIR/$TEXMFDIR/tex/latex/fithesis
yes | tex fithesis.ins 1>/dev/null

echo "installing documentation"
cd $TEXDIR/$TEXMFDIR/tex/latex/fithesis
latex --interaction nonstopmode fithesis.dtx 1>/dev/null
dvips fithesis.dvi 1>/dev/null 2>/dev/null
pdflatex --interaction nonstopmode fithesis.dtx 1>/dev/null
rm fithesis.toc
rm fithesis.log
rm fithesis.aux
rm fithesis.idx
rm missfont.log

echo "regenering texhash"
texhash 1>/dev/null

echo "removing temporary files"
rm -rf $TMPDIR

echo "installation succesful"
