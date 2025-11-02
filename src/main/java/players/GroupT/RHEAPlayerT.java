package players.GroupT;

import core.*;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import players.IAnyTimePlayer;
import players.PlayerConstants;
import players.mcts.MASTPlayer;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class RHEAPlayerT extends AbstractPlayer implements IAnyTimePlayer {

    private static final AbstractPlayer randomPlayer = new RandomPlayer();

    // MAST statistics – NOT final (we re‑assign after decay)
    private List<Map<Object, Pair<Integer, Double>>> MASTStatistics;
    private List<RHEAIndividualT> population = new ArrayList<>();
    private final List<RHEAIndividualT> hallOfFame = new ArrayList<>(10);

    // budgets
    protected double timePerIteration = 0, timeTaken = 0, initTime = 0;
    protected int numIters = 0;
    protected int fmCalls = 0;
    protected int copyCalls = 0;
    protected int repairCount, nonRepairCount;
    private MASTPlayer mastPlayer;

    // adaptive mutation
    private double mutationRate = 0.1;
    private int successCount = 0, failureCount = 0;

    /* --------------------------------------------------------------------- */
    public RHEAPlayerT(RHEAParamsT params) {
        super(params, "RHEAPlayerT");
        MASTStatistics = new ArrayList<>();
    }

    public RHEAPlayerT(RHEAParamsT params, String name) {
        super(params, name);
        MASTStatistics = new ArrayList<>();
    }

    @Override public RHEAParamsT getParameters() { return (RHEAParamsT) parameters; }

    @Override
    public void initializePlayer(AbstractGameState state) {
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) MASTStatistics.add(new HashMap<>());
        population.clear();
        mutationRate = 0.1;
        successCount = failureCount = 0;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState stateObs, List<AbstractAction> possibleActions) {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();
        timer.setMaxTimeMillis(parameters.budget);
        numIters = fmCalls = copyCalls = repairCount = nonRepairCount = 0;
        RHEAParamsT p = getParameters();

        /* -------------------- MAST setup -------------------- */
        if (p.useMAST) {
            if (MASTStatistics.isEmpty()) {
                for (int i = 0; i < stateObs.getNPlayers(); i++) MASTStatistics.add(new HashMap<>());
            } else {
                MASTStatistics = MASTStatistics.stream()
                        .map(m -> Utils.decay(m, p.discountFactor))
                        .collect(Collectors.toList());
            }
            mastPlayer = new MASTPlayer(null, 1.0, 0.0, System.currentTimeMillis(), 0.0);
            mastPlayer.setMASTStats(MASTStatistics);
        }

        /* -------------------- Shift‑left reuse -------------------- */
        if (p.shiftLeft && !population.isEmpty()) {
            population.forEach(i -> i.value = Double.NEGATIVE_INFINITY);
            for (RHEAIndividualT ind : population) {
                if (!budgetLeft(timer)) break;
                System.arraycopy(ind.actions, 1, ind.actions, 0, ind.actions.length - 1);
                ind.gameStates[0] = stateObs.copy();
                Pair<Integer, Integer> calls = ind.rollout(getForwardModel(), 0, getPlayerID(), true);
                fmCalls += calls.a; copyCalls += calls.b;
            }
        } else {
            population.clear();
            for (int i = 0; i < p.populationSize; i++) {
                if (!budgetLeft(timer)) break;
                population.add(new RHEAIndividualT(
                        p.horizon, p.discountFactor, getForwardModel(), stateObs,
                        getPlayerID(), rnd, p.heuristic,
                        p.useMAST ? mastPlayer : randomPlayer));
                RHEAIndividualT ind = population.get(i);
                fmCalls += ind.length;
                copyCalls += ind.length;
            }
        }

        population.sort(Comparator.naturalOrder());
        initTime = timer.elapsedMillis();

        /* -------------------- Evolution loop -------------------- */
        while (budgetLeft(timer)) runIteration();

        timeTaken = timer.elapsedMillis();
        timePerIteration = numIters == 0 ? 0.0 : (timeTaken - initTime) / numIters;

        /* -------------------- Hall of Fame -------------------- */
        RHEAIndividualT best = population.get(0);
        if (hallOfFame.isEmpty() || best.value > hallOfFame.get(0).value) {
            hallOfFame.add(0, new RHEAIndividualT(best));
            if (hallOfFame.size() > 10) hallOfFame.remove(hallOfFame.size() - 1);
        }

        /* -------------------- Return best action -------------------- */
        AbstractAction ret = best.actions[0];
        if (!getForwardModel().computeAvailableActions(stateObs).contains(ret))
            throw new AssertionError("Chosen action not legal");
        return ret;
    }

    /* --------------------------------------------------------------------- */
    private void runIteration() {
        RHEAParamsT p = getParameters();
        List<RHEAIndividualT> offspring = new ArrayList<>();

        // 1. Elites (deep copy)
        population.sort(Comparator.naturalOrder());
        for (int i = 0; i < Math.min(p.eliteCount, population.size()); i++)
            offspring.add(new RHEAIndividualT(population.get(i)));

        // 2. Children
        for (int i = 0; i < p.childCount; i++) {
            RHEAIndividualT[] parents = selectParents();
            RHEAIndividualT child = crossover(parents[0], parents[1]);
            child = mutate(child);
            offspring.add(child);
        }

        // 3. Survivor selection
        offspring.sort(Comparator.naturalOrder());
        int keep = Math.min(p.populationSize, offspring.size());
        population = new ArrayList<>(offspring.subList(0, keep));

        // 4. Hall‑of‑Fame injection (every 3 generations)
        if (numIters % 3 == 0 && !hallOfFame.isEmpty() && population.size() < p.populationSize) {
            population.add(new RHEAIndividualT(hallOfFame.get(rnd.nextInt(Math.min(3, hallOfFame.size())))));
            population.sort(Comparator.naturalOrder());
            if (population.size() > p.populationSize) population.remove(population.size() - 1);
        }

        // 5. Adaptive mutation
        updateMutationRate();

        numIters++;
    }

    /* --------------------------------------------------------------------- */
    private RHEAIndividualT mutate(RHEAIndividualT ind) {
        RHEAParamsT p = getParameters();                     // <-- FIXED: local reference
        int mutations = Math.max(1, (int) (ind.length * mutationRate));
        Pair<Integer, Integer> calls = ind.mutate(getForwardModel(), getPlayerID(), mutations);
        fmCalls += calls.a; copyCalls += calls.b;
        repairCount += ind.repairCount;
        nonRepairCount += ind.nonRepairCount;

        if (p.useMAST) MASTBackup(ind.actions, ind.value, getPlayerID());

        // success/failure for adaptation
        if (ind.value > parentAverage()) successCount++; else failureCount++;
        return ind;
    }

    private double parentAverage() {
        RHEAParamsT p = getParameters();
        return population.stream()
                .limit(p.eliteCount)
                .mapToDouble(i -> i.value)
                .average()
                .orElse(0.0);
    }

    private void updateMutationRate() {
        if (successCount + failureCount >= 10) {
            double sr = (double) successCount / (successCount + failureCount);
            if (sr > 0.2) mutationRate = Math.min(0.3, mutationRate * 1.1);
            else if (sr < 0.2) mutationRate = Math.max(0.05, mutationRate * 0.9);
            successCount = failureCount = 0;
        }
    }

    /* --------------------------------------------------------------------- */
    private RHEAIndividualT[] selectParents() {
        RHEAIndividualT[] parents = new RHEAIndividualT[2];
        switch (getParameters().selectionType) {
            case TOURNAMENT -> { parents[0] = tournament(); parents[1] = tournament(); }
            case RANK -> { parents[0] = rank(); parents[1] = rank(); }
            case TRUNCATION_TOURNAMENT -> { parents[0] = truncation(); parents[1] = truncation(); }
        }
        return parents;
    }

    private RHEAIndividualT tournament() {
        RHEAIndividualT best = null;
        RHEAParamsT p = getParameters();
        for (int i = 0; i < p.tournamentSize; i++) {
            RHEAIndividualT cand = population.get(rnd.nextInt(population.size()));
            if (best == null || cand.value > best.value) best = cand;
        }
        return best;
    }

    private RHEAIndividualT rank() {
        int sum = population.size() * (population.size() + 1) / 2;
        int r = rnd.nextInt(sum);
        int acc = 0;
        for (int i = 0; i < population.size(); i++) {
            acc += population.size() - i;
            if (acc >= r) return population.get(i);
        }
        return population.get(population.size() - 1);
    }

    private RHEAIndividualT truncation() {
        RHEAParamsT p = getParameters();
        int keep = Math.max(1, (int) Math.ceil(population.size() * p.truncationRatio));
        RHEAIndividualT best = null;
        for (int i = 0; i < p.tournamentSize; i++) {
            RHEAIndividualT cand = population.get(rnd.nextInt(keep));
            if (best == null || cand.value > best.value) best = cand;
        }
        return best;
    }

    /* --------------------------------------------------------------------- */
    private RHEAIndividualT crossover(RHEAIndividualT p1, RHEAIndividualT p2) {
        return switch (getParameters().crossoverType) {
            case NONE -> new RHEAIndividualT(p1);
            case UNIFORM -> uniform(p1, p2);
            case ONE_POINT -> onePoint(p1, p2);
            case TWO_POINT -> twoPoint(p1, p2);
        };
    }

    private RHEAIndividualT uniform(RHEAIndividualT p1, RHEAIndividualT p2) {
        RHEAIndividualT child = new RHEAIndividualT(p1);
        copyCalls += child.length;
        int len = Math.min(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            if (rnd.nextBoolean()) {
                child.actions[i] = p2.actions[i];
                child.gameStates[i] = p2.gameStates[i] != null ? p2.gameStates[i].copy() : null;
            }
        }
        return child;
    }

    private RHEAIndividualT onePoint(RHEAIndividualT p1, RHEAIndividualT p2) {
        RHEAIndividualT child = new RHEAIndividualT(p1);
        copyCalls += child.length;
        int point = 1 + rnd.nextInt(Math.min(p1.length, p2.length) - 1);
        for (int i = point; i < Math.min(p1.length, p2.length); i++) {
            child.actions[i] = p2.actions[i];
            child.gameStates[i] = p2.gameStates[i] != null ? p2.gameStates[i].copy() : null;
        }
        return child;
    }

    private RHEAIndividualT twoPoint(RHEAIndividualT p1, RHEAIndividualT p2) {
        RHEAIndividualT child = new RHEAIndividualT(p1);
        copyCalls += child.length;
        int len = Math.min(p1.length, p2.length);
        int a = rnd.nextInt(len / 3), b = len - rnd.nextInt(len / 3);
        for (int i = 0; i < a; i++) { child.actions[i] = p2.actions[i]; child.gameStates[i] = p2.gameStates[i]; }
        for (int i = b; i < len; i++) { child.actions[i] = p2.actions[i]; child.gameStates[i] = p2.gameStates[i]; }
        return child;
    }

    /* --------------------------------------------------------------------- */
    private boolean budgetLeft(ElapsedCpuTimer timer) {
        RHEAParamsT p = getParameters();
        return switch (p.budgetType) {
            case BUDGET_TIME -> timer.remainingTimeMillis() > p.breakMS;
            case BUDGET_FM_CALLS -> fmCalls < p.budget;
            case BUDGET_COPY_CALLS -> copyCalls < p.budget;
            case BUDGET_FMANDCOPY_CALLS -> (fmCalls + copyCalls) < p.budget;
            case BUDGET_ITERATIONS -> numIters < p.budget;
            default -> throw new AssertionError("Unknown budget type");
        };
    }

    protected void MASTBackup(AbstractAction[] actions, double value, int player) {
        for (AbstractAction a : actions) {
            if (a == null) break;
            Pair<Integer, Double> stats = MASTStatistics.get(player).getOrDefault(a, new Pair<>(0, 0.0));
            stats.a++; stats.b += value;
            MASTStatistics.get(player).put(a.copy(), stats);
        }
    }

    @Override public RHEAPlayerT copy() {
        RHEAParamsT np = (RHEAParamsT) parameters.copy();
        np.setRandomSeed(rnd.nextInt());
        RHEAPlayerT c = new RHEAPlayerT(np, toString());
        c.setForwardModel(getForwardModel());
        return c;
    }

    @Override public void setBudget(int budget) { parameters.budget = budget; parameters.setParameterValue("budget", budget); }
    @Override public int getBudget() { return parameters.budget; }
 
    public double getBestValue() {
        if (population.isEmpty()) return 0.0;
        population.sort(Comparator.naturalOrder());
        return population.get(0).value;
    }

    public double getWorstValue() {
        if (population.isEmpty()) return 0.0;
        population.sort(Comparator.naturalOrder());
        return population.get(population.size() - 1).value;
    }

    public double getMedianValue() {
        if (population.isEmpty()) return 0.0;
        population.sort(Comparator.naturalOrder());
        int mid = population.size() / 2;
        return population.size() % 2 == 1 ?
                population.get(mid).value :
                (population.get(mid - 1).value + population.get(mid).value) / 2.0;
    }

    public int getPopulationSize() {
        return population.size();
    }

    public int getNumIters() { return numIters; }
    public int getFmCalls() { return fmCalls; }
    public int getCopyCalls() { return copyCalls; }
    public double getTimeTaken() { return timeTaken; }
    public double getTimePerIteration() { return timePerIteration; }
    public double getInitTime() { return initTime; }
    public int getRepairCount() { return repairCount; }
    public int getNonRepairCount() { return nonRepairCount; }

}