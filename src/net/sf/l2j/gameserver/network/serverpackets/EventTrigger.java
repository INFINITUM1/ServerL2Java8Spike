package net.sf.l2j.gameserver.network.serverpackets;

public class EventTrigger extends L2GameServerPacket {

    private int id, on;

    public EventTrigger(int id, int on) {
        this.id = id;
        this.on = on;
    }

    @Override
    protected final void writeImpl() {
        writeC(0xCF);
        writeD(id);
        writeC(on);
    }
}