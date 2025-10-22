package players.project;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;

public class ProjectPlayer extends AbstractPlayer {

    public ProjectPlayer(String name) {
        super(null, name);
    }

    public ProjectPlayer(ProjectPlayer p) {
        super(p.parameters, "ProjectPlayer");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        // Always play the first action
        return possibleActions.get(0);
    }

    @Override
    public AbstractPlayer copy() {
        return null;
    }
}
