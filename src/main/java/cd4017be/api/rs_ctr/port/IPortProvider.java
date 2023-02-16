package cd4017be.api.rs_ctr.port;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The signal system API interface to be implemented by devices offering communication ports.
 * @author CD4017BE
 */
public interface IPortProvider {

	/**
	 * @param pin the SignalPort's pin id
	 * @return the SignalPort for given pin
	 */
	@Nullable Port getPort(int pin);

	/**
	 * get the callback of the given sink/receiving port<dl>
	 * Warning: this method and the returned function may get called from within {@link Port#onLoad()}
	 * so be careful when initializing Ports during {@link TileEntity#onLoad()} because blocks can't be accessed at that point.
	 * @param pin the SignalPort's pin id
	 * @return a function to call whenever the state of the transmitted signal changes
	 */
	@Nonnull Object getPortCallback(int pin);

	/**
	 * set the callback of the given source/sending port
	 * @param pin the SignalPort's pin id
	 * @param callback a function to call whenever the state of the transmitted signal changes.<br>
	 * null indicates that transmission is currently not possible (the receiver became unloaded)
	 */
	void setPortCallback(int pin, @Nullable Object callback);

	/**
	 * @param pin the SignalPort's pin id
	 * @param event what happened: {@link #E_CONNECT}, {@link #E_DISCONNECT}, {@link #E_CON_ADD}, {@link #E_CON_REM}
	 */
	void onPortModified(Port port, int event);

	/**port was functionally connected to another port */
	public static final int E_CONNECT = 1;
	/**port was functionally disconnected from another port */
	public static final int E_DISCONNECT = 2;
	/**the port's connection object was set */
	public static final int E_CON_ADD = 20;
	/**the port's connection object was removed */
	public static final int E_CON_REM = 24;
	/**the port's connection state needs client sync */
	public static final int E_CON_UPDATE = 16;

	/**
	 * @param world the world
	 * @param pos block position in the world
	 * @param pin the pin id, by convention:<br>
	 * pin < 0 for Entities, 0 <= pin < 0x8000 for TileEntities, 0xe8000 <= pin < 0xa000 for RelayPorts
	 * @return the signal port hosted at given location
	 */
	public static Port getPort(World world, BlockPos pos, int pin) {
		if (world == null || pos == null) return null;
		if (pin >= 0) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IPortProvider)
				return ((IPortProvider)te).getPort(pin);
		} else for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos), (e)-> e instanceof IPortProvider)) {
			Port port = ((IPortProvider)e).getPort(pin);
			if (port != null) return port;
		}
		return null;
	}

}
