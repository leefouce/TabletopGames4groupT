package players.GroupT;

import core.AbstractPlayer;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;

import java.util.*;

public class RHEAMetricsT implements IMetricsCollection {

    public static class RHEAStatsT extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> stats) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (player instanceof RHEAPlayerT) {
                RHEAPlayerT rheaPlayerT = (RHEAPlayerT) player;
                stats.put("iterations", rheaPlayerT.numIters);
                stats.put("fmCalls", rheaPlayerT.numIters == 0 ? 0 : rheaPlayerT.fmCalls / rheaPlayerT.numIters);
                stats.put("copyCalls", rheaPlayerT.numIters == 0 ? 0 : rheaPlayerT.copyCalls / rheaPlayerT.numIters);
                stats.put("time", rheaPlayerT.timeTaken);
                stats.put("timePerIteration", rheaPlayerT.timePerIteration);
                stats.put("initTime", rheaPlayerT.initTime);
                stats.put("hiReward", rheaPlayerT.numIters == 0 ? 0 : rheaPlayerT.population.get(0).value);
                stats.put("loReward", rheaPlayerT.numIters == 0 ? 0 : rheaPlayerT.population.get(rheaPlayerT.population.size() - 1).value);
                stats.put("medianReward", rheaPlayerT.numIters == 0 ? 0 : rheaPlayerT.population.size() == 1 ?
                        rheaPlayerT.population.get(0).value :
                        rheaPlayerT.population.get(rheaPlayerT.population.size() / 2 - 1).value);
                stats.put("repairProportion", rheaPlayerT.repairCount == 0 ? 0.0 : rheaPlayerT.repairCount / (double) (rheaPlayerT.repairCount + rheaPlayerT.nonRepairCount));
                stats.put("repairsPerIteration", rheaPlayerT.repairCount == 0 ? 0.0 : rheaPlayerT.repairCount / (double) rheaPlayerT.numIters);
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> stats = new LinkedHashMap<>();
            stats.put("iterations", Integer.class);
            stats.put("fmCalls", Integer.class);
            stats.put("copyCalls", Integer.class);
            stats.put("time", Double.class);
            stats.put("timePerIteration", Double.class);
            stats.put("initTime", Double.class);
            stats.put("hiReward", Double.class);
            stats.put("loReward", Double.class);
            stats.put("medianReward", Double.class);
            stats.put("repairProportion", Double.class);
            stats.put("repairsPerIteration", Double.class);
            return stats;
        }
    }
}
