package net.sf.l2j.gameserver.network.serverpackets;


public class DirectUrl1 extends L2GameServerPacket {

    public String _URL = "http://cp.l2grandzone.pw/cabinet#main";


    @Override
    protected void writeImpl() {
        writeC(0xFF);
        writeC(0x03);
        writeS(_URL);
    }
}