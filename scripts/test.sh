#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

rm -rf out-test
mkdir -p out-test
javac -d out-test -encoding UTF-8 $(find src tests -name '*.java')
java -cp out-test tests.ProtocolIntegrationTest
java -cp out-test tests.ClientDispatchTest
