package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Random;

import net.sf.l2j.gameserver.util.Online;

public final class SendStatus extends L2GameServerPacket {

    protected int online_players;
    protected int max_online;
    protected int online_priv_store;
    private boolean can_writeImpl = false;

    public SendStatus() {
        online_players = Online.getInstance().getCurrentOnline();
        online_priv_store = Online.getInstance().getOfflineTradersOnline();
        max_online = Online.getInstance().getMaxOnline();
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        writeC(0x00); // Packet ID 
        writeD(0x01); // World ID 
        writeD(max_online); // Max Online 
        writeD(online_players); // Current Online  
        writeD(online_players); // Current Online  
        writeD(online_priv_store); // Priv.Sotre Chars 

        Random ppc = new Random();

        // SEND TRASH 
        writeH(0x30);
        writeH(0x2C);
        for (int x = 0; x < 11; x++) {
            writeH(41 + ppc.nextInt(17));
        }
        writeD(43 + ppc.nextInt(17));
        int z = 36219 + ppc.nextInt(1987);
        writeD(z);
        writeD(z);
        writeD(37211 + ppc.nextInt(2397));
        writeD(0x00);
        writeD(0x02);
    }
}
