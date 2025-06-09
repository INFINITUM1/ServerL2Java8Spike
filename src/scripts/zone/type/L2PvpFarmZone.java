package scripts.zone.type;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;
import scripts.zone.L2ZoneType;

public class L2PvpFarmZone extends L2ZoneType {

    private FastList<Integer> _EMPTY = new FastList<Integer>();
    private FastList<Integer> _forbiddenItems = new FastList<Integer>();
    private FastList<EventReward> _pvpReward = new FastList<EventReward>();
    //
    private boolean _REWARD = false;
    private FastList<EventReward> _rewardStart = new FastList<EventReward>();
    private FastList<EventReward> _rewardFinish = new FastList<EventReward>();
    //
    private static long ANNOUNCE_DELAY = TimeUnit.MINUTES.toMillis(Config.REWARD_PVP_ZONE_ANNOUNCE);

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("items")) {
            String[] token = value.split(",");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }
                _forbiddenItems.add(Integer.parseInt(point));
            }
        } else if (name.equals("pvpRewards")) {
            String[] token = value.split(";");
            for (String item : token) {
                if (item.isEmpty()) {
                    continue;
                }

                String[] id = item.split(",");
                try {
                    _pvpReward.add(new EventReward(Integer.parseInt(id[0]), Integer.parseInt(id[1]), Integer.parseInt(id[2])));
                } catch (NumberFormatException nfe) {
                }
            }
        } else if (name.equals("time")) {
            //12:00-12:30;13:00-13:30
            String[] data = value.split(";");
            for (String hour : data) {
                //
                String[] sf = hour.split("-"); //12:00-12:30
                //
                String start = sf[0]; //12:00
                String finish = sf[1]; //12:30
                //
                String[] s = start.split(":");
                String[] f = finish.split(":");
                //
                try {
                    _rewardStart.add(new EventReward(Integer.parseInt(s[0]), Integer.parseInt(s[1]), 0));
                    _rewardFinish.add(new EventReward(Integer.parseInt(f[0]), Integer.parseInt(f[1]), 0));
                } catch (NumberFormatException nfe) {
                    //
                }
            }

            EventReward task = null;
            for (FastList.Node<EventReward> n = _rewardStart.head(), end = _rewardStart.tail(); (n = n.getNext()) != end;) {
                task = n.getValue();
                if (task == null) {
                    continue;
                }

                ThreadPoolManager.getInstance().scheduleAi(new RewardStart(), ((getNextSpawnTime(0, false, task.id, task.count) - System.currentTimeMillis()) - ANNOUNCE_DELAY), false);
            }
            task = null;
            for (FastList.Node<EventReward> n = _rewardFinish.head(), end = _rewardFinish.tail(); (n = n.getNext()) != end;) {
                task = n.getValue();
                if (task == null) {
                    continue;
                }

                ThreadPoolManager.getInstance().scheduleAi(new RewardFinish(), ((getNextSpawnTime(0, false, task.id, task.count) - System.currentTimeMillis()) - ANNOUNCE_DELAY), false);
            }
        } else {
            super.setParameter(name, value);
        }
    }

    private class RewardStart implements Runnable {

        @Override
        public void run() {
            //System.out.println("###RewardStart#1#");
            ThreadPoolManager.getInstance().scheduleAi(new ManageReward(true), ANNOUNCE_DELAY, false);
            Announcements.getInstance().announceToAll("Через " + Config.REWARD_PVP_ZONE_ANNOUNCE + " минут будут включены награды в PVP-зоне!");
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            }
            for (int i = (Config.REWARD_PVP_ZONE_ANNOUNCE - 1); i > 0; i--) {
                Announcements.getInstance().announceToAll("Через " + i + " минут будут включены награды в PVP-зоне!");
                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                }
            }
        }
    }

    private class RewardFinish implements Runnable {

        @Override
        public void run() {
            ThreadPoolManager.getInstance().scheduleAi(new ManageReward(false), ANNOUNCE_DELAY, false);
            Announcements.getInstance().announceToAll("Через " + Config.REWARD_PVP_ZONE_ANNOUNCE + " минут будут отключены награды в PVP-зоне!");
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            }
            for (int i = (Config.REWARD_PVP_ZONE_ANNOUNCE - 1); i > 0; i--) {
                Announcements.getInstance().announceToAll("Через " + i + " минут будут отключены награды в PVP-зоне!");
                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                }
            }
        }
    }

    private class ManageReward implements Runnable {

        public boolean trigger;

        public ManageReward(boolean trigger) {
            this.trigger = trigger;
        }

        @Override
        public void run() {
            _REWARD = trigger;
            if (_REWARD) {
                Announcements.getInstance().announceToAll("Включены награды в PVP-зоне!");
            } else {
                Announcements.getInstance().announceToAll("Выключены награды в PVP-зоне!");
            }
            //System.out.println("###ManageReward#1#" + trigger);
            //System.out.println("###ManageReward#2#" + _REWARD);
        }
    }

    private static long getNextSpawnTime(long respawn, boolean nextday, int hour, int minute) {
        Calendar tomorrow = new GregorianCalendar();
        if (nextday) {
            tomorrow.add(Calendar.DATE, 1);
        }
        Calendar result = new GregorianCalendar(
                tomorrow.get(Calendar.YEAR),
                tomorrow.get(Calendar.MONTH),
                tomorrow.get(Calendar.DATE),
                hour,
                minute);
        respawn = result.getTimeInMillis();
        //System.out.println("####" + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date(respawn)));
        if (respawn < System.currentTimeMillis()) {
            return getNextSpawnTime(0, true, hour, minute);
        }
        return respawn;
    }

    public L2PvpFarmZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(L2Character character) {
        character.setInPvpFarmZone(true);
        character.sendMessage("Вы вошли в PvP-фарм зону.");
        if (Config.REWARD_PVP_ZONE) {
            checkForbiddenItems(character.getPlayer());
            //
            character.setFordItems(_forbiddenItems);
        }
    }

    @Override
    protected void onExit(L2Character character) {
        character.setInPvpFarmZone(false);
        character.sendMessage("Вы вышли из PvP-фарм зоны.");
        //
        if (Config.REWARD_PVP_ZONE) {
            character.setFordItems(_EMPTY);
        }
    }

    @Override
    public void onDieInside(L2Character character) {
    }

    @Override
    public void onReviveInside(L2Character character) {
    }

    private void checkForbiddenItems(L2PcInstance player) {
        if (player == null) {
            return;
        }
        // снятие переточеных вещей
        boolean f = false;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (_forbiddenItems.contains(item.getItemId())) {
                f = true;
                player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
            }
        }

        if (f) {
            player.sendItems(false);
            player.broadcastUserInfo();
        }
    }

    public void giveRewards(L2PcInstance player) {
        //System.out.println("###4#");
        if (!_REWARD) {
            return;
        }
        //System.out.println("###5#");
        if (_pvpReward.isEmpty()) {
            return;
        }
        //System.out.println("###6#");
        EventReward reward = null;
        for (FastList.Node<EventReward> k = _pvpReward.head(), endk = _pvpReward.tail(); (k = k.getNext()) != endk;) {
            reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (reward.chance == 100 || Rnd.get(100) < reward.chance) {
                player.addItem("PvpFarmZone", reward.id, reward.count, player, true);
            }
        }
        reward = null;
    }
}
