package players.GroupT;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import players.PlayerParameters;

import java.util.Arrays;

public class RHEAParamsT extends PlayerParameters
{

    public int horizon = 10;
    public double discountFactor = 0.9;
    public int populationSize = 10;
    public int eliteCount = 2;
    public int childCount = 10;
    public int mutationCount = 1;
    public RHEAEnumsT.SelectionType selectionType = RHEAEnumsT.SelectionType.TOURNAMENT;
    public int tournamentSize = 4;
    public RHEAEnumsT.CrossoverType crossoverType = RHEAEnumsT.CrossoverType.UNIFORM;
    public boolean shiftLeft;
    public IStateHeuristic heuristic = AbstractGameState::getGameScore;
    public boolean useMAST;
    public double truncationRatio = 0.3;


    public RHEAParamsT() {
        addTunableParameter("horizon", 10, Arrays.asList(1, 3, 5, 10, 20, 30));
        addTunableParameter("discountFactor", 0.9, Arrays.asList(0.5, 0.8, 0.9, 0.95, 0.99, 0.999, 1.0));
        addTunableParameter("populationSize", 10, Arrays.asList(6, 8, 10, 12, 14, 16, 18, 20));
        addTunableParameter("eliteCount", 2, Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20));
        addTunableParameter("childCount", 10, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        addTunableParameter("selectionType", RHEAEnumsT.SelectionType.TOURNAMENT, Arrays.asList(RHEAEnumsT.SelectionType.values()));
        addTunableParameter("tournamentSize", 4, Arrays.asList(1, 2, 3, 4, 5, 6));
        addTunableParameter("crossoverType", RHEAEnumsT.CrossoverType.UNIFORM, Arrays.asList(RHEAEnumsT.CrossoverType.values()));
        addTunableParameter("shiftLeft", false, Arrays.asList(false, true));
        addTunableParameter("mutationCount", 1, Arrays.asList(1, 3, 10));
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getGameScore);
        addTunableParameter("useMAST", false, Arrays.asList(false, true));
        addTunableParameter("truncationRatio", 0.3, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5));
    }

    @Override
    public void _reset() {
        super._reset();
        horizon = (int) getParameterValue("horizon");
        discountFactor = (double) getParameterValue("discountFactor");
        populationSize = (int) getParameterValue("populationSize");
        eliteCount = (int) getParameterValue("eliteCount");
        childCount = (int) getParameterValue("childCount");
        selectionType = (RHEAEnumsT.SelectionType) getParameterValue("selectionType");
        tournamentSize = (int) getParameterValue("tournamentSize");
        crossoverType = (RHEAEnumsT.CrossoverType) getParameterValue("crossoverType");
        shiftLeft = (boolean) getParameterValue("shiftLeft");
        mutationCount = (int) getParameterValue("mutationCount");
        useMAST = (boolean) getParameterValue("useMAST");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        truncationRatio = (double) getParameterValue("truncationRatio");
        if (heuristic instanceof TunableParameters<?> tunableHeuristic) {
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
    }

    @Override
    protected RHEAParamsT _copy() {
        return new RHEAParamsT();
    }


    @Override
    public RHEAPlayerT instantiate() {
        return new RHEAPlayerT(this);
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

}