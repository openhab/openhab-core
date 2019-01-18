#!/bin/sh

ESH_BASEDIR="/home/rathgeb/bin/pkgs/eclipse/esh/git/smarthome"

cd "${ESH_BASEDIR}"

DIR=extensions/ui/org.eclipse.smarthome.ui.basic
mkdir -p "${DIR}"/src/main/resources
mv "${DIR}"/snippets "${DIR}"/src/main/resources

DIR=extensions/ui/org.eclipse.smarthome.ui.classic
mkdir -p "${DIR}"/src/main/resources
mv "${DIR}"/snippets "${DIR}"/src/main/resources

if false; then
find -type d -name "ESH-INF" | grep -v -e '/target/' -e 'src/main/resources/ESH-INF' | while read ESHINF_DIR; do
    PROJECT_DIR="$(dirname "${ESHINF_DIR}")"
    mkdir -p "${PROJECT_DIR}/src/main/resources"
    mv "${ESHINF_DIR}" "${PROJECT_DIR}/src/main/resources"
done
fi
