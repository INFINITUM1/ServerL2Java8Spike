package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2SymbolVillageInstance extends L2VillageMasterInstance {
    //private static Logger _log = Logger.getLogger(L2SymbolVillageInstance.class.getName());

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.equals("Draw")) {
            player.sendPacket(new HennaEquipList(player));
        } else if (command.equals("RemoveList")) {
            showRemoveChat(player);
        } else if (command.startsWith("Remove ")) {
            int slot = Integer.parseInt(command.substring(7));
            player.removeHenna(slot);
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    public L2SymbolVillageInstance(int objectID, L2NpcTemplate template) {
        super(objectID, template);
    }

    @Override
    public String getHtmlPath(int npcId, int val) {
        return "data/html/newsymbolmaker/SymbolMaker.htm";
    }

    /*
     * (non-Javadoc) @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return false;
    }
}
