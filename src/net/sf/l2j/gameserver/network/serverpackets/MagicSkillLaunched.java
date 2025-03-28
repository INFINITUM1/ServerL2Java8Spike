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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import javolution.util.FastList;
import javolution.util.FastList.Node;

/**
 *
 * sample
 *
 * 0000: 8e  d8 a8 10 48  10 04 00 00  01 00 00 00  01 00 00    ....H...........
 * 0010: 00  d8 a8 10 48                                     ....H
 *
 *
 * format   ddddd d
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class MagicSkillLaunched extends L2GameServerPacket {

    private int _charObjId;
    private int _skillId;
    private int _skillLevel;
    private FastList<L2Object> _targets = new FastList<L2Object>();

    public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, FastList<L2Object> targets) {
        _charObjId = cha.getObjectId();
        _skillId = skillId;
        _skillLevel = skillLevel;
        if (targets == null || targets.isEmpty()) {
            return;
        }
        _targets.addAll(targets);
    }

    public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel) {
        _charObjId = cha.getObjectId();
        _skillId = skillId;
        _skillLevel = skillLevel;
        _targets.add(cha);
    }

    @Override
    protected final void writeImpl() {
        writeC(0x76);
        writeD(_charObjId);
        writeD(_skillId);
        writeD(_skillLevel);
        writeD(_targets.size());
        for (FastList.Node<L2Object> n = _targets.head(), end = _targets.tail(); (n = n.getNext()) != end;) {
            if (n == null) {
                continue;
            }

            write(n.getValue());
        }
        //_targets.clear();
    }

    private void write(L2Object target) {
        if (target == null) {
            return;
        }
        writeD(target.getObjectId());
    }

    @Override
    public void gcb() {
        //_targets.clear();
        //_targets = null;
    }
}
