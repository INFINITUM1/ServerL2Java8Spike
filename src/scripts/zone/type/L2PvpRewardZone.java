package scripts.zone.type;

import javolution.util.FastList;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.model.L2Character;
import scripts.zone.L2ZoneType;

public class L2PvpRewardZone extends L2ZoneType {

    public L2PvpRewardZone(int id) {
        super(id);
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("PvpRewards")) {
            FastList<EventReward> rewards = new FastList<EventReward>();
            String[] token = value.split(";");
            for (String reward : token) {
                if (reward.equals("")) {
                    continue;
                }

                String[] loc = reward.split(",");
                Integer id = Integer.valueOf(loc[0]);
                Integer count = Integer.valueOf(loc[1]);
                Integer chance = Integer.valueOf(loc[2]);
                if (id == null || count == null || chance == null) {
                    continue;
                }

                rewards.add(new EventReward(id, count, chance));
            }
            CustomServerData.getInstance().addPvpReward(getId(), rewards);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInPvpRewardZone(getId());
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInPvpRewardZone(0);
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }
}
