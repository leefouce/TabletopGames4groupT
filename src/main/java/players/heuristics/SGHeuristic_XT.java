package players.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;


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


public class SGHeuristic_XT extends TunableParameters<Object> implements IStateHeuristic {

    // Score weights
    public double WEIGHT_CURRENT_SCORE = 1.0;
    public double WEIGHT_COMBO_CARD_SCORE = 1.0;
    public double WEIGHT_DUMPLING_CARD_SCORE = 1.0;
    public double WEIGHT_WASABI_CARD_SCORE = 1.0;
    public double WEIGHT_MAKI_CARD_SCORE = 1.0;
    public double WEIGHT_PUDDING_CARD_SCORE = 1.0;

    // Normalization switch
    public boolean SWITCH_NORMALIZATION = true;
    public double NORMALIZE_Z        = 15.0;
    public double SCORE_DIFF_NORM    = 10.0;

    // card constant
    public double NIGIRI_MEAN_SCORE = 2.0;


    // Probability of picking a card each round
    public double PICKED_PROBABILITY = 1.0;

    /**
     * Constructor: Initial parameters
     */
    public SGHeuristic_XT() {
        addTunableParameter("WEIGHT_CURRENT_SCORE", WEIGHT_CURRENT_SCORE);
        addTunableParameter("WEIGHT_COMBO_CARD_SCORE", WEIGHT_COMBO_CARD_SCORE);
        addTunableParameter("WEIGHT_DUMPLING_CARD_SCORE", WEIGHT_DUMPLING_CARD_SCORE);
        addTunableParameter("WEIGHT_WASABI_CARD_SCORE", WEIGHT_WASABI_CARD_SCORE);
        addTunableParameter("WEIGHT_MAKI_CARD_SCORE", WEIGHT_MAKI_CARD_SCORE);
        addTunableParameter("WEIGHT_PUDDING_CARD_SCORE", WEIGHT_PUDDING_CARD_SCORE);
        addTunableParameter("SWITCH_NORMALIZATION", SWITCH_NORMALIZATION);
        addTunableParameter("NORMALIZE_Z", NORMALIZE_Z);
        addTunableParameter("SCORE_DIFF_NORM", SCORE_DIFF_NORM);
        addTunableParameter("NIGIRI_MEAN_SCORE", NIGIRI_MEAN_SCORE);
        addTunableParameter("PICKED_PROBABILITY", PICKED_PROBABILITY);
    }


    // implement Interface(ITunableParameters)
    @Override
    public AbstractParameters _copy() {
        SGHeuristic_XT retValue = new SGHeuristic_XT();
        retValue.WEIGHT_CURRENT_SCORE = WEIGHT_CURRENT_SCORE;
        retValue.WEIGHT_COMBO_CARD_SCORE = WEIGHT_COMBO_CARD_SCORE;
        retValue.WEIGHT_DUMPLING_CARD_SCORE = WEIGHT_DUMPLING_CARD_SCORE;
        retValue.WEIGHT_WASABI_CARD_SCORE = WEIGHT_WASABI_CARD_SCORE;
        retValue.WEIGHT_MAKI_CARD_SCORE = WEIGHT_MAKI_CARD_SCORE;
        retValue.WEIGHT_PUDDING_CARD_SCORE = WEIGHT_PUDDING_CARD_SCORE;
        retValue.SWITCH_NORMALIZATION = SWITCH_NORMALIZATION;
        retValue.NORMALIZE_Z = NORMALIZE_Z;
        retValue.SCORE_DIFF_NORM = SCORE_DIFF_NORM;
        retValue.NIGIRI_MEAN_SCORE = NIGIRI_MEAN_SCORE;
        retValue.PICKED_PROBABILITY = PICKED_PROBABILITY;

        return retValue;
    }

    @Override
    public boolean _equals(Object o) {
        if (o instanceof SGHeuristic_XT) {
            SGHeuristic_XT other = (SGHeuristic_XT) o;
            return other.WEIGHT_CURRENT_SCORE == WEIGHT_CURRENT_SCORE
                    && other.WEIGHT_COMBO_CARD_SCORE == WEIGHT_COMBO_CARD_SCORE
                    && other.WEIGHT_DUMPLING_CARD_SCORE == WEIGHT_DUMPLING_CARD_SCORE
                    && other.WEIGHT_WASABI_CARD_SCORE == WEIGHT_WASABI_CARD_SCORE
                    && other.WEIGHT_MAKI_CARD_SCORE == WEIGHT_MAKI_CARD_SCORE
                    && other.WEIGHT_PUDDING_CARD_SCORE == WEIGHT_PUDDING_CARD_SCORE
                    && other.SWITCH_NORMALIZATION == SWITCH_NORMALIZATION
                    && other.NORMALIZE_Z == NORMALIZE_Z
                    && other.SCORE_DIFF_NORM == SCORE_DIFF_NORM
                    && other.NIGIRI_MEAN_SCORE == NIGIRI_MEAN_SCORE
                    && other.PICKED_PROBABILITY == PICKED_PROBABILITY;
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
        WEIGHT_CURRENT_SCORE      = getDoubleParam("WEIGHT_CURRENT_SCORE", WEIGHT_CURRENT_SCORE);
        WEIGHT_COMBO_CARD_SCORE   = getDoubleParam("WEIGHT_COMBO_CARD_SCORE", WEIGHT_COMBO_CARD_SCORE);
        WEIGHT_DUMPLING_CARD_SCORE= getDoubleParam("WEIGHT_DUMPLING_CARD_SCORE", WEIGHT_DUMPLING_CARD_SCORE);
        WEIGHT_WASABI_CARD_SCORE  = getDoubleParam("WEIGHT_WASABI_CARD_SCORE", WEIGHT_WASABI_CARD_SCORE);
        WEIGHT_MAKI_CARD_SCORE    = getDoubleParam("WEIGHT_MAKI_CARD_SCORE", WEIGHT_MAKI_CARD_SCORE);
        WEIGHT_PUDDING_CARD_SCORE = getDoubleParam("WEIGHT_PUDDING_CARD_SCORE", WEIGHT_PUDDING_CARD_SCORE);

        SWITCH_NORMALIZATION = getBoolParam("SWITCH_NORMALIZATION", SWITCH_NORMALIZATION);
        NORMALIZE_Z          = getDoubleParam("NORMALIZE_Z", NORMALIZE_Z);
        SCORE_DIFF_NORM      = getDoubleParam("SCORE_DIFF_NORM", SCORE_DIFF_NORM);
        NIGIRI_MEAN_SCORE    = getDoubleParam("NIGIRI_MEAN_SCORE", NIGIRI_MEAN_SCORE);
        PICKED_PROBABILITY   = getDoubleParam("PICKED_PROBABILITY", PICKED_PROBABILITY);
    }

    private double getDoubleParam(String key, double fallback) {
        Object v = getParameterValue(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); } catch (Exception ignore) {}
        }
        return fallback;
    }

    private boolean getBoolParam(String key, boolean fallback) {
        Object v = getParameterValue(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        if (v instanceof Number) return ((Number) v).doubleValue() != 0.0;
        return fallback;
    }

    /**
     * Overall idea: compute the score linearly
     * Score = W1*S1 + W2*S2 + W3*S3 + W4*S4 + W5*S5 + W6*S6
     * W = [ WEIGHT_CURRENT_SCORE, WEIGHT_COMBO_CARD_SCORE, WEIGHT_DUMPLING_CARD_SCORE,
     *      WEIGHT_WASABI_CARD_SCORE, WEIGHT_MAKI_CARD_SCORE, WEIGHT_PUDDING_CARD_SCORE ]
     * S = [ evaluateCurrentScore, evaluateComboScore, evaluateDumplingScore,
     *      evaluateWasabiScore, evaluateMakiScore, evaluatePuddingScore ]
     *
     * TAG parameters docs: https://tabletopgames.ai/wiki/running/ParameterSearch
     *
     * @param gs - game state to evaluate and score.
     * @param playerId - id of the player we're evaluating the game for.
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        SGGameState sgState = (SGGameState) gs;
        SGParameters sgParams = (SGParameters) sgState.getGameParameters();
        int myPickLeft = sgState.getPlayerHands().get(playerId).getSize();


        // calculate all score
        double S1 = evaluateCurrentScore(sgState, playerId);
        double S2 = evaluateComboScore(sgState, playerId, myPickLeft);
        double S3 = evaluateDumplingScore(sgState, sgParams, playerId, myPickLeft);
        double S4 = evaluateWasabiScore(sgState, sgParams, playerId);
        double S5 = evaluateMakiScore(sgState, sgParams, playerId);
        double S6 = evaluatePuddingScore(sgState, sgParams, playerId);

        // final score
        double finalScore = WEIGHT_CURRENT_SCORE * S1
                + WEIGHT_COMBO_CARD_SCORE * S2
                + WEIGHT_DUMPLING_CARD_SCORE * S3
                + WEIGHT_WASABI_CARD_SCORE * S4
                + WEIGHT_MAKI_CARD_SCORE * S5
                + WEIGHT_PUDDING_CARD_SCORE * S6;

        if(SWITCH_NORMALIZATION)
            return Math.tanh(finalScore / NORMALIZE_Z);
        else
            return  finalScore;

    }

    // All evaluation methods only consider incremental effects

    /**
     * calculate score diff
     * currScore = (myScore - oppScore) / norm
     *
     * @param state
     * @param playerId
     * @return
     */
    public double evaluateCurrentScore(SGGameState state, int playerId) {
        double myScore = state.getGameScore(playerId);
        int nPlayers = state.getNPlayers();
        double sum = 0.0;

        for (int i = 0; i < nPlayers; i++) {
            if (i != playerId) {
                double playerScore = state.getPlayerScore()[i].getValue();
                sum += playerScore;
            }
        }
        double oppoAvgScore = sum / (nPlayers - 1);
        return (myScore - oppoAvgScore) / SCORE_DIFF_NORM;
    }


    /**
     * Balance between the number of required cards (need)
     * and the number of remaining picks (myPicksLeft)
     * Formula: score = card_type_score * F(card_type)
     * Tempura & Sashimi
     * if myPicksLeft >= need => F(card_type)=1
     * if myPicksLeft < need => myPicksLeft / need
     *
     * @param state
     * @param playerId
     * @param myPickLeft
     * @return
     */
    public double evaluateComboScore(SGGameState state, int playerId, int myPickLeft) {

        double socreT = 0.0;
        double socreS = 0.0;

        // ----------- Handle Tempura ------------

        int nTempura = state.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerId).getValue();
        int needT = (nTempura % 2 == 0) ? 2 : 1;
        socreT = (myPickLeft >= needT) ? 1.0 : (double) (myPickLeft / needT);

        // ----------- Handle Sashimi ------------
        int nSashimi = state.getPlayedCardTypes(SGCard.SGCardType.Sashimi, playerId).getValue();
        int tmp = nSashimi % 3;
        int needS = (tmp == 0) ? 3 : 3 - tmp;
        socreS = (myPickLeft >= needS) ? 1.0 : (double) (myPickLeft / needS);

        return socreT + socreS;
    }

    /**
     * value(num_dumpling) = [0, 1, 3, 6, 10, 15]
     * The incremental value of each new dumpling = [1, 2, 3, 4, 5]
     * n = current number of dumplings
     * P(a) = probability of getting more dumplings: min(5 - n, myPicksLeft * pickRate), max 5 dumplings
     * Formula: score = sum(value(n ~ n + int(P(a)))) + fractional(P(a)) * inc[n + int(P(a))]
     * Example:
     * n = 1, myPicksLeft = 5, pickRate = 0.5
     * a = min(5 - 1, 5 * 0.5) = 2.5
     * score = sum(value(n ~ 1 + 2)) + 0.5 * inc[1 + 2] = 6 + 0.5 * 4 = 8
     *
     * @param state
     * @param params
     * @param playerId
     * @param myPickLeft
     * @return
     */
    public double evaluateDumplingScore(SGGameState state, SGParameters params, int playerId, int myPickLeft) {
        int nDumpling = state.getPlayedCardTypes(SGCard.SGCardType.Dumpling, playerId).getValue();
        // probability of getting more dumplings
        double pDumping = Math.min(params.valueDumpling.length - nDumpling, PICKED_PROBABILITY * myPickLeft);
        // nDumpling over 5 we dont pick anymore
        if (pDumping < 0)
            return 0.0;
        // just hardcode this for now
        int[] inc = new int[] {1, 2, 3, 4, 5};

        // Get the integer and fractional parts of pDumping
        int intP = (int) Math.floor(pDumping);
        double fracP = pDumping % 1;

        double score = 0.0;
        for (int j = 0; j < intP; j++) {
            int idx = Math.min(4, nDumpling + j); // limitation is 4
            score += inc[idx];
        }
        if (fracP > 0.0) {
            int idx = Math.min(4, nDumpling + intP);
            score += fracP * inc[idx];
        }
        return score;
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
     * @param state
     * @param params
     * @param playerId
     * @return
     */
    public double evaluateWasabiScore(SGGameState state, SGParameters params, int playerId) {
        return 0;
    }

    /**
     * myM = my total Maki count
     * [m1, m2, m3...m5] are opponents' Maki counts in descending order,
     * if 1st (myM >= m1 + 1)  -> score = 6
     * else if 2nd (m2 + 1 <= myM <= m1) -> score = 3
     * else -> score = 0
     *
     * @param state
     * @param params
     * @param playerId
     * @return
     */
    public double evaluateMakiScore(SGGameState state, SGParameters params, int playerId) {
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
     * @param state
     * @param params
     * @param playerId
     * @return
     */
    public double evaluatePuddingScore(SGGameState state, SGParameters params, int playerId) {
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