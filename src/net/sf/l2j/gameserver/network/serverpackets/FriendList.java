
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import javolution.util.FastMap;

/**
 * Support for "Chat with Friends" dialog.
 *
 * Format: ch (hdSdh)
 * h: Total Friend Count
 *
 * h: Unknown
 * d: Player Object ID
 * S: Friend Name
 * d: Online/Offline
 * h: Unknown
 *
 * @author Tempy
 *
 */
public class FriendList extends L2GameServerPacket
{
    private FastMap<Integer, String> _friends;  

    private L2PcInstance _activeChar;

    public FriendList(L2PcInstance character)
    {
    	_activeChar = character;
    }

	@Override
	protected final void writeImpl()
	{
		if (_activeChar == null)
			return;

		_friends = new FastMap<Integer, String>(); 
		_friends.putAll(_activeChar.getFriends());
		
		if (_friends == null || _friends.isEmpty())
			return;

        writeC(0xfa);
		writeH(_friends.size());
				
        L2PcInstance friend = null;
		for (FastMap.Entry<Integer, String> e = _friends.head(), end = _friends.tail(); (e = e.getNext()) != end;) 
		{
			Integer id = e.getKey(); // No typecast necessary.
			String name = e.getValue(); // No typecast necessary.
			if (id == null || name == null)
				continue;
			
			if (id == _activeChar.getObjectId())
				continue;
			
			friend = L2World.getInstance().getPlayer(id);    			

    		writeH(0); // ??
    		writeD(id);
    		writeS(name);

    		if (friend == null)
    			writeD(0); // offline
    		else
    			writeD(1); // online

    		writeH(0); // ??
		}
		/*for(final Integer fId : _friends.keySet())
		{
			if (fId == null || fId == _activeChar.getObjectId())
				continue;
				
			String friendName = _friends.get(fId);
				
			friend = L2World.getInstance().getPlayer(friendName);    			

    		writeH(0); // ??
    		writeD(fId);
    		writeS(friendName);

    		if (friend == null)
    			writeD(0); // offline
    		else
    			writeD(1); // online

    		writeH(0); // ??
		}*/
	}
	
	@Override
	public void gc()
	{
		_friends.clear();
		_friends = null;
	}
}
