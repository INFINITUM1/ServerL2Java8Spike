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


public class SpecialCamera extends L2GameServerPacket
{
    private int _id;
    private int _dist;
    private int _yaw;
    private int _pitch;
    private int _time;
    private int _duration;
	private final int _turn;
	private final int _rise;
	private final int _widescreen;
	private final int _unknown;

    public SpecialCamera(int id,int dist, int yaw, int pitch, int time, int duration)
    {
        _id = id;
        _dist = dist;
        _yaw = yaw;
        _pitch = pitch;
        _time = time;
        _duration = duration;
		_turn = 0;
		_rise = 0;
		_widescreen = 1;
		_unknown = 0;
    }
	
	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int widescreen, int unk)
	{
		_id = id;
		_dist = dist;
		_yaw = yaw;
		_pitch = pitch;
		_time = time;
		_duration = duration;
		_turn = turn;
		_rise = rise;
		_widescreen = widescreen;
		_unknown = unk;
		//_unknown2 = unk;
	}

    @Override
	public void writeImpl()
    {
        writeC(0xc7);
        writeD(_id);
        writeD(_dist);
        writeD(_yaw);
        writeD(_pitch);
        writeD(_time);
        writeD(_duration);
		
		writeD(_turn);
		writeD(_rise);
		writeD(_widescreen);
		writeD(_unknown);
		//writeD(_unknown2);
    }
}
