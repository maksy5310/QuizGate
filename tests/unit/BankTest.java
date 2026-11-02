import com.tvguard.tutor.Question;
import com.tvguard.tutor.QuestionBank;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题库单元测试（纯 JVM，不需要 Android）。
 *
 * 覆盖：
 *  1. 结构：选项必须 4 个、答案下标合法、选项不重复、题干非空
 *  2. 正确性：数学题用独立求解器反算，必须与 answerIndex 指向的选项一致
 *  3. 分布：三科都必须出现，且分布大致均衡
 *  4. 稳定性：大批量生成不抛异常
 *  5. 无连续重复：同一张知识表不会连续抽到同一行
 *
 * 运行：javac ... && java BankTest [轮数] [年级]
 */
public class BankTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 500;
        int grade = args.length > 1 ? Integer.parseInt(args[1]) : 6;

        QuestionBank bank = new QuestionBank();

        int mathCount = 0;
        int chineseCount = 0;
        int englishCount = 0;
        int mathVerified = 0;
        int mathUnparsed = 0;

        String lastChinese = null;
        String lastEnglish = null;
        int consecRepeat = 0;

        for (int i = 0; i < rounds; i++) {
            Question q;
            try {
                q = bank.next(grade, true, true, true);
            } catch (Throwable t) {
                fail("第 " + i + " 题生成抛异常: " + t);
                continue;
            }

            // --- 结构检查 ---
            check(q.stem != null && q.stem.trim().length() > 0, "题干为空: " + q.stem);
            check(q.options.length == 4, "选项数不是 4: " + q.stem);
            check(q.answerIndex >= 0 && q.answerIndex < q.options.length,
                    "答案下标越界: " + q.stem);
            check(q.explain != null && q.explain.length() > 0,
                    "解析为空: " + q.stem);

            Set<String> uniq = new HashSet<String>();
            for (String o : q.options) {
                check(o != null && o.trim().length() > 0, "选项为空: " + q.stem);
                check(uniq.add(o), "选项重复 [" + o + "]: " + q.stem);
            }

            // --- 分类 ---
            boolean isMath = looksLikeMath(q);
            if (isMath) {
                mathCount++;
                Integer expected = solveMath(q.stem);
                if (expected == null) {
                    mathUnparsed++;
                } else {
                    String actualStr = q.options[q.answerIndex];
                    try {
                        int actual = Integer.parseInt(actualStr.trim());
                        check(actual == expected.intValue(),
                                "答案错: " + q.stem + " 期望 " + expected + " 实得 " + actual);
                        mathVerified++;
                    } catch (NumberFormatException e) {
                        check(false, "数学题答案不是整数: " + q.stem + " -> " + actualStr);
                    }
                }
            } else {
                // 用英文/中文字符判断科目
                if (isEnglish(q.stem)) {
                    englishCount++;
                    if (q.stem.equals(lastEnglish)) {
                        consecRepeat++;
                        fail("英语连续重复: " + q.stem);
                    }
                    lastEnglish = q.stem;
                } else {
                    chineseCount++;
                    if (q.stem.equals(lastChinese)) {
                        consecRepeat++;
                        fail("语文连续重复: " + q.stem);
                    }
                    lastChinese = q.stem;
                }
            }
        }

        System.out.println("------------------------------");
        System.out.println("总题数      : " + rounds);
        System.out.println("数学 / 语文 / 英语 : " + mathCount + " / " + chineseCount + " / " + englishCount);
        System.out.println("数学已核验  : " + mathVerified + "（无法解析 " + mathUnparsed + "）");
        System.out.println("连续重复    : " + consecRepeat);
        System.out.println("断言总数    : " + checks);
        System.out.println("失败        : " + failures);

        // --- 分布断言 ---
        if (rounds >= 30) {
            check(mathCount > 0, "三科未全部出现：数学 = 0");
            check(chineseCount > 0, "三科未全部出现：语文 = 0");
            check(englishCount > 0, "三科未全部出现：英语 = 0");
            int min = Math.min(mathCount, Math.min(chineseCount, englishCount));
            check(min * 10 >= rounds, "分布严重不均，最少一科只有 " + min + " 题");
        }
        if (mathCount > 0) {
            // 数学题只要有把握解析的，必须全部核验通过
            check(mathUnparsed * 5 <= mathCount,
                    "太多数学题无法解析，核验覆盖率过低：" + mathUnparsed + "/" + mathCount);
        }

        System.out.println();
        System.out.println(failures == 0 ? "RESULT: PASS" : "RESULT: FAIL (" + failures + " 处失败)");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------

    private static void check(boolean ok, String msg) {
        checks++;
        if (!ok) {
            fail(msg);
        }
    }

    private static void fail(String msg) {
        failures++;
        if (failures <= 20) {
            System.out.println("FAIL: " + msg);
        }
    }

    private static boolean isEnglish(String stem) {
        int latin = 0;
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                latin++;
            }
        }
        return latin >= 3;
    }

    /** 粗判：选项全是数字，且题干不是长句中文 → 数学题 */
    private static boolean looksLikeMath(Question q) {
        for (String o : q.options) {
            if (!o.trim().matches("-?\\d+")) {
                return false;
            }
        }
        return true;
    }

    /** 独立求解器：重算数学题答案，解析不出返回 null */
    private static Integer solveMath(String s) {
        Matcher m;

        // a op b = ?
        m = Pattern.compile("^(-?\\d+)\\s*([+\\-x/])\\s*(-?\\d+)\\s*=\\s*\\?$").matcher(s);
        if (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(3));
            char op = m.group(2).charAt(0);
            if (op == '+') return Integer.valueOf(a + b);
            if (op == '-') return Integer.valueOf(a - b);
            if (op == 'x') return Integer.valueOf(a * b);
            if (op == '/') return Integer.valueOf(a / b);
        }

        // (a + b) x c = ?
        m = Pattern.compile("^\\((\\d+)\\s*\\+\\s*(\\d+)\\)\\s*x\\s*(\\d+)\\s*=\\s*\\?$").matcher(s);
        if (m.find()) {
            return Integer.valueOf((Integer.parseInt(m.group(1))
                    + Integer.parseInt(m.group(2))) * Integer.parseInt(m.group(3)));
        }

        // X 的 Y% 是多少？
        m = Pattern.compile("^(\\d+)\\s*的\\s*(\\d+)%").matcher(s);
        if (m.find()) {
            return Integer.valueOf(Integer.parseInt(m.group(1))
                    * Integer.parseInt(m.group(2)) / 100);
        }

        // 比 1:N，和 M，较小数
        m = Pattern.compile("比是 1:(\\d+)，两数之和是 (\\d+)").matcher(s);
        if (m.find()) {
            return Integer.valueOf(Integer.parseInt(m.group(2)) / (1 + Integer.parseInt(m.group(1))));
        }

        // 每盒 A 支，买 B 盒
        m = Pattern.compile("每盒有 (\\d+) 支铅笔，买 (\\d+) 盒").matcher(s);
        if (m.find()) {
            return Integer.valueOf(Integer.parseInt(m.group(1)) * Integer.parseInt(m.group(2)));
        }

        // 一箱 B 盒，每盒 P 个，用掉 U 个
        m = Pattern.compile("一箱有 (\\d+) 盒，每盒 (\\d+) 个，用掉 (\\d+) 个").matcher(s);
        if (m.find()) {
            int b = Integer.parseInt(m.group(1));
            int p = Integer.parseInt(m.group(2));
            int u = Integer.parseInt(m.group(3));
            return Integer.valueOf(b * p - u);
        }

        // 数列：取题干里连续 4 个数字
        m = Pattern.compile("(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+)").matcher(s);
        if (m.find()) {
            int[] v = new int[]{
                    Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))
            };
            int d = v[1] - v[0];
            if (v[2] - v[1] == d && v[3] - v[2] == d) {
                return Integer.valueOf(v[3] + d);
            }
            boolean sq = true;
            int[] r = new int[4];
            for (int i = 0; i < 4; i++) {
                int root = (int) Math.round(Math.sqrt(v[i]));
                if (root * root != v[i]) {
                    sq = false;
                    break;
                }
                r[i] = root;
            }
            if (sq && r[1] - r[0] == 1 && r[2] - r[1] == 1 && r[3] - r[2] == 1) {
                int next = r[3] + 1;
                return Integer.valueOf(next * next);
            }
            if (v[0] != 0 && v[0] != 0) {
                double ratio = (double) v[1] / v[0];
                if (Math.abs((double) v[2] / v[1] - ratio) < 1e-9
                        && Math.abs((double) v[3] / v[2] - ratio) < 1e-9) {
                    return Integer.valueOf((int) Math.round(v[3] * ratio));
                }
            }
        }
        return null;
    }
}
