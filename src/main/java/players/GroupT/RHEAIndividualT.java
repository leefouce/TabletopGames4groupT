package players.GroupT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import utilities.Pair;

import java.util.List;
import java.util.Random;

public class RHEAIndividualT implements Comparable<RHEAIndividualT> {

    protected int repairCount;
    protected int nonRepairCount;
    AbstractAction[] actions;         // Actions in individual. Intended max length of individual = actions.length
    AbstractGameState[] gameStates;   // Game states in individual.
    double value;                     // Fitness of individual, to be maximised.
    int length;                       // Actual length of individual, <= actions.length
    double discountFactor;            // Discount factor for calculating rewards
    IStateHeuristic heuristic;
    AbstractPlayer rolloutPolicy;  // TODO-GT: design a Opponent modelling, evaluate the opponent score at this first round ending or every game tick,
    private Random gen;               // Random generator

    RHEAIndividualT(int L, double discountFactor, AbstractForwardModel fm, AbstractGameState gs,
                    int playerID, Random gen, IStateHeuristic heuristic,
                    AbstractPlayer rolloutPolicy) {
        // Initialize
        this.gen = gen;
        this.discountFactor = discountFactor;
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L + 1];
        this.heuristic = heuristic;
        this.rolloutPolicy = rolloutPolicy;

        // Rollout with random actions and assign fitness value
        gameStates[0] = gs.copy();
        rollout(fm, 0, playerID, true);
    }

    // Copy constructor
    RHEAIndividualT(RHEAIndividualT I) {
        actions = new AbstractAction[I.actions.length];
        gameStates = new AbstractGameState[I.gameStates.length];
        length = I.length;
        discountFactor = I.discountFactor;
        heuristic = I.heuristic;
        rolloutPolicy = I.rolloutPolicy;

        for (int i = 0; i < length; i++) {
            actions[i] = I.actions[i]; //.copy();
            gameStates[i] = I.gameStates[i]; //.copy(); // Should not need to copy game states, as we always copy before we use!
        }

        value = I.value;
        gen = I.gen;
    }

    /**
     * Mutates this individual, by picking an index and changing all genes from that point on.
     * Updates the length of the individual in case the rollout hits game end.
     * Also evaluates the individual as a rollout is needed for mutation, and updates the value.
     *
     * @param fm       - forward model
     * @param playerID - ID of player, used in evaluation of fitness
     * @return number of calls to the FM.next() function, as the difference between length after rollout and start
     * index of rollout
     */
    public Pair<Integer, Integer> mutate(AbstractForwardModel fm, int playerID, int mutationCount) {
        // Find index from which to mutate individual, random in range of currently valid length
        int startIndex = actions.length;
        for (int mutation = 0; mutation < mutationCount; mutation++) {
            // 只在“当前有效长度 length”范围内抽
            int position = gen.nextInt(length); // we only consider actions up to the end of the game (which will therefore increase mutation rate towards game end)
            if (gameStates[position] != null) {
                List<AbstractAction> available = fm.computeAvailableActions(gameStates[position]);
                // 随机换成一个合法动作
                actions[position] = available.get(gen.nextInt(available.size()));
                if (position < startIndex)
                    // 记录“最早变动位”
                    startIndex = position;  // start the rollout from the first mutation
            }
        }

        // Perform rollout and return number of FM calls taken.
        if (gameStates[startIndex] == null) {
            return new Pair<>(0, 0);
        } else {
            // rollout 评估 & 返回模拟成本
            return rollout(fm, startIndex, playerID, true);
        }
    }

    /**
     * Performs a rollout with random actions from startIndex to endIndex in the individual, from root game state gs.
     * Starts by repairing the full individual, then mutates it, and finally evaluates it.
     * Evaluates the final state reached and returns the number of calls to the FM.next() function.
     * Usage:
     * 1.	初始化个体（构造函数）
     * 2.	shift-left（跨 tick 复用）
     * 3.	变异（runIteration）
     *
     * @param fm         - forward model
     * @param startIndex - index in individual from which to start rollout
     * @param playerID   - ID of player, used in state evaluation
     * @return - number of calls to the FM.next() function
     */
    public Pair<Integer, Integer> rollout(AbstractForwardModel fm, int startIndex, int playerID, boolean repair) {
        length = 0;
        double delta = 0; // 用于累计启发式得分变化（最终成为个体的 value）
        double previousScore = 0; // 用来存“上一状态”的启发式值
        int fmCalls = 0, copyCalls = 0;
        AbstractGameState gs = gameStates[startIndex].copy(); // 作为rollout 起点

        // This lot are a local record for use in debugging; Very useful, with no compute overhead for keeping a local copy
        AbstractGameState[] oldGameStates = new AbstractGameState[gameStates.length];
        List<AbstractAction>[] availableActions = new List[gameStates.length];
        AbstractAction[] oldActions = new AbstractAction[actions.length];
        boolean[] illegalActions = new boolean[actions.length];

        // （如果从中途开始）补齐前缀的差分
        // 当 startIndex > 0（比如 shift-left或者变异从中间改起）时，
        // 前缀 gameStates[1..startIndex] 的历史差分要补回 delta，保证后续和整条序列的差分相接。
        for (int i = 0; i < startIndex; i++) {
            double score;
            score = heuristic.evaluateState(gameStates[i + 1], playerID);
            if (Double.isNaN(score))
                throw new AssertionError("Illegal heuristic value - should be a number");
            delta += Math.pow(discountFactor, i) * (score - previousScore);
            previousScore = score;
        }

        /*
            逐基因位推进到地平线或终局：
            1. 挑动作（合法性→修复）
            2. 推进自己动作
            3. 快进对手回合
            4. 更新轨迹、长度、差分得分
         */
        for (int i = startIndex; i < actions.length; i++) {
            // Rolls from chosen index to the end, randomly changing actions and game states
            // Length of individual is updated depending on if it reaches a terminal game state
            if (gs.isNotTerminal()) {
                // is the action valid
                AbstractAction action;
                AbstractGameState gsCopy = gs.copy();
                copyCalls++;
                // 获取可行动作集，并检测当前基因是否合法
                List<AbstractAction> currentActions = fm.computeAvailableActions(gsCopy, rolloutPolicy.getParameters().actionSpace);
                availableActions[i] = currentActions;
                boolean illegalAction = !currentActions.contains(actions[i]);
                illegalActions[i] = illegalAction;

                // 选择/修复要执行的动作
                if (illegalAction || actions[i] == null) {
                    oldActions[i] = actions[i];
                    action = rolloutPolicy.getAction(gsCopy, currentActions);
                    if (repair || actions[i] == null) // if we are repairing then we override an illegal action with a random legitimate one
                        actions[i] = action;
                    if (repair && illegalAction)
                        repairCount++;
                } else {
                    action = actions[i];
                    nonRepairCount++;
                }
                // TODO: Add a closed loop option to not copy the state (expensively) if the action is valid, but jump to the next state stored
                // TODO: When implemented, this will also need to take account of shiftLeft

                // 执行我方动作，推进一步
                fm.next(gsCopy, action.copy());
                fmCalls++;

                // If it's my turn, store this in the individual
                // 快进对手回合（直到轮到我或终局）
                while (gsCopy.isNotTerminal() && !(gsCopy.getCurrentPlayer() == playerID)) {
                    // now we fast forward through any opponent moves with a random OM
                    // TODO: Add in other opponent model options, and record other player moves for MAST
                    List<AbstractAction> moves = fm.computeAvailableActions(gsCopy);
                    if (moves.isEmpty()) {
                        throw new AssertionError("No moves found in state " + gsCopy);
                    }
                    fm.next(gsCopy, moves.get(gen.nextInt(moves.size())));
                    fmCalls++;
                }
                // 写回轨迹、长度
                oldGameStates[i + 1] = gameStates[i + 1];
                gameStates[i + 1] = gsCopy;
                // Individual length increased
                length++;

                // Add value of state, discounted
                // 用启发式做“折扣差分累计”
                double score;
                score = heuristic.evaluateState(gameStates[i + 1], playerID);
                if (Double.isNaN(score))
                    throw new AssertionError("Illegal heuristic value - should be a number");
                delta += Math.pow(discountFactor, i) * (score - previousScore);
                previousScore = score;

                gs = gsCopy;

            } else {
                break;
            }
        }
//        this.value = gs.getScore(playerID);
        // 收尾：把累计值写到个体
        this.value = delta;
        return new Pair<>(fmCalls, copyCalls);
    }

    @Override
    public int compareTo(RHEAIndividualT b) {
        RHEAIndividualT a = this;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RHEAIndividualT)) return false;

        RHEAIndividualT a = this;
        RHEAIndividualT b = (RHEAIndividualT) o;

        for (int i = 0; i < actions.length; i++) {
            if (!a.actions[i].equals(b.actions[i])) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + value + ": ");
        for (AbstractAction action : actions) s.append(action).append(" ");
        return s.toString();
    }
}
