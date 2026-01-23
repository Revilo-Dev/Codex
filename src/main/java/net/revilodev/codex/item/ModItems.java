// src/main/java/net/revilodev/codex/item/ModItems.java
package net.revilodev.codex.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.revilodev.codex.CodexMod;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CodexMod.MOD_ID);

    public static final DeferredItem<Item> SKILLS_BOOK =
            ITEMS.registerItem("skills_book", SkillsBookItem::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
