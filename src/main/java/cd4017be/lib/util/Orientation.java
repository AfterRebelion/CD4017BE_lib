package cd4017be.lib.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.minecraft.util.EnumFacing.*;

import java.util.Arrays;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.client.renderer.block.model.ModelRotation;

/**
 * 
 * @author CD4017BE
 */
public enum Orientation implements IStringSerializable {
	N(NORTH),	E(EAST),	S(SOUTH),	W(WEST),
	Bn(DOWN),	Be(DOWN),	Bs(DOWN),	Bw(DOWN),
	Rn(SOUTH),	Re(WEST),	Rs(NORTH),	Rw(EAST),
	Tn(UP),		Te(UP),		Ts(UP),		Tw(UP);

	public final EnumFacing front, back;
	public final Vec3d X, Y, Z;

	private Orientation(EnumFacing front) {
		this.front = front;
		this.back = front.getOpposite();
		this.X = rotate(new Vec3d(1, 0, 0));
		this.Y = rotate(new Vec3d(0, 1, 0));
		this.Z = rotate(new Vec3d(0, 0, 1));
	}

	@Override
	public String getName() {
		return name().toLowerCase();
	}

	public Orientation reverse() {
		int i = ordinal();
		i = (4 - (i >> 2)) % 4 << 2 | (4 - i) % 4;
		return values()[i];
	}

	public static Orientation fromFacing(EnumFacing front) {
		switch(front) {
		case DOWN: return Bn;
		case UP: return Tn;
		case SOUTH: return S;
		case WEST: return W;
		case EAST: return E;
		default: return N;
		}
	}

	public EnumFacing rotate(EnumFacing dir) {
		if (dir == NORTH) return front;
		if (dir.getAxis() != Axis.X) {
			if ((ordinal() & 4) != 0) dir = dir.rotateAround(Axis.X);
			if ((ordinal() & 8) != 0) dir = dir.getOpposite();
		}
		if (dir.getAxis() != Axis.Y) {
			if ((ordinal() & 1) != 0) dir = dir.rotateY();
			if ((ordinal() & 2) != 0) dir = dir.getOpposite();
		}
		return dir;
	}

	public EnumFacing invRotate(EnumFacing dir) {
		if (dir == front) return NORTH;
		if (dir.getAxis() != Axis.Y) {
			if ((ordinal() & 2) != 0) dir = dir.getOpposite();
			if ((ordinal() & 1) != 0) dir = dir.rotateYCCW();
		}
		if (dir.getAxis() != Axis.X) {
			if ((ordinal() & 8) != 0) dir = dir.getOpposite();
			if ((ordinal() & 4) != 0) dir = dir.getOpposite().rotateAround(Axis.X);
		}
		return dir;
	}

	public AxisAlignedBB rotate(AxisAlignedBB box) {
		switch(ordinal() >> 2) {
		case 1: box = new AxisAlignedBB(box.minX, box.minZ, 1.0 - box.maxY, box.maxX, box.maxZ, 1.0 - box.minY); break;
		case 2: box = new AxisAlignedBB(box.minX, 1.0 - box.maxY, 1.0 - box.maxZ, box.maxX, 1.0 - box.minY, 1.0 - box.minZ); break;
		case 3: box = new AxisAlignedBB(box.minX, 1.0 - box.maxZ, box.minY, box.maxX, 1.0 - box.minZ, box.maxY); break;
		}
		switch(ordinal() & 3) {
		case 1: box = new AxisAlignedBB(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX); break;
		case 2: box = new AxisAlignedBB(1.0 - box.maxX, box.minY, 1.0 - box.maxZ, 1.0 - box.minX, box.maxY, 1.0 - box.minZ); break;
		case 3: box = new AxisAlignedBB(box.minZ, box.minY, 1.0 - box.maxX, box.maxZ, box.maxY, 1.0 - box.minX); break;
		}
		return box;
	}

	public Vec3d rotate(Vec3d vec) {
		double x = vec.x, y, z;
		switch(ordinal() >> 2) {
		case 1: y = vec.z; z = -vec.y; break;
		case 2: y = -vec.y; z = -vec.z; break;
		case 3: y = -vec.z; z = vec.y; break;
		default: y = vec.y; z = vec.z;
		}
		switch(ordinal() & 3) {
		case 1: return new Vec3d(-z, y, x);
		case 2: return new Vec3d(-x, y, -z);
		case 3: return new Vec3d(z, y, -x);
		default: return new Vec3d(x, y, z);
		}
	}

	public Vec3d invRotate(Vec3d vec) {
		double x, y = vec.y, z;
		switch(ordinal() & 3) {
		case 1: x = vec.z; z = -vec.x; break;
		case 2: x = -vec.x; z = -vec.z; break;
		case 3: x = -vec.z; z = vec.x; break;
		default: x = vec.x; z = vec.z;
		}
		switch(ordinal() >> 2) {
		case 1: return new Vec3d(x, -z, y);
		case 2: return new Vec3d(x, -y, -z);
		case 3: return new Vec3d(x, z, -y);
		default: return new Vec3d(x, y, z);
		}
	}

	@SideOnly(Side.CLIENT)
	public ModelRotation getModelRotation() {
		return ModelRotation.values()[ordinal()];
	}

	public static final PropertyEnum<Orientation> XY_12_ROT = PropertyEnum.create("orient", Orientation.class, Arrays.asList(Bn, Bs, Bw, Be, Tn, Ts, Tw, Te, N, S, W, E));
	public static final PropertyEnum<Orientation> ALL_AXIS = PropertyEnum.create("orient", Orientation.class, Arrays.asList(Bn, Tn, N, S, W, E));
	public static final PropertyEnum<Orientation> HOR_AXIS = PropertyEnum.create("orient", Orientation.class, Arrays.asList(N, S, W, E));
	public static final PropertyEnum<Orientation> ALL_AXIS_INV = PropertyEnum.create("orient", Orientation.class, Arrays.asList(Bs, Ts, Rn, Rs, Rw, Re));
	public static final PropertyEnum<Orientation> HOR_AXIS_INV = PropertyEnum.create("orient", Orientation.class, Arrays.asList(Rn, Rs, Rw, Re));
}
