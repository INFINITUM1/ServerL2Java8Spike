package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;

public class RequestMagicSkillList extends L2GameClientPacket
{
	/**
	 * packet type id 0x38
	 * format:		c
	 * @param rawPacket
	 */

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		player.sendSkillList();
	}
}