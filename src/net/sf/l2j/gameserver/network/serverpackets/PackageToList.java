package net.sf.l2j.gameserver.network.serverpackets;

//import java.util.Iterator;
import java.util.Map;
import javolution.util.FastList;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class PackageToList extends L2GameServerPacket {

    private boolean can_writeImpl = false;
    private FastList<CharInfo> chars = new FastList<>();

    public PackageToList(L2PcInstance activeChar) {
        Map<Integer, String> characters = activeChar.getAccountChars();
        // No other chars in the account of this player
        if (characters.size() < 1) {
            activeChar.sendMessage("Этого символа нет");
            return;
        }

        for (Map.Entry<Integer, String> e : characters.entrySet()) {
            chars.add(new CharInfo(characters.get(e.getKey()), e.getKey()));
        }
        can_writeImpl = true;
    }

    /**
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
     */
    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0xC2);
        writeD(chars.size());
        CharInfo _char = null;
        for (FastList.Node<CharInfo> n = chars.head(), end = chars.tail(); (n = n.getNext()) != end;) {
            _char = n.getValue();
            if (_char == null) {
                continue;
            }

            writeD(_char._id); // Character object id
            writeS(_char._name); // Character name
        }
    }

    @Override
    public void gc() {
        chars.clear();
        chars = null;
    }

    static class CharInfo {

        public String _name;
        public int _id;

        public CharInfo(String __name, int __id) {
            _name = __name;
            _id = __id;
        }
    }
}
