package cd4017be.lib.util;

import java.util.List;

import cd4017be.api.automation.IOperatingArea;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketPlayerAbilities;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 *
 * @author CD4017BE
 */
public class MovedBlock {

	public final NBTTagCompound nbt;
	public final IBlockState block;

	public MovedBlock(IBlockState block, NBTTagCompound tile) {
		this.block = block;
		this.nbt = tile;
	}

	public boolean set(World world, BlockPos pos) {
		TileEntity tile = null;
		//boolean multipart = false;
		if (nbt != null) {
			nbt.setInteger("x", pos.getX());
			nbt.setInteger("y", pos.getY());
			nbt.setInteger("z", pos.getZ());
			tile = TileEntity.create(world, nbt);
			if (tile instanceof IOperatingArea) {
				int [] area = ((IOperatingArea)tile).getOperatingArea();
				area[0] += pos.getX(); area[3] += pos.getX();
				area[1] += pos.getY(); area[4] += pos.getY();
				area[2] += pos.getZ(); area[5] += pos.getZ();
			}
			/* multipart = nbt.getString("id").equals("savedMultipart");
			if (multipart) {
				try {
					Class multipartHelper = Class.forName("codechicken.multipart.MultipartHelper");
					Method m = multipartHelper.getMethod("createTileFromNBT", new Class[] { World.class, NBTTagCompound.class });
					tile = (TileEntity)m.invoke(null, new Object[] { world, nbt });
				} catch (Exception e) {e.printStackTrace();}
			} else {
				
			}*/
		}
		/*if (multipart && set) {
			try {
				Class multipartHelper = Class.forName("codechicken.multipart.MultipartHelper");
				multipartHelper.getMethod("sendDescPacket", new Class[] { World.class, TileEntity.class }).invoke(null, new Object[] { world, tile });
				Class tileMultipart = Class.forName("codechicken.multipart.TileMultipart");
				tileMultipart.getMethod("onMoved", new Class[0]).invoke(tile, new Object[0]);
			} catch (Exception e) {e.printStackTrace();}
		}*/
		return setBlock(world, pos, block, tile);
	}

	public static MovedBlock get(World world, BlockPos pos) {
		IBlockState id = world.getBlockState(pos);
		NBTTagCompound nbt = null;
		TileEntity te = world.getTileEntity(pos);
		if (te != null) {
			if (te instanceof IOperatingArea) {
				int[] area = ((IOperatingArea)te).getOperatingArea();
				area[0] -= pos.getX(); area[3] -= pos.getX();
				area[1] -= pos.getY(); area[4] -= pos.getY();
				area[2] -= pos.getZ(); area[5] -= pos.getZ();
			}
			nbt = new NBTTagCompound();
			te.writeToNBT(nbt);
		}
		return new MovedBlock(id, nbt);
	}

	/**
	 * Place a Block without notify anything
	 * @param world the World
	 * @param x block x position
	 * @param y block y position
	 * @param z block z position
	 * @param state the block
	 * @param tile block TileEntity
	 * @return true if placed successfully
	 */
	public static boolean setBlock(World world, BlockPos pos, IBlockState state, TileEntity tile) {
		if (!world.isBlockLoaded(pos)) return false;
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		IBlockState state0 = chunk.getBlockState(pos);
		Block block = state.getBlock();
		int oldLight = state0.getLightValue(world, pos);
		int oldOpac = state0.getLightOpacity(world, pos);
		
		//Chunk.setBlockState() {
		
		world.removeTileEntity(pos);
		if (state0 == state) {
			world.setTileEntity(pos, tile);
			world.notifyBlockUpdate(pos, state0, state, 3);
			return true;
		}
		
		int bx = pos.getX() & 15;
		int y = pos.getY();
		int bz = pos.getZ() & 15;
		int p = bz << 4 | bx;
		
		if (y >= chunk.precipitationHeightMap[p] - 1) chunk.precipitationHeightMap[p] = -999;
		int h = chunk.getHeightMap()[p];
		
		ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
		boolean flag = false;
		if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE) {
			if (block == Blocks.AIR) return false;
			extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, world.provider.hasSkyLight());
			flag = y >= h;
		}
		
		extendedblockstorage.set(bx, y & 15, bz, state);
		if (extendedblockstorage.get(bx, y & 15, bz).getBlock() != block) return false;
		
		if (flag) chunk.generateSkylightMap();
		else {
			int opac = state.getLightOpacity(world, pos);
			
			if (opac > 0) {
				if (y >= h) chunk.relightBlock(bx, y + 1, bz);
			} else if (y == h - 1) chunk.relightBlock(bx, y, bz);
			
			if (opac != oldOpac && (opac < oldOpac || chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0 || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0)) {
				chunk.propagateSkylightOcclusion(bx, bz);
			}
		}
		
		world.setTileEntity(pos, tile);
		chunk.setModified(true);
		
		//}
		
		if (state.getLightOpacity(world, pos) != oldOpac || state.getLightValue(world, pos) != oldLight) {
			world.profiler.startSection("checkLight");
			world.checkLight(pos);
			world.profiler.endSection();
		}
		world.notifyBlockUpdate(pos, state0, state, 3);
		return true;
	}

	/**
	 * Move an entity to a new Position
	 * @param entity the Entity to move
	 * @param dim new world dimension
	 * @param x new x position
	 * @param y new y position
	 * @param z new z position
	 */
	public static void moveEntity(Entity entity, int dimN, double x, double y, double z) {
		if (!entity.isEntityAlive() || entity.isRiding()) return;
		WorldServer worldO = (WorldServer)entity.world;
		int dimO = entity.dimension;
		if (dimN == dimO) {
			if (entity instanceof EntityPlayerMP)
				((EntityPlayerMP)entity).setPositionAndUpdate(x, y, z);
			else
				entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
			return;
		}
		List<Entity> passengers = entity.getPassengers();
		for (Entity e : passengers) {
			e.dismountRidingEntity();
			moveEntity(e, dimN, x, y, z);
		}
		MinecraftServer server = worldO.getMinecraftServer();
		WorldServer worldN = server.getWorld(dimN);
		if (entity instanceof EntityPlayerMP) {
			PlayerList pl = server.getPlayerList();
			EntityPlayerMP player = (EntityPlayerMP)entity;
			player.dimension = dimN;
			player.connection.sendPacket(new SPacketRespawn(player.dimension, worldN.getDifficulty(), worldN.getWorldInfo().getTerrainType(), player.interactionManager.getGameType()));
			pl.updatePermissionLevel(player);
			worldO.removeEntityDangerously(player);
			transferEntityToWorld(player, worldN, x, y, z);
			pl.preparePlayer(player, worldO);
			player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
			player.interactionManager.setWorld(worldN);
			player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));
			pl.updateTimeAndWeatherForPlayer(player, worldN);
			pl.syncPlayerInventory(player);
			for (PotionEffect potioneffect : player.getActivePotionEffects())
				player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
			net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, dimO, dimN);
		} else {
			entity.dimension = dimN;
			entity.world.removeEntity(entity);
			transferEntityToWorld(entity, worldN, x, y, z);
		}
		for (Entity e : passengers) e.startRiding(entity, true);
	}

	private static void transferEntityToWorld(Entity entity, WorldServer world, double x, double y, double z) {
		entity.isDead = false;
		entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
		world.spawnEntity(entity);
		world.updateEntityWithOptionalForce(entity, false);
		entity.setWorld(world);
	}

}
