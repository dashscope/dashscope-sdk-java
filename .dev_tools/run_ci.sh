#!/bin/bash

# Guard the 3.0 root-package layout: the com.alibaba.dashscope.v3 intermediate packages and
# the root common leaf were eliminated by the package finalization. A stale pre-migration copy
# re-added under them slips through a merge with zero conflict signal (the merge base usually
# lacks the path) and breaks the build against classes that no longer exist, so fail fast here
# before the format loop and mvn package spend minutes on it.
STALE_LAYOUT_FILES=$(find src -type f -name "*.java" 2>/dev/null | grep -E "^src/(main|test)/java/com/alibaba/dashscope/(v3|common)/" || true)
if [ -n "$STALE_LAYOUT_FILES" ]; then
    echo "Found files under eliminated packages (com.alibaba.dashscope.v3 / root common):"
    echo "$STALE_LAYOUT_FILES"
    echo "The 3.0 layout owns the root packages; drop the stale copy or move the code there."
    exit 1
fi


# get all java files
FILES_TO_CHECK=$(find . -type f -name "*.java" | grep "./*/src/.*java")

HAS_CHANGES=false

for FILE in $FILES_TO_CHECK; do
    echo "Checking file: $FILE"
    java -jar .dev_tools/google-java-format-1.16.0-all-deps.jar $FILE | diff $FILE -
    if [ $? -ne 0 ]; then
        echo "File $FILE has changes after formatting."
        HAS_CHANGES=true
    else
        echo "File $FILE has no changes after formatting."
    fi
done

if [ "$HAS_CHANGES" = true ]; then
    echo "Run formatting failed, please try to run `sh lint.sh` and re-commit your java files!"
    exit 1
fi

mvn package

if [ $? -ne 0 ]; then
  echo "mvn package failed, please check if any unittest is failed!"
  exit 1
fi

echo "CI passed."
