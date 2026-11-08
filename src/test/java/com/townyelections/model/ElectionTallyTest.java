package com.townyelections.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ballot handling and tallying on {@link Election}: the per-system
 * counts, tie detection, and the cleanup that happens when a candidate
 * withdraws.
 */
class ElectionTallyTest {

    private static final UUID TOWN = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID V1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID V2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID V3 = UUID.fromString("00000000-0000-0000-0000-000000000103");

    private Election election(VotingSystem system) {
        Election election = new Election(TOWN, "Oakvale", ElectionPhase.VOTING, 0L);
        election.setVotingSystem(system);
        election.addCandidate(new Candidate(ALICE, "Alice", "More farms."));
        election.addCandidate(new Candidate(BOB, "Bob", "Lower taxes."));
        election.addCandidate(new Candidate(CAROL, "Carol", "Better roads."));
        return election;
    }

    @Test
    void pluralityOnlyCountsFirstChoices() {
        Election election = election(VotingSystem.PLURALITY);
        election.castVote(V1, ALICE);
        election.castVote(V2, ALICE);
        election.castVote(V3, BOB);

        assertEquals(2, election.tally().get(ALICE));
        assertEquals(1, election.tally().get(BOB));
        assertEquals(0, election.tally().get(CAROL));
        assertEquals(List.of(ALICE), election.topCandidates());
        assertEquals(3, election.getTotalVotes());
    }

    @Test
    void approvalCountsEveryApprovedCandidate() {
        Election election = election(VotingSystem.APPROVAL);
        election.castBallot(V1, List.of(ALICE, BOB));
        election.castBallot(V2, List.of(BOB));

        assertEquals(1, election.tally().get(ALICE));
        assertEquals(2, election.tally().get(BOB));
        assertEquals(0, election.tally().get(CAROL));
    }

    @Test
    void rankedChoiceTallyUsesTheFirstPreference() {
        Election election = election(VotingSystem.RANKED_CHOICE);
        election.castBallot(V1, List.of(BOB, ALICE));

        assertEquals(1, election.tally().get(BOB));
        assertEquals(0, election.tally().get(ALICE));
    }

    @Test
    void tiedLeadersAreAllReturned() {
        Election election = election(VotingSystem.PLURALITY);
        election.castVote(V1, ALICE);
        election.castVote(V2, BOB);

        List<UUID> top = election.topCandidates();

        assertEquals(2, top.size());
        assertTrue(top.containsAll(List.of(ALICE, BOB)));
    }

    @Test
    void withdrawingACandidateDropsBallotsThatOnlyRankedThem() {
        Election election = election(VotingSystem.PLURALITY);
        election.castVote(V1, ALICE);
        election.castVote(V2, ALICE);

        election.removeCandidate(ALICE);

        assertEquals(0, election.getTotalVotes());
        assertFalse(election.hasVoted(V1));
    }

    @Test
    void withdrawingACandidateKeepsLaterPreferences() {
        Election election = election(VotingSystem.RANKED_CHOICE);
        election.castBallot(V1, List.of(ALICE, BOB));
        election.castBallot(V2, List.of(ALICE, CAROL));

        election.removeCandidate(ALICE);

        assertEquals(List.of(BOB), election.getBallot(V1));
        assertEquals(List.of(CAROL), election.getBallot(V2));
        assertEquals(2, election.getTotalVotes());
    }

    @Test
    void castingAgainReplacesTheEarlierBallot() {
        Election election = election(VotingSystem.PLURALITY);
        election.castVote(V1, ALICE);
        election.castVote(V1, CAROL);

        assertEquals(1, election.getTotalVotes());
        assertEquals(CAROL, election.getVoteChoice(V1));
    }
}
