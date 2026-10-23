package fr.anger.echoes.fireforge.mechanism;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La lumière de la Lampe à vapeur : un bloc invisible (modèle transparent), sans forme, que la lampe pose là où regarde son porteur.
 * Il se retire de lui-même dès qu'aucune lampe ne l'éclaire plus. Les packs de shaders peuvent le reconnaître
 * à son identifiant ({@code echoes_fireforge:lamp_light}) pour colorer sa lumière.
 */
public class LampLightBlock extends Block {

    public static final MapCodec<LampLightBlock> CODEC = simpleCodec(LampLightBlock::new);
    /** Il vérifie toutes les secondes qu'une lampe l'éclaire encore. */
    public static final int CHECK_TICKS = 20;

    public LampLightBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * Un modèle minuscule et entièrement transparent (en découpe) : rien ne se voit, mais le bloc envoie des sommets.
     * Sans eux, les packs de shaders (Iris) ne le verraient jamais : ils repèrent les sources de lumière colorée
     * dans la géométrie du terrain, pendant la passe d'ombre.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (SteamLamp.lights(level, pos)) level.scheduleTick(pos, this, CHECK_TICKS);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
