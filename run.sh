#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
java -jar target/example-security-load-tester-1.0.0.jar

