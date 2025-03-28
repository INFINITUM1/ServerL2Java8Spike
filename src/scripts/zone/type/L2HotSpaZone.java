/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripts.zone.type;

import net.sf.l2j.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

public class L2HotSpaZone extends L2ZoneType {

    private int _skillId;

    public L2HotSpaZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("skillId")) {
            _skillId = Integer.parseInt(value);
        }else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        //character.getEffect(_skillId, 1);
        character.stopSkillEffects(4554);
        character.getEffect(4559, 1);
        switch (_skillId)
        {
            case 4556:
                character.stopSkillEffects(4551);
                break;
            case 4557:
                character.stopSkillEffects(4552);
                break;
            case 4558:
                character.stopSkillEffects(4553);
                break;
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