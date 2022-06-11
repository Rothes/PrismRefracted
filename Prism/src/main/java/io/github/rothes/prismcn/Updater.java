package io.github.rothes.prismcn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import network.darkhelmet.prism.Prism;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Updater implements Listener {

    private final String VERSION_CHANNCEL = "Stable-1.17-1.18";
    private final int VERSION_NUMBER = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Prism.getInstance().getResource("metadata.yml"), StandardCharsets.UTF_8)).getInt("versionId");
    private final HashMap<String, Integer> msgTimesMap = new HashMap<>();
    private final List<String> messages = new ArrayList<>();

    public void start() {
        new Metrics(Prism.getInstance(), 15030);
        Bukkit.getPluginManager().registerEvents(this, Prism.getInstance());
        Bukkit.getScheduler().runTaskTimerAsynchronously(Prism.getInstance(), () -> {
            try {
                checkJson(getJson());
            } catch (IllegalStateException | NullPointerException ignored) {
//                Prism.warn("§c无法正常解析版本信息 Json, 请更新您的插件至最新版本: " + e);
            }
        }, 0L, 72000L);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(Prism.getInstance(), () -> {
            if (e.getPlayer().hasPermission("prism.cnupdater.notify") || e.getPlayer().isOp()) {
                for (String message : messages) {
                    Prism.messenger.sendMessage(e.getPlayer(), Prism.messenger.playerHeaderMsg(Component.text(message)));
                }

            }
        });
    }

    private String getJson() {
        try (
                InputStream stream = new URL("https://raw.fastgit.org/Rothes/PrismRefracted/v3-cn/Version%20Infos.json")
                        .openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        ){
            StringBuilder jsonBuilder = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                jsonBuilder.append(line).append("\n");
            }
            return jsonBuilder.toString();
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return null;
    }

    private void checkJson(String json) {
        JsonElement element = new JsonParser().parse(json);
        JsonObject root = element.getAsJsonObject();
        JsonObject channels = root.getAsJsonObject("Version_Channels");

        messages.clear();
        if (channels.has(VERSION_CHANNCEL)) {
            JsonObject channel = channels.getAsJsonObject(VERSION_CHANNCEL);
            if (channel.has("Message")
                    && Integer.parseInt(channel.getAsJsonPrimitive("Latest_Version_Number").getAsString()) > VERSION_NUMBER) {
                sendJsonMessage(channel, "updater");
            }
        } else {
            Prism.warn("§c您使用的插件处于一个未知的版本通道(\"" + VERSION_CHANNCEL + "\"). 请更新您的插件到最新版本.");
            messages.add("§c您使用的插件处于一个未知的版本通道(\"" + VERSION_CHANNCEL + "\"). 请更新您的插件到最新版本.");
            return;
        }

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("Version_Actions").entrySet()) {
            String[] split = entry.getKey().split("-");
            if (Integer.parseInt(split[1]) > VERSION_NUMBER
                    && VERSION_NUMBER > Integer.parseInt(split[0])) {
                JsonObject message = (JsonObject) entry.getValue();
                if (message.has("Message"))
                    sendJsonMessage(message, entry.getKey());
            }
        }

    }

    private void sendJsonMessage(JsonObject json, String id) {
        JsonObject msgJson = json.getAsJsonObject("Message");
        String msg = msgJson.get("zh-CN").getAsString();

        int msgTimes = json.has("Message_Times") ? json.get("Message_Times").getAsInt() : -1;
        int curTimes = msgTimesMap.get(id) == null? 0 : msgTimesMap.get(id);

        if (msgTimes == -1 || curTimes < msgTimes) {

            String logLevel = json.has("Log_Level") ? json.get("Log_Level").getAsString() : "default maybe";

            for (String s : msg.split("\n")) {
                messages.add(s);
                switch (logLevel) {
                    case "Warn":
                        Prism.warn(s);
                        break;
                    case "Info":
                    default:
                        Prism.log(s);
                        break;
                }
            }
            msgTimesMap.put(id, ++curTimes);
        }

        for (JsonElement action : json.getAsJsonArray("Actions")) {
            if (action.getAsString().equals("Prohibit")) {
                Bukkit.getPluginManager().disablePlugin(Prism.getInstance());
            }
        }

    }

}
