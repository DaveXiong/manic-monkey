#!/bin/bash

TAG=$1

if [ -z "$1" ]; then
   echo 'Usage: ./upgrade.sh TAG';
   exit 1;
fi

##########################################
#CONFIG THE FOLLOWING VARIABLES
##########################################
INSTALL_DIR=/usr/share/tomcat/webapps
CONFIG_DIR=/etc/manic
##########################################
#CONFIG THE ABOVE VARIABLES
##########################################

RULE_ENGINE_CONFIG=$INSTALL_DIR/manic/WEB-INF/classes

FETECH="git fetch --tags";
CHECKOUT="git checkout $TAG";
CLEAN="./gradew clean"
COMPILE="./gradlew build -x test"
UPGRADE_1="rm -rf $INSTALL_DIR/manic"
UPGRADE_2="mkdir -p $INSTALL_DIR/manic"
UPGRADE_3="unzip build/libs/*.war $INSTALL_DIR/manic/"

CP_CHAOS="cp $CONFIG_DIR/db.properties $RULE_ENGINE_CONFIG/chaos.properties"
CP_CLIENT="cp $CONFIG_DIR/ldap.properties $RULE_ENGINE_CONFIG/client.properties"
CP_SIMIANARMY="cp $CONFIG_DIR/engine.properties $RULE_ENGINE_CONFIG/simianarmy.properties"
TOMCAT_RESTART="service tomcat restart"
TOMCAT_LOG="journalctl -flu tomcat"

$FETECH;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $FETECH ";
   exit 1;
else
   echo "[OK] $FETECH ";
fi


$CHECKOUT;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $CHECKOUT ";
   exit 1;
else
   echo "[OK] $CHECKOUT ";
fi


$CLEAN;
$COMPILE;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $COMPILE ";
   exit 1;
else
   echo "[OK] $COMPILE ";
fi


$UPGRADE_1;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $UPGRADE1 ";
   exit 1;
else
   echo "[OK] $UPGRADE1 ";
fi

$UPGRADE_2;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $UPGRADE2 ";
   exit 1;
else
   echo "[OK] $UPGRADE2 ";
fi

$UPGRADE_3;
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $UPGRADE3 ";
   exit 1;
else
   echo "[OK] $UPGRADE3 ";
fi



ALLGOOD=0;

yes | $CP_CHAOS
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $CP_CHAOS ";
   ALLGOOD=1;
else
   echo "[OK] $CP_CHAOS ";
fi


yes | $CP_CLIENT
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $CP_CLIENT ";
   ALLGOOD=2;
else
   echo "[OK] $CP_CLIENT ";
fi

yes | $CP_SIMIANARMY
if [ "$?" -ne 0 ]; then 
   echo "[FAILED] $CP_SIMIANARMY ";
   ALLGOOD=3;
else
   echo "[OK] $CP_SIMIANARMY ";
fi


if [ "$ALLGOOD" -ne 0 ] ; then
   echo "[FAILED]Upgrade to $TAG failed,pls configure the properties and restart tomcat"
   exit 1;
fi


#$TOMCAT_RESTART;
#$TOMCAT_LOG;