package net.sf.l2j.gameserver.network.serverpackets;

/**
 *
 * @author  devScarlet & mrTJO
 */
public class ServerClose extends L2GameServerPacket
{
	/**
	 * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0x26);
	}
}
