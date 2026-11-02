"""
端到端测试：在真实电视上跑完整链路。

覆盖（对应已知 bug）：
  T1 默认配置正确（5 分钟 / 6 年级 / 语英数）
  T2 点「开始监控」后，设置界面必须消失    <- BUG 修复验证
  T3 到点自动弹出做题界面
  T4 返回键被吞掉，界面不退出
  T5 答对达标后放行，且不再回到设置界面    <- BUG 修复验证

用法：python tests/e2e/e2e_test.py
前置：电视已 adb 连接、应用已安装
"""
import os
import re
import shutil
import subprocess
import sys
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ADB = os.environ.get("ADB") or shutil.which("adb") or "adb"
PKG = "com.tvguard.tutor"
UI_PATH = "/sdcard/e2e_ui.xml"
LOCAL_UI = os.path.join(ROOT, "build", "e2e_ui.xml")

results = []


def sh(cmd):
    return subprocess.run([ADB, "shell", cmd], capture_output=True,
                          text=True, encoding="utf-8", errors="ignore").stdout


def dump_text():
    sh("uiautomator dump " + UI_PATH)
    subprocess.run([ADB, "pull", UI_PATH, LOCAL_UI], capture_output=True)
    try:
        xml = open(LOCAL_UI, encoding="utf-8").read()
    except Exception:
        return "", []
    nodes = re.findall(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    texts = []
    boxes = []
    for t, a, b, c, d in nodes:
        t = t.strip()
        if not t:
            continue
        texts.append(t)
        boxes.append((t, (int(a) + int(c)) // 2, (int(b) + int(d)) // 2))
    return " | ".join(texts), boxes


def focused():
    """返回 '包名/Activity名' 形式，例如 com.tvguard.tutor/.QuizActivity"""
    out = sh("dumpsys activity activities")
    m = re.search(r"mFocusedActivity: ActivityRecord\{\S+ u0 ([\w.]+)/([^\s}]+)", out)
    return (m.group(1) + "/" + m.group(2)) if m else ""


def record(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("  PASS  " if ok else "  FAIL  ") + name + (("  -> " + detail) if detail else ""))


BANK_SRC = os.path.join(ROOT, "app", "src", "com", "tvguard", "tutor", "QuestionBank.java")


def load_bank():
    """从 QuestionBank.java 源码解析语文/英语知识表，得到 题干->正确答案 的映射。
    这样测试脚本也能答对文科题，不用另建数据副本。"""
    try:
        src = open(BANK_SRC, encoding="utf-8").read()
    except Exception:
        return {}
    pairs = {}
    pat = re.compile(
        r'\{"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\}')
    for m in pat.finditer(src):
        pairs[m.group(1).strip()] = m.group(2).strip()
    return pairs


BANK = load_bank()


def solve(q):
    m = re.match(r"(-?\d+)\s*([+\-x/])\s*(-?\d+)\s*=\s*\?", q)
    if m:
        a, op, b = int(m.group(1)), m.group(2), int(m.group(3))
        return {"+": a + b, "-": a - b, "x": a * b, "/": a // b if b else None}[op]
    m = re.match(r"\((\d+)\s*\+\s*(\d+)\)\s*x\s*(\d+)\s*=\s*\?", q)
    if m:
        return (int(m.group(1)) + int(m.group(2))) * int(m.group(3))
    m = re.match(r"(\d+)\s*的\s*(\d+)%", q)
    if m:
        return int(int(m.group(1)) * int(m.group(2)) / 100)
    m = re.search(r"比是 1:(\d+)，两数之和是 (\d+)", q)
    if m:
        return int(m.group(2)) // (1 + int(m.group(1)))
    m = re.search(r"每盒有 (\d+) 支铅笔，买 (\d+) 盒", q)
    if m:
        return int(m.group(1)) * int(m.group(2))
    m = re.search(r"一箱有 (\d+) 盒，每盒 (\d+) 个，用掉 (\d+) 个", q)
    if m:
        return int(m.group(1)) * int(m.group(2)) - int(m.group(3))
    m = re.search(r"(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)", q)
    if m:
        v = [int(m.group(i)) for i in range(1, 5)]
        d = v[1] - v[0]
        if v[2] - v[1] == d and v[3] - v[2] == d:
            return v[3] + d
        roots = []
        for n in v:
            r = int(round(n ** 0.5))
            if r * r != n:
                roots = None
                break
            roots.append(r)
        if roots and all(roots[i + 1] - roots[i] == 1 for i in range(3)):
            return (roots[-1] + 1) ** 2
        if v[0]:
            r = v[1] / v[0]
            if abs(v[2] / v[1] - r) < 1e-9 and abs(v[3] / v[2] - r) < 1e-9:
                return int(round(v[3] * r))
    return None


def main():
    print("=" * 52)
    print("护眼小助手 端到端测试")
    print("=" * 52)

    # ---- 前置 ----
    dev = sh("getprop ro.product.model").strip()
    if not dev:
        print("设备未连接，终止")
        return 1
    out = sh("pm list packages " + PKG)
    if PKG not in out:
        print("应用未安装，终止")
        return 1
    print("设备: " + dev + "\n")

    sh("am force-stop " + PKG)
    sh("pm clear " + PKG)          # 清掉上一轮残留的配置，保证测的是默认值
    time.sleep(1)
    sh("am start -n %s/.MainActivity" % PKG)
    time.sleep(4)

    # ---- T1 默认配置 ----
    txt = ""
    for _ in range(4):
        txt, _ = dump_text()
        if "护眼小助手" in txt:
            break
        time.sleep(2)
    record("T1 默认间隔 5 分钟", "间隔：5 分钟" in txt, txt[:80])
    record("T1 默认年级 6 年级", "年级：6 年级" in txt)
    record("T1 默认科目 语英数", "语英数" in txt)

    # ---- T2 通过自动化入口设 1 分钟并启动监控 ----
    # 走的是和「点开始监控」同一条代码路径（prefs.setRunning + startService + finish）
    sh("am force-stop " + PKG)
    time.sleep(1)
    sh("am start -n %s/.MainActivity --ei tvguard.interval 1 --ez tvguard.autostart true" % PKG)
    time.sleep(3)
    fg_after_start = focused()
    home_gone = PKG + "/.MainActivity" not in fg_after_start
    record("T2 启动监控后设置界面消失", home_gone, fg_after_start)

    if not home_gone:
        finish()
        return 1

    # ---- T3 等待弹题 ----
    print("\n  等待自动弹题（最多 100 秒）...")
    sh("logcat -c")
    shown = False
    for _ in range(25):
        time.sleep(4)
        if "QuizActivity" in focused():
            shown = True
            break
    if not shown:
        # 兜底：视频 App 可能瞬时抢回焦点，查应用日志确认是否弹过
        logs = sh("logcat -d -s TvGuard")
        shown = "quiz shown" in logs
    record("T3 到点自动弹出做题界面", shown, focused())

    if not shown:
        finish()
        return 1

    # ---- T4 返回键被吞 ----
    sh("input keyevent 4")
    time.sleep(2)
    record("T4 返回键不退出做题界面", "QuizActivity" in focused(), focused())

    # ---- T5 答题放行 ----
    print("\n  开始答题...")
    released = False
    rounds = 0
    for attempt in range(14):
        txt, boxes = dump_text()
        fg = focused()
        # 只有明确读到别的 Activity 才算放行，避免解析失败被误判
        if fg and "QuizActivity" not in fg:
            released = True
            break
        # 找题目（最长的中文句子）
        candidates = [b for b in boxes if "休息一下" not in b[0] and "还需答对" not in b[0]]
        if len(candidates) < 4:
            # 界面可能正在过渡
            time.sleep(2)
            continue
        question = candidates[0][0]
        opts = candidates[1:5]
        pick = None
        ans = solve(question)
        if ans is not None:
            for t, x, y in opts:
                if re.fullmatch(r"-?\d+", t.strip()) and int(t) == ans:
                    pick = (x, y)
                    break
        else:
            # 文科题：查题库拿正确答案，再在选项里找
            correct = BANK.get(question)
            if correct:
                for t, x, y in opts:
                    if t.strip() == correct:
                        pick = (x, y)
                        break
        if pick is None:
            pick = (opts[0][1], opts[0][2])
        print("    [%d] %s -> 点 (%d,%d)" % (attempt, question[:36], pick[0], pick[1]))
        sh("input tap %d %d" % pick)
        rounds += 1
        time.sleep(3.2)

    print("    共答题 %d 轮" % rounds)
    record("T5 答对达标后自动放行", released)

    # ---- T5b 放行后不回设置界面 ----
    time.sleep(2)
    fg_after_done = focused()
    back_to_home = PKG + "/.MainActivity" not in fg_after_done
    record("T5b 放行后不再回到设置界面", back_to_home, fg_after_done)

    finish()


def finish():
    print("\n" + "-" * 52)
    passed = sum(1 for r in results if r[1])
    total = len(results)
    print("结果: %d/%d 通过" % (passed, total))
    fatal = [r[0] for r in results if not r[1]]
    if fatal:
        print("失败项:")
        for f in fatal:
            print("  - " + f)
    print("RESULT: " + ("PASS" if passed == total else "FAIL"))
    sys.exit(0 if passed == total else 1)


if __name__ == "__main__":
    main()
