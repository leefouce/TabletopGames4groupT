package players.heuristics;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.List;

public class SGHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs;

        // 1. Base game score (points from completed rounds)
        // We want to fokus on winning the round not the game (the one implicits the other)
        double gameScore = 0; // state.getGameScore(playerId);

        // 2. Current round guaranteed points
        double roundScore = 0.0;
        // Count current played cards by type for this player
        int makiCount = state.getPlayedCardTypes(SGCard.SGCardType.Maki, playerId).getValue();
        int tempuraCount   = state.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerId).getValue();
        int sashimiCount   = state.getPlayedCardTypes(SGCard.SGCardType.Sashimi, playerId).getValue();
        int dumplingCount  = state.getPlayedCardTypes(SGCard.SGCardType.Dumpling, playerId).getValue();
        // Nigiri counts (assuming separate types for each nigiri)
        int squidNigiriCount  = state.getPlayedCardTypes(SGCard.SGCardType.SquidNigiri, playerId).getValue();
        int salmonNigiriCount = state.getPlayedCardTypes(SGCard.SGCardType.SalmonNigiri, playerId).getValue();
        int eggNigiriCount    = state.getPlayedCardTypes(SGCard.SGCardType.EggNigiri, playerId).getValue();
        int wasabiCount    = state.getPlayedCardTypes(SGCard.SGCardType.Wasabi, playerId).getValue();
        int chopsticksCount= state.getPlayedCardTypes(SGCard.SGCardType.Chopsticks, playerId).getValue();
        int puddingCount   = state.getPlayedCardTypes(SGCard.SGCardType.Pudding, playerId).getValue();

        int[] HandCardCounts = new int[SGCard.SGCardType.values().length];
        List<Deck<SGCard>> handcards = state.getPlayerHands();
        for(int i = 0; i < handcards.size(); i++) {
            if(!state.isHandKnown(playerId, i)) continue; // ignore unknown hands

            Deck<SGCard> hand = handcards.get(i);

            for (SGCard card : hand.getComponents()) {
                HandCardCounts[card.type.ordinal()] += 1;
            }
        }
        System.out.println("HANDCOUNTS: " + java.util.Arrays.toString(HandCardCounts));

        System.out.println("Player " + playerId + " - Current played cards: Maki=" + makiCount +
                ", Tempura=" + tempuraCount +
                ", Sashimi=" + sashimiCount +
                ", Dumpling=" + dumplingCount +
                ", SquidNigiri=" + squidNigiriCount +
                ", SalmonNigiri=" + salmonNigiriCount +
                ", EggNigiri=" + eggNigiriCount +
                ", Wasabi=" + wasabiCount +
                ", Chopsticks=" + chopsticksCount +
                ", Pudding=" + puddingCount);

        // Add flat points from Nigiri
        roundScore += eggNigiriCount + salmonNigiriCount * 2 + squidNigiriCount * 3;

        System.out.println("Player " + playerId + " - Nigiri points: " + roundScore);

        // Add Dumpling points for current dumplingCount
        if (dumplingCount >= 5) {
            roundScore += 15;  // max
        } else {
            // scoring sequence: 0→0, 1→1, 2→3, 3→6, 4→10, 5→15
            int dumplingPoints = switch (dumplingCount) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 3;
                case 3 -> 6;
                case 4 -> 10;
                default -> 15; // just in case >5
            };
            roundScore += dumplingPoints;
        }

        System.out.println("Player " + playerId + " - Dumpling points: " + (roundScore - (eggNigiriCount + salmonNigiriCount * 2 + squidNigiriCount * 3)));

        // Add completed Tempura/Sashimi sets
        roundScore += (tempuraCount / 2) * 5; // ignore the error so we do integer division
        roundScore += (sashimiCount / 3) * 10;
        // (Incomplete sets not counted here)

        System.out.println("Player " + playerId + " - Tempura: " + ((tempuraCount / 2) * 5));
        System.out.println("Player " + playerId + " - Sashimi: " + ((tempuraCount / 3) * 10));

        // 3. Potential points for future scoring opportunities
        double potentialScore = 0.0;
        // Incomplete Tempura
        if (tempuraCount % 2 == 1) {
            double tempuraInHand = HandCardCounts[SGCard.SGCardType.Tempura.ordinal()];
            // int tempuraScale = Math.min(2, tempuraInHand/state.getNPlayers()); // max 2 tempura can help complete
            double tempuraScale = 2 * ((tempuraInHand/state.getNPlayers()) / 7); // number of tempura per player (normed to 2 because 7 is max possible)
            potentialScore += tempuraScale;  // one tempura waiting for a partner
        }

        System.out.println("Player " + playerId + " - Potential after Tempura: " + potentialScore);
        double temp = potentialScore;

        // Incomplete Sashimi
        int sashimiLeftover = sashimiCount % 3;
        if (sashimiLeftover == 1) {
            double sashimiInHand = HandCardCounts[SGCard.SGCardType.Sashimi.ordinal()];
            // int sashimiScale = Math.min(2, sashimiInHand/state.getNPlayers()); // max 2 sashimi can help complete
            double sashimiScale = 3 * ((sashimiInHand/state.getNPlayers()) / 7); // number of sashimi per player (normed to 2 because 7 is max possible)
            potentialScore += sashimiScale;  // one sashimi (low potential)
        } else if (sashimiLeftover == 2) {
            double sashimiInHand = HandCardCounts[SGCard.SGCardType.Sashimi.ordinal()];
            // int sashimiScale = Math.min(1, sashimiInHand/state.getNPlayers()); // max 1 sashimi can help complete
            double sashimiScale = 5 * ((sashimiInHand/state.getNPlayers()) / 3.5); // number of sashimi per player (normed to 2 because 7 is max possible)
            potentialScore += sashimiScale;  // two sashimi (close to a set)
        }

        System.out.println("Player " + playerId + " - Potential after Sashimi: " + potentialScore + " (added " + (potentialScore - temp) + ")");
        temp = potentialScore;

        // Dumpling incremental potential
        if (dumplingCount < 5) {
            // points increase if one more dumpling is gained
            int nextDumplingPoints = switch (dumplingCount + 1) {
                case 1 -> 1;
                case 2 -> 3;
                case 3 -> 6;
                case 4 -> 10;
                case 5 -> 15;
                default -> 15;
            };
            int currentDumplingPoints = switch (dumplingCount) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 3;
                case 3 -> 6;
                case 4 -> 10;
                default -> 15;
            };
            int delta = nextDumplingPoints - currentDumplingPoints;
            // Determine remaining turns in this round for scaling
            // TODO: more accurate estimate of dumplings left
            double dumplingsInHand = HandCardCounts[SGCard.SGCardType.Dumpling.ordinal()];
            // int dumplingScale = Math.min(1, dumplingsInHand/state.getNPlayers()); // max 1 dumpling can help
            double dumplingScale = (dumplingsInHand/state.getNPlayers()) / 5; // number of dumplings per player (normed to 1 because 5 is max possible)
            // WTF DO I DO???
            int picksLeft = state.getPlayerHands().get(playerId).getSize();
            int numPlayers = state.getNPlayers();
            // NEED: ensure we have the number of cards left for this player (hand size) for picksLeft
            double factor = (numPlayers > 0) ? Math.min(1.0, (double)picksLeft / numPlayers) : 1.0; // IS DOUBLE CORRECT?
            potentialScore += 0.5 * delta * factor;
        }

        System.out.println("Player " + playerId + " - Potential after Dumplings: " + potentialScore + " (added " + (potentialScore - temp) + ")");
        temp = potentialScore;

        // Wasabi potential
        if (wasabiCount > 0) {
            double squidNigiriInHand  = HandCardCounts[SGCard.SGCardType.SquidNigiri.ordinal()];
            double salmonNigiriInHand = HandCardCounts[SGCard.SGCardType.SalmonNigiri.ordinal()];
            double eggNigiriInHand    = HandCardCounts[SGCard.SGCardType.EggNigiri.ordinal()];

            double squidScale  = 3 * ((squidNigiriInHand/state.getNPlayers()) / 2.5); // normed to 3 because 7 is max possible
            double salmonScale = 2 * ((salmonNigiriInHand/state.getNPlayers()) / 5); // normed to 2 because 7 is max possible
            double eggScale    = 1 * ((eggNigiriInHand/state.getNPlayers()) / 2.5); // normed to 1 because 7 is max possible

            double nigiriScale = squidScale + salmonScale + eggScale;

            potentialScore += nigiriScale;
        }

        System.out.println("Player " + playerId + " - Potential after Wasabi: " + potentialScore + " (added " + (potentialScore - temp) + ")");
        temp = potentialScore;

        // Maki roll majority potential
        // Calculate total maki icons for each player to see ranks
        int myMakiIcons = 0;


        // NEED: a way to get total Maki icons count for player (sum of 1-roll,2-roll,3-roll cards)
        // Example approach: if SGCardType had Maki, MakiRoll2, MakiRoll3:
        myMakiIcons = makiCount;
        // Find highest and second-highest maki counts among players
        int highestMaki = myMakiIcons;
        int secondHighestMaki = 0;
        for (int other = 0; other < state.getNPlayers(); other++) {
            if (other == playerId) continue;
            int otherMaki = state.getPlayedCardTypes(SGCard.SGCardType.Maki, other).getValue();
            if (otherMaki > highestMaki) {
                secondHighestMaki = highestMaki;
                highestMaki = otherMaki;
            } else if (otherMaki > secondHighestMaki) {
                secondHighestMaki = otherMaki;
            }
        }
        boolean isTopMaki = (myMakiIcons >= highestMaki && myMakiIcons > 0);
        boolean isSecondMaki = (!isTopMaki && myMakiIcons >= secondHighestMaki && myMakiIcons > 0);
        if (state.getNPlayers() == 2) {
            // In 2-player, only first place gets 6, second (last) gets 0
            if (isTopMaki) potentialScore += 6;
        } else {
            if (isTopMaki) {
                potentialScore += 6;
            } else if (isSecondMaki) {
                potentialScore += 3;
            }
        }

        System.out.println("Player " + playerId + " - Potential after Maki: " + potentialScore + " (added " + (potentialScore - temp) + ")");
        temp = potentialScore;

        // Pudding potential (end game)
        // Check pudding counts for all players
        int maxPudding = puddingCount;
        int minPudding = puddingCount;
        for (int other = 0; other < state.getNPlayers(); other++) {
            int otherPudding = state.getPlayedCardTypes(SGCard.SGCardType.Pudding, other).getValue();
            if (otherPudding > maxPudding) maxPudding = otherPudding;
            if (otherPudding < minPudding) minPudding = otherPudding;
        }
        if (puddingCount == maxPudding && puddingCount > 0) {
            potentialScore += 6;  // currently in lead (or tied for lead) in puddings
        }
        if (puddingCount == minPudding && puddingCount > 0) {
            // Note: if everyone has >0 and this player is lowest, or if player has 0 and others have some
            if (puddingCount < maxPudding) {
                potentialScore -= 6;  // currently last (or tied for last) in puddings
            }
        } else if (puddingCount == 0) {
            // If player has none, they are candidate for last place if others have any
            // (Already covered by above if others have >0)
            // TODO: do I need more here?
        }
        // Chopsticks potential
        if (chopsticksCount > 0) {
            // TODO: THIS LOOKS SUSPICIOUSLY LIKE A MAGIC NUMBER
            // Estimate chopsticks value based on potential combos and average card value
            double avgCardValue = 2.0;
            double comboPotentialSoFar = potentialScore;
            // (We might exclude pudding and maki from comboPotential for this calc, focusing on immediate combos)
            // For simplicity, use current potentialScore as a proxy for combo opportunities
            double chopValue = (comboPotentialSoFar + avgCardValue) / 2.0;
            if (chopValue < 2) chopValue = 2;
            if (chopValue > 6) chopValue = 6;
            potentialScore += chopValue;
        }

        System.out.println("Player " + playerId + " - Potential after Pudding and Chopsticks: " + potentialScore + " (added " + (potentialScore - temp) + ")");

        // TODO: Should I use this?
        // 4. Synergy with cards in hand (immediate next move potential)#
        double handSynergyScore = 0.0;

        Deck<SGCard> hand = state.getPlayerHands().get(playerId);
        double synergySum = 0.0;
        int usefulCount = 0;

        // We assume SGCard has a method or property to get its type and possibly value
        for (SGCard card : hand.getComponents()) {  // using getComponents() to iterate through cards in hand
            SGCard.SGCardType type = card.type;
            switch (type) {
                case Tempura:
                    if (tempuraCount % 2 == 1) {
                        // We have an unmatched Tempura on table, this card would complete it
                        synergySum += 5;  // complete the set for 5 points
                        usefulCount++;
                    }
                    break;
                case Sashimi:
                    if (sashimiCount % 3 == 2) {
                        // Two sashimi on table, this would complete a set
                        synergySum += 10;
                        usefulCount++;
                    } else if (sashimiCount % 3 == 1) {
                        // One sashimi on table, this would make it two
                        synergySum += 4;  // not complete, but gets closer (approx value)
                        usefulCount++;
                    }
                    break;
                case Dumpling:
                    if (dumplingCount > 0 && dumplingCount < 5) {
                        // This dumpling will increase dumpling score by a known increment
                        int currentPts, nextPts;
                        currentPts = switch (dumplingCount) {
                            case 0 -> 0;
                            case 1 -> 1;
                            case 2 -> 3;
                            case 3 -> 6;
                            case 4 -> 10;
                            default -> 15;
                        };
                        nextPts = switch (dumplingCount + 1) {
                            case 1 -> 1;
                            case 2 -> 3;
                            case 3 -> 6;
                            case 4 -> 10;
                            case 5 -> 15;
                            default -> 15;
                        };
                        synergySum += (nextPts - currentPts);
                        usefulCount++;
                    }
                    break;
                case Wasabi:
                    // Wasabi in hand isn't directly useful for existing cards (it affects future nigiri, not current ones)
                    // We skip hand synergy for Wasabi; its value is accounted when played (potentialScore).
                    break;
                case SquidNigiri:
                case SalmonNigiri:
                case EggNigiri:
                    // If we have a Wasabi on table unused, this nigiri can go on it
                    if (wasabiCount > 0) {
                        // Determine nigiri base value
                        int baseVal;
                        if (type == SGCard.SGCardType.SquidNigiri) baseVal = 3;
                        else if (type == SGCard.SGCardType.SalmonNigiri) baseVal = 2;
                        else baseVal = 1;
                        int tripleVal = baseVal * 3;
                        // The benefit of putting this on wasabi vs playing it alone is +2*baseVal
                        // But since without wasabi it would still give baseVal, and we already count baseVal if played (would be in roundScore after played)
                        // Here, consider the full triple as potential since it's not on table yet.
                        synergySum += tripleVal;
                        usefulCount++;
                    }
                    // (If no wasabi on table, a nigiri in hand doesn't specifically combo with current state beyond being flat points,
                    // so we don't count it as "synergy" here. Its value will be realized when played, but that would be evaluated in a future state.)
                    break;
                case Pudding:
                    // If player is currently last in puddings, having a pudding in hand is useful (can play to avoid -6).
                    if (puddingCount == minPudding) {
                        synergySum += 6;
                        usefulCount++;
                    }
                    break;
                case Chopsticks:
                    // Chopsticks in hand doesn't improve current state (only once played). So no immediate synergy.
                    break;
                case Maki:
                    // A Maki card in hand could help lead, but we consider that when it's played (future state).
                    // If currently someone else is leading maki and this card could put us in lead, we might consider synergy.
                    // For simplicity, not doing explicit hand synergy for Maki; it's handled by potential when played.
                    break;
                default:
                    break;
            }
        }

        if (usefulCount > 0) {
            handSynergyScore = synergySum / usefulCount;  // average value of useful cards
        }


        // Total heuristic score = sum of all components
        return gameScore + roundScore + (potentialScore/10) + (handSynergyScore/10);
    }
}
