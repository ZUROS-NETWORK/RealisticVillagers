package me.matsubara.realisticvillagers.gui.types;

import lombok.Getter;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import me.matsubara.realisticvillagers.gui.PaginatedGUI;
import me.matsubara.realisticvillagers.util.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

@Getter
public class WhistleGUI extends PaginatedGUI {

    private final String keyword;

    private static final int PREVIOUS_SLOT = 19;
    private static final int NEXT_SLOT = 25;
    private static final int SEARCH_SLOT = 22;

    public WhistleGUI(RealisticVillagers plugin, Player player, @NotNull Stream<IVillagerNPC> family, @Nullable Integer page, @Nullable String keyword) {
        super(plugin, null, "whistle", getValidSize(plugin, "whistle", 36), player, family
                .filter(npc -> keyword == null || npc.getVillagerName().toLowerCase().contains(keyword.toLowerCase()))
                .map(npc -> {
                    String name = npc.getVillagerName();
                    return new ItemBuilder(plugin.getItem("gui.whistle.items.villager").build())
                            .setHead(plugin.getNPCTextureURL(npc), true)
                            .setData(plugin.getVillagerUUIDKey(), PersistentDataType.STRING, npc.getUniqueId().toString())
                            .replace("%villager-name%", name)
                            .build();
                })
                .toList(), page);
        this.keyword = keyword;
    }

    @Override
    public void addButtons() {
        int extra = 9 * (size == 36 ? 0 : size == 45 ? 1 : 2);
        if (currentPage > 0) inventory.setItem(PREVIOUS_SLOT + extra, getGUIItem("previous"));
        if (currentPage < pages - 1) inventory.setItem(NEXT_SLOT + extra, getGUIItem("next"));
        inventory.setItem(SEARCH_SLOT + extra, getSearchItem(keyword));
    }
}