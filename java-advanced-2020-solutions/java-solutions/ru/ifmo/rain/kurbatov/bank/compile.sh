#!/bin/bash

MODULE="ru.ifmo.rain.kurbatov"
ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build"
KG_P="${ROOT}/java-advanced-2020"

mkdir -p "${TEMP}"

WORK_DIR="${TEMP}/modules"

mkdir -p "${WORK_DIR}"

cp -r "${MY_P}/java-solutions/." "${WORK_DIR}/${MODULE}"

COMPILED="${TEMP}/compilied"

javac -d "${COMPILED}" -p "${KG_P}/artifacts/":"${KG_P}/lib/":"${MY_P}/lib/":"${MY_P}/lib/junit" --module-source-path "${WORK_DIR}" --module "${MODULE}"
