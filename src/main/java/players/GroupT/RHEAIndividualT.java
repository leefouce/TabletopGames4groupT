package players.GroupT;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.AbstractPlayer;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A single candidate solution (action sequence) for the RHEA player.
 */
public class RHEAIndividualT implements Comparable<RHEAIndividualT> {

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
        rollout(fm, 0, playerID, true);
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
    /*  MUTATION                                                            */
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
        return start < actions.length ? rollout(fm, start, playerID, true) : new Pair<>(0, 0);
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

        // restore prefix delta
        for (int i = 0; i < start; i++) {
            double score = heuristic.evaluateState(gameStates[i + 1], playerID);
            delta += Math.pow(discountFactor, i) * (score - prev);
            prev = score;
        }

        for (int i = start; i < actions.length && gs.isNotTerminal(); i++) {
            AbstractGameState temp = gs.copy();
            copyCalls++;
            List<AbstractAction> legal = fm.computeAvailableActions(temp);
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
}