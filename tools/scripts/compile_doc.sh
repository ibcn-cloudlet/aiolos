#!/bin/bash

# should be executed from the tools directory

target=generated/docs

rm -r $target
mkdir -p $target

# copy all figures
echo "Converting Markdown documents"

cp -r ../doc/figures $target/figures

# convert all .md files to .html and change links from .md to .html
for file in `ls ../doc/`; 
do
	if echo "$file" | grep -q '.md'; then
		cat ../doc/$file | markdown | sed s/.md/.html/ > $target"/"${file%%.*}".html"
	fi
done

# generate javadoc from api project
echo "Including javadoc from api project"
cd ..
cd be.iminds.aiolos.api
ant javadoc
cp -r generated/javadoc ../tools/$target/javadoc
cd ../tools

# make index.html from README.md, remove doc/ dir in links
echo "Generate index.html"
cat ../README.md | markdown | sed s/.md/.html/ | sed 's/doc\///' > $target"/index.html"

echo "Documentation complete"