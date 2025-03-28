package net.sf.l2j.gameserver.network.serverpackets;

/**
 * Чисто технический пакет, либо пакет С5. Во всяком случе С4 клиент на него никак не реагирует.
 */
public class ServerCloseSocket extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xAF);
		writeD(0x01); //Always 1??!?!?!
	}
}
