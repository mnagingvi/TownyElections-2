package com.townyelections.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioural tests for the instant-runoff counter that powers
 * {@link VotingSystem#RANKED_CHOICE} elections.
 */
class InstantRunoffTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void firstRoundMajorityWinsImmediately() {
        List<List<UUID>> ballots = List.of(
                List.of(A), List.of(A), List.of(A), List.of(B));

        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B), ballots);

        assertEquals(List.of(A), outcome.finalists());
        assertEquals(1, outcome.rounds().size());
        assertEquals(3, outcome.rounds().get(0).counts().get(A));
    }

    @Test
    void weakestCandidateIsEliminatedAndVotesTransfer() {
        // Round 1: A=2, B=2, C=1 -> C is eliminated, its ballot goes to B.
        // Round 2: B=3 of 5 continuing ballots -> B holds a majority.
        List<List<UUID>> ballots = List.of(
                List.of(A), List.of(A), List.of(B), List.of(B), List.of(C, B));

        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B, C), ballots);

        assertEquals(List.of(B), outcome.finalists());
        assertEquals(2, outcome.rounds().size());
        assertEquals(List.of(C), outcome.rounds().get(0).eliminated());
        assertEquals(3, outcome.rounds().get(1).counts().get(B));
        assertEquals(2, outcome.rounds().get(1).counts().get(A));
    }

    @Test
    void ballotsExhaustWhenTheirPreferencesAreAllEliminated() {
        // Round 1: A=3, B=2, C=2 -> B and C are both eliminated.
        // Round 2: only A remains, and the four other ballots are exhausted.
        List<List<UUID>> ballots = List.of(
                List.of(A), List.of(A), List.of(A),
                List.of(B), List.of(B),
                List.of(C), List.of(C));

        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B, C), ballots);

        assertEquals(List.of(A), outcome.finalists());
        assertEquals(2, outcome.rounds().size());
        assertEquals(List.of(B, C), outcome.rounds().get(0).eliminated());
        assertEquals(4, outcome.rounds().get(1).exhausted());
    }

    @Test
    void completeTieHandsEverySurvivorToTheTieBreaker() {
        List<List<UUID>> ballots = List.of(List.of(A), List.of(B));

        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B), ballots);

        assertEquals(2, outcome.finalists().size());
        assertTrue(outcome.finalists().containsAll(List.of(A, B)));
        assertEquals(1, outcome.rounds().size());
    }

    @Test
    void finishOrderRanksWinnerThenEliminatedCandidates() {
        List<List<UUID>> ballots = List.of(
                List.of(A), List.of(A), List.of(B), List.of(C));

        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B, C), ballots);

        assertEquals(List.of(A), outcome.finalists());
        assertEquals(List.of(A, B, C), outcome.finishOrder());
    }

    @Test
    void emptyBallotBoxDoesNotProduceAWinner() {
        InstantRunoff.Outcome outcome = InstantRunoff.count(List.of(A, B), List.of());

        assertEquals(1, outcome.rounds().size());
        Map<UUID, Integer> counts = outcome.rounds().get(0).counts();
        assertEquals(0, counts.get(A));
        assertEquals(0, counts.get(B));
    }
}
