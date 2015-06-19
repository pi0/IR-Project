#!/usr/bin/env bash
java -Xms128m -Xmx128m -cp out/production/IR-PROJECT/:libs/trove-3.1a1.jar ir.pi0.irproject.Main "$@"
