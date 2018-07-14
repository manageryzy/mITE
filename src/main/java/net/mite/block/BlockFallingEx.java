package net.mite.block;

import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.mite.entity.EntityFallingBlock;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BlockFallingEx extends Block {
    public static boolean fallInstantly;
    public int fallingDistHor = 0;
    public int fallingDistVec = 0;

    public BlockFallingEx() {
        super(Material.SAND);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    public BlockFallingEx(Material materialIn) {
        super(materialIn);
    }

    public BlockFallingEx(Material rock, MapColor color) {
        super(rock, color);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isRemote) {
            this.checkFallable(worldIn, pos);
        }
    }


    private void checkFallable(World worldIn, BlockPos pos) {
        if (worldIn.getEntities(EntityFallingBlock.class, new Predicate<EntityFallingBlock>() {
            @Override
            public boolean apply(@Nullable EntityFallingBlock input) {
                return true;
            }

            @Override
            public boolean equals(@Nullable Object object) {
                return false;
            }
        }).size() > 128) {
            return;
        }

        checkFallableDirect(worldIn, pos);
    }

    private void checkFallableDirect(World worldIn, BlockPos pos) {

        if (fallingDistHor == 0 && fallingDistVec == 0) {
            checkFallingDirect(worldIn, pos);
        } else if (!fallInstantly) {
            if (fallingDistVec == 0) {
                checkFallingHor(worldIn, pos);
            } else {
                checkFallingVec(worldIn, pos);
            }
        }
    }

    private void checkFallingVec(World worldIn, BlockPos pos) {
        if (!worldIn.isRemote) {

            if (!checkFallingVec(worldIn, pos, pos, 1)) {
                return;
            }
            if (!checkFallingVec(worldIn, pos, pos.east(), fallingDistVec)) {
                return;
            }
            if (!checkFallingVec(worldIn, pos, pos.south(), fallingDistVec)) {
                return;
            }
            if (!checkFallingVec(worldIn, pos, pos.west(), fallingDistVec)) {
                return;
            }

            checkFallingVec(worldIn, pos, pos.north(), fallingDistVec);
        }
    }

    private boolean checkFallingVec(World worldIn, BlockPos pos, BlockPos p, int dist) {
        if(!worldIn.isBlockLoaded(pos)) {
            return true;
        }

        boolean supported = false;
        for (int i = 1; i < dist + 1; ++i) {
            if (p.down(i).getY() <= 0) {
                supported = true;
            }
            if (!canFallThrough(worldIn.getBlockState(p.down(i)))) {
                supported = true;
            }
        }

        if (!supported) {
            if ((!fallInstantly && worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32)))) {
                EntityFallingBlock entityfallingblock =
                        new EntityFallingBlock(worldIn, (double) p.getX() + 0.5D, (double) p.getY(), (double) p.getZ() + 0.5D, pos, worldIn.getBlockState(pos));
                this.onStartFalling(entityfallingblock);
                worldIn.spawnEntity(entityfallingblock);
            } else {
                IBlockState state = worldIn.getBlockState(pos);
                worldIn.setBlockToAir(pos);

                for (BlockPos blockpos = p.down();
                     blockpos.getY() > 0;
                     blockpos = blockpos.down()) {
                    if ((worldIn.isAirBlock(blockpos) || canFallThrough(worldIn.getBlockState(blockpos)))) {
                        worldIn.setBlockState(blockpos.up(), state); //Forge: Fix loss of state information during world gen.
                    }
                }
            }

        }

        return supported;
    }

    private void checkFallingHor(World worldIn, BlockPos pos) {
        if (!worldIn.isRemote) {
            IBlockState bs = worldIn.getBlockState(pos.up());
            if (bs.getBlock() == this) {
                return;
            }

            //search support
            Map<BlockPos, Integer> dists = new HashMap<>(16);
            supportingStructureWalker(worldIn, pos, dists, 0);

            boolean isSupported = false;
            for (BlockPos po : dists.keySet()) {
                if (!canFallThrough(worldIn.getBlockState(po.down()))) {
                    isSupported = true;
                }
            }

            if (!isSupported) {
                if (!fallInstantly && worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
                    EntityFallingBlock entityfallingblock = new EntityFallingBlock(worldIn, (double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, worldIn.getBlockState(pos));
                    this.onStartFalling(entityfallingblock);
                    worldIn.spawnEntity(entityfallingblock);
                } else {
                    IBlockState state = worldIn.getBlockState(pos);
                    worldIn.setBlockToAir(pos);

                    for (BlockPos blockpos = pos.down();
                         blockpos.getY() > 0;
                         blockpos = blockpos.down()) {
                        if ((worldIn.isAirBlock(blockpos) || canFallThrough(worldIn.getBlockState(blockpos)))) {
                            worldIn.setBlockState(blockpos.up(), state); //Forge: Fix loss of state information during world gen.
                        }
                    }


                }
            }

        }
    }

    private void supportingStructureWalker(World worldIn, BlockPos pos, Map<BlockPos, Integer> dists, int currentDist) {
        if (currentDist > fallingDistHor) {
            return;
        }

        if(!worldIn.isBlockLoaded(pos)) {
            return;
        }

        if (dists.containsKey(pos)) {
            if (dists.get(pos) <= currentDist) {
                return;
            }
        }
        if (!canFallThrough(worldIn.getBlockState(pos))) {
            dists.put(pos, currentDist);
        } else {
            return;
        }

        supportingStructureWalker(worldIn, pos.east(), dists, currentDist + 1);
        supportingStructureWalker(worldIn, pos.south(), dists, currentDist + 1);
        supportingStructureWalker(worldIn, pos.west(), dists, currentDist + 1);
        supportingStructureWalker(worldIn, pos.north(), dists, currentDist + 1);
    }

    private void checkFallingDirect(World worldIn, BlockPos pos) {
        if ((worldIn.isAirBlock(pos.down()) || canFallThrough(worldIn.getBlockState(pos.down()))) && pos.getY() >= 0) {

            if (!fallInstantly && worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
                if (!worldIn.isRemote) {
                    EntityFallingBlock entityfallingblock = new EntityFallingBlock(worldIn, (double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, worldIn.getBlockState(pos));
                    this.onStartFalling(entityfallingblock);
                    worldIn.spawnEntity(entityfallingblock);
                }
            } else {
                IBlockState state = worldIn.getBlockState(pos);
                worldIn.setBlockToAir(pos);

                for (BlockPos blockpos = pos.down();
                     blockpos.getY() > 0;
                     blockpos = blockpos.down()) {
                    if ((worldIn.isAirBlock(blockpos) || canFallThrough(worldIn.getBlockState(blockpos)))) {
                        worldIn.setBlockState(blockpos.up(), state); //Forge: Fix loss of state information during world gen.
                    }
                }

            }
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)) {
            worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
        }

        return false;
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        super.onEntityWalk(worldIn, pos, entityIn);
        worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        checkFallableDirect(worldIn, pos);
    }

    protected void onStartFalling(EntityFallingBlock fallingEntity) {
        fallingEntity.setHurtEntities(true);
    }

    @Override
    public int tickRate(World worldIn) {
        return 2;
    }

    public static boolean canFallThrough(IBlockState state) {
        Block block = state.getBlock();
        Material material = state.getMaterial();
        return block == Blocks.FIRE || material == Material.AIR || material == Material.WATER || material == Material.LAVA;
    }

    public void onEndFalling(World worldIn, BlockPos pos, IBlockState p_176502_3_, IBlockState p_176502_4_) {
    }

    public void onBroken(World worldIn, BlockPos pos) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (rand.nextInt(16) == 0) {
            BlockPos blockpos = pos.down();

            if (canFallThrough(worldIn.getBlockState(blockpos))) {
                double d0 = (double) ((float) pos.getX() + rand.nextFloat());
                double d1 = (double) pos.getY() - 0.05D;
                double d2 = (double) ((float) pos.getZ() + rand.nextFloat());
                worldIn.spawnParticle(EnumParticleTypes.FALLING_DUST, d0, d1, d2, 0.0D, 0.0D, 0.0D, Block.getStateId(stateIn));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public int getDustColor(IBlockState state) {
        return -16777216;
    }
}