#!/bin/sh

find -type d -name "ESH-INF" | grep -v -e '/target/' -e 'src/main/resources/ESH-INF' | while read ESHINF_DIR; do
    PROJECT_DIR="$(dirname "${ESHINF_DIR}")"
    mkdir -p "${PROJECT_DIR}/src/main/resources"
    mv "${ESHINF_DIR}" "${PROJECT_DIR}/src/main/resources"
done

