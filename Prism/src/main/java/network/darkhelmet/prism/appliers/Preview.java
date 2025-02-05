package network.darkhelmet.prism.appliers;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actions.BlockAction;
import network.darkhelmet.prism.actions.GenericAction;
import network.darkhelmet.prism.actions.HangingItemAction;
import network.darkhelmet.prism.actions.ItemStackAction;
import network.darkhelmet.prism.actions.SignChangeAction;
import network.darkhelmet.prism.api.BlockStateChange;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.actions.Handler;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import network.darkhelmet.prism.api.commands.Flag;
import network.darkhelmet.prism.api.objects.ApplierResult;
import network.darkhelmet.prism.events.EventHelper;
import network.darkhelmet.prism.events.PrismRollBackEvent;
import network.darkhelmet.prism.text.ReplaceableTextComponent;
import network.darkhelmet.prism.utils.EntityUtils;
import network.darkhelmet.prism.utils.folia.PrismScheduler;
import network.darkhelmet.prism.utils.folia.PrismTask;
import network.darkhelmet.prism.wands.RollbackWand;
import network.darkhelmet.prism.wands.Wand;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class Preview implements Previewable {

    protected final Prism plugin;
    protected final CommandSender sender;
    protected final Player player;
    protected final QueryParameters parameters;
    protected final List<BlockStateChange> blockStateChanges = new ArrayList<>();
    private final PrismProcessType processType;
    private final HashMap<Entity, Integer> entitiesMoved = new HashMap<>();
    private final List<Handler> worldChangeQueue = Collections.synchronizedList(new LinkedList<>());
    private final List<Handler> updateRollbackedList = new ArrayList<>();
    private boolean isPreview = false;
    private long startTime;
    private int totalChangesCount;
    private int skippedBlockCount;
    private int stateSkippedCount = 0;
    private int changesAppliedCount;
    private int changesPlannedCount;
    private int blockChangesRead = 0;
    private PrismTask worldChangeQueueTask;
    private ApplierCallback callback;

    /**
     * Constructor.
     *
     * @param plugin Prism
     */
    public Preview(Prism plugin, CommandSender sender, Collection<Handler> results, QueryParameters parameters,
                   ApplierCallback callback) {

        this.processType = parameters.getProcessType();
        this.plugin = plugin;
        this.sender = sender;
        this.parameters = parameters;

        if (sender instanceof Player) {
            this.player = (Player) sender;
        } else {
            this.player = null;
        }

        if (callback != null) {
            this.callback = callback;
        }

        // Append all actions to the queue.
        worldChangeQueue.addAll(results);

    }

    @Override
    public void setIsPreview(boolean isPreview) {
        this.isPreview = isPreview;
    }

    @Override
    public void cancel_preview() {
        if (player == null) {
            return;
        }
        if (!blockStateChanges.isEmpty()) {

            // pull all players that are part of this preview
            final List<CommandSender> previewPlayers = parameters.getSharedPlayers();
            previewPlayers.add(player);

            for (final BlockStateChange u : blockStateChanges) {
                Location loc = u.getOriginalBlock().getLocation();
                BlockData data = u.getOriginalBlock().getBlockData();

                for (final CommandSender sharedPlayer : previewPlayers) {
                    if (sharedPlayer instanceof Player) {
                        EntityUtils.sendBlockChange((Player) sharedPlayer, loc, data);
                    }
                }
            }
        }
        Prism.messenger.sendMessage(sender,
                Prism.messenger.playerHeaderMsg(Il8nHelper.getMessage("preview-cancel")));
    }

    @Override
    public void apply_preview() {
        if (player == null) {
            return;
        }
        Prism.messenger.sendMessage(sender,
                Prism.messenger.playerHeaderMsg(Il8nHelper.getMessage("preview-apply-start")));
        setIsPreview(false);
        changesAppliedCount = 0;
        skippedBlockCount = 0;
        changesPlannedCount = 0;
        apply();
    }

    @Override
    public void preview() {
    }

    @Override
    public void apply() {

        if (!worldChangeQueue.isEmpty()) {

            if (!isPreview && player != null) {

                Wand oldWand = null;
                if (Prism.playersWithActiveTools.containsKey(player.getName())) {
                    // Pull the wand in use
                    oldWand = Prism.playersWithActiveTools.get(player.getName());
                }

                boolean showNearby = true;
                if (oldWand instanceof RollbackWand) {
                    showNearby = false;
                }
                if (showNearby) {
                    // Inform nearby players
                    plugin.notifyNearby(player, parameters.getRadius(), ReplaceableTextComponent.builder("notify-near")
                          .replace("<player>", player.getDisplayName())
                          .replace("<processType>", processType.getLocale())
                          .build());
                    // Inform staff
                    if (plugin.getConfig().getBoolean("prism.alerts.alert-staff-to-applied-process")) {
                        final String cmd = parameters.getOriginalCommand();
                        if (cmd != null) {
                            plugin.alertPlayers(player, ReplaceableTextComponent.builder("notify-staff")
                                    .replace("<player>", player.getDisplayName())
                                    .replace("<processType>", processType.getLocale())
                                    .replace("<originalCommand>", parameters.getOriginalCommand(),
                                            Style.style(NamedTextColor.GRAY))
                                    .build().colorIfAbsent(NamedTextColor.WHITE), null);
                        }
                    }
                }
            }

            // Offload the work of world changes
            // to a scheduled sync task
            processWorldChanges();
        }
    }

    private void processWorldChanges() {

        startTime = System.nanoTime();
        blockChangesRead = 0;
        totalChangesCount = worldChangeQueue.size();
        Prism.debug("世界更改队列大小: " + totalChangesCount);

        if (worldChangeQueue.isEmpty()) {
            Prism.messenger.sendMessage(sender,
                    Prism.messenger.playerError(Il8nHelper.getMessage("preview-no-actions")));
            return;
        }

        boolean match = parameters.hasFlag(Flag.MATCHES_STATE);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        nf.setRoundingMode(RoundingMode.DOWN);

        Component progressComponent = ReplaceableTextComponent.builder("applier-actionbar-applying")
                .replace("<processType>", processType.getLocale() + (isPreview ? "预览" : "")).build();
        Pattern percentage = Pattern.compile("<percentage>");
        Pattern elapsed = Pattern.compile("<elapsed>");

        // TODO: FOLIA TEST
        worldChangeQueueTask = PrismScheduler.scheduleSyncRepeatingTask(() -> {

            Prism.messenger.sendActionBar(sender, progressComponent
                    .replaceText(percentage, builder -> Component.text(nf.format((isPreview ?
                            (blockChangesRead / (float) worldChangeQueue.size() * 100) :
                            ((totalChangesCount - worldChangeQueue.size()) / (float) totalChangesCount * 100)))))
                    .replaceText(elapsed, builder -> Component.text(((System.nanoTime() - startTime) / 1000000000) + "s"))
                    .color(NamedTextColor.GOLD));

            int iterationCount = 0;
            final int currentQueueOffset = blockChangesRead;

            if (currentQueueOffset < worldChangeQueue.size()) {
                for (final Iterator<Handler> iterator = worldChangeQueue.listIterator(currentQueueOffset);
                      iterator.hasNext();) {
                    final Handler a = iterator.next();
                    if (isPreview) {
                        blockChangesRead++;
                    }
                    iterationCount++;
                    if (iterationCount >= 1000) {
                        break;
                    }
                    if (processType.equals(PrismProcessType.ROLLBACK) && !a.getActionType().canRollback()) {
                        iterator.remove();
                        continue;
                    }

                    if (processType.equals(PrismProcessType.RESTORE) && !a.getActionType().canRestore()) {
                        iterator.remove();
                        continue;
                    }

                    ChangeResult result = null;

                    try {
                        if (a instanceof GenericAction) {
                            GenericAction action = (GenericAction) a;
                            if (processType.equals(PrismProcessType.ROLLBACK)) {
                                if (match && action.isRollbacked()) {
                                    // We don't need to check the action because it can be rolled-back.
                                    stateSkippedCount++;
                                    skippedBlockCount++;
                                    iterator.remove();
                                    continue;
                                }
                                result = action.applyRollback(player, parameters, isPreview);
                                if (!isPreview && result.getType() == ChangeResultType.APPLIED) {
                                    action.setRollbacked(true);
                                    updateRollbackedList.add(action);
                                }
                            }
                            if (processType.equals(PrismProcessType.RESTORE)) {
                                if (match && !action.isRollbacked()) {
                                    if (action instanceof BlockAction || action instanceof HangingItemAction
                                            || action instanceof ItemStackAction || action instanceof SignChangeAction) {
                                        stateSkippedCount++;
                                        skippedBlockCount++;
                                    }
                                    iterator.remove();
                                    continue;
                                }
                                result = action.applyRestore(player, parameters, isPreview);
                                if (!isPreview && result.getType() == ChangeResultType.APPLIED) {
                                    action.setRollbacked(false);
                                    updateRollbackedList.add(action);
                                }
                            }
                            if (processType.equals(PrismProcessType.UNDO)) {
                                result = action.applyUndo(player, parameters, isPreview);
                            }
                        }

                        if (result == null) {
                            iterator.remove();
                            continue;
                        }
                        if (result.getType() == null) {
                            skippedBlockCount++;
                            iterator.remove();
                            continue;
                        } else if (result.getType().equals(ChangeResultType.SKIPPED)) {
                            skippedBlockCount++;
                            iterator.remove();
                            continue;
                        } else if (result.getType().equals(ChangeResultType.PLANNED)) {
                            changesPlannedCount++;
                            continue;
                        } else {
                            blockStateChanges.add(result.getBlockStateChange());
                            changesAppliedCount++;
                        }
                        if (!isPreview) {
                            iterator.remove();
                        }
                    } catch (final Exception e) {
                        String msg = e.getMessage() == null ? "未知原因" : e.getMessage();
                        Prism.log(String.format("应用器错误: %s (ID: %d)", msg, a.getId()));
                        Prism.log(String.format("方块类型: %s (旧类型: %s)", a.getMaterial(), a.getOldMaterial()));
                        Prism.log(String.format("方块坐标: %d, %d, %d",
                                a.getLoc().getBlockX(),
                                a.getLoc().getBlockY(),
                                a.getLoc().getBlockZ()));
                        e.printStackTrace();

                        // Count as skipped, remove from queue
                        skippedBlockCount++;
                        iterator.remove();
                    }
                }
            }

            // The task for this action is done being used
            if (worldChangeQueue.isEmpty() || blockChangesRead >= worldChangeQueue.size()) {
                worldChangeQueueTask.cancel();
                if (isPreview) {
                    postProcessPreview();
                } else {
                    PrismScheduler.runTaskAsynchronously(() -> new ActionsQuery(plugin).
                            updateRollbacked(updateRollbackedList.toArray(new Handler[0])));
                    postProcess();
                }

                Prism.messenger.sendActionBar(sender, ReplaceableTextComponent.builder("applier-actionbar-finished")
                        .replace("<processType>", processType.getLocale() + (isPreview? "预览": ""))
                        .replace("<elapsed>", ((System.nanoTime() - startTime) / 1000000000f) + "s")
                        .build().color(NamedTextColor.GOLD));
            }
        }, worldChangeQueue.get(0).getLoc(), 2L, 2L);
    }

    private void postProcessPreview() {
        // If there's planned changes, save the preview
        if (isPreview && (changesAppliedCount > 0 || changesPlannedCount > 0)) {
            // Append the preview and blocks temporarily
            final PreviewSession ps = new PreviewSession(player, this);
            plugin.playerActivePreviews.put(player.getName(), ps);
            moveEntitiesToSafety();
        }
        fireApplierCallback();
    }

    private void postProcess() {

        moveEntitiesToSafety();

        fireApplierCallback();

    }

    private void moveEntitiesToSafety() {
        if (parameters.getWorld() != null && player != null) {
            // TODO: Folia - may throw `IllegalStateException: Cannot getEntities asynchronously` if large radius; Waiting for API
            final List<Entity> entities = player.getNearbyEntities(
                    parameters.getRadius(), parameters.getRadius(), parameters.getRadius());
            entities.add(player);
            for (final Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    int add = 0;
                    if (EntityUtils.inCube(parameters.getPlayerLocation(), parameters.getRadius(),
                            entity.getLocation())) {
                        final Location l = entity.getLocation();
                        while (!EntityUtils.playerMayPassThrough(l.getBlock().getType())) {
                            add++;
                            if (l.getY() >= 256) {
                                break;
                            }
                            l.setY(l.getY() + 1);
                        }
                        if (add > 0) {
                            entitiesMoved.put(entity, add);
                            if (Prism.isFolia) {
                                entity.teleportAsync(l);
                            } else {
                                entity.teleport(l);
                            }
                        }
                    }
                }
            }

        }
    }

    private void fireApplierCallback() {

        // If previewing, the applied count will never apply, we'll
        // assume it's all planned counts
        if (isPreview) {
            changesPlannedCount += changesAppliedCount;
            changesAppliedCount = 0;
        }

        final ApplierResult results = new ApplierResult(isPreview, changesAppliedCount, skippedBlockCount,
                changesPlannedCount, stateSkippedCount, blockStateChanges, parameters, entitiesMoved);

        if (callback != null) {
            callback.handle(sender, results);
        }

        // Trigger the events
        if (processType.equals(PrismProcessType.ROLLBACK)) {
            final PrismRollBackEvent event = EventHelper.createRollBackEvent(blockStateChanges, player, parameters,
                  results);
            plugin.getServer().getPluginManager().callEvent(event);
        }

        plugin.eventTimer.recordTimedEvent("应用器操作已完成");

        // record timed events to log
        if (Prism.isDebug()) {
            // Flush timed data
            plugin.eventTimer.printTimeRecord();
            Prism.debug("变化数: " + changesAppliedCount);
            Prism.debug("计划数: " + changesPlannedCount);
            Prism.debug("跳过数: " + skippedBlockCount + (stateSkippedCount > 0 ? "(" + stateSkippedCount + " 因状态忽略)" : ""));
        }
    }
}