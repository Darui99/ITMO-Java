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

javac -d "${COMPILED}" -p "${KG_P}/artifacts/":"${KG_P}/lib/" --module-source-path "${WORK_DIR}" --module "${MODULE}"

MANIFEST="${TEMP}/MANIFEST.MF"
(
    echo "Manifest-Version: 1.0"
    echo "Main-Class: ru.ifmo.rain.kurbatov.implementor.JarImplementor"
    echo "Class-Path: info.kgeorgiy.java.advanced.implementor.jar"
) > "${MANIFEST}"
jar -c -f "_implementor.jar" -m "${MANIFEST}" -C "${COMPILED}/${MODULE}" .

rm -r "${TEMP}"
