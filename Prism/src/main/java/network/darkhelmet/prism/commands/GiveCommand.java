package network.darkhelmet.prism.commands;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.QueryResult;
import network.darkhelmet.prism.actions.ItemStackAction;
import network.darkhelmet.prism.api.actions.Handler;
import network.darkhelmet.prism.commandlibs.CallInfo;
import network.darkhelmet.prism.commandlibs.SubHandler;
import network.darkhelmet.prism.utils.TypeUtils;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GiveCommand implements SubHandler {

    private final Prism plugin;

    /**
     * GiveCommand.
     *
     * @param plugin Prism.
     */
    GiveCommand(Prism plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command.
     */
    @Override
    public void handle(CallInfo call) {

        // Is there anything even stored to paginate?
        String keyName = "console";
        if (call.getSender() instanceof Player) {
            keyName = call.getSender().getName();
        }
        if (!plugin.cachedQueries.containsKey(keyName) && !call.getArg(1).contains("id:")) {
            Prism.messenger.sendMessage(call.getSender(), Prism.messenger.playerError(
                    "您没有已保存的查询. 也许它们过期了? 试试再查询一次."));
            return;
        }

        // Parse the incoming ident
        String ident = call.getArg(1);
        if (ident.contains("id:")) {
            ident = ident.replace("id:", "");
        }

        // Determine result index to give to - either an id, or the next/previous
        // id
        long recordId;
        if (ident.equals("next") || ident.equals("prev") || ident.equals("下") || ident.equals("上")) {
            // Get stored results
            final QueryResult results = plugin.cachedQueries.get(keyName);
            recordId = results.getLastGiveIndex();
            recordId = (recordId == 0 ? 1 : recordId);
            if (recordId > 0) {
                long tempId = recordId;
                if (ident.equals("next") || ident.equals("下")) {
                    while (results.getActionResults().size() > tempId) {
                        tempId++;
                        if (results.getActionResults().get((int) tempId - 1) instanceof ItemStackAction) {
                            recordId = tempId;
                            break;
                        }
                    }
                } else {
                    while (tempId > 1) {
                        tempId--;
                        if (results.getActionResults().get((int) tempId - 1) instanceof ItemStackAction) {
                            recordId = tempId;
                            break;
                        }
                    }
                }
            }
        } else {
            if (!TypeUtils.isNumeric(ident)) {
                Prism.messenger.sendMessage(call.getPlayer(), Prism.messenger
                        .playerError("您必须提供一个结果编号数字或者记录ID来给予您物品."));
                return;
            }
            recordId = Integer.parseInt(ident);
            if (recordId <= 0) {
                Prism.messenger.sendMessage(call.getPlayer(),
                        Prism.messenger.playerError("结果编号或记录ID必须大于0."));
                return;
            }
        }

        // If a record id provided, re-query the database
        final Handler targetAction;
        if (call.getArg(1).contains("id:")) {

            // Build params
            final QueryParameters params = new QueryParameters();
            params.setWorld(call.getPlayer().getWorld().getName());
            params.setId(recordId);

            // Query
            final ActionsQuery aq = new ActionsQuery(plugin);
            final QueryResult results = aq.lookup(params, call.getPlayer());
            if (results.getActionResults().isEmpty()) {
                Prism.messenger.sendMessage(call.getPlayer(),
                        Prism.messenger.playerError("此 ID 的记录不存在."));
                return;
            }

            // Get the first result
            targetAction = results.getActionResults().get(0);

        } else {

            // Get stored results
            final QueryResult results = plugin.cachedQueries.get(keyName);

            if (recordId > results.getActionResults().size()) {
                Prism.messenger.sendMessage(call.getPlayer(), Prism.messenger.playerError(
                        "此索引的记录不存在. 可能您想使用的是 /pr give id:" + recordId + " ?"));
                return;
            }

            final int key = (int) (recordId - 1);

            // Get the result index specified
            targetAction = results.getActionResults().get(key);

            // Refresh the query time and replace
            results.setQueryTime();
            results.setLastGiveIndex(recordId);
            plugin.cachedQueries.replace(keyName, results);
        }

        Player player = call.getPlayer();
        if (!(targetAction instanceof ItemStackAction)) {
            Prism.messenger.sendMessage(player, Prism.messenger.playerError("此行为记录不属于 ItemStackAction."));
            return;
        }
        ItemStack item = ((ItemStackAction) targetAction).getItem();
        item.setAmount(item.getAmount() * targetAction.getAggregateCount());
        player.getInventory().addItem(item);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1);
    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        return null;
    }

    @Override
    public String[] getHelp() {
        return new String[]{Il8nHelper.getRawMessage("help-give")};
    }

    @Override
    public String getRef() {
        return "/lookup.html#giving";
    }

}
