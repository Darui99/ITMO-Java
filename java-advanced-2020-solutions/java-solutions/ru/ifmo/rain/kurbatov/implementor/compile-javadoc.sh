#!/bin/bash

MODULE="ru.ifmo.rain.kurbatov"
ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build/my"
TEMPKG="${MY_P}/_build/kg"
KG_P="${ROOT}/java-advanced-2020"
LINK="https://docs.oracle.com/en/java/javase/11/docs/api/"
KG_M="${KG_P}/modules/info.kgeorgiy.java.advanced.implementor"
DATA="${TEMPKG}/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"

mkdir -p "${TEMP}"
mkdir -p "${TEMPKG}"

cp -r "${MY_P}/java-solutions/." "${TEMP}/${MODULE}"
cp -r "${KG_M}" "${TEMPKG}/info.kgeorgiy.java.advanced.implementor"

javadoc -link ${LINK} \
        -private \
        -d "_javadoc" \
        -p "${KG_P}/artifacts":"${KG_P}/lib" \
        --module-source-path "${TEMP}:${TEMPKG}" --module "${MODULE}" \
        "${DATA}Impler.java" "${DATA}ImplerException.java" "${DATA}JarImpler.java"

rm -r "${MY_P}/_build"
