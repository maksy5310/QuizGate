#!/usr/bin/env bash
# 题库单元测试：纯 JVM 运行，不需要 Android 设备
#
# 用法：bash tests/unit/run_unit.sh [轮数] [年级]
# 环境变量：TVGUARD_TOOLCHAIN 工具链目录（默认 build/toolchain）
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TC="${TVGUARD_TOOLCHAIN:-$ROOT/build/toolchain}"
JDK="$TC/jdk17"
OUT="$ROOT/build/testout"

ROUNDS="${1:-500}"
GRADE="${2:-6}"

if [ -f "$JDK/bin/java.exe" ]; then
    EXE=".exe"
else
    EXE=""
fi

JAVAC="$JDK/bin/javac$EXE"
JAVA="$JDK/bin/java$EXE"

if [ ! -f "$JAVAC" ]; then
    echo "缺少 $JAVAC，请先准备工具链（见 README「编译」一节）"
    exit 1
fi

rm -rf "$OUT"
mkdir -p "$OUT"

"$JAVAC" -encoding UTF-8 -d "$OUT" \
    "$ROOT/app/src/com/tvguard/tutor/Question.java" \
    "$ROOT/app/src/com/tvguard/tutor/QuestionBank.java" \
    "$ROOT/tests/unit/BankTest.java"

"$JAVA" -Dfile.encoding=UTF-8 -cp "$OUT" BankTest "$ROUNDS" "$GRADE"
