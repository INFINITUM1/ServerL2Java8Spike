package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Appearing Packet Handler<p>
 * <p>
 * 0000: 30 <p>
 * <p>
 *
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/29 23:15:33 $
 */
public final class Appearing extends L2GameClientPacket {

    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
        final L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPAH() < 200) {
            return;
        }

        player.sCPAH();

        if (player.isDeleting()) {
            player.sendActionFailed();
            return;
        }

        /*if(player.getObserverMode() == 1)
        {
        System.out.println("##Appearing##1#" + player.getName());
        player.appearObserverMode();
        return;
        }
        
        if(player.getObserverMode() == 2)
        {
        System.out.println("##Appearing##2#" + player.getName());
        player.returnFromObserverMode();
        return;
        }*/

        if (!player.isTeleporting() && !player.inObserverMode()) {
            //System.out.println("##Appearing##3#" + player.getName());
            player.sendActionFailed();
            return;
        }

        if (player.getObserverMode() == 2) {
            player.returnFromObserverMode();
        }

        player.onTeleported();
        /*if (!player.inObserverMode())
        {
        //System.out.println(TimeLogger.getLogTime()+"Player "+player.getName()+" tried to use TP bug. / Kicked.");
        player.logout();
        return;
        }*/

        // ѕерсонаж по€вл€етс€ только после полной прогрузки
		/*ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
        {
        public void run()
        {*/
        /*if(player.isTeleporting())
        player.onTeleported();
        else
        {
        if (!player.inObserverMode())
        {
        //System.out.println(TimeLogger.getLogTime()+"Player "+player.getName()+" tried to use TP bug. / Kicked.");
        player.logout();
        return;
        }
        }
        
        sendPacket(new UserInfo(player));
        player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);*/
        /*}
        }, 1500);*/
    }
}
