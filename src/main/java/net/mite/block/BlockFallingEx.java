package net.mite.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
        if (fallingDistHor == 0 && fallingDistVec == 0) {
            checkFallingDirect(worldIn, pos);
        } else if (fallingDistVec == 0) {
            checkFallingHor(worldIn, pos);
        } else {
            checkFallingVec(worldIn, pos);
        }
    }

    private void checkFallingVec(World worldIn, BlockPos pos) {
        if (!worldIn.isRemote) {

        }
    }

    private void checkFallingHor(World worldIn, BlockPos pos) {
        if (!worldIn.isRemote) {
//            IBlockState bs = worldIn.getBlockState(pos.up());
//            if (bs.getBlock() == this) {
//                return;
//            }

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