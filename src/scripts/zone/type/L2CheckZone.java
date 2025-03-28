package scripts.zone.type;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.util.Location;
import scripts.zone.L2ZoneType;

public class L2CheckZone extends L2ZoneType {

    private static FastList<Integer> penalty_items = new FastList<Integer>();
    private Location loc;

    public L2CheckZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("items")) {
            String[] items = value.split(",");
            for (String item : items) {
                if (item.equalsIgnoreCase("")) {
                    continue;
                }

                penalty_items.add(Integer.parseInt(item));
            }
        } else if (name.equals("loc")) {
            String[] xyz = value.split(",");
            loc = new Location(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (!character.isPlayer()) {
            return;
        }
        for (L2ItemInstance item : character.getPcInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (penalty_items.contains(item.getItemId())) {
                character.teleToLocation(loc);
                break;
            }
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
