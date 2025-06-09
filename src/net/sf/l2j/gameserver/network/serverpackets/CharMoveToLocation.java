package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2Character;

/**
 * 0000: 01 7a 73 10 4c b2 0b 00 00 a3 fc 00 00 e8 f1 ff .zs.L........... 0010:
 * ff bd 0b 00 00 b3 fc 00 00 e8 f1 ff ff .............
 *
 *
 * ddddddd
 *
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:57 $
 */
public class CharMoveToLocation extends L2GameServerPacket {

    private int _charObjId, _x, _y, _z, _xDst, _yDst, _zDst;

    public CharMoveToLocation(L2Character cha) {
        _charObjId = cha.getObjectId();
        _x = cha.getX();
        _y = cha.getY();
        _z = cha.getZ();
        _xDst = cha.getXdestination();
        _yDst = cha.getYdestination();
        _zDst = cha.getZdestination();
    }

    @Override
    protected final void writeImpl() {
        writeC(0x01);

        writeD(_charObjId);

        writeD(_xDst);
        writeD(_yDst);
        writeD(_zDst);

        writeD(_x);
        writeD(_y);
        writeD(_z);
    }
}
