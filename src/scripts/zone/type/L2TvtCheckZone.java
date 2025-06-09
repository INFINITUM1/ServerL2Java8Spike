/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

/**
 *
 * @author Администратор
 */
public class L2TvtCheckZone extends L2ZoneType {

    public L2TvtCheckZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        character.enableTvtReward();
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
