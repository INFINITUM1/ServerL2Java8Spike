package net.sf.l2j.gameserver.network.serverpackets;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class PartySmallWindowAll extends L2GameServerPacket {

    private int leader_id, loot;
    private FastList<MemberInfo> _members = new FastList<MemberInfo>();

    public PartySmallWindowAll(L2Party party, L2PcInstance exclude) {
        leader_id = party.getPartyLeaderOID();
        loot = party.getLootDistribution();
        for (L2PcInstance member : party.getPartyMembers()) {
            if (member == null || member.equals(exclude)) {
                continue;
            }

            _members.add(new MemberInfo(member.getName(), member.getObjectId(), (int) member.getCurrentCp(), member.getMaxCp(), (int) member.getCurrentHp(), member.getMaxHp(), (int) member.getCurrentMp(), member.getMaxMp(), member.getLevel(), member.getClassId().getId(), member.getRace().ordinal()));
        }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x4e);
        writeD(leader_id); // c3 party leader id
        writeD(loot); //c3 party loot type (0,1,2,....)
        writeD(_members.size());

        for (FastList.Node<MemberInfo> n = _members.head(), end = _members.tail(); (n = n.getNext()) != end;) {
            MemberInfo member = n.getValue();
            if (member == null) {
                continue;
            }

            writeD(member._id);
            writeS(member._name);
            writeD(member.curCp);
            writeD(member.maxCp);
            writeD(member.curHp);
            writeD(member.maxHp);
            writeD(member.curMp);
            writeD(member.maxMp);
            writeD(member.level);
            writeD(member.class_id);
            writeD(0);//writeD(0x01); ??
            writeD(member.race);
        }
        //members.clear();
    }

    @Override
    public void gcb() {
        _members.clear();
        //_members = null;
    }

    static class MemberInfo {

        public String _name;
        public int _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id, race;

        public MemberInfo(String __name, int __id, int _curCp, int _maxCp, int _curHp, int _maxHp, int _curMp, int _maxMp, int _level, int _class_id, int race) {
            _name = __name;
            _id = __id;
            curCp = _curCp;
            maxCp = _maxCp;
            curHp = _curHp;
            maxHp = _maxHp;
            curMp = _curMp;
            maxMp = _maxMp;
            level = _level;
            class_id = _class_id;
            this.race = race;
        }
    }
}
