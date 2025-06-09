/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.zone.type;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

/**
 *
 * @author Администратор
 */
public class L2ZakenWelcomeZone extends L2ZoneType {

    public L2ZakenWelcomeZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
            if (character.getLevel() > Config.ZAKEN_PLAYER_MAX_LVL) {
                character.teleToLocation(47471, 186695, -3480);
                return;
            }
        if (character.isPlayer()) {
            GrandBossManager.getInstance().getZone(55242, 219131, -3251).allowPlayerEntry(character.getPlayer(), 9000000);
        }
    }

    @Override
    protected void onExit(L2Character character) {
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}
