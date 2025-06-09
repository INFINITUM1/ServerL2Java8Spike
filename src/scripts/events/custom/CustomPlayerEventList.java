/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.events.custom;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author Администратор
 */
public class CustomPlayerEventList {

    private static final FastList<CustomPlayerEventHandler> _handlers = new FastList<>();

    public CustomPlayerEventList() {
    }

    public static void register(CustomPlayerEventHandler handler) {
        _handlers.add(handler);
        System.out.println("CustomPlayerEventList: " + handler.getClass().getSimpleName() + " loaded.");
    }

    public static void notifyPlayerLogin(L2PcInstance player) {
        _handlers.stream().filter((handler) -> !(handler == null)).forEach((handler) -> {
            handler.onPlayerLogin(player);
        });
    }

    public static void notifyPlayerDie(L2PcInstance victim, L2Character killer) {
        _handlers.stream().filter((handler) -> !(handler == null)).forEach((handler) -> {
            handler.onPlayerDie(victim, killer);
        });
    }

    public static void notifyPlayerCreate(L2PcInstance player) {
        _handlers.stream().filter((handler) -> !(handler == null)).forEach((handler) -> {
            handler.onPlayerCreate(player);
        });
    }
}
