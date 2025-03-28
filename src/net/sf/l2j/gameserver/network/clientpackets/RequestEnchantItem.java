package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;

public class RequestEnchantItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestEnchantItem.class.getName());
	private static final int[] ENCHANT_SCROLLS = { 729, 730, 947, 948, 951, 952, 955, 956, 959, 960 };
	private static final int[] CRYSTAL_SCROLLS = { 731, 732, 949, 950, 953, 954, 957, 958, 961, 962 };
	private static final int[] BLESSED_SCROLLS = { 6569, 6570, 6571, 6572, 6573, 6574, 6575, 6576, 6577, 6578 };

    @Override
	protected void readImpl()
    {
    }

    @Override
	protected void runImpl()
    {

	}
	
	public void cancelActiveEnchant(L2PcInstance player)
	{
		player.sendPacket(Static.INAPPROPRIATE_ENCHANT_CONDITION);
		player.setActiveEnchantItem(null); 
		player.sendPacket(new EnchantResult(0, true));
		player.sendActionFailed();
	}
}
