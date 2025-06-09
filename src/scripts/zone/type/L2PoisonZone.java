package scripts.zone.type;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2PoisonZone extends L2ZoneType {

    private int _skillId;
    private int _skillLvl;
    private boolean _danger;

    public L2PoisonZone(int id) {
        super(id);

        _danger = true;
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("skillId")) {
            _skillId = Integer.parseInt(value);
        } else if (name.equals("skillLvl")) {
            _skillLvl = Integer.parseInt(value);
        } else if (name.equals("Danger")) {
            _danger = Boolean.parseBoolean(value);
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (character.isPlayer()) {
            L2PcInstance player = (L2PcInstance) character;
            SkillTable.getInstance().getInfo(_skillId, _skillLvl).getEffects(player, player);
            if (_danger) {
                player.setInDangerArea(true);
            }
        }
    }

    @Override
    protected void onExit(L2Character character) {
        if (character.isPlayer()) {
            L2PcInstance player = (L2PcInstance) character;

            player.stopSkillEffects(_skillId);

            if (_danger) {
                player.setInDangerArea(false);
            }
        }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}
