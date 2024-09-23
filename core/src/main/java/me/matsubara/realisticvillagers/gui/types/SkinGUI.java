package me.matsubara.realisticvillagers.gui.types;

import lombok.Getter;
import lombok.Setter;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.files.Config;
import me.matsubara.realisticvillagers.files.Messages;
import me.matsubara.realisticvillagers.gui.PaginatedGUI;
import me.matsubara.realisticvillagers.util.InventoryUpdate;
import me.matsubara.realisticvillagers.util.ItemBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
public class SkinGUI extends PaginatedGUI {

    private final boolean isMale;
    private final boolean isAdult;
    private final String keyword;
    private final Map<String, ItemStack> professionItems = new LinkedHashMap<>();
    private @Setter String currentProfession;

    private static final int AGE_STAGE_SLOT = 0;
    private static final int PROFESSION_SLOT = 27;
    private static final int PREVIOUS_SLOT = 19;
    private static final int NEXT_SLOT = 25;
    private static final int TOGGLE_SEX = 21;
    private static final int SEARCH_SLOT = 22;
    private static final int NEW_SKIN = 23;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final Map<String, Material> PROFESSION_ICON = new LinkedHashMap<>();
    public static final Map<Integer, ItemStack> CACHE_MALE_HEADS = new ConcurrentHashMap<>();
    public static final Map<Integer, ItemStack> CACHE_FEMALE_HEADS = new ConcurrentHashMap<>();

    static {
        PROFESSION_ICON.put("NONE", Material.BARRIER);
        PROFESSION_ICON.put("ARMORER", Material.BLAST_FURNACE);
        PROFESSION_ICON.put("BUTCHER", Material.SMOKER);
        PROFESSION_ICON.put("CARTOGRAPHER", Material.CARTOGRAPHY_TABLE);
        PROFESSION_ICON.put("CLERIC", Material.BREWING_STAND);
        PROFESSION_ICON.put("FARMER", Material.COMPOSTER);
        PROFESSION_ICON.put("FISHERMAN", Material.BARREL);
        PROFESSION_ICON.put("FLETCHER", Material.FLETCHING_TABLE);
        PROFESSION_ICON.put("LEATHERWORKER", Material.CAULDRON);
        PROFESSION_ICON.put("LIBRARIAN", Material.LECTERN);
        PROFESSION_ICON.put("MASON", Material.STONECUTTER);
        PROFESSION_ICON.put("NITWIT", Material.BARRIER);
        PROFESSION_ICON.put("SHEPHERD", Material.LOOM);
        PROFESSION_ICON.put("TOOLSMITH", Material.SMITHING_TABLE);
        PROFESSION_ICON.put("WEAPONSMITH", Material.GRINDSTONE);
        PROFESSION_ICON.put("WANDERING_TRADER", Material.EMERALD);
    }

    private SkinGUI(RealisticVillagers plugin,
                    Player player,
                    List<ItemStack> heads,
                    boolean isMale,
                    boolean isAdult,
                    @Nullable Integer page,
                    @Nullable String keyword) {
        super(plugin, null, "skin", getValidSize(plugin, "skin", 36), player, heads, page);

        this.isMale = isMale;
        this.isAdult = isAdult;
        this.keyword = keyword;

        for (String profession : PROFESSION_ICON.keySet()) {
            professionItems.put(profession, new ItemBuilder(getGUIItem("profession"))
                    .setType(PROFESSION_ICON.get(profession))
                    .replace("%profession%", plugin.getProfessionFormatted(profession.toLowerCase().replace("_", "-"), isMale))
                    .build());
        }

        InventoryUpdate.updateInventory(player, getTitle());
    }

    public static void openMenu(@NotNull RealisticVillagers plugin, Player player, @NotNull String sex, boolean isAdult, @Nullable Integer page, @Nullable String keyword) {
        boolean isMale = sex.equals("male");
        if ((isMale ? SkinGUI.CACHE_MALE_HEADS : SkinGUI.CACHE_FEMALE_HEADS).isEmpty()) {
            plugin.getMessages().send(player, Messages.Message.NO_SKIN_CACHED);
        }
        CompletableFuture.supplyAsync((Supplier<List<ItemStack>>) () -> {
            Pair<File, FileConfiguration> pair = plugin.getTracker().getFile(sex + ".yml");
            FileConfiguration config = pair.getValue();

            ConfigurationSection section = config.getConfigurationSection("none");
            if (section == null) return Collections.emptyList();

            List<ItemStack> heads = new ArrayList<>();
            for (String skinId : section.getKeys(false).stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList()) {
                ItemStack skinItem = createSkinItem(plugin, config, sex, isAdult, Integer.parseInt(skinId), keyword);
                if (skinItem != null) heads.add(skinItem);
            }
            return heads;
        }).thenAccept(heads -> plugin.getServer().getScheduler().runTask(plugin, () -> new SkinGUI(plugin, player, heads, isMale, isAdult, page, keyword)));
    }

    @Override
    protected String getTitle() {
        return super.getTitle()
                .replace("%sex%", isMale ? Config.MALE.asString() : Config.FEMALE.asString())
                .replace("%age-stage%", isAdult ? Config.ADULT.asString() : Config.KID.asString());
    }

    private static @Nullable ItemStack createSkinItem(@NotNull RealisticVillagers plugin, @NotNull FileConfiguration config, String sex, boolean isAdult, int id, @Nullable String keyword) {
        String texture = config.getString("none." + id + ".texture");
        if (texture == null || texture.isBlank() || texture.isEmpty()) return null;

        boolean forBabies = config.getBoolean("none." + id + ".for-babies");
        if ((isAdult && forBabies) || (!isAdult && !forBabies)) {
            return null;
        }

        String adddedByUUID = config.getString("none." + id + ".added-by", null);
        boolean fromConsole = false;
        OfflinePlayer addedBy = (adddedByUUID != null && !(fromConsole = adddedByUUID.equalsIgnoreCase("Console"))) ? Bukkit.getOfflinePlayer(UUID.fromString(adddedByUUID)) : null;
        boolean validAdder = addedBy != null && addedBy.hasPlayedBefore() && addedBy.getName() != null;

        if (keyword != null) {
            String lowerKeyword = keyword.toLowerCase();
            if (lowerKeyword.startsWith("by:")) {
                if (lowerKeyword.contains("by:unknown")) {
                    // If looking for unknown, the adder should be invalid.
                    if (validAdder || fromConsole) return null;
                } else if (lowerKeyword.contains("by:console")) {
                    if (validAdder || !fromConsole) return null;
                } else if (!validAdder || !lowerKeyword.contains("by:" + addedBy.getName().toLowerCase())) {
                    // If looking for someone, the adder should be valid and the keyword should match the current skin adder.
                    return null;
                }
            } else if (!("#" + id).contains(lowerKeyword)) return null;
        }

        boolean isMale = sex.equals("male");
        Map<Integer, ItemStack> cache = isMale ? CACHE_MALE_HEADS : CACHE_FEMALE_HEADS;
        if (cache.containsKey(id)) {
            return cache.get(id);
        }

        long when = config.getLong("none." + id + ".when", -1L);

        int generated = 0;
        List<String> professions = new ArrayList<>();


        for (String key : PROFESSION_ICON.keySet()) {
            String profession = key.toLowerCase().replace("_", "-");
            if (config.contains(profession + "." + id)) {
                professions.add("&a" + plugin.getProfessionFormatted(profession, isMale));
                generated++;
            } else {
                professions.add("&c" + plugin.getProfessionFormatted(profession, isMale));
            }
        }

        String unknown = Config.UNKNOWN.asString();
        String addedByString = validAdder ? addedBy.getName() : fromConsole ? Config.CONSOLE.asString() : unknown;
        String whenString = when != -1L ? TIME_FORMAT.format(new Date(when)) : unknown;

        ItemStack item = new ItemBuilder(plugin.getItem("gui.skin.items.skin").build())
                .setHead(texture, false)
                .setData(plugin.getSkinDataKey(), PersistentDataType.STRING, String.format("%s:%s", sex, id))
                .applyMultiLineLore(professions, "%profession%", "%professions%", "???", String.join(", ", professions))
                .replace("%skin-id%", id)
                .replace("%added-by%", addedByString)
                .replace("%when%", whenString)
                .replace("%generated%", generated)
                .replace("%max-professions%", PROFESSION_ICON.size())
                .build();

        cache.put(id, item);
        return item;
    }

    @Override
    public void addButtons() {
        int extra = 9 * (size == 36 ? 0 : size == 45 ? 1 : 2);

        inventory.setItem(AGE_STAGE_SLOT, keyword != null ?
                (!animation.isGuiAnim() ? animation.getDefaultItem() : null) :
                (isAdult ? getGUIItem("adult") : getGUIItem("kid")));

        ItemStack professionItem;
        if (professionItems != null) {
            String oldSelected = plugin.getTracker().getSelectedProfession().get(player.getUniqueId());
            professionItem = professionItems.get((currentProfession = oldSelected != null ? oldSelected : "NONE"));
        } else professionItem = null;
        inventory.setItem(PROFESSION_SLOT + extra, professionItem);

        if (currentPage > 0) inventory.setItem(PREVIOUS_SLOT + extra, getGUIItem("previous"));
        if (currentPage < pages - 1) inventory.setItem(NEXT_SLOT + extra, getGUIItem("next"));
        inventory.setItem(TOGGLE_SEX + extra, keyword != null ? null : isMale ? getGUIItem("male") : getGUIItem("female"));
        inventory.setItem(SEARCH_SLOT + extra, getSearchItem(keyword));
        inventory.setItem(NEW_SKIN + extra, keyword != null ? null : getGUIItem("add-new-skin"));
    }
}