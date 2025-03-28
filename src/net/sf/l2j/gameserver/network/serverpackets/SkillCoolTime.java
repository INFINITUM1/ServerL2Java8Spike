package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Map;
import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character.DisabledSkill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class SkillCoolTime extends L2GameServerPacket {

    private int id = 0;
    private FastList<ReuseDelay> _reuse = new FastList<ReuseDelay>();

    public SkillCoolTime(L2PcInstance player, Map<Integer, DisabledSkill> map) {
        for (Map.Entry<Integer, DisabledSkill> entry : map.entrySet()) {
            if (entry == null) {
                continue;
            }
            putSkill(player, entry.getKey(), entry.getValue());
        }
    }

    public SkillCoolTime(int id) {
        this.id = id;
    }

    private void putSkill(L2PcInstance player, Integer id, DisabledSkill ds) {
        if (id == null || ds == null) {
            return;
        }

        if (!ds.isInReuse()) {
            player.removeDisabledSkill(id);
            return;
        }
        int expire = (int) (Math.max(ds.expire - System.currentTimeMillis(), 0)) / 1000;
        if (expire == 0) {
            player.removeDisabledSkill(id);
            return;
        }
        _reuse.add(new ReuseDelay(id, ds.delay, expire));
    }

    private static class ReuseDelay {

        public int id;
        public int reuse;
        public int expire;

        public ReuseDelay(int id, int reuse, int expire) {
            this.id = id;
            this.reuse = (int) reuse / 1000;
            this.expire = expire;
        }
    }

    /**
     * @see
     * net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl() {
        //SkillCoolTime:d(d) d(d) d(d) d(d)
        writeC(0xc1); //packet type
        writeD(_reuse.size()); // list size
        for (ReuseDelay ts : _reuse) {
            if (ts == null) {
                continue;
            }

            writeD(ts.id);
            writeD(0x00);
            writeD(ts.reuse);
            writeD(ts.expire);
        }
    }
}
