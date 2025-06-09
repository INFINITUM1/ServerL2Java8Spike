/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.events.custom;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author Администратор
 */
public interface CustomPlayerEventHandler {

    public void onPlayerLogin(L2PcInstance player);

    public void onPlayerDie(L2PcInstance victim, L2Character killer);

    public void onPlayerCreate(L2PcInstance player);

}
