package com.avairebot.plugin.utility;

import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.plugin.JavaPlugin;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SuggestCommand extends Command {

    private final JavaPlugin plugin;

    public SuggestCommand(JavaPlugin plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Suggest Command";
    }

    @Override
    public String getDescription() {
        return plugin.getConfig().getString(
                "command-settings.description",
                "Suggest a feature to the server."
        );
    }

    @Override
    public List<String> getUsageInstructions() {
        return plugin.getConfig().getStringList(
                "command-settings.usage-instructions"
        );
    }

    @Override
    public List<String> getExampleUsage() {
        return plugin.getConfig().getStringList(
                "command-settings.usage-example"
        );
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("suggest");
    }

    @Override
    public List<String> getMiddleware() {
        int cooldown = plugin.getConfig().getInt("command-settings.cooldown");
        if (cooldown < 1) {
            return super.getMiddleware();
        }
        return Collections.singletonList("throttle:user,1," + cooldown);
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        if (args.length == 0) {
            return sendErrorMessage(context, "You must include a suggestion to suggestion something... Duh!");
        }

        int length = plugin.getConfig().getInt("suggestion.min-length", 30);
        if (String.join(" ", args).length() < length) {
            return sendErrorMessage(context, "The suggest must be at least `%s` characters long!", String.valueOf(length));
        }

        String[] split = context.getMessage().getContentRaw().split(" ");
        String prefix = String.join(" ", Arrays.copyOfRange(split, 0, context.isMentionableCommand() ? 2 : 1));

        String suggestion = context.getMessage().getContentRaw().substring(
                prefix.length(), context.getMessage().getContentRaw().length()
        );

        TextChannel channel = avaire.getShardManager().getTextChannelById(
                plugin.getConfig().getString("suggestion.channel-id")
        );

        if (channel == null) {
            return sendErrorMessage(context, "Failed to find a text channel with the ID of `%s`",
                    plugin.getConfig().getString("suggestion.channel-id")
            );
        }

        channel.sendMessage(new EmbedBuilder()
                .setDescription(suggestion)
                .setAuthor(
                        "Suggestion by " + context.getAuthor().getName() + "#" + context.getAuthor().getDiscriminator(),
                        null, context.getAuthor().getEffectiveAvatarUrl()
                )
                .setFooter("User ID: " + context.getAuthor().getId(), null)
                .setTimestamp(Instant.now())
                .build()
        ).queue(message -> {
            context.makeSuccess(plugin.getConfig().getString("suggestion.success", "Thank you for your suggestion!"))
                    .queue(newMsg -> newMsg.delete().queueAfter(45, TimeUnit.SECONDS));

            Emote yesEmote = avaire.getShardManager().getEmoteById(
                    plugin.getConfig().getString("suggestion.yes-emote")
            );
            if (yesEmote != null) {
                message.addReaction(yesEmote).queue();
            }

            Emote noEmote = avaire.getShardManager().getEmoteById(
                    plugin.getConfig().getString("suggestion.no-emote")
            );
            if (noEmote != null) {
                message.addReaction(noEmote).queue();
            }
        }, error -> context.makeWarning("Failed to send the suggestion message, please make sure I can send embed messages in the suggestion channel!").queue());

        return true;
    }
}
