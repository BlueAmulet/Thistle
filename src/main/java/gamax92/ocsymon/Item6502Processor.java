package gamax92.ocsymon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class Item6502Processor extends Item {
	public Item6502Processor() {
		super();
		setUnlocalizedName("cpu6502");
		setCreativeTab(li.cil.oc.api.CreativeTab.instance);
	}
	
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister)
	{
		this.itemIcon = par1IconRegister.registerIcon(OCSymon.MODID + ":" + this.getUnlocalizedName().substring(5));
	}
}
