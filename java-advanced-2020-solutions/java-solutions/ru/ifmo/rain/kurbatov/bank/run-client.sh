#!/bin/bash

ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build"
COMPILED="${TEMP}/compilied"
MODULE="ru.ifmo.rain.kurbatov"

"$(dirname $0)/"compile.sh

java -cp "${COMPILED}/${MODULE}" ru.ifmo.rain.kurbatov.bank.Client ${1} ${2} ${3} ${4} ${5}

rm -r "${TEMP}"
