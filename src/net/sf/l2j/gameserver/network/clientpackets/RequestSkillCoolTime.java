package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author: Death @date: 16/2/2007 @time: 21:16:06
 */
public class RequestSkillCoolTime extends L2GameClientPacket {

    @Override
    public void readImpl() {
    }

    @Override
    public void runImpl() {
        L2PcInstance pl = getClient().getActiveChar();
        if (pl == null) {
            return;
        }

        if (System.currentTimeMillis() - pl.gCPAT() < 1000) {
            return;
        }

        pl.sCPAT();
        pl.sendSkillCoolTime();
    }
}