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

    // Score weights
    double WEIGHT_CURRENT_SCORE = 1.0;
    double WEIGHT_COMBO_CARD_SCORE = 1.0;
    double WEIGHT_DUMPLING_CARD_SCORE = 1.0;
    double WEIGHT_WASABI_CARD_SCORE = 1.0;
    double WEIGHT_MAKI_CARD_SCORE = 1.0;
    double WEIGHT_PUDDING_CARD_SCORE = 1.0;

    // Normalization switch
    boolean SWITCH_NORMALIZATION = true;
    double NORMALIZE_Z        = 15.0;
    double SCORE_DIFF_NORM    = 10.0;


    // Probability of picking a card each round
    double pickProb = 1.0;

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
     * Overall idea: compute the score linearly
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

    // All evaluation methods only consider incremental effects

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
     * Balance between the number of required cards (need)
     * and the number of remaining picks (myPicksLeft)
     * Formula: score = card_type_score * F(card_type)
     * Tempura & Sashimi
     * if myPicksLeft/need >= 1 => F(card_type)=1
     * if myPicksLeft/need < 1 =>
     *                          AS Tempura: if need = 2, myPicksLeft < 2 => F(card_type)=0
     *                          AS Sashimi: if need = 3, myPicksLeft < 3 => F(card_type)=0
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
     * The incremental value of each new dumpling = [1, 2, 3, 4, 5]
     * n = current number of dumplings
     * P(a) = probability of getting more dumplings: min(5 - n, myPicksLeft * pickRate), max 5 dumplings
     * Formula: score = value(n + int(P(a))) + fractional(P(a)) * inc[n + int(P(a))]
     * Example:
     * n = 1, myPicksLeft = 5, pickRate = 0.5
     * a = min(5 - 1, 5 * 0.5) = 2.5
     * score = value(1 + 2) + 0.5 * inc[1 + 2] = 6 + 0.5 * 4 = 8
     *
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateDumplingScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * avg(Wasabi * Nigiri) = 1*5 + 2*10 + 3*5 / (5+10+5) = 2
     * n = number of Wasabi cards currently held
     * t = number of possible successful bindings = min(n, myPickLeft)
     * if t == 0, t++
     * Formula: score = (3 - 1) * avg * (t * pickRate)
     * (3 - 1) represents the multiplier gain since Nigiri already has a base score
     * Example:
     * n = 2, myPickLeft = 5, pickRate = 0.5
     * t = min(2, 5) = 2
     * score = (3 - 1) * avg * (2 * 0.5) = 2 * 2 * 1 = 4
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
     * myM = my total Maki count
     * [m1, m2, m3...m5] are opponents' Maki counts in descending order,
     * if 1st (myM >= m1 + 1)  -> score = 6
     * else if 2nd (m2 + 1 <= myM <= m1) -> score = 3
     * else -> score = 0
     * @param gs
     * @param playerId
     * @return
     */
    public double evaluateMakiScore(AbstractGameState gs, int playerId) {
        return 0;
    }

    /**
     * Since Pudding is a limited resource, the score must consider the round weight:
     * W(rounds) = {1:0.5, 2:0.8, 3:1.0}
     * myP = my pudding count
     * oppMin = opponent's minimum pudding count
     * oppMax = opponent's maximum pudding count
     * To ensure non-negative heuristic scores, the lowest pudding score = 0, middle = 1
     * For 2 players: Score = W(rounds) * 6 if myP > oppMax else 0
     * For more than 2 players:
     *     Score = W(rounds) * 6 if myP > oppMax
     *             else 0 if myP < oppMin
     *             else 1
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
     * lamp a value within [min, max]
     * @param value
     * @param min
     * @param max
     * @return
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }



}
