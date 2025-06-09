package scripts.zone.type;

import javolution.util.FastList;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.zone.L2ZoneType;

public class L2SpecialZone extends L2ZoneType {

    private FastList<Integer> _forbiddenItem = new FastList<Integer>();
    private int _teleId = 0;

    public L2SpecialZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("itemId")) {
            String[] token = value.split(",");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }
                _forbiddenItem.add(Integer.parseInt(point));
            }
        } else if (name.equals("teleId")) {
            _teleId = Integer.parseInt(value);
            CustomServerData.getInstance().addTeleItem(_teleId, _forbiddenItem);
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        manageEnter(character.getPlayer(), true);
    }

    @Override
    protected void onExit(L2Character character) {
        manageEnter(character.getPlayer(), false);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    private void manageEnter(L2PcInstance player, boolean enter) {
        if (player == null) {
            return;
        }

        player.setForbItem(enter ? _teleId : 0);
        if (player.checkForbiddenItems()) {
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
        }
    }
}
