package com.townyelections.night;

import com.palmergames.bukkit.towny.object.Resident;
import com.townyelections.TownyElections;
import com.townyelections.integration.Constituency;
import com.townyelections.manager.CommandConfig;
import com.townyelections.manager.ConfigManager;
import com.townyelections.manager.MessageManager;
import com.townyelections.model.ElectionResult;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The "Election Night" show: a staged, in-game reveal of a concluded election.
 *
 * <p>Instead of the winner being printed the instant voting closes, the tally is
 * presented like a results broadcast. A title card opens the show, then each
 * candidate is revealed from the bottom of the field up (or, for ranked-choice
 * elections, every counting round is replayed one at a time), and finally the
 * winner is crowned with titles, sound cues and party-coloured fireworks.
 *
 * <p>The show is purely presentational: the result is already recorded and the
 * winner's rewards are already applied before the first stage runs, so a
 * cancelled/replayed show can never change the outcome of an election.
 *
 * <p>All scheduling happens on the main server thread through the Bukkit
 * scheduler; a show is a chain of {@link Stage}s, each of which runs an action
 * and then waits a configured number of ticks before the next one starts.
 */
public final class ElectionNight {

    /** Ranks/mayor granted to the winner, echoed by the show's closing stage. */
    public record RewardSummary(List<String> ranks, boolean madeLeader) {

        public static final RewardSummary NONE = new RewardSummary(List.of(), false);

        public RewardSummary {
            ranks = ranks == null ? List.of() : List.copyOf(ranks);
        }
    }

    /** One step of a show: an action plus the delay (in ticks) before the next step. */
    private record Stage(int holdTicks, Runnable action) {
    }

    /** Width of the chat bar chart, in characters. */
    private static final int BAR_WIDTH = 16;

    private final TownyElections plugin;
    private final ConfigManager config;
    private final MessageManager messages;
    private final Random random = new Random();

    /** town uuid -> the show currently running for that town. */
    private final Map<UUID, Show> shows = new HashMap<>();

    public ElectionNight(TownyElections plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessageManager();
    }

    // ========================================================================
    //  Public API
    // ========================================================================

    public boolean isEnabled() {
        return config.isNightEnabled();
    }

    public boolean isRunning(UUID townUuid) {
        return townUuid != null && shows.containsKey(townUuid);
    }

    /**
     * Whether a show would run for this result right now: the feature is enabled,
     * the election had enough ballots, and at least one resident is online to see
     * it. Used by the election manager to decide whether to withhold the usual
     * instant announcements.
     */
    public boolean shouldRun(Constituency constituency, ElectionResult result) {
        if (!config.isNightEnabled() || constituency == null || result == null) {
            return false;
        }
        if (isRunning(constituency.getUuid())) {
            return false;
        }
        if (result.getTotalVotes() < config.getNightMinimumVotes()) {
            return false;
        }
        if (result.getStandings().isEmpty() && result.getRounds().isEmpty()) {
            return false;
        }
        return !audience(constituency).isEmpty();
    }

    /**
     * Starts the show for a freshly concluded election.
     *
     * @param rewards         rewards granted to the winner, echoed at the end
     * @param announceRewards whether the show should announce those rewards itself
     *                        (true when the normal announcements were deferred)
     * @return true if a show started, false if it was skipped
     */
    public boolean start(Constituency constituency, ElectionResult result,
                         RewardSummary rewards, boolean announceRewards) {
        if (constituency == null || result == null || !config.isNightEnabled()) {
            return false;
        }
        if (isRunning(constituency.getUuid())) {
            return false;
        }
        List<Player> audience = audience(constituency);
        if (audience.isEmpty()) {
            return false;
        }
        Show show = new Show(constituency, result,
                rewards == null ? RewardSummary.NONE : rewards, announceRewards);
        shows.put(constituency.getUuid(), show);
        show.begin();
        return true;
    }

    /** Replays an already recorded result as a show (no rewards are re-announced). */
    public boolean replay(Constituency constituency, ElectionResult result) {
        return start(constituency, result, RewardSummary.NONE, false);
    }

    /** Fast-forwards the running show in a town, revealing everything at once. */
    public boolean skip(UUID townUuid) {
        if (townUuid == null) {
            return false;
        }
        Show show = shows.get(townUuid);
        if (show == null) {
            return false;
        }
        show.fastForward();
        return true;
    }

    /** Cancels any running show for a town without announcing anything further. */
    public void cancelShow(UUID townUuid) {
        if (townUuid == null) {
            return;
        }
        Show show = shows.get(townUuid);
        if (show != null) {
            show.cancel();
        }
    }

    /** Cancels every running show. Called on plugin disable. */
    public void shutdown() {
        for (Show show : new ArrayList<>(shows.values())) {
            show.cancel();
        }
        shows.clear();
    }

    /** Online residents of the constituency, in Towny resident order. */
    private List<Player> audience(Constituency constituency) {
        List<Player> players = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        for (Resident resident : constituency.getResidents()) {
            if (resident == null) {
                continue;
            }
            Player online = Bukkit.getPlayer(resident.getUuid());
            if (online != null && seen.add(online.getUniqueId())) {
                players.add(online);
            }
        }
        return players;
    }

    // ========================================================================
    //  The show
    // ========================================================================

    private final class Show {

        private final Constituency constituency;
        private final ElectionResult result;
        private final RewardSummary rewards;
        private final boolean announceRewards;
        private final List<Stage> stages = new ArrayList<>();

        private int cursor;
        private BukkitTask task;
        private BossBar bar;
        private boolean finished;

        private Show(Constituency constituency, ElectionResult result,
                     RewardSummary rewards, boolean announceRewards) {
            this.constituency = constituency;
            this.result = result;
            this.rewards = rewards;
            this.announceRewards = announceRewards;
        }

        // ---- Lifecycle -----------------------------------------------------

        private void begin() {
            buildStages();
            if (config.isNightBossBar() && !stages.isEmpty()) {
                bar = Bukkit.createBossBar(
                        messages.legacy("night.boss-title", basePlaceholders()),
                        BarColor.YELLOW, BarStyle.SOLID);
                bar.setProgress(0.0);
                bar.setVisible(true);
                refreshBarAudience();
            }
            advance();
        }

        private void advance() {
            if (finished) {
                return;
            }
            if (audience(constituency).isEmpty()) {
                // Nobody left to watch; stop quietly rather than talking to an empty town.
                finish();
                return;
            }
            while (true) {
                if (cursor >= stages.size()) {
                    finish();
                    return;
                }
                Stage stage = stages.get(cursor++);
                runStage(stage);
                if (stage.holdTicks() > 0) {
                    task = Bukkit.getScheduler().runTaskLater(plugin, this::advance,
                            Math.max(1L, stage.holdTicks()));
                    return;
                }
            }
        }

        private void runStage(Stage stage) {
            try {
                stage.action().run();
            } catch (Throwable t) {
                plugin.getLogger().warning("Election night stage failed for "
                        + constituency.getName() + ": " + t.getMessage());
                if (config.isDebug()) {
                    t.printStackTrace();
                }
            }
        }

        /** Executes every remaining stage immediately, then closes the show. */
        private void fastForward() {
            if (finished) {
                return;
            }
            if (task != null) {
                task.cancel();
                task = null;
            }
            while (cursor < stages.size()) {
                runStage(stages.get(cursor++));
            }
            finish();
        }

        private void finish() {
            if (finished) {
                return;
            }
            finished = true;
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (bar != null) {
                bar.setVisible(false);
                bar.removeAll();
                bar = null;
            }
            if (shows.get(constituency.getUuid()) == this) {
                shows.remove(constituency.getUuid());
            }
        }

        /** Hard stop used when the plugin disables or a new election starts. */
        private void cancel() {
            if (finished) {
                return;
            }
            finished = true;
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (bar != null) {
                bar.setVisible(false);
                bar.removeAll();
                bar = null;
            }
            shows.remove(constituency.getUuid());
        }

        // ---- Stage construction -------------------------------------------

        private void buildStages() {
            stages.add(new Stage(config.getNightIntroDelayTicks(), this::introStage));

            if (!result.getRounds().isEmpty()) {
                buildRoundStages();
            } else {
                buildStandingStages();
            }

            stages.add(new Stage(config.getNightWinnerHoldTicks(), this::winnerStage));
            stages.add(new Stage(0, this::closingStage));
        }

        private void introStage() {
            Map<String, String> ph = basePlaceholders();
            if (config.isNightTitles()) {
                showTitle("night.title", "night.subtitle", ph);
            }
            updateBar("night.boss-title", BarColor.YELLOW, 0.05, ph);
            broadcast("night.intro", ph);
            playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f);
            playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f);
        }

        /** Plurality / approval: reveal each candidate from worst to best. */
        private void buildStandingStages() {
            List<ElectionResult.Standing> order = revealOrder();
            int total = Math.max(1, order.size());
            int maxVotes = 1;
            for (ElectionResult.Standing standing : order) {
                maxVotes = Math.max(maxVotes, standing.votes);
            }
            final int revealMax = maxVotes;

            int index = 0;
            for (ElectionResult.Standing standing : order) {
                index++;
                final ElectionResult.Standing revealed = standing;
                final int step = index;
                stages.add(new Stage(config.getNightStageDelayTicks(), () -> {
                    Map<String, String> ph = basePlaceholders();
                    ph.put("candidate", revealed.name);
                    ph.put("party", partyDisplay(revealed));
                    ph.put("votes", String.valueOf(revealed.votes));
                    ph.put("percent", percent(revealed.votes, result.getTotalVotes()));
                    ph.put("bar", barChart(revealed.votes, revealMax, revealed.partyColor));
                    ph.put("rank", String.valueOf(result.getStandings().indexOf(revealed) + 1));

                    broadcast("night.candidate-line", ph);
                    updateBar("night.boss-candidate", barColor(revealed.partyColor),
                            (double) step / total, ph);
                    playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f + (0.15f * step));
                }));
            }

            // Standings are recorded best-first, so the leader is always first,
            // regardless of the order the candidates were revealed in.
            ElectionResult.Standing leader = result.getStandings().isEmpty()
                    ? null : result.getStandings().get(0);
            if (leader != null && order.size() > 1) {
                final ElectionResult.Standing top = leader;
                stages.add(new Stage(config.getNightStageDelayTicks(), () -> {
                    Map<String, String> ph = basePlaceholders();
                    ph.put("leader", top.name);
                    ph.put("votes", String.valueOf(top.votes));
                    ph.put("party", partyDisplay(top));
                    broadcast("night.leading", ph);
                    playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f);
                }));
            }
        }

        /** Ranked choice: replay every instant-runoff counting round. */
        private void buildRoundStages() {
            List<ElectionResult.Round> rounds = result.getRounds();
            int totalRounds = Math.max(1, rounds.size());

            for (ElectionResult.Round round : rounds) {
                final ElectionResult.Round current = round;
                stages.add(new Stage(shortHold(), () -> {
                    Map<String, String> ph = basePlaceholders();
                    ph.put("round", String.valueOf(current.number()));
                    ph.put("rounds", String.valueOf(totalRounds));
                    broadcast("night.round-header", ph);
                    updateBar("night.boss-round-header", BarColor.YELLOW,
                            (double) current.number() / totalRounds, ph);
                    playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f);
                }));

                int maxVotes = 1;
                for (ElectionResult.RoundEntry entry : round.entries()) {
                    maxVotes = Math.max(maxVotes, entry.votes());
                }
                final int entryMax = maxVotes;

                for (ElectionResult.RoundEntry entry : round.entries()) {
                    final ElectionResult.RoundEntry revealed = entry;
                    stages.add(new Stage(shortHold(), () -> {
                        Map<String, String> ph = basePlaceholders();
                        ph.put("candidate", revealed.name());
                        ph.put("votes", String.valueOf(revealed.votes()));
                        ph.put("bar", barChart(revealed.votes(), entryMax, ""));
                        ph.put("round", String.valueOf(current.number()));
                        broadcast("night.round-line", ph);
                        updateBar("night.boss-round-line", BarColor.YELLOW,
                                (double) current.number() / totalRounds, ph);
                        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f);
                    }));
                }

                if (!round.eliminated().isEmpty()) {
                    final String eliminated = String.join(", ", round.eliminated());
                    stages.add(new Stage(shortHold(), () -> {
                        Map<String, String> ph = basePlaceholders();
                        ph.put("candidates", eliminated);
                        ph.put("round", String.valueOf(current.number()));
                        broadcast("night.round-eliminated", ph);
                        playSound(Sound.BLOCK_ANVIL_LAND, 0.6f);
                    }));
                }
                if (round.exhausted() > 0) {
                    final int exhausted = round.exhausted();
                    stages.add(new Stage(shortHold(), () -> {
                        Map<String, String> ph = basePlaceholders();
                        ph.put("count", String.valueOf(exhausted));
                        ph.put("round", String.valueOf(current.number()));
                        broadcast("night.round-exhausted", ph);
                    }));
                }
            }
        }

        /** Ascending, descending, or shuffled — per {@code election-night.reveal-order}. */
        private List<ElectionResult.Standing> revealOrder() {
            List<ElectionResult.Standing> order = new ArrayList<>(result.getStandings());
            switch (config.getNightRevealOrder()) {
                case "BEST_FIRST" -> {
                    // Standings are already recorded best-first.
                }
                case "RANDOM" -> Collections.shuffle(order, random);
                default -> Collections.reverse(order);
            }
            return order;
        }

        // ---- The finale ----------------------------------------------------

        private void winnerStage() {
            Map<String, String> ph = basePlaceholders();
            if (!result.hasWinner()) {
                if (config.isNightTitles()) {
                    showTitle("night.no-winner-title", "night.no-winner-subtitle", ph);
                }
                updateBar("night.boss-no-winner", BarColor.WHITE, 1.0, ph);
                broadcast("night.no-winner-chat", ph);
                playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f);
                return;
            }

            ph.put("winner", result.getWinnerName());
            ph.put("winner_party", winnerPartyDisplay());
            ph.put("votes", String.valueOf(result.getWinnerVotes()));
            ph.put("percent", percent(result.getWinnerVotes(), result.getTotalVotes()));

            if (config.isNightTitles()) {
                showTitle("night.winner-title", "night.winner-subtitle", ph);
            }
            updateBar("night.boss-winner", barColor(winnerPartyColor()), 1.0, ph);
            broadcast("night.winner-chat", ph);

            if (config.isNightBroadcastServerWide()) {
                Bukkit.broadcast(messages.prefixed(fill(messages.raw("night.winner-server"), ph)));
            }

            playSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9f);
            playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f);
            playSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f);

            if (config.isNightFireworks() && config.getNightFireworkCount() > 0) {
                String color = winnerPartyColor();
                for (Player viewer : audience(constituency)) {
                    FireworkCelebration.launch(viewer, color, config.getNightFireworkCount(), random);
                }
            }
        }

        private void closingStage() {
            Map<String, String> ph = basePlaceholders();
            broadcast("night.turnout", ph);

            if (result.hasWinner()) {
                ph.put("winner", result.getWinnerName());
                ph.put("winner_party", winnerPartyDisplay());
                if (announceRewards) {
                    if (!rewards.ranks().isEmpty()) {
                        ph.put("ranks", String.join(", ", rewards.ranks()));
                        broadcast("winner.ranks-granted", ph);
                    }
                    if (rewards.madeLeader()) {
                        broadcast("winner.made-mayor", ph);
                    }
                }
            }

            ph.put("label", "election");
            ph.put("results", plugin.getCommandConfig().literal(CommandConfig.RESULTS));
            broadcast("night.close", ph);
            playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.4f);
        }

        // ---- Presentation helpers -----------------------------------------

        private void broadcast(String key, Map<String, String> placeholders) {
            for (Player player : audience(constituency)) {
                messages.send(player, key, placeholders);
            }
        }

        private void showTitle(String titleKey, String subtitleKey, Map<String, String> placeholders) {
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(300), Duration.ofMillis(2200), Duration.ofMillis(700));
            Title title = Title.title(
                    messages.component(titleKey, placeholders),
                    messages.component(subtitleKey, placeholders),
                    times);
            for (Player player : audience(constituency)) {
                player.showTitle(title);
            }
        }

        private void updateBar(String titleKey, BarColor color, double progress,
                               Map<String, String> placeholders) {
            if (bar == null) {
                return;
            }
            bar.setTitle(messages.legacy(titleKey, placeholders));
            bar.setColor(color);
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            refreshBarAudience();
        }

        /** Keeps the boss bar attached to anyone who logged in mid-show. */
        private void refreshBarAudience() {
            if (bar == null) {
                return;
            }
            List<Player> viewers = audience(constituency);
            Set<UUID> attached = new HashSet<>();
            for (Player player : bar.getPlayers()) {
                attached.add(player.getUniqueId());
            }
            for (Player player : viewers) {
                if (!attached.contains(player.getUniqueId())) {
                    bar.addPlayer(player);
                }
            }
        }

        private void playSound(Sound sound, float pitch) {
            if (!config.isNightSounds()) {
                return;
            }
            for (Player player : audience(constituency)) {
                player.getWorld().playSound(player.getLocation(), sound, 1.0f, pitch);
            }
        }

        // ---- Placeholders --------------------------------------------------

        private Map<String, String> basePlaceholders() {
            int voters = result.getTotalVotes();
            int residents = Math.max(1, result.getResidentCount());
            return MessageManager.placeholders(
                    "town", result.getTownName(),
                    "system", messages.raw(result.getVotingSystem().messageKey()),
                    "total", String.valueOf(voters),
                    "voters", String.valueOf(voters),
                    "residents", String.valueOf(result.getResidentCount()),
                    "turnout", String.valueOf(Math.round((voters * 100.0) / residents)),
                    "candidates", String.valueOf(result.getStandings().size()),
                    "rounds", String.valueOf(result.getRounds().size()),
                    "winner", result.hasWinner() ? result.getWinnerName() : "",
                    "label", "election");
        }

        private String partyDisplay(ElectionResult.Standing standing) {
            String color = standing.partyColor == null ? "" : standing.partyColor;
            return color + standing.partyName;
        }

        private String winnerPartyColor() {
            for (ElectionResult.Standing standing : result.getStandings()) {
                if (standing.uuid.equals(result.getWinnerUuid())) {
                    return standing.partyColor == null ? "" : standing.partyColor;
                }
            }
            return "";
        }

        private String winnerPartyDisplay() {
            for (ElectionResult.Standing standing : result.getStandings()) {
                if (standing.uuid.equals(result.getWinnerUuid())) {
                    return partyDisplay(standing);
                }
            }
            return plugin.getConfigManager().getDefaultPartyName();
        }

        private int shortHold() {
            return Math.max(10, config.getNightStageDelayTicks() / 2);
        }
    }

    // ========================================================================
    //  Shared formatting helpers
    // ========================================================================

    /** Renders a coloured bar chart for chat lines. */
    private String barChart(int votes, int maxVotes, String colorCode) {
        int max = Math.max(1, maxVotes);
        int filled = (int) Math.round((votes * (double) BAR_WIDTH) / max);
        filled = Math.max(votes > 0 ? 1 : 0, Math.min(BAR_WIDTH, filled));
        String color = colorCode == null || colorCode.isEmpty() ? "&f" : colorCode;
        return color + "█".repeat(filled) + "&8" + "░".repeat(BAR_WIDTH - filled);
    }

    private String percent(int votes, int total) {
        if (total <= 0) {
            return "0";
        }
        return String.valueOf(Math.round((votes * 100.0) / total));
    }

    private String fill(String text, Map<String, String> placeholders) {
        String out = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return out;
    }

    /** Closest boss-bar colour for a legacy/hex colour code. */
    private BarColor barColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return BarColor.WHITE;
        }
        if (colorCode.startsWith("&#") && colorCode.length() >= 8) {
            try {
                int rgb = Integer.parseInt(colorCode.substring(2, 8), 16);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red >= green && red >= blue) {
                    return BarColor.RED;
                }
                if (green >= red && green >= blue) {
                    return BarColor.GREEN;
                }
                return BarColor.BLUE;
            } catch (NumberFormatException ignored) {
                return BarColor.WHITE;
            }
        }
        char code = Character.toLowerCase(colorCode.charAt(colorCode.length() - 1));
        return switch (code) {
            case '1', '3', '9', 'b' -> BarColor.BLUE;
            case '2', 'a' -> BarColor.GREEN;
            case '4', 'c' -> BarColor.RED;
            case '5', 'd' -> BarColor.PURPLE;
            case '6', 'e' -> BarColor.YELLOW;
            case '0', '8', '7', 'f', 'r' -> BarColor.WHITE;
            default -> BarColor.WHITE;
        };
    }
}
