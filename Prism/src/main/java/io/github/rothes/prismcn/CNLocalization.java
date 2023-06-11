package io.github.rothes.prismcn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.darkhelmet.prism.Prism;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CNLocalization {

    private static final Map<EntityType, String> entityLocalize = new EnumMap<>(EntityType.class);
    private static final Map<Material, String> materialLocalize = new EnumMap<>(Material.class);
    private static final Map<PotionEffectType, String> effectLocalize = new HashMap<>();
    private static final Map<Enchantment, String> enchantmentLocalize = new HashMap<>();

    private static final Map<String, String> entityLocalizeRestore = new HashMap<>();
    private static final Map<String, String> materialLocalizeRestore = new HashMap<>();

    public static void initialize(Prism plugin) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new InputStreamReader(plugin.getResource("languages/Spigot-Lang.yml"), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException | NullPointerException exception) {
            Prism.getInstance().getLogger().log(Level.WARNING, "无法加载本地化语言文件", exception);
        }

        JsonElement root;
        try (
                InputStream stream = plugin.getResource("languages/Minecraft-Lang.json");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
        ){
            root = new JsonParser().parse(reader);
        } catch (IOException | NullPointerException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "无法加载本地化语言文件", e);
            root = new JsonParser().parse("{How=\"How\"}");
        }
        JsonObject object = root.getAsJsonObject();

        List<String> missing = new ArrayList<>();
        for (EntityType value : EntityType.values()) {
            if (value == EntityType.UNKNOWN) {
                entityLocalize.put(value, "未知");
                continue;
            }
            JsonElement element = object.get("entity.minecraft." + value.getKey().getKey());
            if (element == null) {
                missing.add(value.name());
                entityLocalize.put(value, value.name().toLowerCase().replace("_", " "));
                entityLocalizeRestore.put(value.name().toLowerCase().replace("_", " "), value.name());
            } else {
                entityLocalize.put(value, element.getAsString());
                entityLocalizeRestore.put(element.getAsString(), value.name());
            }
        }
        warnMissing("EntityType", missing);
        missing.clear();

        for (Material value : Material.values()) {
            switch (value) {
                case WHITE_WALL_BANNER:
                    materialLocalize.put(value, "白色旗帜");
                    materialLocalizeRestore.put("白色旗帜", value.name());
                    break;
                case ORANGE_WALL_BANNER:
                    materialLocalize.put(value, "橙色旗帜");
                    materialLocalizeRestore.put("橙色旗帜", value.name());
                    break;
                case MAGENTA_WALL_BANNER:
                    materialLocalize.put(value, "品红色旗帜");
                    materialLocalizeRestore.put("品红色旗帜", value.name());
                    break;
                case LIGHT_BLUE_WALL_BANNER:
                    materialLocalize.put(value, "淡蓝色旗帜");
                    materialLocalizeRestore.put("淡蓝色旗帜", value.name());
                    break;
                case YELLOW_WALL_BANNER:
                    materialLocalize.put(value, "黄色旗帜");
                    materialLocalizeRestore.put("黄色旗帜", value.name());
                    break;
                case LIME_WALL_BANNER:
                    materialLocalize.put(value, "黄绿色旗帜");
                    materialLocalizeRestore.put("黄绿色旗帜", value.name());
                    break;
                case PINK_WALL_BANNER:
                    materialLocalize.put(value, "粉红色旗帜");
                    materialLocalizeRestore.put("粉红色旗帜", value.name());
                    break;
                case GRAY_WALL_BANNER:
                    materialLocalize.put(value, "灰色旗帜");
                    materialLocalizeRestore.put("灰色旗帜", value.name());
                    break;
                case LIGHT_GRAY_WALL_BANNER:
                    materialLocalize.put(value, "淡灰色旗帜");
                    materialLocalizeRestore.put("淡灰色旗帜", value.name());
                    break;
                case CYAN_WALL_BANNER:
                    materialLocalize.put(value, "青色旗帜");
                    materialLocalizeRestore.put("青色旗帜", value.name());
                    break;
                case PURPLE_WALL_BANNER:
                    materialLocalize.put(value, "紫色旗帜");
                    materialLocalizeRestore.put("紫色旗帜", value.name());
                    break;
                case BLUE_WALL_BANNER:
                    materialLocalize.put(value, "蓝色旗帜");
                    materialLocalizeRestore.put("蓝色旗帜", value.name());
                    break;
                case BROWN_WALL_BANNER:
                    materialLocalize.put(value, "棕色旗帜");
                    materialLocalizeRestore.put("棕色旗帜", value.name());
                    break;
                case GREEN_WALL_BANNER:
                    materialLocalize.put(value, "绿色旗帜");
                    materialLocalizeRestore.put("绿色旗帜", value.name());
                    break;
                case RED_WALL_BANNER:
                    materialLocalize.put(value, "红色旗帜");
                    materialLocalizeRestore.put("红色旗帜", value.name());
                    break;
                case BLACK_WALL_BANNER:
                    materialLocalize.put(value, "黑色旗帜");
                    materialLocalizeRestore.put("黑色旗帜", value.name());
                    break;
                default:
                    if (value.isLegacy()) {
                        // For some servers.
                        break;
                    }
                    JsonElement element = object.get("item.minecraft." + value.getKey().getKey());
                    if (element == null) {
                        element = object.get("block.minecraft." + value.getKey().getKey());
                    }
                    if (element == null) {
                        missing.add(value.name());
                        materialLocalize.put(value, value.name().toLowerCase().replace("_", " "));
                        materialLocalizeRestore.put(value.name().toLowerCase().replace("_", " "), value.name());
                    } else {
                        materialLocalize.put(value, element.getAsString());
                        materialLocalizeRestore.put(element.getAsString(), value.name());
                    }
                    break;
            }
        }
        warnMissing("Material", missing);
        missing.clear();

        for (PotionEffectType value : PotionEffectType.values()) {
            if (Prism.getInstance().getServerMajorVersion() >= 19) {
                JsonElement element = object.get("effect.minecraft." + value.getKey().getKey());
                if (element == null) {
                    missing.add(value.getKey().getKey());
                    effectLocalize.put(value, value.getKey().getKey().toLowerCase().replace("_", " "));
                } else {
                    effectLocalize.put(value, element.getAsString());
                }
            } else {
                String locale = yaml.getString("Effect." + value.getName());
                if (locale == null) {
                    missing.add(value.getName());
                    effectLocalize.put(value, value.getName().toLowerCase().replace("_", " "));
                } else {
                    effectLocalize.put(value, locale);
                }
            }
        }
        warnMissing("PotionEffectType", missing);
        missing.clear();

        for (Enchantment value : Enchantment.values()) {
            JsonElement element = object.get("enchantment.minecraft." + value.getKey().getKey());
            if (element == null) {
                missing.add(value.getKey().getKey());
                enchantmentLocalize.put(value, value.getKey().getKey().toLowerCase().replace("_", " "));
            } else {
                enchantmentLocalize.put(value, element.getAsString());
            }
        }
        warnMissing("Enchantment", missing);
    }

    private static void warnMissing(String type, List<String> list) {
        if (list.size() == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder("缺少本地化语言: ").append(type).append(" = ");
        if (list.size() >= 20) {
            for (int i = 0; i < 20; i++) {
                sb.append(list.get(i)).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length())
                    .append("... 等");
        } else {
            for (String ty: list) {
                sb.append(ty).append(", ");
            }
        }
        sb.append("共 ").append(list.size()).append(" 个项目");
        Prism.warn(sb.toString());
    }

    public static String getMaterialLocale(Material material) {
        return materialLocalize.get(material);
    }

    public static String getEntityLocale(EntityType entityType) {
        return entityLocalize.get(entityType);
    }

    public static String getEntityLocale(String entityType) {
        return entityLocalize.get(EntityType.valueOf(entityType));
    }

    public static String getEffectLocale(PotionEffectType potionEffectType) {
        return effectLocalize.get(potionEffectType);
    }

    public static String getEnchantmentLocale(Enchantment enchantment) {
        return enchantmentLocalize.get(enchantment);
    }

    public static String restoreEntityLocale(String type) {
        return entityLocalizeRestore.getOrDefault(type, type);
    }

    public static String restoreMaterialLocale(String type) {
        return materialLocalizeRestore.getOrDefault(type, type);
    }

}
