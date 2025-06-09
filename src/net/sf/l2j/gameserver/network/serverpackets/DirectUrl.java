package net.sf.l2j.gameserver.network.serverpackets;


public class DirectUrl extends L2GameServerPacket {

    public String _URL = "http://l2top.ru/vote/30601/";


    @Override
    protected void writeImpl() {
        writeC(0xFF);
        writeC(0x03);
        writeS(_URL);
    }
}