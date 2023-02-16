package cd4017be.lib.network;

import java.util.ArrayDeque;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Lib;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Provides GUIs and handles their network communication.<dl>
 * To open GUIs with this, simply let your Blocks, Entities and Items implement the corresponding {@link IGuiHandlerBlock}, {@link IGuiHandlerEntity} or {@link IGuiHandlerItem}.
 * And for network communication let your Containers implement {@link IServerPacketReceiver} and {@link IPlayerPacketReceiver}.
 * @author CD4017BE
 */
@SuppressWarnings("deprecation")
public class GuiNetworkHandler extends NetworkHandler implements IGuiHandler {

	public static final int BLOCK_GUI_ID = 0, ENTITY_GUI_ID = -1, ITEM_GUI_ID = -256;
	/**the instance */
	public static GuiNetworkHandler GNH_INSTANCE;

	private static final int MAX_QUEUED = 16;
	private ArrayDeque<PacketBuffer> packetQueue = new ArrayDeque<PacketBuffer>(MAX_QUEUED);

	public static void register() {
		if (GNH_INSTANCE == null) GNH_INSTANCE = new GuiNetworkHandler("4017g");
	}

	private GuiNetworkHandler(String channel) {
		super(channel);
		NetworkRegistry.INSTANCE.registerGuiHandler(Lib.instance, this);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		Container container = Minecraft.getMinecraft().player.openContainer;
		int curId = container.windowId;
		int id = pkt.markReaderIndex().readInt();
		if ((curId == id || curId == 0) && container instanceof IServerPacketReceiver) {
			((IServerPacketReceiver)container).handleServerPacket(pkt);
			if (pkt.readableBytes() > 0) {
				StringBuilder sb = new StringBuilder("Packet > GUI: ");
				printPacketData(sb, pkt);
				Lib.LOG.info(NETWORK, sb.toString());
			}
		} else if (id > curId) {//packet received too early, schedule it for later processing when GUI is opened
			if (packetQueue.size() >= MAX_QUEUED) {
				packetQueue.remove();
				Lib.LOG.warn(NETWORK, "GUI packet queue overflow!");
			}
			pkt.resetReaderIndex();
			packetQueue.add(pkt);
		} else Lib.LOG.warn(NETWORK, "received packet for invalid GUI {} @CLIENT, expected id {} ({})", id, container.windowId, container.getClass());
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, EntityPlayerMP sender) throws Exception {
		int id = pkt.readInt();
		Container container = sender.openContainer;
		if (container.windowId == id && container instanceof IPlayerPacketReceiver) {
			((IPlayerPacketReceiver)container).handlePlayerPacket(pkt, sender);
			if (pkt.readableBytes() > 0) {
				StringBuilder sb = new StringBuilder("Packet > SERVER: ");
				printPacketData(sb, pkt);
				Lib.LOG.info(NETWORK, sb.toString());
			}
		} else Lib.LOG.warn(NETWORK, "received packet for invalid GUI {} @SERVER, expected id {} ({})", id, container.windowId, container.getClass());
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID >= BLOCK_GUI_ID) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.isBlockLoaded(pos)) return null;
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof IGuiHandlerBlock)
				return ((IGuiHandlerBlock)block).getContainer(state, world, pos, player, ID);
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IGuiHandlerTile)
				return ((IGuiHandlerTile)te).getContainer(player, ID);
		} else if (ID == ENTITY_GUI_ID) {
			Entity entity = world.getEntityByID(z);
			if (entity instanceof IGuiHandlerEntity)
				return ((IGuiHandlerEntity)entity).getContainer(player, x, y);
		} else if (ID >= ITEM_GUI_ID) {
			ID -= ITEM_GUI_ID;
			if (ID >= player.inventory.getSizeInventory()) return null;
			ItemStack stack = player.inventory.getStackInSlot(ID);
			Item item = stack.getItem();
			if (item instanceof IGuiHandlerItem)
				return ((IGuiHandlerItem)item).getContainer(stack, player, ID, x, y, z);
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID >= BLOCK_GUI_ID) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.isBlockLoaded(pos)) return null;
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof IGuiHandlerBlock)
				return ((IGuiHandlerBlock)block).getGuiScreen(state, world, pos, player, ID);
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IGuiHandlerTile)
				return ((IGuiHandlerTile)te).getGuiScreen(player, ID);
		} else if (ID == ENTITY_GUI_ID) {
			Entity entity = world.getEntityByID(z);
			if (entity instanceof IGuiHandlerEntity)
				return ((IGuiHandlerEntity)entity).getGuiScreen(player, x, y);
		} else if (ID >= ITEM_GUI_ID) {
			ID -= ITEM_GUI_ID;
			if (ID >= player.inventory.getSizeInventory()) return null;
			ItemStack stack = player.inventory.getStackInSlot(ID);
			Item item = stack.getItem();
			if (item instanceof IGuiHandlerItem)
				return ((IGuiHandlerItem)item).getGuiScreen(stack, player, ID, x, y, z);
		}
		return null;
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onGuiOpened(InitGuiEvent.Post event) {
		if (event.getGui() instanceof GuiContainer)
			for (int i = packetQueue.size(); i > 0; i--) {
				PacketBuffer buf = packetQueue.remove();
				try {handleServerPacket(buf);}
				catch (Exception e) {logError(buf, "QUEUED", e);}
			}
	}

	/**
	 * open a Block managed GUI
	 * @param player the player accessing the GUI
	 * @param pos the target block's position
	 * @param id arbitrary data (must be >= 0)
	 * @see IGuiHandlerBlock#getContainer(IBlockState, World, BlockPos, EntityPlayer, int)
	 */
	public static void openBlockGui(EntityPlayer player, BlockPos pos, int id) {
		if (id < 0) throw new IllegalArgumentException("id must not be negative!");
		if (player.world.isRemote && !BlockGuiHandler.OPEN_CLIENT) return;
		player.openGui(Lib.instance, BLOCK_GUI_ID + id, player.world, pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * open an Entity managed GUI
	 * @param player the player accessing the GUI
	 * @param entity the target Entity
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @see IGuiHandlerEntity#getContainer(EntityPlayer, int, int)
	 */
	public static void openEntityGui(EntityPlayer player, Entity entity, int x, int y) {
		if (player.world.isRemote && !BlockGuiHandler.OPEN_CLIENT) return;
		player.openGui(Lib.instance, ENTITY_GUI_ID, player.world, x, y, entity.getEntityId());
	}

	/**
	 * open an Item managed GUI
	 * @param player the player accessing the GUI
	 * @param slot inventory slot of the target ItemStack
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @param z arbitrary data
	 * @see IGuiHandlerItem#getContainer(ItemStack, EntityPlayer, int, int, int, int)
	 */
	public static void openItemGui(EntityPlayer player, int slot, int x, int y, int z) {
		if (slot < 0 || slot >= player.inventory.getSizeInventory())
			throw new IndexOutOfBoundsException("slot index out of range: " + slot);
		if (player.world.isRemote && !BlockGuiHandler.OPEN_CLIENT) return;
		player.openGui(Lib.instance, ITEM_GUI_ID + slot, player.world, x, y, z);
	}

	/**
	 * open an Item managed GUI for a held Item
	 * @param player the player accessing the GUI
	 * @param hand the held item to target
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @param z arbitrary data
	 */
	public static void openHeldItemGui(EntityPlayer player, EnumHand hand, int x, int y, int z) {
		openItemGui(player, hand == EnumHand.OFF_HAND ? 40 : player.inventory.currentItem, x, y, z);
	}

	/**
	 * @param container the container involved in GUI communication
	 * @return a new PacketBuffer with prepared header
	 */
	public static PacketBuffer preparePacket(Container container) {
		PacketBuffer pkt = new PacketBuffer(Unpooled.buffer());
		pkt.writeInt(container.windowId);
		return pkt;
	}

}
