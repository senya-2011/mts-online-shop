#!/usr/bin/env bash
set -euo pipefail

WILDFLY_HOME="${WILDFLY_HOME:-$HOME/wildfly}"
DEPLOYMENTS="${WILDFLY_HOME}/standalone/deployments"
WAR_FILE="${1:-online-shop.war}"

mkdir -p "${DEPLOYMENTS}"
rm -f "${DEPLOYMENTS}/online-shop.war" "${DEPLOYMENTS}/online-shop.war.deployed" "${DEPLOYMENTS}/online-shop.war.failed"

cp "${WAR_FILE}" "${DEPLOYMENTS}/online-shop.war"
touch "${DEPLOYMENTS}/online-shop.war.dodeploy"

echo "Deployed ${WAR_FILE} to WildFly at ${DEPLOYMENTS}"
