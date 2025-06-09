/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model;

import java.util.List;

import javolution.util.FastList;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.actor.instance.L2BossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.ExMultiPartyCommandChannelInfo;

/**
 *
 * @author  chris_00
 */
public class L2CommandChannel
{
	private List<L2Party> _partys = null;
	private L2PcInstance _commandLeader = null;
	private int _channelLvl;

	/**
	 * Creates a New Command Channel and Add the Leaders party to the CC
	 *
	 * @param CommandChannelLeader
	 *
	 */
	public L2CommandChannel(L2PcInstance leader)
	{
		_commandLeader = leader;
		_partys = new FastList<L2Party>();
		_partys.add(leader.getParty());
		_channelLvl = leader.getParty().getLevel();
		leader.getParty().setCommandChannel(this);
		leader.getParty().broadcastToPartyMembers(Static.ExOpenMPCC);
	}

	/**
	 * Adds a Party to the Command Channel
	 * @param Party
	 */
	public void addParty(L2Party party)
	{
		_partys.add(party);
		if (party.getLevel() > _channelLvl)
			_channelLvl = party.getLevel();
		party.setCommandChannel(this);
		party.broadcastToPartyMembers(Static.ExOpenMPCC);
		broadcastToChannelMembers(new ExMultiPartyCommandChannelInfo(this));
	}

	/**
	 * Removes a Party from the Command Channel
	 * @param Party
	 */
	public void removeParty(L2Party party)
	{
		_partys.remove(party);
		_channelLvl = 0;
		for (L2Party pty : _partys)
		{
			if (pty.getLevel() > _channelLvl)
				_channelLvl = pty.getLevel();
		}
		party.setCommandChannel(null);
		party.broadcastToPartyMembers(Static.ExCloseMPCC);
		if(_partys.size() < 2)
		{
    		broadcastToChannelMembers(Static.THE_COMMAND_CHANNEL_HAS_BEEN_DISBANDED);
			disbandChannel();
		}
	}

	/**
	 * disbands the whole Command Channel
	 */
	public void disbandChannel()
	{
		for (L2Party party : _partys)
		{
			if(party != null)
				removeParty(party);
		}
		_partys = null;
	}

	/**
	 * @return overall membercount of the Command Channel
	 */
	public int getMemberCount()
	{
		int count = 0;
		for (L2Party party : _partys)
		{
			if(party != null)
				count += party.getMemberCount();
		}
		return count;
	}

	/**
	 * Broadcast packet to every channelmember
	 * @param L2GameServerPacket
	 */
	public void broadcastToChannelMembers(L2GameServerPacket gsp)
	{
		if (_partys == null || _partys.isEmpty())
			return;
		
		for (L2Party party : _partys)
		{
			if(party != null)
				party.broadcastToPartyMembers(gsp);
		}
	}


	/**
	 * @return list of Parties in Command Channel
	 */
	public List<L2Party> getPartys()
	{
		return _partys;
	}

	/**
	 * @return list of all Members in Command Channel
	 */
	public List<L2PcInstance> getMembers()
	{
		List<L2PcInstance> members = new FastList<L2PcInstance>();
		for (L2Party party : getPartys())
		{
			members.addAll(party.getPartyMembers());
		}
		return members;
	}

	/**
	 *
	 * @return Level of CC
	 */
	public int getLevel() { return _channelLvl; }

	/**
	 * @param sets the leader of the Command Channel
	 */
	public void setChannelLeader(L2PcInstance leader)
	{
		_commandLeader = leader;
	}

	/**
	 * @return the leader of the Command Channel
	 */
	public L2PcInstance getChannelLeader()
	{
		return _commandLeader;
	}
	
	//телепорт всех патей
	public void teleTo(int x, int y,  int z)
	{
		for (L2Party party : getPartys())
		{
			if (party == null)
				continue;
			
			party.teleTo(x, y,  z);
		}
	}
}
