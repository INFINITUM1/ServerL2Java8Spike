package net.sf.l2j.gameserver.network.serverpackets;

public class TradeStartOk extends L2GameServerPacket {
    
    @Override
    protected final void writeImpl() {

        writeC(0x1F);
    }
}
