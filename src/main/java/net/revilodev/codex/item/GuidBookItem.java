package net.revilodev.codex.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.revilodev.codex.network.CodexNetwork;

public class GuidBookItem extends Item {

    public GuidBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        // SERVER side → tell client to open screen
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CodexNetwork.sendOpenCodexBook(sp);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
