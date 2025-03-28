package scripts.items.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import scripts.items.IItemHandler;

public class ChristmasTree implements IItemHandler {

    private static final int[] ITEM_IDS = {
        5560, /* x-mas tree */
        5561 /* Special x-mas tree*/};

    public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl) {
        L2PcInstance player = (L2PcInstance) playable;
        if (player.isMounted()) {
            player.sendActionFailed();
            return;
        }

        L2Skill skill = SkillTable.getInstance().getInfo((item.getItemId() == 5560 ? 2137 : 2138), 1);
        if (skill.checkCondition(player, player, false)) {
            player.useMagic(skill, false, false);
        } else {
            player.sendActionFailed();
        }
    }

    public int[] getItemIds() {
        return ITEM_IDS;
    }
}
