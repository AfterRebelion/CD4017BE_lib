package cd4017be.api.rs_ctr.com;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Communication object that allows devices to remotely interact with dynamically selected blocks in the world.
 * @author CD4017BE
 */
public class BlockReference {

	public final World world;
	public final BlockPos pos;
	public final EnumFacing face;

	public BlockReference(World world, BlockPos pos, EnumFacing face) {
		this.world = world;
		this.pos = pos;
		this.face = face;
	}

	/**
	 * @return whether actions on this block can be performed without potentially force loading chunks.
	 */
	public boolean isLoaded() {
		return world.isBlockLoaded(pos);
	}

	/**
	 * @return this block's state
	 */
	public IBlockState getState() {
		return world.getBlockState(pos);
	}

	/**
	 * @return the TileEntity of this block
	 */
	public TileEntity getTileEntity() {
		return world.getTileEntity(pos);
	}

	/**
	 * @param <C>
	 * @param cap the capability to obtain
	 * @return an instance of the given capability or null if not available
	 */
	public @Nullable <C> C getCapability(Capability<C> cap) {
		TileEntity te = world.getTileEntity(pos);
		if (te == null) return null;
		return te.getCapability(cap, face);
	}

	/**
	 * The callback interface for transmitting BlockReferences.
	 */
	@FunctionalInterface
	public interface BlockHandler {

		/**
		 * called when the BlockReference changes
		 * @param ref the new BlockReference
		 */
		void updateBlock(BlockReference ref);

	}

}
