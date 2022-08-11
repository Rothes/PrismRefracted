/*
 * Prism (Refracted)
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package network.darkhelmet.prism.commands;

import com.google.inject.Inject;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.NamedArguments;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import dev.triumphteam.cmd.core.argument.named.Arguments;

import network.darkhelmet.prism.api.activities.ActivityQuery;
import network.darkhelmet.prism.api.services.purges.IPurgeQueue;
import network.darkhelmet.prism.loader.services.configuration.ConfigurationService;
import network.darkhelmet.prism.services.messages.MessageService;
import network.darkhelmet.prism.services.purge.PurgeService;
import network.darkhelmet.prism.services.query.QueryService;
import network.darkhelmet.prism.services.translation.TranslationKey;

import org.bukkit.command.CommandSender;

@Command(value = "prism", alias = {"pr"})
public class PurgeCommand extends BaseCommand {
    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The query service.
     */
    private final QueryService queryService;

    /**
     * The task chain provider.
     */
    private final PurgeService purgeService;

    /**
     * Construct the purge command.
     *
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param queryService The query service
     * @param purgeService The task chain provider
     */
    @Inject
    public PurgeCommand(
            ConfigurationService configurationService,
            MessageService messageService,
            QueryService queryService,
            PurgeService purgeService) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.queryService = queryService;
        this.purgeService = purgeService;
    }

    /**
     * Run the purge command.
     *
     * @param sender The command sender
     * @param arguments The arguments
     */
    @NamedArguments("params")
    @SubCommand(value = "purge")
    @Permission("prism.admin")
    public void onPurge(final CommandSender sender, final Arguments arguments) {
        if (!purgeService.queueFree()) {
            messageService.error(sender, new TranslationKey("purge-queue-not-free"));
        }

        final ActivityQuery query = queryService
            .queryFromArguments(sender, arguments)
            .limit(configurationService.prismConfig().purges().limit())
            .build();

        IPurgeQueue purgeQueue = purgeService.newQueue(result -> {
            messageService.purgeCycle(sender, result);
        }, result -> messageService.purgeComplete(sender, result.deleted()));

        purgeQueue.add(query);
        purgeQueue.start();

        messageService.purgeStarting(sender);
    }
}