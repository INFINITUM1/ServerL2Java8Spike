/*
 * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 *
 * @author godson
 */
public class ExOlympiadUserInfo extends L2GameServerPacket
{
	// chcdSddddd
	private String _name;
	private int _side, _objId, _classId, _curHp, _maxHp, _curCp, _maxCp;

	/**
	 * @param _player
	 * @param _side (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfo(L2PcInstance player)
	{
		_side = player.getOlympiadSide();
		_objId = player.getObjectId();
		_classId = player.getClassId().getId();
		_curHp = (int) player.getCurrentHp();
		_maxHp = player.getMaxHp();
		_curCp = (int) player.getCurrentCp();
		_maxCp = player.getMaxCp();
		_name = player.getName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x29);
		writeC(_side);
		writeD(_objId);
		writeS(_name);
		writeD(_classId);
		writeD(_curHp);
		writeD(_maxHp);
		writeD(_curCp);
		writeD(_maxCp);
	}
}