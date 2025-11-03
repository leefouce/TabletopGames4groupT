package players.GroupT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.AbstractPlayer;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A single candidate solution (action sequence) for the RHEA player.
 */
public class RHEAIndividualT implements Comparable<RHEAIndividualT> {

    // ----------------------------
    // HARDCODED hyper-parameters
    // ----------------------------
    // Seeding ratios for initPopulation(...) – % of population allocated to each seed type
    private static final double SEED_GREEDY_RATIO   = 0.20; // HARDCODED: 20% individuals via 1-step greedy OSLA
    private static final double SEED_ROLLOUT_RATIO  = 0.20; // HARDCODED: 20% individuals via random rollouts
    // Remaining 60% are purely random (diversity)

    // Elite carryover on shift&repair – fraction of population kept as elites after reuse
    private static final double ELITE_RATIO         = 0.15; // HARDCODED: keep top 15% after shift-and-repair

    // Tournament selection size for evolveOneGeneration(...) (optional; ignore if you have your own selection)
    private static final int TOURNAMENT_SIZE        = 3;    // HARDCODED: 3-way tournament selection

    // Mutation controls
    private static final double MUT_SWAP_PROB       = 0.20; // HARDCODED: 20% use adjacent-swap (light tweak)
    private static final double DIVERSITY_FIRST_PROB= 0.10; // HARDCODED: 10% chance to nudge first action for diversity
    private static final double EARLY_GENE_BIAS_EXP = 1.6;  // HARDCODED: >1 biases mutation point towards earlier genes

    // Monte-Carlo rollout seeding
    private static final int    ROLLOUT_SEED_TRIALS = 3;    // HARDCODED: pick best of 3 random individuals for seeding

    protected int repairCount, nonRepairCount;
    AbstractAction[] actions;
    AbstractGameState[] gameStates;
    double value;
    int length;
    double discountFactor;
    IStateHeuristic heuristic;
    AbstractPlayer rolloutPolicy;   // random or MAST
    private final Random gen;

    /* --------------------------------------------------------------------- */
    /*  CONSTRUCTORS                                                        */
    /* --------------------------------------------------------------------- */
    public RHEAIndividualT(int L, double df, AbstractForwardModel fm, AbstractGameState gs,
                           int playerID, Random gen, IStateHeuristic h, AbstractPlayer rp) {
        this.gen = gen;
        this.discountFactor = df;
        this.heuristic = h;
        this.rolloutPolicy = rp;
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L + 1];
        gameStates[0] = gs.copy();
        // Build plan & states, then auto-recompute value from stored states
        rollout(fm, 0, playerID, true);
        recomputeFromStoredStates(playerID);
    }

    /** Deep copy */
    public RHEAIndividualT(RHEAIndividualT o) {
        this.gen = o.gen;
        this.discountFactor = o.discountFactor;
        this.heuristic = o.heuristic;
        this.rolloutPolicy = o.rolloutPolicy;
        this.actions = Arrays.copyOf(o.actions, o.actions.length);
        this.gameStates = new AbstractGameState[o.gameStates.length];
        this.length = o.length;
        this.value = o.value;
        for (int i = 0; i <= length; i++) {
            if (o.gameStates[i] != null)
                this.gameStates[i] = o.gameStates[i].copy();
        }
    }

    /* --------------------------------------------------------------------- */
    /*  MUTATION (baseline single/naive – kept intact)                       */
    /* --------------------------------------------------------------------- */
    public Pair<Integer, Integer> mutate(AbstractForwardModel fm, int playerID, int count) {
        int start = actions.length;
        for (int i = 0; i < count; i++) {
            int pos = gen.nextInt(Math.max(1, length));
            if (gameStates[pos] != null) {
                List<AbstractAction> legal = fm.computeAvailableActions(gameStates[pos]);
                actions[pos] = legal.get(gen.nextInt(legal.size()));
                start = Math.min(start, pos);
            }
        }
        Pair<Integer, Integer> res = start < actions.length ? rollout(fm, start, playerID, true) : new Pair<>(0, 0);
        // Auto-recompute without extra FM calls
        recomputeFromStoredStates(playerID);
        return res;
    }

    /* --------------------------------------------------------------------- */
    /*  ROLLOUT (evaluation + repair)                                       */
    /* --------------------------------------------------------------------- */
    public Pair<Integer, Integer> rollout(AbstractForwardModel fm, int start, int playerID, boolean repair) {
        length = start;
        double delta = 0, prev = 0;
        int fmCalls = 0, copyCalls = 0;
        AbstractGameState gs = gameStates[start].copy();
        copyCalls++;

        // restore prefix delta (no FM calls)
        for (int i = 0; i < start; i++) {
            double score = heuristic.evaluateState(gameStates[i + 1], playerID);
            delta += Math.pow(discountFactor, i) * (score - prev);
            prev = score;
        }

        for (int i = start; i < actions.length && gs.isNotTerminal(); i++) {
            AbstractGameState temp = gs.copy();
            copyCalls++;
            List<AbstractAction> legal = fm.computeAvailableActions(temp);
            if (legal == null || legal.isEmpty()) break;

            boolean invalid = actions[i] == null || !legal.contains(actions[i]);

            AbstractAction action = invalid ?
                    rolloutPolicy.getAction(temp, legal) :
                    actions[i];

            if ((repair || actions[i] == null) && invalid) {
                actions[i] = action;
                repairCount++;
            } else if (!invalid) {
                nonRepairCount++;
            }

            fm.next(temp, action.copy());
            fmCalls++;

            // fast-forward opponent turns
            while (temp.isNotTerminal() && temp.getCurrentPlayer() != playerID) {
                List<AbstractAction> oppMoves = fm.computeAvailableActions(temp);
                if (oppMoves == null || oppMoves.isEmpty()) break;
                fm.next(temp, oppMoves.get(gen.nextInt(oppMoves.size())));
                fmCalls++;
            }

            gameStates[i + 1] = temp;
            length++;

            double score = heuristic.evaluateState(temp, playerID);
            delta += Math.pow(discountFactor, i) * (score - prev);
            prev = score;
            gs = temp;
        }

        this.value = delta;
        return new Pair<>(fmCalls, copyCalls);
    }

    /* --------------------------------------------------------------------- */
    /*  COMPARABLE & UTILS                                                  */
    /* --------------------------------------------------------------------- */
    @Override
    public int compareTo(RHEAIndividualT o) {
        return Double.compare(o.value, value);   // higher first
    }

    @Override
    public String toString() {
        return String.format("%.3f: %s", value, Arrays.toString(actions));
    }

    /* ===================================================================== */
    /* =====================  POPULATION-LEVEL HELPERS  ==================== */
    /* ===================================================================== */

    /**
     * Initialize a population using a mix of:
     *  - Greedy OSLA seeds (SEED_GREEDY_RATIO)
     *  - Monte-Carlo rollout seeds (SEED_ROLLOUT_RATIO)
     *  - Random seeds (remainder)
     */
    public static List<RHEAIndividualT> initPopulation(
            int popSize, int horizon, double df,
            AbstractForwardModel fm, AbstractGameState gs,
            int playerID, Random gen, IStateHeuristic h, AbstractPlayer rp) {

        List<RHEAIndividualT> pop = new ArrayList<>(popSize);

        int nGreedy   = (int)Math.round(SEED_GREEDY_RATIO * popSize);   // HARDCODED %
        int nRollout  = (int)Math.round(SEED_ROLLOUT_RATIO * popSize);  // HARDCODED %
        int nRandom   = Math.max(0, popSize - nGreedy - nRollout);

        // Greedy OSLA seeds
        for (int i = 0; i < nGreedy; i++) {
            pop.add(buildGreedyIndividual(horizon, df, fm, gs, playerID, gen, h, rp));
        }

        // Monte-Carlo rollout seeds (best-of-k random individuals)
        for (int i = 0; i < nRollout; i++) {
            pop.add(buildRolloutSeedIndividual(horizon, df, fm, gs, playerID, gen, h, rp));
        }

        // Purely random seeds
        for (int i = 0; i < nRandom; i++) {
            pop.add(new RHEAIndividualT(horizon, df, fm, gs, playerID, gen, h, rp));
        }

        Collections.sort(pop);
        return pop;
    }

    /**
     * OPTIONAL: Evolve one generation (kept for convenience).
     * If you already have selection, ignore this and call the lower-level helpers directly.
     */
    public static void evolveOneGeneration(
            List<RHEAIndividualT> pop,
            AbstractForwardModel fm,
            AbstractGameState rootState,
            int playerID) {

        if (pop.isEmpty()) return;
        Random gen = pop.get(0).gen; // shared RNG

        // Parent selection (tournament) – ignore if you have your own selection
        RHEAIndividualT parent = tournament(pop, TOURNAMENT_SIZE, gen);

        // Offspring via partial re-roll mutation or adjacent swap
        RHEAIndividualT child = new RHEAIndividualT(parent);
        boolean didSwap = gen.nextDouble() < MUT_SWAP_PROB; // HARDCODED

        if (didSwap) {
            tryAdjacentSwap(child, fm, rootState, playerID);        // uses FM to repair
            child.recomputeFromStoredStates(playerID);              // no extra FM to evaluate
        } else {
            int k = pickBiasedMutationPoint(child.length, gen);
            // Null tail from k, then repair via rollout
            for (int i = k; i < child.actions.length; i++) {
                child.actions[i] = null;
                child.gameStates[i + 1] = null;
            }
            child.gameStates[0] = rootState.copy();
            child.rollout(fm, Math.max(0, Math.min(k, child.length)), playerID, true);
            child.recomputeFromStoredStates(playerID);              // no extra FM to evaluate
        }

        // Diversity nudge on first action
        if (gen.nextDouble() < DIVERSITY_FIRST_PROB) { // HARDCODED
            forceDifferentFirstAction(child, fm, rootState, playerID, pop, gen);
            child.recomputeFromStoredStates(playerID);              // ensure fresh value
        }

        // Replace worst if better
        RHEAIndividualT worst = pop.get(pop.size() - 1);
        if (child.value > worst.value) {
            pop.set(pop.size() - 1, child);
            Collections.sort(pop);
        }
    }

    /**
     * Shift-buffer + repair + elitism + immigrants after a real action was taken.
     * Keeps top ELITE_RATIO, fills the rest with repaired carry-overs and fresh individuals.
     * Auto-recomputes values from stored states after any change.
     */
    public static List<RHEAIndividualT> shiftRepairAndReuse(
            List<RHEAIndividualT> pop,
            int horizon, double df,
            AbstractForwardModel fm,
            AbstractGameState newState,
            int playerID,
            IStateHeuristic h,
            AbstractPlayer rp) {

        if (pop == null || pop.isEmpty()) return pop;
        Random gen = pop.get(0).gen;

        // Shift actions left by 1, null last; reset root state; rollout to repair
        for (RHEAIndividualT ind : pop) {
            if (ind.actions.length > 1) {
                System.arraycopy(ind.actions, 1, ind.actions, 0, ind.actions.length - 1);
            }
            ind.actions[ind.actions.length - 1] = null; // will be re-generated by rollout
            Arrays.fill(ind.gameStates, null);
            ind.gameStates[0] = newState.copy();
            ind.rollout(fm, 0, playerID, true);    // repair into new reality (uses FM once)
            ind.recomputeFromStoredStates(playerID); // then evaluate without extra FM
        }

        // Sort and elitism
        Collections.sort(pop);
        int elites = Math.max(1, (int)Math.round(ELITE_RATIO * pop.size())); // HARDCODED
        List<RHEAIndividualT> next = new ArrayList<>(pop.subList(0, elites));

        // Keep best of the rest (already repaired)
        int target = pop.size();
        for (int i = elites; i < pop.size() && next.size() < target; i++) {
            next.add(pop.get(i));
        }

        // Inject immigrants (fresh random individuals) to refresh diversity
        int immigrants = Math.max(0, target - next.size());
        for (int i = 0; i < immigrants; i++) {
            RHEAIndividualT newbie = new RHEAIndividualT(horizon, df, fm, newState, playerID, gen, h, rp);
            newbie.recomputeFromStoredStates(playerID);
            next.add(newbie);
        }

        Collections.sort(next);
        return next;
    }

    /**
     * Pick the best action (first gene) of the current population champion.
     */
    public static AbstractAction bestAction(List<RHEAIndividualT> pop) {
        if (pop == null || pop.isEmpty()) return null;
        RHEAIndividualT best = pop.get(0);
        return (best.actions.length > 0) ? best.actions[0] : null;
    }

    /* ===================================================================== */
    /* ========================  SEEDING HELPERS  ========================== */
    /* ===================================================================== */

    private static RHEAIndividualT buildGreedyIndividual(
            int horizon, double df,
            AbstractForwardModel fm, AbstractGameState gs,
            int playerID, Random gen, IStateHeuristic h, AbstractPlayer rp) {

        RHEAIndividualT ind = new RHEAIndividualT(horizon, df, fm, gs, playerID, gen, h, rp);
        // Overwrite actions by OSLA (one-step lookahead) to bias towards strong local choices
        Arrays.fill(ind.actions, null);
        Arrays.fill(ind.gameStates, null);
        ind.gameStates[0] = gs.copy();

        AbstractGameState cur = gs.copy();
        for (int i = 0; i < horizon && cur.isNotTerminal(); i++) {
            List<AbstractAction> legal = fm.computeAvailableActions(cur);
            if (legal == null || legal.isEmpty()) break;

            AbstractAction bestA = legal.get(0);
            double bestV = Double.NEGATIVE_INFINITY;

            for (AbstractAction a : legal) {
                AbstractGameState tmp = cur.copy();
                fm.next(tmp, a.copy());
                // fast-forward opponents
                while (tmp.isNotTerminal() && tmp.getCurrentPlayer() != playerID) {
                    List<AbstractAction> oppMoves = fm.computeAvailableActions(tmp);
                    if (oppMoves == null || oppMoves.isEmpty()) break;
                    fm.next(tmp, oppMoves.get(gen.nextInt(oppMoves.size())));
                }
                double v = h.evaluateState(tmp, playerID);
                if (v > bestV) {
                    bestV = v;
                    bestA = a;
                }
            }

            ind.actions[i] = bestA;
            fm.next(cur, bestA.copy());
            // fast-forward opponents
            while (cur.isNotTerminal() && cur.getCurrentPlayer() != playerID) {
                List<AbstractAction> oppMoves = fm.computeAvailableActions(cur);
                if (oppMoves == null || oppMoves.isEmpty()) break;
                fm.next(cur, oppMoves.get(gen.nextInt(oppMoves.size())));
            }
            ind.gameStates[i + 1] = cur.copy();
        }
        // Repair any remaining nulls & auto-evaluate
        ind.rollout(fm, 0, playerID, true);
        ind.recomputeFromStoredStates(playerID);
        return ind;
    }

    private static RHEAIndividualT buildRolloutSeedIndividual(
            int horizon, double df,
            AbstractForwardModel fm, AbstractGameState gs,
            int playerID, Random gen, IStateHeuristic h, AbstractPlayer rp) {

        // Best-of-k random individuals (constructor builds random rollout via rolloutPolicy)
        RHEAIndividualT best = null;
        for (int t = 0; t < ROLLOUT_SEED_TRIALS; t++) { // HARDCODED
            RHEAIndividualT cand = new RHEAIndividualT(horizon, df, fm, gs, playerID, gen, h, rp);
            cand.recomputeFromStoredStates(playerID);
            if (best == null || cand.value > best.value) best = cand;
        }
        return best;
    }

    /* ===================================================================== */
    /* ========================  MUTATION HELPERS  ========================= */
    /* ===================================================================== */

    /**
     * Pick a mutation start index with bias towards earlier genes.
     * Index is in [0, length-1].
     */
    private static int pickBiasedMutationPoint(int length, Random gen) {
        if (length <= 1) return 0;
        // Inverse-CDF for power-law bias towards 0
        double u = gen.nextDouble();
        double x = Math.pow(u, 1.0 / EARLY_GENE_BIAS_EXP); // HARDCODED exponent
        int idx = (int)Math.floor(x * (length - 1));       // map to [0, length-1]
        return Math.max(0, Math.min(length - 1, idx));
    }

    /**
     * Try swapping two adjacent genes; then repair with rollout once,
     * finally re-evaluate from stored states (no extra FM).
     */
    private static void tryAdjacentSwap(RHEAIndividualT ind,
                                        AbstractForwardModel fm,
                                        AbstractGameState root,
                                        int playerID) {
        if (ind.length < 2) {
            ind.gameStates[0] = root.copy();
            ind.rollout(fm, 0, playerID, true);
            return;
        }
        int i = Math.max(0, Math.min(ind.length - 2, ind.gen.nextInt(Math.max(1, ind.length - 1))));
        AbstractAction tmp = ind.actions[i];
        ind.actions[i] = ind.actions[i + 1];
        ind.actions[i + 1] = tmp;

        Arrays.fill(ind.gameStates, null);
        ind.gameStates[0] = root.copy();
        ind.rollout(fm, 0, playerID, true);
    }

    /**
     * Encourage exploration: if population's first action is dominated by one choice,
     * force a different (still legal) first action for this child, then re-evaluate.
     */
    private static void forceDifferentFirstAction(RHEAIndividualT child,
                                                  AbstractForwardModel fm,
                                                  AbstractGameState root,
                                                  int playerID,
                                                  List<RHEAIndividualT> pop,
                                                  Random gen) {
        if (child.actions.length == 0) return;
        // Find most common first action in population (by equals())
        Map<AbstractAction, Integer> freq = new HashMap<>();
        for (RHEAIndividualT ind : pop) {
            if (ind.actions.length > 0 && ind.actions[0] != null)
                freq.merge(ind.actions[0], 1, Integer::sum);
        }
        AbstractAction mostCommon = null;
        int best = -1;
        for (Map.Entry<AbstractAction, Integer> e : freq.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                mostCommon = e.getKey();
            }
        }
        if (mostCommon == null) return;

        // If child matches the herd, try to switch to another valid first action
        if (mostCommon.equals(child.actions[0])) {
            AbstractGameState cur = root.copy();
            List<AbstractAction> legal = fm.computeAvailableActions(cur);
            if (legal != null && legal.size() > 1) {
                List<AbstractAction> alternatives = legal.stream()
                        .filter(a -> !a.equals(child.actions[0]))
                        .collect(Collectors.toList());
                if (!alternatives.isEmpty()) {
                    child.actions[0] = alternatives.get(gen.nextInt(alternatives.size()));
                    Arrays.fill(child.gameStates, null);
                    child.gameStates[0] = root.copy();
                    child.rollout(fm, 0, playerID, true);
                }
            }
        }
    }

    /* ===================================================================== */
    /* ========================  SELECTION HELPERS  ======================== */
    /* ===================================================================== */

    private static RHEAIndividualT tournament(List<RHEAIndividualT> pop, int k, Random gen) {
        RHEAIndividualT best = null;
        for (int i = 0; i < k; i++) {
            RHEAIndividualT cand = pop.get(gen.nextInt(pop.size()));
            if (best == null || cand.compareTo(best) < 0) {
                best = cand;
            }
        }
        return best;
    }

    /* ===================================================================== */
    /* ====================  AUTO EVALUATION (NO FM)  ====================== */
    /* ===================================================================== */

    /**
     * Recompute 'value' purely from stored gameStates using the discount scheme:
     * value = sum_{i=0..length-1} (discountFactor^i) * (score_i - prevScore),
     * where score_i = heuristic.evaluateState(gameStates[i+1]), prevScore starts at 0.
     * This mirrors the accumulation done in rollout, but without any FM calls.
     */
    private void recomputeFromStoredStates(int playerID) {
        double delta = 0.0;
        double prev = 0.0;
        int maxI = Math.min(length, gameStates.length - 1);
        for (int i = 0; i < maxI; i++) {
            AbstractGameState st = gameStates[i + 1];
            if (st == null) break;
            double score = heuristic.evaluateState(st, playerID);
            delta += Math.pow(discountFactor, i) * (score - prev);
            prev = score;
        }
        this.value = delta;
    }
}
