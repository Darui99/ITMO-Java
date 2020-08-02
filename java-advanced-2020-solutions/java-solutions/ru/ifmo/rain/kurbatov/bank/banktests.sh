#!/bin/bash

ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build"
COMPILED="${TEMP}/compilied"
MODULE="ru.ifmo.rain.kurbatov"

"$(dirname $0)/"compile.sh

cd "${COMPILED}/${MODULE}"

java -jar "../../../lib/junit-platform-console-standalone-1.6.2.jar" \
     -cp . \
     -c "${MODULE}.bank.BankTests" \

rm -r "../../../_build"
