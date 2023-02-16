package cd4017be.lib.templates;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class TabMaterials extends CreativeTabs {

	public ItemStack item;

	public TabMaterials(String label) {
		super(label);
	}

	@Override
	public ItemStack getIcon() {
		return item;
	}

	@Override
	public ItemStack createIcon() {
		return item;
	}

}
