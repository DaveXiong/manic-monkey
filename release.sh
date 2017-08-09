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

export RELEASE_PROJECT=$PROJECT

echo ${RELEASE_PROJECT}

export RELEASE_VERSION_PREFIX='WFE-'
export RELEASE_VERSION=$TAG
export RELEASE_URL="https://github.com/iixlabs/"$PROJECT"/releases/tag/"$TAG;

jira-release-notes

git push

git stash pop

fi
