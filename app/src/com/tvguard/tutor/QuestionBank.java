package com.tvguard.tutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 题目生成器。三科：数学（模板生成）、语文、英语（知识表）。
 * 出题按"科目轮转 + 随机起点"，保证三科均衡，不会连续出同一科。
 */
public class QuestionBank {

    public static final int SUBJ_MATH = 0;
    public static final int SUBJ_CHINESE = 1;
    public static final int SUBJ_ENGLISH = 2;

    /** 轮转队列，静态保存，跨 Activity 重建不重置 */
    private static final List<Integer> QUEUE = new ArrayList<Integer>();
    private static final Random SHUFFLER = new Random();

    private final Random rnd = new Random();

    // ------------------------------------------------------------------
    // 对外入口
    // ------------------------------------------------------------------

    public Question next(int grade, boolean chinese, boolean english, boolean math) {
        int subject = pickSubject(chinese, english, math);
        if (subject == SUBJ_CHINESE) {
            return fromTable(CHINESE, true);
        }
        if (subject == SUBJ_ENGLISH) {
            return fromTable(ENGLISH, false);
        }
        return math(grade);
    }

    private static synchronized int pickSubject(boolean c, boolean e, boolean m) {
        if (!c && !e && !m) {
            return SUBJ_MATH;
        }
        for (int guard = 0; guard < 12; guard++) {
            if (QUEUE.isEmpty()) {
                if (c) QUEUE.add(Integer.valueOf(SUBJ_CHINESE));
                if (e) QUEUE.add(Integer.valueOf(SUBJ_ENGLISH));
                if (m) QUEUE.add(Integer.valueOf(SUBJ_MATH));
                Collections.shuffle(QUEUE, SHUFFLER);
            }
            int s = QUEUE.remove(0).intValue();
            if ((s == SUBJ_CHINESE && c) || (s == SUBJ_ENGLISH && e) || (s == SUBJ_MATH && m)) {
                return s;
            }
        }
        return SUBJ_MATH;
    }

    /** 洗牌袋：把整张表的下标打乱后依次抽取，取完再重洗。
     *  保证一轮内绝不重复，跨 Activity 重建也保持（静态）。 */
    private static final List<Integer> CHINESE_BAG = new ArrayList<Integer>();
    private static final List<Integer> ENGLISH_BAG = new ArrayList<Integer>();
    private static final Random BAG_RND = new Random();

    private Question fromTable(String[][] table, boolean isChinese) {
        List<Integer> bag = isChinese ? CHINESE_BAG : ENGLISH_BAG;
        if (bag.isEmpty()) {
            for (int i = 0; i < table.length; i++) {
                bag.add(Integer.valueOf(i));
            }
            Collections.shuffle(bag, BAG_RND);
        }
        int idx = bag.remove(0).intValue();
        if (idx >= table.length) {
            idx = BAG_RND.nextInt(table.length);
        }

        String[] row = table[idx];
        List<String> opts = new ArrayList<String>();
        opts.add(row[1]);
        opts.add(row[2]);
        opts.add(row[3]);
        opts.add(row[4]);
        Collections.shuffle(opts, rnd);
        String[] arr = new String[opts.size()];
        opts.toArray(arr);
        return new Question(row[0], arr, opts.indexOf(row[1]), row[5]);
    }

    // ------------------------------------------------------------------
    // 语文（小学高年级）
    // 每行：题干 / 正确 / 干扰1 / 干扰2 / 干扰3 / 解析
    // ------------------------------------------------------------------

    private static final String[][] CHINESE = {
        {"「床前明月光」的下一句是？", "疑是地上霜", "举头望明月", "低头思故乡", "月是故乡明",
         "李白《静夜思》：床前明月光，疑是地上霜。"},
        {"「春眠不觉晓」的下一句是？", "处处闻啼鸟", "夜来风雨声", "花落知多少", "春去花还在",
         "孟浩然《春晓》：春眠不觉晓，处处闻啼鸟。"},
        {"「欲穷千里目」的下一句是？", "更上一层楼", "白日依山尽", "黄河入海流", "一览众山小",
         "王之涣《登鹳雀楼》：欲穷千里目，更上一层楼。"},
        {"「两个黄鹂鸣翠柳」的下一句是？", "一行白鹭上青天", "窗含西岭千秋雪", "门泊东吴万里船", "千山鸟飞绝",
         "杜甫《绝句》：两个黄鹂鸣翠柳，一行白鹭上青天。"},
        {"「海内存知己」的下一句是？", "天涯若比邻", "无为在歧路", "儿女共沾巾", "青山一道同云雨",
         "王勃《送杜少府之任蜀州》：海内存知己，天涯若比邻。"},
        {"「但愿人长久，千里共婵娟」的作者是？", "苏轼", "李白", "杜甫", "辛弃疾",
         "出自苏轼《水调歌头·明月几时有》。"},
        {"「会当凌绝顶，一览众山小」的作者是？", "杜甫", "李白", "王维", "孟浩然",
         "出自杜甫《望岳》。"},
        {"「三人行，必有我师焉」出自哪部典籍？", "《论语》", "《孟子》", "《大学》", "《中庸》",
         "出自《论语·述而》。"},
        {"「守株待兔」中的「株」指的是什么？", "树桩", "树根", "树枝", "田埂",
         "株：露出地面的树桩。比喻死守经验，不知变通。"},
        {"「刻舟求剑」这个故事告诉我们？", "拘泥成法，不知变通", "做事要有恒心", "要善于观察", "要团结协作",
         "剑掉进水里，船在动而记号不动，说明不能死守旧办法。"},
        {"「揠苗助长」这个故事告诉我们？", "违反规律急于求成反而坏事", "要勤奋努力", "要多请教别人", "要保护庄稼",
         "把苗拔高想让它快长，结果苗都枯死了。"},
        {"与「忽然」意思最接近的词语是？", "突然", "依然", "竟然", "偶然",
         "忽然与突然都表示事情发生得快、出乎意料。"},
        {"与「鼓励」意思最接近的词语是？", "激励", "打击", "批评", "安慰",
         "鼓励和激励都是使人振作、增加信心。"},
        {"「犹豫」的反义词是？", "果断", "迟疑", "徘徊", "思索",
         "犹豫是拿不定主意，反义为果断。"},
        {"「谦虚」的反义词是？", "骄傲", "虚心", "谨慎", "诚实",
         "谦虚是虚心不自满，反义为骄傲。"},
        {"「太阳像一个大火球」用了什么修辞手法？", "比喻", "拟人", "夸张", "排比",
         "把太阳比作火球，是比喻。"},
        {"「飞流直下三千尺」用了什么修辞手法？", "夸张", "比喻", "拟人", "对偶",
         "用「三千尺」夸大瀑布的高度，是夸张。"},
        {"「再接再励」的正确写法是？", "再接再厉", "再接再励", "再接再利", "再接再立",
         "厉同「砺」，磨快，指继续努力。"},
        {"「迫不急待」的正确写法是？", "迫不及待", "迫不急待", "迫不急戴", "迫不极待",
         "及是「来得及」，不能写成急。"},
        {"「人固有一死，或重于泰山，或轻于鸿毛」的作者是？", "司马迁", "屈原", "王安石", "韩愈",
         "出自司马迁《报任安书》。"},
        {"「不为五斗米折腰」说的是谁？", "陶渊明", "李白", "杜甫", "苏轼",
         "陶渊明不愿为微薄俸禄向权贵低头，辞官归隐。"},
        {"「四书」不包括下面哪一部？", "《诗经》", "《大学》", "《中庸》", "《孟子》",
         "四书为《大学》《中庸》《论语》《孟子》，《诗经》属五经。"},

        // ----- 唐诗宋词 -----
        {"「大漠孤烟直，长河落日圆」的作者是？", "王维", "李白", "杜甫", "岑参",
         "出自王维《使至塞上》。"},
        {"「独在异乡为异客，每逢佳节倍思亲」写的是哪个节日？", "重阳节", "春节", "中秋节", "端午节",
         "王维《九月九日忆山东兄弟》，九月九日为重阳节。"},
        {"「爆竹声中一岁除，春风送暖入屠苏」写的是哪个节日？", "春节", "元宵节", "清明节", "冬至",
         "王安石《元日》，元日即农历正月初一春节。"},
        {"「锄禾日当午，汗滴禾下土」出自哪首诗？", "《悯农》", "《村居》", "《四时田园杂兴》", "《归园田居》",
         "李绅《悯农》，写农民劳作的辛苦。"},
        {"「碧玉妆成一树高，万条垂下绿丝绦」描写的是什么？", "柳树", "竹子", "荷花", "梅花",
         "贺知章《咏柳》，把柳条比作绿丝带。"},
        {"「小荷才露尖尖角」的下一句是？", "早有蜻蜓立上头", "映日荷花别样红", "接天莲叶无穷碧", "清泉石上流",
         "杨万里《小池》：小荷才露尖尖角，早有蜻蜓立上头。"},
        {"「停车坐爱枫林晚」的下一句是？", "霜叶红于二月花", "远上寒山石径斜", "白云生处有人家", "夜泊秦淮近酒家",
         "杜牧《山行》：停车坐爱枫林晚，霜叶红于二月花。"},
        {"「春色满园关不住」的下一句是？", "一枝红杏出墙来", "万紫千红总是春", "草色遥看近却无", "不知细叶谁裁出",
         "叶绍翁《游园不值》：春色满园关不住，一枝红杏出墙来。"},
        {"「野火烧不尽，春风吹又生」描写的是什么？", "草", "柳树", "梅花", "竹子",
         "白居易《赋得古原草送别》，写野草顽强的生命力。"},
        {"「飞流直下三千尺」写的是哪座山？", "庐山", "泰山", "黄山", "华山",
         "李白《望庐山瀑布》。"},
        {"「姑苏城外寒山寺，夜半钟声到客船」的作者是？", "张继", "杜牧", "白居易", "孟浩然",
         "出自张继《枫桥夜泊》。"},
        {"「生当作人杰，死亦为鬼雄」的作者是？", "李清照", "苏轼", "辛弃疾", "柳永",
         "李清照《夏日绝句》，她是宋代著名女词人。"},
        {"「稻花香里说丰年，听取蛙声一片」的作者是？", "辛弃疾", "苏轼", "陆游", "李清照",
         "出自辛弃疾《西江月·夜行黄沙道中》。"},
        {"「不知细叶谁裁出，二月春风似剪刀」用了什么修辞？", "比喻", "夸张", "排比", "反问",
         "把春风比作剪刀，是比喻。"},
        {"「墙角数枝梅，凌寒独自开」的作者是？", "王安石", "陆游", "林逋", "苏轼",
         "出自王安石《梅花》。"},
        {"「千山鸟飞绝，万径人踪灭」的作者是？", "柳宗元", "王维", "孟浩然", "刘长卿",
         "出自柳宗元《江雪》。"},

        // ----- 国学 -----
        {"《三字经》的第一句是？", "人之初，性本善", "弟子规，圣人训", "天地玄黄", "赵钱孙李",
         "《三字经》开篇：人之初，性本善。"},
        {"「弟子规，圣人训」的下一句是？", "首孝悌，次谨信", "泛爱众，而亲仁", "有余力，则学文", "父母呼，应勿缓",
         "《弟子规》：弟子规，圣人训。首孝悌，次谨信。"},
        {"《论语》记录的是谁的言行？", "孔子及其弟子", "孟子", "老子", "庄子",
         "《论语》由孔子弟子及再传弟子编纂，记孔子及其弟子言行。"},
        {"「己所不欲，勿施于人」出自哪部典籍？", "《论语》", "《孟子》", "《老子》", "《荀子》",
         "出自《论语·卫灵公》。"},
        {"「老吾老以及人之老」出自哪部典籍？", "《孟子》", "《论语》", "《礼记》", "《墨子》",
         "出自《孟子·梁惠王上》。"},
        {"「天行健，君子以自强不息」出自哪部典籍？", "《周易》", "《论语》", "《尚书》", "《诗经》",
         "出自《周易·乾卦》。"},
        {"「温故而知新」的下一句是？", "可以为师矣", "不亦说乎", "思而不学则殆", "焉得知",
         "《论语》：温故而知新，可以为师矣。"},
        {"「学而不思则罔」的下一句是？", "思而不学则殆", "可以为师矣", "不耻下问", "择其善者而从之",
         "《论语》：学而不思则罔，思而不学则殆。"},
        {"「五经」不包括下面哪一部？", "《论语》", "《诗经》", "《礼记》", "《春秋》",
         "五经为《诗》《书》《礼》《易》《春秋》，《论语》属四书。"},
        {"「孟母三迁」讲的是谁的母亲？", "孟子", "孔子", "荀子", "曾子",
         "孟母为给孟子选择好环境三次搬家。"},
        {"「千里之行，始于足下」出自哪部典籍？", "《老子》", "《论语》", "《庄子》", "《韩非子》",
         "出自《老子》第六十四章。"},
        {"「青出于蓝而胜于蓝」出自哪部典籍？", "《荀子》", "《论语》", "《孟子》", "《礼记》",
         "出自《荀子·劝学》。"},

        // ----- 历史 -----
        {"中国历史上第一个统一的中央集权王朝是？", "秦朝", "汉朝", "周朝", "唐朝",
         "公元前 221 年秦始皇统一六国，建立秦朝。"},
        {"「焚书坑儒」发生在哪位皇帝在位时？", "秦始皇", "汉武帝", "唐太宗", "隋炀帝",
         "秦始皇为加强思想控制而焚书坑儒。"},
        {"「三国」不包括下面哪个政权？", "唐朝", "魏", "蜀", "吴",
         "三国为魏、蜀、吴。"},
        {"「贞观之治」出现在哪位皇帝在位期间？", "唐太宗", "唐高祖", "汉武帝", "宋太祖",
         "唐太宗李世民在位期间政治清明，史称贞观之治。"},
        {"唐朝的开国皇帝是？", "李渊", "李世民", "李隆基", "杨坚",
         "李渊于 618 年建立唐朝，是为唐高祖。"},
        {"郑和下西洋发生在哪个朝代？", "明朝", "元朝", "宋朝", "清朝",
         "明成祖时期郑和七次下西洋。"},
        {"中国古代「四大发明」不包括下面哪一项？", "地动仪", "造纸术", "火药", "指南针",
         "四大发明为造纸术、印刷术、火药、指南针。"},
        {"古代「丝绸之路」的起点是？", "长安", "洛阳", "开封", "杭州",
         "汉代丝绸之路从长安出发，经河西走廊通往西域。"},
        {"《资治通鉴》的作者是？", "司马光", "司马迁", "班固", "陈寿",
         "司马光主编《资治通鉴》，司马光与司马迁并称「史学两司马」。"},
        {"中国最后一个封建王朝的末代皇帝是？", "溥仪", "光绪", "咸丰", "康熙",
         "爱新觉罗·溥仪，清朝末代皇帝。"},
        {"「卧薪尝胆」讲的是哪位君主？", "越王勾践", "吴王夫差", "秦始皇", "齐桓公",
         "越王勾践战败后卧薪尝胆，最终灭吴。"},
        {"「纸上谈兵」说的是哪位将领？", "赵括", "廉颇", "白起", "李牧",
         "赵括熟读兵书却不会实战，长平之战大败。"},

        // ----- 地理 -----
        {"中国最长的河流是？", "长江", "黄河", "珠江", "黑龙江",
         "长江全长约 6300 公里，是中国第一长河。"},
        {"中国最大的岛屿是？", "台湾岛", "海南岛", "崇明岛", "舟山岛",
         "台湾岛面积约 3.6 万平方公里，是中国第一大岛。"},
        {"中国最大的淡水湖是？", "鄱阳湖", "洞庭湖", "太湖", "青海湖",
         "鄱阳湖在江西省，是中国最大淡水湖；青海湖是最大湖泊但为咸水湖。"},
        {"世界最高的山峰是？", "珠穆朗玛峰", "乔戈里峰", "干城章嘉峰", "洛子峰",
         "珠穆朗玛峰海拔 8848.86 米，是世界第一高峰。"},
        {"中国面积最大的省级行政区是？", "新疆", "西藏", "内蒙古", "青海",
         "新疆维吾尔自治区面积约 166 万平方公里。"},
        {"长江发源于哪座山脉？", "唐古拉山", "昆仑山", "祁连山", "秦岭",
         "长江源头在青藏高原唐古拉山脉各拉丹冬雪山。"},
        {"「四大高原」不包括下面哪一个？", "东北平原", "青藏高原", "内蒙古高原", "云贵高原",
         "四大高原为青藏、内蒙古、黄土、云贵高原；东北平原属平原。"},
        {"地球表面大约百分之多少被海洋覆盖？", "71%", "51%", "61%", "81%",
         "海洋约占地球表面积的 71%。"},
        {"中国最大的沙漠是？", "塔克拉玛干沙漠", "腾格里沙漠", "巴丹吉林沙漠", "库姆塔格沙漠",
         "塔克拉玛干沙漠在新疆塔里木盆地，是中国最大沙漠。"},
        {"黄河最终注入哪个海？", "渤海", "黄海", "东海", "南海",
         "黄河在山东东营注入渤海。"},
        {"世界上面积最大的大洋是？", "太平洋", "大西洋", "印度洋", "北冰洋",
         "太平洋面积约 1.8 亿平方公里，是最大的大洋。"},
        {"北京的气候类型是？", "温带季风气候", "亚热带季风气候", "热带季风气候", "温带大陆性气候",
         "北京夏季高温多雨、冬季寒冷干燥，属温带季风气候。"},
        {"下列哪个国家不与我国接壤？", "泰国", "越南", "老挝", "缅甸",
         "泰国与我国不接壤，中间隔着老挝和缅甸。"}
    };

    // ------------------------------------------------------------------
    // 英语（小学高年级）
    // ------------------------------------------------------------------

    private static final String[][] ENGLISH = {
        {"apple 的中文意思是？", "苹果", "香蕉", "橙子", "葡萄", "apple = 苹果"},
        {"library 的中文意思是？", "图书馆", "实验室", "体育馆", "食堂", "library = 图书馆"},
        {"dictionary 的中文意思是？", "字典", "日记本", "教科书", "练习册", "dictionary = 字典"},
        {"beautiful 的中文意思是？", "美丽的", "勇敢的", "安静的", "聪明的", "beautiful = 美丽的"},
        {"Monday 的中文意思是？", "星期一", "星期二", "星期日", "周末", "Monday = 星期一"},
        {"I ___ a student. 应该填？", "am", "is", "are", "be",
         "主语是 I，be 动词用 am。"},
        {"She ___ to school every day. 应该填？", "goes", "go", "going", "went",
         "第三人称单数，动词加 es。"},
        {"There ___ some books on the desk. 应该填？", "are", "is", "be", "was",
         "books 是复数，用 are。"},
        {"He is good ___ math. 应该填？", "at", "in", "on", "for",
         "固定搭配 be good at，擅长。"},
        {"I like ___ books. 应该填？", "reading", "read", "reads", "to reading",
         "like doing sth，喜欢做某事。"},
        {"child 的复数形式是？", "children", "childs", "childes", "child",
         "child 是不规则复数，变为 children。"},
        {"box 的复数形式是？", "boxes", "boxs", "boxies", "box",
         "以 x 结尾，加 es。"},
        {"go 的过去式是？", "went", "goed", "gone", "going",
         "go 是不规则动词，过去式 went。"},
        {"see 的过去式是？", "saw", "seen", "seed", "sawed",
         "see 的过去式 saw，过去分词 seen。"},
        {"run 的过去式是？", "ran", "runs", "running", "runned",
         "run 是不规则动词，过去式 ran。"},
        {"big 的比较级是？", "bigger", "biger", "more big", "biggest",
         "重读闭音节，双写末尾字母加 er。"},
        {"good 的比较级是？", "better", "gooder", "more good", "best",
         "good 是不规则比较，比较级 better，最高级 best。"},
        {"别人说 Thank you，最合适的回答是？", "You're welcome.", "Thank you too.", "No thanks.", "Yes, please.",
         "回应感谢用 You're welcome，不客气。"},
        {"What's your name? 的正确回答是？", "My name is Tom.", "I'm fine.", "He is Tom.", "Yes, I am.",
         "问名字，回答 My name is..."},
        {"表示「现在八点整」，正确的英语是？", "It's eight o'clock.", "It's eight hours.", "It's eight clock.", "It's at eight.",
         "整点用 o'clock。"}
    };

    // ------------------------------------------------------------------
    // 数学
    // ------------------------------------------------------------------

    private Question math(int grade) {
        int t = rnd.nextInt(10);
        if (t < 5) return arithmetic(grade);
        if (t < 8) return sequence(grade);
        return wordProblem(grade);
    }

    private Question pack(String stem, int answer, Set<Integer> extras, String explain) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(Integer.valueOf(answer));
        for (Integer d : extras) {
            if (list.size() >= 4) break;
            if (d != null && d.intValue() != answer && !list.contains(d)) list.add(d);
        }
        int guard = 0;
        while (list.size() < 4 && guard < 300) {
            int cand = answer + rnd.nextInt(21) - 10;
            if (cand != answer && !list.contains(Integer.valueOf(cand))) {
                list.add(Integer.valueOf(cand));
            }
            guard++;
        }
        Collections.shuffle(list, rnd);
        String[] opts = new String[list.size()];
        int idx = 0;
        for (int i = 0; i < list.size(); i++) {
            opts[i] = String.valueOf(list.get(i));
            if (list.get(i).intValue() == answer) idx = i;
        }
        return new Question(stem, opts, idx, explain);
    }

    private Set<Integer> mk(int... vs) {
        Set<Integer> s = new HashSet<Integer>();
        for (int i = 0; i < vs.length; i++) {
            if (vs[i] >= 0) s.add(Integer.valueOf(vs[i]));
        }
        return s;
    }

    private Question arithmetic(int grade) {
        if (grade <= 2) {
            if (rnd.nextBoolean()) {
                int a = 5 + rnd.nextInt(60);
                int b = 3 + rnd.nextInt(35);
                int ans = a + b;
                return pack(a + " + " + b + " = ?", ans, mk(ans - 1, ans + 1, ans - 10),
                        "先算 " + a + " + " + b + "，答案是 " + ans + "。");
            }
            int a = 20 + rnd.nextInt(70);
            int b = 3 + rnd.nextInt(Math.max(2, a - 5));
            int ans = a - b;
            return pack(a + " - " + b + " = ?", ans, mk(ans - 1, ans + 1, ans + 10),
                    "从 " + a + " 里去掉 " + b + "，剩下 " + ans + "。");
        }
        if (grade <= 4) {
            if (rnd.nextBoolean()) {
                int a = 2 + rnd.nextInt(8);
                int b = 2 + rnd.nextInt(8);
                int ans = a * b;
                return pack(a + " x " + b + " = ?", ans, mk(ans + a, ans - b, ans + 1),
                        "乘法口诀：" + a + " x " + b + " = " + ans + "。");
            }
            int b = 2 + rnd.nextInt(8);
            int ans = 2 + rnd.nextInt(9);
            int a = b * ans;
            return pack(a + " / " + b + " = ?", ans, mk(ans + 1, ans - 1, ans + b),
                    "想：" + b + " 乘几等于 " + a + "，答案是 " + ans + "。");
        }
        if (grade == 5) {
            int base = 20 * (1 + rnd.nextInt(10));
            int pct = 25 * (1 + rnd.nextInt(4));
            int ans = base * pct / 100;
            return pack(base + " 的 " + pct + "% 是多少？", ans, mk(ans + base / 10, ans - 5, ans + 5),
                    base + " x " + pct + "% = " + ans + "。");
        }
        // 六年级：百分数 / 四则混合 / 比例
        int k = rnd.nextInt(3);
        if (k == 0) {
            int base = 40 * (1 + rnd.nextInt(10));
            int pct = 5 * (1 + rnd.nextInt(19));
            int ans = base * pct / 100;
            return pack(base + " 的 " + pct + "% 是多少？", ans, mk(ans + 2, ans - 2, ans + base / 20),
                    base + " x " + pct + "% = " + ans + "。");
        }
        if (k == 1) {
            int a = 5 + rnd.nextInt(20);
            int b = 2 + rnd.nextInt(8);
            int c = 2 + rnd.nextInt(6);
            int ans = (a + b) * c;
            return pack("(" + a + " + " + b + ") x " + c + " = ?", ans, mk(a + b + c, ans - c, ans + c),
                    "先算括号：" + a + " + " + b + " = " + (a + b) + "，再乘 " + c + " 得 " + ans + "。");
        }
        int ratio = 2 + rnd.nextInt(5);
        int smaller = 5 * (1 + rnd.nextInt(9));
        int total = smaller * (1 + ratio);
        return pack("两个数的比是 1:" + ratio + "，两数之和是 " + total + "，较小的数是多少？",
                smaller, mk(smaller + ratio, total - smaller, smaller - 1),
                total + " / (" + 1 + " + " + ratio + ") = " + smaller + "。");
    }

    private Question sequence(int grade) {
        int t;
        if (grade >= 4) {
            t = rnd.nextInt(3);
        } else {
            t = rnd.nextBoolean() ? 0 : 2;
        }
        if (t == 0) {
            int step = 1 + rnd.nextInt(grade <= 2 ? 3 : 9);
            int start = 1 + rnd.nextInt(grade <= 2 ? 5 : 20);
            int s0 = start, s1 = start + step, s2 = start + 2 * step, s3 = start + 3 * step;
            int ans = start + 4 * step;
            return pack("每项加 " + step + "：" + s0 + ", " + s1 + ", " + s2 + ", " + s3 + "，下一项是多少？",
                    ans, mk(ans - 1, ans + 1, ans - step),
                    "相邻两项相差 " + step + "，" + s3 + " + " + step + " = " + ans + "。");
        }
        if (t == 1) {
            int start = 1 + rnd.nextInt(3);
            int a0 = start * start;
            int a1 = (start + 1) * (start + 1);
            int a2 = (start + 2) * (start + 2);
            int a3 = (start + 3) * (start + 3);
            int ans = (start + 4) * (start + 4);
            return pack(a0 + ", " + a1 + ", " + a2 + ", " + a3 + "，下一项是多少？",
                    ans, mk(ans - 2, ans + 3, a3 + 5),
                    "这是平方数：" + start + " 的平方、... 下一项是 " + (start + 4) + " 的平方 = " + ans + "。");
        }
        int base = 2 + rnd.nextInt(grade <= 2 ? 1 : 2);
        int b0 = 1, b1 = base, b2 = base * base, b3 = base * base * base;
        int ans = b3 * base;
        return pack(b0 + ", " + b1 + ", " + b2 + ", " + b3 + "，下一项是多少？",
                ans, mk(ans - 1, ans + base, b3 + b2),
                "每项是前一项的 " + base + " 倍：" + b3 + " x " + base + " = " + ans + "。");
    }

    private Question wordProblem(int grade) {
        if (rnd.nextBoolean()) {
            int a = 3 + rnd.nextInt(7);
            int b = 2 + rnd.nextInt(6);
            int ans = a * b;
            return pack("每盒有 " + a + " 支铅笔，买 " + b + " 盒，一共有多少支？",
                    ans, mk(ans + a, ans - b, ans + 1),
                    a + " x " + b + " = " + ans + "（支）。");
        }
        int per = 5 + rnd.nextInt(10);
        int box = 3 + rnd.nextInt(5);
        int total = per * box;
        int used = 2 + rnd.nextInt(per);
        int ans = total - used;
        return pack("一箱有 " + box + " 盒，每盒 " + per + " 个，用掉 " + used + " 个，还剩多少个？",
                ans, mk(total + used, ans + per, ans - 1),
                "一共 " + box + " x " + per + " = " + total + " 个，用掉 " + used + " 个，还剩 " + ans + " 个。");
    }
}
