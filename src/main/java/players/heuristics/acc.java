//package players.heuristics;
//
//import core.AbstractGameState;
//import core.interfaces.IStateHeuristic;
//
//import java.util.Arrays;
//
///**
// * Sushi Go 强启发式（裁剪版）
// * 组件：S1 当前分差；S2 组合（Tempura+Sashimi）；S3 Dumpling；
// *      S4 Wasabi×Nigiri（只算额外乘数）；S5 Maki；S6 Pudding（仅此项用轮次权重）
// *
// * 使用适配层 Extractor 把游戏状态抽象成所需的计数/参数，避免直接依赖具体 GameState 的字段名/方法名。
// */
//public class aac implements IStateHeuristic {
//
//    /* ==========================
//     *  权重（总分线性合成系数）
//     * ========================== */
//    protected double W_CUR = 1.0;   // S1
//    protected double W_COMBO = 1.0; // S2
//    protected double W_DUMP = 1.0;  // S3
//    protected double W_WASA = 1.0;  // S4
//    protected double W_MAKI = 1.0;  // S5
//    protected double W_PUDD = 1.0;  // S6
//
//    /* ==========================
//     *  归一化/缩放常量
//     * ========================== */
//    protected double SCORE_DIFF_NORM = 10.0; // S1 分差缩放
//    protected double NORMALIZE_Z     = 15.0; // 最终 tanh 缩放
//
//    /* ==========================
//     *  简化概率/可达性系数
//     * ========================== */
//    protected double RHO_D = 0.35;  // Dumpling 可得性
//    protected double RHO_N = 0.40;  // Nigiri 可得性（Wasabi 绑定）
//
//    /* ==========================
//     *  其他语义常量
//     * ========================== */
//    protected double MU_TEMPURA_START = 0.30;  // Tempura 未启动时的弱潜力（仅 myPicksLeft>=2 时计）
//    protected double V_NIGIRI_MEAN    = 2.0;   // Nigiri 平均面值（蛋/鲑/鱿的经验均值）
//
//    /* ==========================
//     *  适配器：把 GameState → 我们需要的数字
//     * ========================== */
//    protected final Extractor extractor;
//
//    /* ==========================
//     *  构造函数
//     * ========================== */
//
//    /** 默认构造：使用空实现的 Extractor（所有值为 0，不会崩），方便你先接线再替换。 */
//    public SGHeuristic_XT() {
//        this(new DummyExtractor());
//    }
//
//    /** 传入你自己的 Extractor 实现 */
//    public SGHeuristic_XT(Extractor extractor) {
//        this.extractor = extractor;
//    }
//
//    /** 全参数构造（可一次性调整所有权重/常量和适配器） */
//    public SGHeuristic_XT(Extractor extractor,
//                          double wCur, double wCombo, double wDump, double wWasa, double wMaki, double wPudd,
//                          double scoreDiffNorm, double normalizeZ,
//                          double rhoD, double rhoN,
//                          double muTempuraStart, double vNigiriMean) {
//        this.extractor = extractor;
//        this.W_CUR = wCur;
//        this.W_COMBO = wCombo;
//        this.W_DUMP = wDump;
//        this.W_WASA = wWasa;
//        this.W_MAKI = wMaki;
//        this.W_PUDD = wPudd;
//        this.SCORE_DIFF_NORM = scoreDiffNorm;
//        this.NORMALIZE_Z = normalizeZ;
//        this.RHO_D = rhoD;
//        this.RHO_N = rhoN;
//        this.MU_TEMPURA_START = muTempuraStart;
//        this.V_NIGIRI_MEAN = vNigiriMean;
//    }
//
//    /* ==========================
//     *  IStateHeuristic 接口实现
//     * ========================== */
//    @Override
//    public double evaluateState(AbstractGameState gs, int playerId) {
//        // 1) 计算 6 个组件
//        double s1 = evaluateCurrentScore(gs, playerId);
//        double s2 = evaluateComboCard(gs, playerId);
//        double s3 = evaluateDumplingCard(gs, playerId);
//        double s4 = evaluateWasabiCard(gs, playerId);
//        double s5 = evaluateMakiCard(gs, playerId);
//        double s6 = evaluatePudding(gs, playerId);
//
//        // 防御式：避免 NaN 传播
//        if (!Double.isFinite(s1)) s1 = 0;
//        if (!Double.isFinite(s2)) s2 = 0;
//        if (!Double.isFinite(s3)) s3 = 0;
//        if (!Double.isFinite(s4)) s4 = 0;
//        if (!Double.isFinite(s5)) s5 = 0;
//        if (!Double.isFinite(s6)) s6 = 0;
//
//        // 2) 线性合成
//        double raw = W_CUR * s1
//                + W_COMBO * s2
//                + W_DUMP * s3
//                + W_WASA * s4
//                + W_MAKI * s5
//                + W_PUDD * s6;
//
//        // 3) 归一化到 [-1, 1]（可换 clamp(raw/Z, -1, 1)）
//        return Math.tanh(raw / NORMALIZE_Z);
//    }
//
//    /* ==========================
//     *  各组件的实现
//     * ========================== */
//
//    /** S1：当前分差（线性缩放） */
//    public double evaluateCurrentScore(AbstractGameState gs, int playerId) {
//        double myScore = extractor.getPlayerScore(gs, playerId);
//        double avgOpp = extractor.getAverageOppScore(gs, playerId);
//        return (myScore - avgOpp) / SCORE_DIFF_NORM;
//    }
//
//    /** S2：组合潜力（Tempura + Sashimi） */
//    public double evaluateComboCard(AbstractGameState gs, int playerId) {
//        int picks = extractor.getMyPicksLeft(gs, playerId);
//
//        // ---- Tempura ----
//        int tempura = clampInt(extractor.getTempuraCount(gs, playerId), 0, 20);
//        int needT = (tempura % 2 == 1) ? 1 : 2;
//
//        double fT = (needT == 0) ? 0.0 : (picks * 1.0 / needT);
//        fT = clamp(fT, 0, 1); // 护栏
//
//        double EV_T;
//        if (needT == 1) {
//            EV_T = extractor.getValueTempuraPair(gs) * fT;  // 主要潜力
//        } else {
//            // 未启动：只在还剩至少 2 手时，给弱潜力
//            EV_T = (picks >= 2 ? extractor.getValueTempuraPair(gs) * MU_TEMPURA_START * fT : 0.0);
//        }
//
//        // ---- Sashimi ----
//        int sashimi = clampInt(extractor.getSashimiCount(gs, playerId), 0, 20);
//        int rS = sashimi % 3;
//        int needS = (rS == 0 ? 3 : 3 - rS);
//
//        double EV_S = 0.0;
//        if (picks >= needS) {
//            double fS = picks * 1.0 / needS;
//            fS = clamp(fS, 0, 1); // 护栏
//            EV_S = extractor.getValueSashimiTriple(gs) * fS;
//        }
//
//        return EV_T + EV_S;
//    }
//
//    /** S3：Dumpling（连续阶梯插值） */
//    public double evaluateDumplingCard(AbstractGameState gs, int playerId) {
//        int k = clampInt(extractor.getDumplingCount(gs, playerId), 0, 5); // 最大只计到 5
//        int picks = extractor.getMyPicksLeft(gs, playerId);
//
//        double ED = Math.min(5 - k, RHO_D * picks);
//        ED = clamp(ED, 0, 5 - k); // 护栏
//
//        int m = (int) Math.floor(ED);
//        double u = ED - m;
//
//        // inc 序列：第 (k+1) 张到第 5 张各自的边际增量
//        int[] inc = extractor.getValueDumplingInc(gs); // 期望 [1,2,3,4,5]
//
//        double sum = 0.0;
//        for (int j = 0; j < m; j++) {
//            int idx = Math.min(4, k + j); // 护栏
//            sum += inc[idx];
//        }
//        if (u > 0) {
//            int idx = Math.min(4, k + m); // 护栏
//            sum += u * inc[idx];
//        }
//        return sum;
//    }
//
//    /** S4：Wasabi × Nigiri（只算 Wasabi 的额外乘数收益） */
//    public double evaluateWasabiCard(AbstractGameState gs, int playerId) {
//        int picks = extractor.getMyPicksLeft(gs, playerId);
//        int a = Math.min(clampInt(extractor.getWasabiActive(gs, playerId), 0, 10), picks); // 最多绑定次数
//        double bindExp = a * RHO_N;
//        bindExp = clamp(bindExp, 0, a); // 护栏：期望 ∈ [0, a]
//
//        int mult = extractor.getMultiplierWasabi(gs); // 通常为 3
//        return (mult - 1) * V_NIGIRI_MEAN * bindExp;
//    }
//
//    /** S5：Maki（2–5 人通用，上界判断） */
//    public double evaluateMakiCard(AbstractGameState gs, int playerId) {
//        int N = extractor.getNPlayers(gs);
//        int picks = extractor.getMyPicksLeft(gs, playerId);
//        int myM = clampInt(extractor.getMakiIcons(gs, playerId), 0, 100);
//
//        int[] oppSorted = extractor.getOpponentsMakiSortedDesc(gs, playerId); // 长度 N-1，降序
//        int m1 = oppSorted.length > 0 ? oppSorted[0] : Integer.MIN_VALUE;
//        int m2 = oppSorted.length > 1 ? oppSorted[1] : Integer.MIN_VALUE;
//
//        int Mmax = 3 * Math.max(0, picks); // 我还能增加的上界
//        int need1 = Math.max(0, (m1 == Integer.MIN_VALUE ? 0 : (m1 + 1 - myM)));
//
//        int T1 = extractor.getValueMakiMost(gs);
//        int T2 = extractor.getValueMakiSecond(gs);
//
//        if (Mmax >= need1) {
//            return T1; // 有机会拿第一
//        } else {
//            if (N <= 2) {
//                return T2; // 两人局：拿不到第一 ⇒ 第二
//            } else {
//                int need2 = Math.max(0, (m2 == Integer.MIN_VALUE ? 0 : (m2 + 1 - myM)));
//                return (Mmax >= need2) ? T2 : 0.0;
//            }
//        }
//    }
//
//    /** S6：Pudding（仅此项用轮次权重） */
//    public double evaluatePudding(AbstractGameState gs, int playerId) {
//        int N = extractor.getNPlayers(gs);
//        int round = extractor.getRoundIndex(gs); // 1/2/3
//
//        double lambda;
//        switch (round) {
//            case 1: default: lambda = 0.5; break;
//            case 2: lambda = 0.8; break;
//            case 3: lambda = 1.0; break;
//        }
//
//        int myP = extractor.getPuddingCount(gs, playerId);
//        int oppMin = extractor.getOppPuddingMin(gs, playerId);
//        int oppMax = extractor.getOppPuddingMax(gs, playerId);
//
//        int Vmax = extractor.getValuePuddingMost(gs);  // 通常 +6
//        int Vmin = extractor.getValuePuddingLeast(gs); // 通常 -6（N>=3 才会用到）
//
//        if (N <= 2) {
//            // 两人局只有最多 +6，没有 -6 惩罚
//            return (myP > oppMax) ? (Vmax * lambda) : 0.0;
//        } else {
//            if (myP >= oppMax + 1) return Vmax * lambda;  // 确定最多
//            if (myP <= oppMin - 1) return Vmin * lambda;  // 确定最少
//            return 0.0;
//        }
//    }
//
//    /* ==========================
//     *  工具方法
//     * ========================== */
//    private double clamp(double value, double min, double max) {
//        if (min > max) { double t = min; min = max; max = t; }
//        if (value < min) return min;
//        if (value > max) return max;
//        return value;
//    }
//    private int clampInt(int v, int min, int max) {
//        if (min > max) { int t = min; min = max; max = t; }
//        return Math.max(min, Math.min(max, v));
//    }
//
//    /* =======================================================================
//     *  适配接口：把具体 GameState 的取数逻辑封装起来（由你来实现具体映射）
//     * ======================================================================= */
//    public interface Extractor {
//
//        // 基本信息
//        int getNPlayers(AbstractGameState gs);
//        int getRoundIndex(AbstractGameState gs); // 1/2/3
//
//        // 分数
//        double getPlayerScore(AbstractGameState gs, int playerId);
//        double getAverageOppScore(AbstractGameState gs, int playerId);
//
//        // 我的剩余可选次数（建议＝当前手牌张数）
//        int getMyPicksLeft(AbstractGameState gs, int playerId);
//
//        // 牌型计数（我方）
//        int getTempuraCount(AbstractGameState gs, int playerId);
//        int getSashimiCount(AbstractGameState gs, int playerId);
//        int getDumplingCount(AbstractGameState gs, int playerId);
//        int getMakiIcons(AbstractGameState gs, int playerId);
//        int getPuddingCount(AbstractGameState gs, int playerId);
//        int getWasabiActive(AbstractGameState gs, int playerId);
//
//        // 对手 Maki 情况（降序，长度 N-1）
//        int[] getOpponentsMakiSortedDesc(AbstractGameState gs, int playerId);
//
//        // 对手 Pudding 极值
//        int getOppPuddingMin(AbstractGameState gs, int playerId);
//        int getOppPuddingMax(AbstractGameState gs, int playerId);
//
//        // 规则参数（通常来自 SGParameters）
//        int getValueMakiMost(AbstractGameState gs);        // T1
//        int getValueMakiSecond(AbstractGameState gs);      // T2
//        int getValueTempuraPair(AbstractGameState gs);     // 5
//        int getValueSashimiTriple(AbstractGameState gs);   // 10
//        int[] getValueDumplingInc(AbstractGameState gs);   // [1,2,3,4,5]
//        int getMultiplierWasabi(AbstractGameState gs);     // 3
//        int getValuePuddingMost(AbstractGameState gs);     // +6
//        int getValuePuddingLeast(AbstractGameState gs);    // -6
//    }
//
//    /** 一个“空实现”方便你先接线；所有值为 0 或合理缺省，不会 NPE。 */
//    public static class DummyExtractor implements Extractor {
//        @Override public int getNPlayers(AbstractGameState gs) { return 2; }
//        @Override public int getRoundIndex(AbstractGameState gs) { return 1; }
//        @Override public double getPlayerScore(AbstractGameState gs, int playerId) { return 0; }
//        @Override public double getAverageOppScore(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getMyPicksLeft(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getTempuraCount(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getSashimiCount(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getDumplingCount(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getMakiIcons(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getPuddingCount(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getWasabiActive(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int[] getOpponentsMakiSortedDesc(AbstractGameState gs, int playerId) { return new int[]{0}; }
//        @Override public int getOppPuddingMin(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getOppPuddingMax(AbstractGameState gs, int playerId) { return 0; }
//        @Override public int getValueMakiMost(AbstractGameState gs) { return 6; }
//        @Override public int getValueMakiSecond(AbstractGameState gs) { return 3; }
//        @Override public int getValueTempuraPair(AbstractGameState gs) { return 5; }
//        @Override public int getValueSashimiTriple(AbstractGameState gs) { return 10; }
//        @Override public int[] getValueDumplingInc(AbstractGameState gs) { return new int[]{1,2,3,4,5}; }
//        @Override public int getMultiplierWasabi(AbstractGameState gs) { return 3; }
//        @Override public int getValuePuddingMost(AbstractGameState gs) { return 6; }
//        @Override public int getValuePuddingLeast(AbstractGameState gs) { return -6; }
//    }
//}