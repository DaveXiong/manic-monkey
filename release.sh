#!/bin/bash

TAG=$1

if [ -z "$1" ] 
then
   echo 'tag is required'
else

git stash
BRANCH=`git rev-parse --symbolic-full-name --abbrev-ref HEAD`;
git pull iixlabs $BRANCH;
git tag -a $TAG -m $TAG
git push iixlabs $BRANCH --tags


PROJECT="${PWD##*/}"

export RELEASE_BRANCH=$BRANCH
export RELEASE_TAG=$TAG
export RELEASE_TAG_PREFIX='MONKEY-'
export RELEASE_PROJECT=$PROJECT
export RELEASE_CHANNEL='#wfe-test'

java -jar toolkit/release-java-LATEST.jar

fi
