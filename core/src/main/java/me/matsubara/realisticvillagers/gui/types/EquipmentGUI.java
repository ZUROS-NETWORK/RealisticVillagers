package me.matsubara.realisticvillagers.gui.types;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import me.matsubara.realisticvillagers.gui.InteractGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public final class EquipmentGUI extends InteractGUI {

    private final Player player;

    private static final ItemStack EMPTY = new ItemStack(Material.AIR);
    public static final EquipmentSlot[] ARMOR_SLOTS_ORDER = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.HAND,
            EquipmentSlot.OFF_HAND};

    public EquipmentGUI(RealisticVillagers plugin, IVillagerNPC npc, @NotNull Player player) {
        super(plugin, npc, "equipment", ((InventoryHolder) npc.bukkit()).getInventory().getSize() + 18, null, true);

        this.player = player;

        fillInventory();
        player.openInventory(inventory);
    }

    private void fillInventory() {
        inventory.setContents(((InventoryHolder) npc.bukkit()).getInventory().getContents());

        int size = ((InventoryHolder) npc.bukkit()).getInventory().getSize();
        int borderEnd = size + 9;

        inventory.setItem(borderEnd, getGUIItem("villager", string -> string.replace("%villager-name%", npc.getVillagerName())));

        int armorStart = borderEnd + 1;
        int armorEnd = armorStart + 6;

        EntityEquipment equipment = npc.bukkit().getEquipment();
        Preconditions.checkNotNull(equipment);

        for (int i = armorStart; i < armorEnd; i++) {
            EquipmentSlot slot = ARMOR_SLOTS_ORDER[i - armorStart];
            inventory.setItem(i, equipment.getItem(slot));
        }

        inventory.setItem(size + 17, getGUIItem("back"));
    }
}