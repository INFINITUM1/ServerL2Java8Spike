package scripts.communitybbs.Manager;

import javolution.text.TextBuilder;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.TimeLogger;

public class AugmentBBSManager extends BaseBBSManager {

    private static AugmentBBSManager _instance;

    public static void init() {
        _instance = new AugmentBBSManager();
    }

    public static AugmentBBSManager getInstance() {
        return _instance;
    }

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        if (command.equalsIgnoreCase("_bbstransaug")) {
            showIndex(player);
        } else if (command.startsWith("_bbstransaug_")) {
            String choise = command.substring(13).trim();
            if (choise.startsWith("show")) {
                int obj = Integer.parseInt(choise.substring(4).trim());
                show1Item(player, obj);
            } else if (choise.equalsIgnoreCase("step2")) {
                showNextItems(player);
            } else if (choise.startsWith("step3")) {
                int obj = Integer.parseInt(choise.substring(5).trim());
                show2Item(player, obj);
            } else if (choise.equalsIgnoreCase("finish")) {
                transFinish(player);
            }
        }
    }

    private void showIndex(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getPwHtm("menu"));
        tb.append("&nbsp;&nbsp;Перенос ЛС:<br>&nbsp;&nbsp;Откуда переносим?<br><br><table width=300>");

        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        String itemIcon = "";
        int itemType = 0;
        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }

            if (!(item.canBeEnchanted())) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            enchantLevel = item.getEnchantLevel();
            itemIcon = item.getItem().getIcon();
            itemType = item.getItem().getType2();

            if (item.isAugmented()) {
                L2Skill skill = item.getAugmentation().getAugmentSkill();
                if (skill == null) {
                    continue;
                }

                tb.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass _bbstransaug_show " + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a><br1>" + getAugmentSkill(skill.getId(), skill.getLevel()) + "</td></tr>");
            }
        }

        player.setTrans1Item(0);
        player.setTrans2Item(0);
        player.setTransAugment(0);

        tb.append("</table><br>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    // информации об аугментации
    private String getAugmentSkill(int skillId, int skillLvl) {
        L2Skill augment = SkillTable.getInstance().getInfo(skillId, 1);
        if (augment == null) {
            return "";
        }

        String augName = augment.getName();
        String type = "";
        if (augment.isActive()) {
            type = "Актив";
        } else if (augment.isPassive()) {
            type = "Пассив";
        } else {
            type = "Шанс";
        }

        augName = augName.replace("Item Skill: ", "");
        return "<font color=336699>Аугмент:</font> <font color=bef574>" + augName + " (" + type + ":" + skillLvl + "lvl)</font>";
    }

    private void show1Item(L2PcInstance player, int objectId) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getPwHtm("menu"));
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item == null) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (!item.isAugmented()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        tb.append("&nbsp;&nbsp;Перенос ЛС:<br>&nbsp;&nbsp;Из этой пушки переносим?<br>");
        tb.append("<table width=300><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><font color=LEVEL>" + item.getItem().getName() + " (+" + item.getEnchantLevel() + ")</font><br></td></tr></table><br><br>");

        L2Skill augment = item.getAugmentation().getAugmentSkill();
        if (augment == null) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        tb.append("<br>&nbsp;&nbsp;" + getAugmentSkill(augment.getId(), augment.getLevel()) + "<br>");

        L2ItemInstance coin = player.getInventory().getItemByItemId(Config.AUGMENT_COIN);
        if (coin != null && coin.getCount() >= Config.AUGMENT_PRICE) {
            tb.append("&nbsp;&nbsp; <font color=33CC00>Стоимость: " + Config.AUGMENT_PRICE + " " + Config.AUGMENT_COIN_NAME + ".</font><br>");
            tb.append("&nbsp;&nbsp;&nbsp;&nbsp;<button value=\"Продолжить\" action=\"bypass _bbstransaug_step2\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            player.setTrans1Item(objectId);
            player.setTransAugment(augment.getId());
        } else {
            tb.append("&nbsp;&nbsp; <font color=FF6666>Стоимость: " + Config.AUGMENT_PRICE + " " + Config.AUGMENT_COIN_NAME + ".</font><br>");
            tb.append("&nbsp;&nbsp;&nbsp;&nbsp;<font color=999999>[Продолжить]</font>");
        }

        tb.append("</body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void showNextItems(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("<html><body>");
        tb.append(getPwHtm("menu"));
        tb.append("&nbsp;&nbsp;Перенос ЛС:<br>&nbsp;&nbsp;Куда переносим?<br><br><table width=300>");
        int objectId = 0;
        String itemName = "";
        int enchantLevel = 0;
        int itemType = 0;
        String itemIcon = "";

        for (L2ItemInstance item : player.getInventory().getItems()) {
            if (item == null) {
                continue;
            }
            if (!(item.canBeEnchanted())) {
                continue;
            }

            objectId = item.getObjectId();
            itemName = item.getItem().getName();
            enchantLevel = item.getEnchantLevel();
            itemType = item.getItem().getType2();
            itemIcon = item.getItem().getIcon();

            if (player.getTrans1Item() != objectId && itemType == L2Item.TYPE2_WEAPON && item.getItem().getItemGrade() >= L2Item.CRYSTAL_C && !item.isAugmented() && !item.isWear() && !item.isEquipped() && !item.isHeroItem() && item.isDestroyable()) {
                tb.append("<tr><td><img src=\"" + itemIcon + "\" width=32 height=32></td><td><a action=\"bypass _bbstransaug_step3 " + objectId + "\">" + itemName + " (+" + enchantLevel + ")</a></td></tr>");
            }
        }

        tb.append("</table><br></body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void show2Item(L2PcInstance player, int objectId) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getPwHtm("menu"));
        L2ItemInstance item = player.getInventory().getItemByObjectId(objectId);
        if (item == null) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (item.isAugmented()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        tb.append("&nbsp;&nbsp; Перенос ЛС:<br>&nbsp;&nbsp;В эту пушку переносим?<br>");
        tb.append("<table width=300><tr><td><img src=\"" + item.getItem().getIcon() + "\" width=32 height=32></td><td><font color=LEVEL>" + item.getItem().getName() + " (+" + item.getEnchantLevel() + ")</font><br></td></tr></table><br><br>");

        L2ItemInstance coin = player.getInventory().getItemByItemId(Config.AUGMENT_COIN);
        if (coin != null && coin.getCount() >= Config.AUGMENT_PRICE) {
            tb.append("&nbsp;&nbsp; <font color=33CC00>Стоимость: " + Config.AUGMENT_PRICE + " " + Config.AUGMENT_COIN_NAME + ".</font><br>");
            tb.append("&nbsp;&nbsp;&nbsp;&nbsp;<button value=\"Продолжить\" action=\"bypass _bbstransaug_finish\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
            player.setTrans2Item(objectId);
        } else {
            tb.append("&nbsp;&nbsp; <font color=FF6666>Стоимость: " + Config.AUGMENT_PRICE + " Ble Eva</font><br>");
            tb.append("&nbsp;&nbsp;&nbsp;&nbsp;<font color=999999>[Продолжить]</font>");
        }

        tb.append("</body></html>");
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private void transFinish(L2PcInstance player) {
        TextBuilder tb = new TextBuilder("");
        tb.append(getPwHtm("menu"));
        L2ItemInstance item1 = player.getInventory().getItemByObjectId(player.getTrans1Item());
        L2ItemInstance item2 = player.getInventory().getItemByObjectId(player.getTrans2Item());
        if (item1 == null || item2 == null) {
            tb.append("</body></html>");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (item2.getItem().getItemGrade() < L2Item.CRYSTAL_C || item2.getItem().getType2() != L2Item.TYPE2_WEAPON || !item2.isDestroyable() || item2.isShadowItem()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (!item1.isAugmented()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (item2.isAugmented()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        L2ItemInstance coin = player.getInventory().getItemByItemId(Config.AUGMENT_COIN);
        if (coin == null || coin.getCount() < Config.AUGMENT_PRICE) {
            tb.append("&nbsp;&nbsp; Стоимость переноса " + Config.AUGMENT_PRICE + " " + Config.AUGMENT_COIN_NAME + ".");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (!player.destroyItemByItemId("bbsl24transaug", Config.AUGMENT_COIN, Config.AUGMENT_PRICE, player, true)) {
            tb.append("&nbsp;&nbsp; Стоимость переноса " + Config.AUGMENT_PRICE + " " + Config.AUGMENT_COIN_NAME + ".");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        L2Skill augment = item1.getAugmentation().getAugmentSkill();
        if (augment == null) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        if (player.getTransAugment() != augment.getId()) {
            tb.append("&nbsp;&nbsp; Пушка не найдена.");
            separateAndSend(tb.toString(), player);
            tb.clear();
            tb = null;
            return;
        }

        int augId = augment.getId();
        int augLevel = augment.getLevel();
        int augEffId = item1.getAugmentation().getAugmentationId();

        String augName = augment.getName();
        String type = "";
        if (augment.isActive()) {
            type = "(Активный)";
        } else if (augment.isPassive()) {
            type = "(Пассивный)";
        } else {
            type = "(Шансовый)";
        }

        item1.getAugmentation().removeBoni(player);
        item1.removeAugmentation();

        item2.setAugmentation(new L2Augmentation(item2, augEffId, augId, augLevel, true));

        tb.append("<br>&nbsp;&nbsp; Аугмент: <font color=bef574>" + augName + "" + type + "</font> <font color=33CC00>...перенесен!");

        player.sendItems(false);
        player.broadcastUserInfo();

        tb.append("</body></html>");
        separateAndSend(tb.toString(), player);
        Log.add(TimeLogger.getTime() + "player: " + player.getName() + "; augment: " + augName + " (id: " + augId + "; level: " + augLevel + "; effect: " + augEffId + "); weapon1: " + player.getTrans1Item() + "; weapon2: " + player.getTrans2Item() + ";", "augment_trans");

        player.setTrans1Item(0);
        player.setTrans2Item(0);
        player.setTransAugment(0);
        tb.clear();
        tb = null;
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }
}
