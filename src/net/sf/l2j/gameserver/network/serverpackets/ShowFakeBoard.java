package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ShowFakeBoard extends L2GameServerPacket
{
    private final String _html;
    private final String _id;

    public ShowFakeBoard(String html, String id)
    {
        _html = html;
        _id = id;
    }

    @Override
    protected final void writeImpl()
    {
        writeC(0x6e); // opcode Community Board
        writeC(0x01); // show window

        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;

        String html = _html;

        if (html != null)
        {
            if (_id.equalsIgnoreCase("101"))
                player.cleanBypasses(false);

            html = player.encodeBypasses(html, false);
        }

        writeS(_id + "\u0008" + (html != null ? html : ""));
    }

    @Override
    public String getType()
    {
        return "S.ShowFakeBoard";
    }
}