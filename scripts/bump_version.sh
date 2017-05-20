#!/bin/bash

function bump_version {
    filename=$1
    from=$2
    to=$3

    sed -i .bak "s;\(fhofherr/stub-fn\)[[:space:]][[:space:]]*\"$from\";\1 \"$to\";g" $filename
}


FROM_VERSION=$1
TO_VERSION=$2

if [ -z "$FROM_VERSION" ] || [ -z "$TO_VERSION" ]
then
    echo "Usage:"
    echo "    $0 <from-version> <to-version>"
    exit 1
fi


# We have to be invoked from the root of this project
if [ ! -f "$PWD/project.clj" ]
then
    echo "$PWD/project.clj does not exist! Have you invoked $0 from the project root?"
    exit 1
fi


bump_version $PWD/project.clj $FROM_VERSION $TO_VERSION
bump_version $PWD/README.adoc $FROM_VERSION $TO_VERSION
