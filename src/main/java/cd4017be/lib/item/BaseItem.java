package cd4017be.lib.item;

import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.util.TooltipUtil;

/**
 *
 * @author CD4017BE
 */
public class BaseItem extends Item {

	public BaseItem(String id) {
		super();
		this.setRegistryName(id);
		this.setTranslationKey(TooltipUtil.unlocalizedNameFor(this));
		this.init();
	}

	protected void init() {
		BlockItemRegistry.registerItemStack(new ItemStack(this), "item." + this.getRegistryName().getPath());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, @Nullable World player, List<String> list, ITooltipFlag b) {
		String s = this.getTranslationKey(item) + ".tip";
		if (TooltipUtil.showShiftHint()) {
			String s1 = TooltipUtil.getConfigFormat(s);
			if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
		} else if (TooltipUtil.showAltHint()) {
			String sA = s + "A";
			String s1 = TooltipUtil.getConfigFormat(sA);
			if (!s1.equals(sA)) list.addAll(Arrays.asList(s1.split("\n")));
		} else {
			if (TooltipUtil.hasTranslation(s) || (hasSubtypes && TooltipUtil.hasTranslation(super.getTranslationKey() + ":i.tip"))) list.add(TooltipUtil.getShiftHint());
			if (TooltipUtil.hasTranslation(s + "A") || (hasSubtypes && TooltipUtil.hasTranslation(super.getTranslationKey() + ":i.tipA"))) list.add(TooltipUtil.getAltHint());
		}
		super.addInformation(item, player, list, b);
	}

	@Override
	public String getTranslationKey(ItemStack item) {
		String s = super.getTranslationKey(item);
		return this.hasSubtypes ? s + ":" + item.getMetadata() : s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return TooltipUtil.translate(this.getTranslationKey(item) + ".name");
	}

}
