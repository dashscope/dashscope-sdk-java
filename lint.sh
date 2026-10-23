#!/bin/bash
java -jar .dev_tools/google-java-format-1.16.0-all-deps.jar -i $(find . -type f -name "*.java" | grep "./*/src/.*java")
