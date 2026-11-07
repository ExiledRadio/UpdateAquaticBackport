package com.exiledradio.updateaquaticbackport.coral;

import net.minecraft.block.material.MapColor;

/** 1.13's five corals, in the order 1.13 lists them, with their map colors. */
public enum CoralType {
    TUBE("tube", MapColor.BLUE),
    BRAIN("brain", MapColor.PINK),
    BUBBLE("bubble", MapColor.PURPLE),
    FIRE("fire", MapColor.RED),
    HORN("horn", MapColor.YELLOW);

    public final String name;
    public final MapColor color;

    CoralType(String name, MapColor color) {
        this.name = name;
        this.color = color;
    }
}
