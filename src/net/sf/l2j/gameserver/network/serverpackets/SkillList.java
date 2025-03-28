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

//import java.util.Vector;
import javolution.util.FastTable;

/**
 *
 *
 * sample
 * 0000: 6d 0c 00 00 00 00 00 00 00 03 00 00 00 f3 03 00    m...............
 * 0010: 00 00 00 00 00 01 00 00 00 f4 03 00 00 00 00 00    ................
 * 0020: 00 01 00 00 00 10 04 00 00 00 00 00 00 01 00 00    ................
 * 0030: 00 2c 04 00 00 00 00 00 00 03 00 00 00 99 04 00    .,..............
 * 0040: 00 00 00 00 00 02 00 00 00 a0 04 00 00 00 00 00    ................
 * 0050: 00 01 00 00 00 c0 04 00 00 01 00 00 00 01 00 00    ................
 * 0060: 00 76 00 00 00 01 00 00 00 01 00 00 00 a3 00 00    .v..............
 * 0070: 00 01 00 00 00 01 00 00 00 c2 00 00 00 01 00 00    ................
 * 0080: 00 01 00 00 00 d6 00 00 00 01 00 00 00 01 00 00    ................
 * 0090: 00 f4 00 00 00
 *
 * format   d (ddd)
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/27 15:29:39 $
 */
public class SkillList extends L2GameServerPacket {

    private FastTable<Skill> _skills;

    static class Skill {

        public int id;
        public int level;
        public int passive;
        public int disabled;

        Skill(int id, int level, boolean passive, boolean disabled) {
            this.id = id;
            this.level = level;
            this.passive = passive ? 1 : 0;
            this.disabled = passive ? 0 : disabled ? 1 : 0;
        }
    }

    public SkillList() {
        _skills = new FastTable<Skill>();
    }

    public void addSkill(int id, int level, boolean passive, boolean disabled) {
        _skills.add(new Skill(id, level, passive, disabled));
    }

    @Override
    protected final void writeImpl() {
        writeC(0x58);
        writeD(_skills.size());

        for (int i = 0, n = _skills.size(); i < n; i++) {
            write(_skills.get(i));
        }
    }

    private void write(Skill s) {
        if (s == null) {
            return;
        }
        writeD(s.passive);
        writeD(s.level);
        writeD(s.id);
        writeC(s.disabled); //c5
    }

    @Override
    public void gc() {
        _skills.clear();
        _skills = null;
    }
}
