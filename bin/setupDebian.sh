#!/usr/bin/env bash

set -e

# Requires sudo, wget, deb, apt-get systemD

# Ensures Script is Running As Root or Elevates with sudo
[ $(whoami) = root ] || { sudo "$0" "$@"; exit $?; }

which sbt || echo "deb https://sbt.bintray.com/debian /" | tee -a /etc/apt/sources.list
which sbt || apt-get install -y sbt

# Gets the location of the running script, and goes up one level to the containing directory
DIR_SRC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

# Base Install Directory
INSTALL_DIR="/opt/alterpass"

# Source Location
INSTALL_SRC="${INSTALL_DIR}/src"

# Configuration Location For .conf and .p12 files
INSTALL_CONF="${INSTALL_DIR}/conf"

# Temporary File location for SQLite and AgingFile
INSTALL_TMP="${INSTALL_DIR}/tmp"


# Create User And Install Location
# Afterwards user should exist with
id -u alterpass > /dev/null 2>&1 || useradd --system alterpass
mkdir -p ${INSTALL_DIR}
mkdir -p ${INSTALL_DIR}/conf
mkdir -p ${INSTALL_DIR}/tmp
mkdir -p ${INSTALL_SRC}

rsync -r ${DIR_SRC}/* ${INSTALL_SRC}
rsync -r ${DIR_SRC}/.* ${INSTALL_SRC}
chown -R alterpass:alterpass ${INSTALL_DIR}

# Move Incomplete Config File Out of Git Repository for Completion
SERVICE_CONF="${INSTALL_CONF}/alterpass.conf"
cp ${INSTALL_SRC}/conf/alterpass.conf ${SERVICE_CONF}
chown alterpass:alterpass ${SERVICE_CONF}
chown 660 ${SERVICE_CONF}

# Generate Service File
which systemctl > /dev/null && systemctl enable ${INSTALL_SRC}/conf/alterpass.service > /dev/null 2>&1 || true
which systemctl > /dev/null && systemctl daemon-reload || true
which systemctl > /dev/null && echo "INFO - alterpass.service has been enabled" || true

echo ""
echo "INFO - To complete the installation please complete the following"
echo "1. Please Transfer Proprietary Files"
echo "2. Complete ${SERVICE_CONF}"
echo "3. Afterwards run - sudo systemctl start alterpass"
echo "4. Check status   - sudo systemctl status alterpass"