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

import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.3.2.5 $ $Date: 2005/03/29 23:15:10 $
 */
public class PetStatusUpdate extends L2GameServerPacket {

    private String title;
    private long exp, cur, next;
    private int _maxHp, _maxMp, _summonType, _summonObj, _maxFed, _curFed, x, y, z, hp, mp, level;

    public PetStatusUpdate(L2Summon summon) {
        _maxHp = summon.getMaxHp();
        _maxMp = summon.getMaxMp();
        if (summon.isPet()) {
            L2PetInstance pet = (L2PetInstance) summon;
            _curFed = pet.getCurrentFed(); // how fed it is
            _maxFed = pet.getMaxFed(); //max fed it can be
        }
        _summonType = summon.getSummonType();
        _summonObj = summon.getObjectId();
        x = summon.getX();
        y = summon.getY();
        z = summon.getZ();
        title = summon.getTitle();
        hp = (int) summon.getCurrentHp();
        mp = (int) summon.getCurrentMp();
        level = summon.getLevel();
        exp = summon.getStat().getExp();
        cur = summon.getExpForThisLevel();
        next = summon.getExpForNextLevel();
    }
    
    public PetStatusUpdate(L2PcInstance partner) {
        _maxHp = partner.getMaxHp();
        _maxMp = partner.getMaxMp();

        _curFed = 100; // how fed it is
        _maxFed = 100; //max fed it can be

        _summonType = 1;
        _summonObj = partner.getObjectId();
        x = partner.getX();
        y = partner.getY();
        z = partner.getZ();
        title = partner.getTitle();
        hp = (int) partner.getCurrentHp();
        mp = (int) partner.getCurrentMp();
        level = partner.getLevel();
        exp = partner.getStat().getExp();
        cur = partner.getStat().getExp();
        next = partner.getStat().getExp();
    }

    @Override
    protected final void writeImpl() {
        writeC(0xb5);
        writeD(_summonType);
        writeD(_summonObj);
        writeD(x);
        writeD(y);
        writeD(z);
        writeS(title);
        writeD(_curFed);
        writeD(_maxFed);
        writeD(hp);
        writeD(_maxHp);
        writeD(mp);
        writeD(_maxMp);
        writeD(level);
        writeQ(exp);
        writeQ(cur);// 0% absolute value
        writeQ(next);// 100% absolute value
    }
}
