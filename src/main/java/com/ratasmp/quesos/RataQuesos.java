package com.ratasmp.quesos;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * RataQuesos - da una probabilidad de que aparezca un "Queso" (lingote de
 * oro especial) junto con el loot normal de cualquier cofre/barril/shulker
 * box/minecart con cofre generado por el propio mundo, y deja canjearlo con
 * clic derecho llamando a la funcion "rata:usar_queso" del datapack.
 *
 * Usa LootGenerateEvent, que se dispara justo en el momento en que el juego
 * genera el contenido de un contenedor de loot (chest/barrel/shulker/minecart
 * con cofre) la PRIMERA vez que se abre. Esto evita por completo los
 * problemas de "cofre sin abrir no deja escribirle nada desde afuera" que
 * tuvimos intentando hacer esto con un datapack: aqui no hace falta vigilar
 * nada, el propio evento nos avisa en el momento exacto.
 */
public class RataQuesos extends JavaPlugin implements Listener {

    private NamespacedKey quesoKey;
    private double probabilidad;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        probabilidad = getConfig().getDouble("probabilidad", 1.0);
        quesoKey = new NamespacedKey(this, "rata_queso");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RataQuesos activo. Probabilidad configurada: " + probabilidad + "%");
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (Math.random() * 100 > probabilidad) {
            return;
        }

        List<ItemStack> loot = new ArrayList<>(event.getLoot());
        loot.add(crearQueso());
        event.setLoot(loot);

        Location loc = event.getLootContext().getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        Player cercano = jugadorMasCercano(loc, 10);
        if (cercano != null) {
            Bukkit.broadcastMessage(ChatColor.GOLD + cercano.getName() + ChatColor.YELLOW
                    + " \u00a1ha encontrado un " + ChatColor.GOLD + "" + ChatColor.BOLD
                    + "queso" + ChatColor.RESET + ChatColor.YELLOW + "!");
            cercano.playSound(cercano.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    @EventHandler
    public void onClic(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action accion = event.getAction();
        if (accion != Action.RIGHT_CLICK_AIR && accion != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack enMano = event.getItem();
        if (enMano == null || enMano.getType() != Material.GOLD_INGOT) {
            return;
        }
        ItemMeta meta = enMano.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(quesoKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        Player jugador = event.getPlayer();
        enMano.setAmount(enMano.getAmount() - 1);

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "execute as " + jugador.getName() + " at " + jugador.getName() + " run function rata:usar_queso"
        );
    }

    private ItemStack crearQueso() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Queso");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Clic derecho en la mano");
        lore.add(ChatColor.GRAY + "para canjearlo por un " + ChatColor.GOLD + "queso" + ChatColor.GRAY + ".");
        meta.setLore(lore);
        try {
            // Disponible desde Paper 1.20.5+. Si tu build de 26.3 renombro este
            // metodo, borra esta linea (el item igual funciona, solo sin brillo).
            meta.setEnchantmentGlintOverride(true);
        } catch (NoSuchMethodError ignored) {
        }
        meta.getPersistentDataContainer().set(quesoKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private Player jugadorMasCercano(Location loc, double radio) {
        Player mejor = null;
        double mejorDistCuadrada = radio * radio;
        for (Player p : loc.getWorld().getPlayers()) {
            if (!p.getLocation().getWorld().equals(loc.getWorld())) {
                continue;
            }
            double d = p.getLocation().distanceSquared(loc);
            if (d <= mejorDistCuadrada) {
                mejor = p;
                mejorDistCuadrada = d;
            }
        }
        return mejor;
    }
}
