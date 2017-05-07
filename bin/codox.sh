#!/bin/bash

set -e

if [[ -z $1 ]]; then
    echo "Expected a valid version to generate documentation for, got '$1'!"
    exit 1;
fi

rm -rf ./doc

git checkout $1
lein with-profile -user codox
cp -r ./target/doc ./docs

git checkout master
git add .
git commit -am "docs: updated for v.$1"
