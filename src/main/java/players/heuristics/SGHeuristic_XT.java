package players.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;


/*
 * ----------------------------- CARD RULE -----------------------------------
 *         put(new Pair<>(SGCard.SGCardType.Maki, 3), 12);
 *         put(new Pair<>(SGCard.SGCardType.Maki, 2), 8);
 *         put(new Pair<>(SGCard.SGCardType.Maki, 1), 6);
 *         put(new Pair<>(SGCard.SGCardType.Chopsticks, 1), 4);
 *         put(new Pair<>(SGCard.SGCardType.Tempura, 1), 14);
 *         put(new Pair<>(SGCard.SGCardType.Sashimi, 1), 14);
 *         put(new Pair<>(SGCard.SGCardType.Dumpling, 1), 14);
 *         put(new Pair<>(SGCard.SGCardType.SquidNigiri, 1), 5);
 *         put(new Pair<>(SGCard.SGCardType.SalmonNigiri, 1), 10);
 *         put(new Pair<>(SGCard.SGCardType.EggNigiri, 1), 5);
 *         put(new Pair<>(SGCard.SGCardType.Wasabi, 1), 6);
 *         put(new Pair<>(SGCard.SGCardType.Pudding, 1), 10);
 * ----------------------------- VALUE RULE -----------------------------------
 *         public int valueMakiMost = 6;
 *         public int valueMakiSecond = 3;
 *         public int valueTempuraPair = 5;
 *         public int valueSashimiTriple = 10;
 *         public int[] valueDumpling = new int[] {1, 3, 6, 10, 15};
 *         public int valueSquidNigiri = 3;
 *         public int valueSalmonNigiri = 2;
 *         public int valueEggNigiri = 1;
 *         public int multiplierWasabi = 3;
 *         public int valuePuddingMost = 6;
 *         public int valuePuddingLeast = -6;
 */




public class SGHeuristic_XT extends TunableParameters implements IStateHeuristic {

    // 比分权重
    double WEIGHT_CURRENT_SCORE = 1.0;
    double WEIGHT_COMBO_CARD_SCORE = 1.0;
    double WEIGHT_DUMPLING_CARD_SCORE = 1.0;
    double WEIGHT_WASABI_CARD_SCORE = 1.0;
    double WEIGHT_MAKI_CARD_SCORE = 1.0;
    double WEIGHT_PUDDING_CARD_SCORE = 1.0;

    // 归一化开关
    boolean SWITCH_NORMALIZATION = true;
    double NORMALIZE_Z        = 15.0;
    double SCORE_DIFF_NORM    = 10.0;


    // 每轮能拿卡的概率
    double pickRate = 1.0;

    /**
     * Constructor: Initial parameters
     */
    public SGHeuristic_XT() {
        super();
    }


    // implement Interface(ITunableParameters)
    @Override
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public Object instantiate() {
        return null;
    }

    @Override
    public void _reset() {

    }



    /**
     * 整体思路：线性计算比分
     * Score = W1*S1 + W2*S2 + W3*S3 + W4*S4 + W5*S5 + W6*S6
     * W = [ WEIGHT_CURRENT_SCORE, WEIGHT_COMBO_CARD_SCORE, WEIGHT_DUMPLING_CARD_SCORE,
     *      WEIGHT_WASABI_CARD_SCORE, WEIGHT_MAKI_CARD_SCORE, WEIGHT_PUDDING_CARD_SCORE ]
     * S = [ evaluateCurrentScore, evaluateComboScore, evaluateDumplingScore,
     *      evaluateWasabiScore, evaluateMakiScore, evaluatePuddingScore ]
     *
     * @param gs - game state to evaluate and score.
     * @param playerId - id of the player we're evaluating the game for.
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return 0;
    }

    // 所有的evaluate都只考虑增量

    /**
     * CurrScore = (myScore - oppScore) / norm
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateCurrentScore(AbstractGameState gs, int playerId) {
        return 0;
    }


    /**
     * 平衡需要的卡牌数量（need），以及剩下还能选择手牌(myPicksLeft)
     * formula: score = card_type_score * F(card_type)
     * Tempura & Sashimi
     * myPicksLeft/need > =1 => F(card_type)=1
     * myPicksLeft/need < 1 =>
     *                          For Tempura: IF need = 2, myPickLeft<2 => F(card_type)=0
     *                          For Sashimi: IF need = 3, myPickLeft<3 => F(card_type)=0
     *                          ELSE: F(card_type) = myPicksLeft/need
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateComboScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * value(num_dumpling) = [0, 1, 3, 6, 10, 15]
     * 每增加一张dumpling的价值 inc = [1, 2, 3, 4, 5]
     * n = 已有饺子
     * P(a) = 能够拿饺子的概率：min(5 - n, myPicksLeft * pickRate), 超过5张无意义
     * formula: score = value(n + P(a)整数部分)+ P(a)小数部分 * inc[n + P(a)整数部分]
     * e.g
     * n = 1, myPicksLeft = 5, pickRate = 0.5
     * a = min( 5-1, 5*0.5) = 2.5
     * score = value(1+2) + 0.5 * inc[1+2] = 6 + 0.5*4 = 8
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateDumplingScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * avg(Wasabi * Nigiri) = 1*5 + 2*10 + 3*5 / 5+10+5 = 2
     * n(Wasabi 已有的量)
     * t(可以绑定成功的次数) = min(n, myPickLeft) 最多只选择w次
     * if t==0,t++
     * formula: score = (3-1) * avg * (t * pickRate)  3-1: 表示增量，因为单独打出Nigiri也有分数
     * e.g.
     * n=2, myPickLeft pickRate = 0.5
     * t=min(2,5)=2
     * score = (3-1) * avg * (2 * 0.5) = 2*2*1 =4
     *
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateWasabiScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * myM(我的maki数量)
     * [m1,m2,m3...m5] 对手的maki数量 m1最大 m5最小，
     * if 1st(myM >= m1 + 1)  score =  6
     * else if 2nd( m2 + 1<= myM <= m1)  score = 3
     * else score = 0
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateMakiScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * 因为Pudding属于有限资源，所以要考虑每轮的权重 W(rounds) {1:0.5, 2:0.8, 3:1.0}
     * myP(我的pudding数量)
     * oppMin(对手最小数量)，oppMax(对手最大数量)
     * 为了保证不出现heuristic出现负数的情况，最少pudding得分为0，中间为1
     * players = 2 : Score = W(rounds) * 6 if myP > oppMax else 0
     *players > 2: Score =  W(rounds) * 6 if myP >  oppMax elif myP < oppMin 0 else 1
     *
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluatePuddingScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * 保证数据落在[min, max]区间
     * @param value
     * @param min
     * @param max
     * @return
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }



}
