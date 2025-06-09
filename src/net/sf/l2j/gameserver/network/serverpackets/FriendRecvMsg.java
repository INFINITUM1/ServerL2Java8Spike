
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2World;
/**
 * Send Private (Friend) Message
 *
 * Format: c dSSS
 *
 * d: Unknown
 * S: Sending Player
 * S: Receiving Player
 * S: Message
 *
 * @author Tempy
 */
public class FriendRecvMsg extends L2GameServerPacket
{
	private String _sender, _receiver, _message;

	public FriendRecvMsg(String sender, String reciever, String message)
	{
		_sender = sender;
		_receiver = reciever;
		_message = message;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfd);

		writeD(0); // ??
		writeS(_receiver);
		writeS(_sender);
		writeS(_message);
	}
}
