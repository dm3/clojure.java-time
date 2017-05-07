#!/bin/bash

set -e

if [[ -z $1 ]]; then
    echo "Expected a valid version to generate documentation for, got '$1'!"
    exit 1;
fi

rm -rf ./doc

lein with-profile -user codox

cp -r ./target/doc ./doc
git add .
git commit -am "docs: updated for v.$1"
