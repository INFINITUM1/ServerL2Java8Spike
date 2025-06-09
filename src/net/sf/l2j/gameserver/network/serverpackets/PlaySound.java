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

/**
 * This class ...
 *
 * @version $Revision: 1.1.6.2 $ $Date: 2005/03/27 15:29:39 $
 */
public class PlaySound extends L2GameServerPacket
{
    private int _unk1;
    private String _soundFile;
    private int _unk3;
    private int _unk4;
    private int _unk5;
    private int _unk6;
    private int _unk7;

    public PlaySound(String soundFile)
    {
        _unk1   = 0;
        _soundFile  = soundFile;
        _unk3   = 0;
        _unk4   = 0;
        _unk5   = 0;
        _unk6   = 0;
        _unk7   = 0;
    }

    public PlaySound(int unk1, String soundFile, int unk3, int unk4, int unk5, int unk6, int unk7)
    {
        _unk1   = unk1;
        _soundFile  = soundFile;
        _unk3   = unk3;
        _unk4   = unk4;
        _unk5   = unk5;
        _unk6   = unk6;
        _unk7   = unk7;
    }


    @Override
	protected final void writeImpl()
    {
        writeC(0x98);
        writeD(_unk1);              //unk 0 for quest and ship;
        writeS(_soundFile);
        writeD(_unk3);              //unk 0 for quest; 1 for ship;
        writeD(_unk4);              //0 for quest; objectId of ship
        writeD(_unk5);              //x
        writeD(_unk6);              //y
        writeD(_unk7);				//z
    }
}
