package cd4017be.lib.property;

import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * 
 * @author CD4017BE
 */
public class PropertyBlock implements IUnlistedProperty<IBlockState> {

	private final String name;

	public PropertyBlock(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(IBlockState value) {
		return true;
	}

	@Override
	public Class<IBlockState> getType() {
		return IBlockState.class;
	}

	@Override
	public String valueToString(IBlockState value) {
		return value == null ? "none" : value.getBlock().getTranslationKey() + ":" + value.getBlock().getMetaFromState(value);
	}

}
