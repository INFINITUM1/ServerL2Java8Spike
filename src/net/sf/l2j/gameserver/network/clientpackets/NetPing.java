/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.l2j.gameserver.network.clientpackets;

/**
 *
 * @author Администратор
 */
public class NetPing extends L2GameClientPacket {

    private int _time, _unk1, _unk2;

    @Override
    protected void readImpl() {
        _time = readD();
        _unk1 = readD();
        _unk2 = readD();
    }

    @Override
    protected void runImpl() {
        if (getClient() == null) {
            return;
        }
        
        getClient().onNetPing(_time);
    }
}
