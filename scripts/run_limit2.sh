#!/usr/bin/env bash
java -Xms180m -Xmx180m -cp out/production/IR-PROJECT/:libs/trove-3.1a1.jar:libs/nanohttpd.jar ir.pi0.irproject.Main "$@"
