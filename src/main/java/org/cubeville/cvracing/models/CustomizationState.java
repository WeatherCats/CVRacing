package org.cubeville.cvracing.models;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomizationState {
    public BoatMaterial boatMaterial = BoatMaterial.OAK;
    public Horse.Color horseColor = Horse.Color.BROWN;
    public Horse.Style horseStyle = Horse.Style.NONE;
    private ItemStack horseArmor;
    public String horseArmorString = "NONE";

    public CustomizationState(String boatMaterial, String horseColor, String horseStyle, String horseArmor) {
        if (boatMaterial != null) {
            try {
                this.boatMaterial = BoatMaterial.valueOf(boatMaterial.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException ignored) {}
        }

        if (horseColor != null) {
            try {
                this.horseColor = Horse.Color.valueOf(horseColor.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException ignored) {}
        }

        if (horseStyle != null) {
            try {
                this.horseStyle = Horse.Style.valueOf(horseStyle.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException ignored) {}
        }

        setArmorFromString(horseArmor);
    }

    public CustomizationState() {}

    public void setColoredHorseArmor(String hex) {
        ItemStack is = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        LeatherArmorMeta lam = (LeatherArmorMeta) is.getItemMeta();
        lam.setColor(hex2Color(hex));
        is.setItemMeta(lam);
        setHorseArmor(is);
    }

    public void setHorseArmor(ItemStack itemStack) {
        horseArmor = itemStack;
    }

    public ItemStack getHorseArmor() {
        return horseArmor;
    }

    private Color hex2Color(String colorStr) {
        return Color.fromRGB(
                Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16),
                Integer.valueOf(colorStr.substring(5, 7), 16)
        );
    }

    public EntityType getBoatType() {
        switch(boatMaterial) {
            case OAK:
                return EntityType.OAK_BOAT;
            case BIRCH:
                return EntityType.BIRCH_BOAT;
            case ACACIA:
                return EntityType.ACACIA_BOAT;
            case JUNGLE:
                return EntityType.JUNGLE_BOAT;
            case SPRUCE:
                return EntityType.SPRUCE_BOAT;
            case DARK_OAK:
                return EntityType.DARK_OAK_BOAT;
        }
        return EntityType.OAK_BOAT;
    }

    public boolean setArmorFromString(String string) {
        if (determineArmor(string)) {
            horseArmorString = string.toUpperCase();
            return true;
        }
        return false;
    }

    public boolean determineArmor(String string) {
        if (string != null) {
            string = string.toUpperCase();
            switch (string) {
                case "DIAMOND":
                    setHorseArmor(new ItemStack(Material.DIAMOND_HORSE_ARMOR));
                    return true;
                case "GOLD":
                    setHorseArmor(new ItemStack(Material.GOLDEN_HORSE_ARMOR));
                    return true;
                case "IRON":
                    setHorseArmor(new ItemStack(Material.IRON_HORSE_ARMOR));
                    return true;
                case "LEATHER":
                    setHorseArmor(new ItemStack(Material.LEATHER_HORSE_ARMOR));
                    return true;
                case "NONE":
                    setHorseArmor(null);
                    return true;
                default:
                    Pattern colorPattern = Pattern.compile("#[0-9a-fA-F]{6}");
                    Matcher m = colorPattern.matcher(string);
                    if (m.matches()) {
                        setColoredHorseArmor(string);
                        return true;
                    }
                    return false;
            }
        }
        return false;
    }
}
