#!/bin/bash

set -e

if [[ -z $1 ]]; then
    echo "Expected a valid version to generate documentation for, got '$1'!"
    exit 1;
fi

rm -rf target/doc
git clone git@github.com:dm3/clojure.java-time.git target/doc
cd target/doc
git symbolic-ref HEAD refs/heads/gh-pages
rm .git/index
git clean -fdx 
cd -

echo "Running codox on $1 in `pwd`..."
git checkout "$1"
lein codox

cd target/doc
git add .
git commit -am "update docs"
git push -u origin gh-pages
cd -

echo "Switching `pwd` back to master..."
git checkout master
