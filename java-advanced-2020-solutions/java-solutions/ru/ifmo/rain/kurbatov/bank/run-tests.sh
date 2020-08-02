#!/bin/bash

ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
LIB="${MY_P}/lib/junit"
TEMP="${MY_P}/_build"
COMPILED="${TEMP}/compilied"
MODULE="ru.ifmo.rain.kurbatov"

"$(dirname $0)/"compile.sh

java -cp "${COMPILED}/${MODULE}":"${LIB}/*" \
	  "${MODULE}.bank.Tester"

rm -r "${TEMP}"
