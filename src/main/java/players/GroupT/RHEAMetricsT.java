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
            if (player instanceof RHEAPlayerT rheaPlayerT) {
                stats.put("iterations", rheaPlayerT.getNumIters());
                stats.put("fmCalls", rheaPlayerT.getNumIters() == 0 ? 0 :
                        rheaPlayerT.getFmCalls() / (double) rheaPlayerT.getNumIters());
                stats.put("copyCalls", rheaPlayerT.getNumIters() == 0 ? 0 :
                        rheaPlayerT.getCopyCalls() / (double) rheaPlayerT.getNumIters());
                stats.put("time", rheaPlayerT.getTimeTaken());
                stats.put("timePerIteration", rheaPlayerT.getTimePerIteration());
                stats.put("initTime", rheaPlayerT.getInitTime());
                stats.put("hiReward", rheaPlayerT.getBestValue());
                stats.put("loReward", rheaPlayerT.getWorstValue());
                stats.put("medianReward", rheaPlayerT.getMedianValue());
                stats.put("repairProportion", rheaPlayerT.getRepairCount() == 0 ? 0.0 :
                        rheaPlayerT.getRepairCount() / (double) (rheaPlayerT.getRepairCount() + rheaPlayerT.getNonRepairCount()));
                stats.put("repairsPerIteration", rheaPlayerT.getNumIters() == 0 ? 0.0 :
                        rheaPlayerT.getRepairCount() / (double) rheaPlayerT.getNumIters());
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
            stats.put("fmCalls", Double.class);
            stats.put("copyCalls", Double.class);
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