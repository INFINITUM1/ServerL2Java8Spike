package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2OlympiadObeliskInstance extends L2NpcInstance {
    //private final int HERO_ITEMS[] = { 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390 };
    private final static int DestinyCirclet = 6842;

    public L2OlympiadObeliskInstance(final int objectId, final L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(final L2PcInstance player, final String command) {
        String filename = Olympiad.OLYMPIAD_HTML_PATH;
        if (command.isEmpty()) {
            if (player.isNoble() || player.isHero()) {
                filename += "obelisk001.htm";
            } else {
                filename += "obelisk001a.htm";
            }
            showPage(player, filename);
        } else if (command.startsWith("Obelisk")) {
            final String val = command.substring(8);

            if (val.equals("010")) {
                if (player.isHero()) {
                    filename += "obelisk010b.htm";
                } else if (Hero.getInstance().isInactiveHero(player.getObjectId())) {
                    filename += "obelisk010.htm";
                } else {
                    filename += "obelisk010a.htm";
                }
            } else if (val.equals("herolist")) {
                player.sendPacket(new ExHeroList());
            } else if (val.equals("020c")) {
                if (!player.isHero()) {
                    filename += "obelisk020d.htm";
                } else if (checkCanGiveDestinyCircletItems(player)) {
                    //final L2ItemInstance createditem = ItemTable.getInstance().createItem(DestinyCirclet, player.getObjectId(), 0, "HeroItems");
                    player.getInventory().addItem("Olympiad", DestinyCirclet, 1, player, null);
                    //Log.LogItem(player, Log.GetItem, createditem);
                    player.sendItems(true);
                    player.sendPacket(SystemMessage.id(SystemMessageId.EARNED_ITEM).addItemName(DestinyCirclet));
                } else {
                    filename += "obelisk020c.htm";
                }
            } else if (val.equals("Hero")) {
                if (Hero.getInstance().isInactiveHero(player.getObjectId())) {
                    Hero.getInstance().activateHero(player);
                } else {
                    filename += "obelisk010a.htm";
                }
            } else if (val.equals("Back")) {
                if (player.isNoble() || player.isHero()) {
                    filename += "obelisk001.htm";
                } else {
                    filename += "obelisk001a.htm";
                }
            }
            if (!filename.equals("") && !filename.equals(Olympiad.OLYMPIAD_HTML_PATH)) {
                showPage(player, filename);
            }
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    private boolean checkCanGiveDestinyCircletItems(final L2PcInstance player) {
        if (player.getInventory().getItemByItemId(DestinyCirclet) != null) {
            return false;
        }
        return true;
    }

    private void showPage(L2PcInstance player, String filename) {
        String page = HtmCache.getInstance().getHtmForce(filename);
        NpcHtmlMessage nhm = NpcHtmlMessage.id(getObjectId());
        nhm.setHtml(page);
        nhm.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(nhm);
    }
}
