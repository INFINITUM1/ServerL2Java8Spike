package scripts.communitybbs.Manager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Log;

public class CustomBBSManager extends BaseBBSManager {

    private static CustomBBSManager _instance;

    public static void init() {
        _instance = new CustomBBSManager();
        _instance.cacheColours();
    }

    public static CustomBBSManager getInstance() {
        return _instance;
    }
    private static FastTable<String[]> _colours = new FastTable<String[]>();
    private static FastTable<String[]> _ncolours = new FastTable<String[]>();
    private static final FastMap<Integer, Integer> PREMIUM_DAY_PRICES = Config.PREMIUM_DAY_PRICES;

    @Override
    public void parsecmd(String command, L2PcInstance player) {
        //System.out.println("CustomBBSManager###->" + command + "<-#");
        if (player.getChannel() > 1) {
            return;
        }

        TextBuilder tb = new TextBuilder("");
        tb.append(getPwHtm("menu"));
        if (command.equalsIgnoreCase("_pwhome")) {
            String content = getPwHtm("menu");
            if (content == null) {
                content = "<html><body><br><br><center>404 :File Not found: '" + PWHTML + "menu.htm' </center></body></html>";
            }
            separateAndSend(content + "</body></html>", player);
            return;
        } else if (command.startsWith("_bbspwhtm")) {
            String htm = command.substring(10).trim();
            String content = getPwHtm(htm);
            if (content == null) {
                content = "<html><body><br><br><center>Страница: " + htm + ".htm не найдена.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsmultisell")) {
            if (!Config.BBS_CURSED_SHOP && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться магазином.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            String[] tmp = command.substring(14).split(" ");
            L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(tmp[1]), player, false, 0);

            String content = getPwHtm(tmp[0]);
            if (content == null) {
                content = "<html><body><br><br><center>Страница: " + tmp[0] + ".htm не найдена.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsbuff")) {
            if (!Config.BBS_CURSED_BUFF && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться баффом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            String[] tmp = command.substring(9).split(" ");
            try {
                int buff_id = Integer.parseInt(tmp[1]);

                if (tmp.length == 6 && !player.isPremium()) {
                    int coin_id = Integer.parseInt(tmp[3]);
                    int coin_cnt = Integer.parseInt(tmp[4]);
                    if (player.getItemCount(coin_id) < coin_cnt) {
                        tb.append("<br><br>&nbsp;&nbsp;Стоимость баффа: " + coin_cnt + " " + tmp[5] + "</body></html>");
                        separateAndSend(tb.toString(), player);
                        tb.clear();
                        tb = null;
                        return;
                    }
                    player.destroyItemByItemId("Buffer", coin_id, coin_cnt, player, true);
                }

                if (CustomServerData.getInstance().isWhiteBuff(buff_id)) {
                    player.stopSkillEffects(buff_id);
                    SkillTable.getInstance().getInfo(buff_id, Integer.parseInt(tmp[2])).getEffects(player, player);
                }
            } catch (Exception ignored) {
                //
            }

            String content = getPwHtm(tmp[0]);
            if (content == null) {
                content = "<html><body><br><br><center>Страница: " + tmp[0] + ".htm не найдена.</center></body></html>";
            }

            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsprembuff")) {
            if (!Config.BBS_CURSED_BUFF && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться баффом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            String[] tmp = command.substring(9).split(" ");
            try {
                int buff_id = Integer.parseInt(tmp[1]);

                if (player.isPremium()) {
                    tb.append("<br><br>&nbsp;&nbsp;Доступно только для премиумов!</body></html>");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }

                if (CustomServerData.getInstance().isWhiteBuff(buff_id)) {
                    player.stopSkillEffects(buff_id);
                    SkillTable.getInstance().getInfo(buff_id, Integer.parseInt(tmp[2])).getEffects(player, player);
                }
            } catch (Exception ignored) {
                //
            }

            String content = getPwHtm(tmp[0]);
            if (content == null) {
                content = "<html><body><br><br><center>Страница: " + tmp[0] + ".htm не найдена.</center></body></html>";
            }

            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsteleto")) {
            if (!Config.BBS_CURSED_TELEPORT && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться телепортом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            String[] tmp = command.substring(11).trim().split("_");
            int type = Integer.parseInt(tmp[0]);
            int x = Integer.parseInt(tmp[1]);
            int y = Integer.parseInt(tmp[2]);
            int z = Integer.parseInt(tmp[3]);

            tb.append("<br><br>&nbsp;&nbsp;Счастливого пути!</body></html>");
            player.teleToLocation(x, y, z, false);
        } else if (command.startsWith("_bbsbDop")) {
            if (!Config.BBS_CURSED_BUFF && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться баффом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            switch (Integer.parseInt(command.substring(8).trim())) {
                case 1:
                    player.stopAllEffectsB();
                    break;
                case 2:
                    player.fullRestore();
                    break;
                case 3:
                    player.doRebuff();
                    break;
                case 4:
                    player.doFullBuff(1);
                    break;
                case 5:
                    player.doFullBuff(2);
                    break;
            }
            String content = getPwHtm("40001");
            if (content == null) {
                content = "<html><body><br><br><center>Страница: 40001.htm не найдена.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsprofileBuff")) {
            if (!Config.BBS_CURSED_BUFF && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться баффом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            player.doBuffProfile(Integer.parseInt(command.substring(15).trim()));
            String content = getPwHtm("40001");
            if (content == null) {
                content = "<html><body><br><br><center>Страница: 40001.htm не найдена.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbssprofileBuff")) {
            if (!Config.BBS_CURSED_BUFF && player.isCursedWeaponEquiped()) {
                tb.append("<br><br>&nbsp;&nbsp;Персонажи с проклятым оружием не могут пользоваться баффом.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            player.saveBuffProfile(Integer.parseInt(command.substring(16).trim()));
            String content = getPwHtm("40001-4");
            if (content == null) {
                content = "<html><body><br><br><center>Страница: 40001-4.htm не найдена.</center></body></html>";
            }
            tb.append(content + "</body></html>");
        } else if (command.startsWith("_bbsaugment")) {
            switch (Integer.parseInt(command.substring(11).trim())) {
                case 1:
                    player.sendPacket(Static.SELECT_THE_ITEM_TO_BE_AUGMENTED);
                    player.sendPacket(Static.ExShowVariationMakeWindow);
                    player.setAugFlag(true);
                    break;
                case 2:
                    player.sendPacket(Static.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
                    player.sendPacket(Static.ExShowVariationCancelWindow);
                    break;
            }
            return;
        } else if (command.startsWith("_bbswarehouse")) {
            switch (Integer.parseInt(command.substring(13).trim())) {
                case 1:
                    player.setActiveWarehouse(player.getWarehouse());
                    player.tempInvetoryDisable();
                    player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
                    break;
                case 2:
                    player.setActiveWarehouse(player.getWarehouse());
                    player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
                    break;
            }
            return;
        } else if (command.equalsIgnoreCase("_bbssell")) {
            player.sendPacket(new SellList(player));
            return;
        } else if (command.equalsIgnoreCase("_bbspwhero")) {
            tb.append("&nbsp;");
            if (player.isHero()) {
                tb.append("<br><br>&nbsp;&nbsp;Вы уже герой.</body></html>");
                separateAndSend(tb.toString(), player);
                return;
            }
            tb.append(getPwHtm("hero"));
        } else if (command.startsWith("_bbspremium")) {
            if (!Config.PREMIUM_ENABLE) {
                tb.append("<br><br>&nbsp;&nbsp;Премиум отключен.</body></html>");
                separateAndSend(tb.toString(), player);
                return;
            }
            int days = Integer.parseInt(command.substring(11).trim());
            if (days <= 0) {
                return;
            }

            Integer price = PREMIUM_DAY_PRICES.get(days);
            if (price == null) {
                price = Config.PREMIUM_PRICE * days;
            }

            L2ItemInstance coins = player.getInventory().getItemByItemId(Config.PREMIUM_COIN);
            if (coins == null || coins.getCount() < price) {
                tb.append("<br><br>&nbsp;&nbsp;Стоимость премиум " + price + " " + Config.PREMIUM_COINNAME + "</body></html>");
                separateAndSend(tb.toString(), player);
                return;
            }
            player.destroyItemByItemId("Donate Shop", Config.PREMIUM_COIN, price, player, true);

            player.storePremium(days);
            Log.addDonate(player, "Premium, " + days + " days.", Config.PREMIUM_PRICE);

            tb.append("<br><br>&nbsp;&nbsp;Статус Премиум: куплен на " + days + " дней.</body></html>");
            separateAndSend(tb.toString(), player);
            return;
        } else if (command.equalsIgnoreCase("_bbspwcustomskills")) {
            String change = "";
            tb.append("&nbsp;&nbsp;Стоимость любого скилла: " + Config.PWCSKILLS_PRICE + " " + Config.PWCSKILLS_COINNAME + ".<br>");
            if (Config.PWCNGSKILLS_COIN > 0) {
                tb.append("&nbsp;&nbsp;Стоимость замены скилла: " + Config.PWCNGSKILLS_PRICE + " " + Config.PWCNGSKILLS_COINNAME + ".<br>");
            }
            tb.append("<table width=280><tr><td>Доступные скиллы: <br></td></tr>");

            for (FastMap.Entry<Integer, Integer> e = Config.PWCSKILLS.head(), end = Config.PWCSKILLS.tail(); (e = e.getNext()) != end;) {
                Integer id = e.getKey(); // No typecast necessary.
                Integer lvl = e.getValue(); // No typecast necessary.
                if (id == null || lvl == null) {
                    continue;
                }

                L2Skill skill = SkillTable.getInstance().getInfo(id, 1);
                if (skill == null) {
                    continue;
                }

                if (player.getKnownSkill(id) != null) {
                    continue;
                }

                if (player.haveCustomSkills() && Config.PWCNGSKILLS_COIN > 0) {
                    change = "<td><button value=\"Заменить\" action=\"bypass _bbspwcngskills " + id + "\" width=\"100\" height=\"14\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>";
                }

                tb.append("<tr><td><a action=\"bypass _bbspwcustomskills " + id + "\"><font color=bef574>" + skill.getName() + " (" + lvl + " уровень)</font></a><br></td>" + change + "</tr>");
            }
            tb.append("</table><br></body></html>");
        } else if (command.equalsIgnoreCase("_bbspwenchantskills")) {
            if (player.getLevel() < 76) {
                tb.append("<br><br>Только для персонажей выше 76 уровня.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            if (player.getClassId().getId() < 88) {
                tb.append("<br><br>Только для персонажей с 3-й профессией.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            tb.append("&nbsp;&nbsp;Стоимость: " + Config.PWENCHSKILL_PRICE + " " + Config.PWENCHSKILL_COINNAME + " за +2 к скиллу.<br>");
            tb.append("<table width=280><tr><td>Выберите скилл: <br></td></tr>");

            int count = 0;
            L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
            for (L2EnchantSkillLearn s : skills) {
                L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
                if (sk == null) {
                    continue;
                }
                if (player.getSkillLevel(s.getId()) >= s.getLevel()) {
                    continue;
                }
                if (player.getKnownSkill(s.getId()) == null) {
                    continue;
                }
                count++;
                tb.append("<tr><td><a action=\"bypass _bbspwenchantskills " + s.getId() + "_" + s.getLevel() + "\"><font color=bef574>" + sk.getName() + " (" + s.getType() + ")</font></a><br></td></tr>");
            }
            if (count == 0) {
                tb.append("</table><br><br>&nbsp;&nbsp;Нет доступных скиллов.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            tb.append("</table><br></body></html>");
        } else if (command.startsWith("_bbspwenchantskills")) {
            if (player.getLevel() < 76) {
                tb.append("<br><br>&nbsp;&nbsp;Только для персонажей выше 76 уровня.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            if (player.getClassId().getId() < 88) {
                tb.append("<br><br>&nbsp;&nbsp;Только для персонажей с 3-й профессией.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            String[] tmp = command.substring(20).trim().split("_");
            int _skillId = Integer.parseInt(tmp[0]);
            int _skillLvl = (Integer.parseInt(tmp[1]) + 1);
            int plus = 1;

            L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
            if (skill == null) {
                plus = 0;
                skill = SkillTable.getInstance().getInfo(_skillId, (_skillLvl - 1));
                if (skill == null) {
                    tb.append("<br><br>&nbsp;&nbsp;Скилл не найден.</body></html>");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }
            }

            if (player.getKnownSkill(_skillId) == null) {
                tb.append("<br><br>&nbsp;&nbsp;Скилл не найден.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            if (player.getSkillLevel(_skillId) >= _skillLvl) {
                tb.append("<br><br>&nbsp;&nbsp;Скилл уже заточен.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            int count = 0;
            L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
            for (L2EnchantSkillLearn s : skills) {
                L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), (s.getLevel() + plus));
                if (sk == null || sk != skill || !sk.getCanLearn(player.getClassId())) {
                    continue;
                }
                count++;
            }

            if (count == 0 && !Config.ALT_GAME_SKILL_LEARN) {
                player.sendMessage("You are trying to learn skill that u can't..");
                Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
                tb.clear();
                tb = null;
                return;
            }
            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.PWENCHSKILL_COIN);
            if (coin == null || coin.getCount() < Config.PWENCHSKILL_PRICE) {
                tb.append("<br><br>&nbsp;&nbsp;Заточка скиллов: У вас не хватает " + Config.PWENCHSKILL_COINNAME + ".</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            player.destroyItemByItemId("pwhero", Config.PWENCHSKILL_COIN, Config.PWENCHSKILL_PRICE, player, true);

            player.addSkill(skill, true);
            player.sendSkillList();

            FastTable<L2ShortCut> allShortCuts = player.getAllShortCuts();
            for (L2ShortCut sc : allShortCuts) {
                if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL) {
                    L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _skillLvl, 1);
                    player.sendPacket(new ShortCutRegister(newsc));
                    player.registerShortCut(newsc);
                }
            }

            tb.append("<br><br>&nbsp;&nbsp;Заточен скилл: <font color=bef574>" + skill.getName() + "</font>.</body></html>");
            Log.addDonate(player, "enchant skill: " + skill.getName(), Config.PWENCHSKILL_PRICE);
        } else if (command.startsWith("_bbspwhero")) {
            int days = 0;
            try {
                days = Integer.parseInt(command.substring(11).trim());
            } catch (Exception e) {
                // бывает xD игрок вводили 5.12.10, 05.12.10, 24.00, 24 hours, 1 col, 1 COL, 1 day, 1col for 24 hour, 1 Coin of luck, 1day или свой ник
                days = -1;
            }
            if (days <= 0) {
                return;
            }
            if (days < Config.PWHERO_MINDAYS) {
                tb.append("<br><br>&nbsp;&nbsp;Геройство минимум от " + Config.PWHERO_MINDAYS + " дней!</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            int total = Config.PWHERO_FPRICE;
            int expire = -1;

            if (days < 999) {
                total = days * Config.PWHERO_PRICE;
                expire = days;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.PWHERO_COIN);
            if (coin == null || coin.getCount() < total) {
                tb.append("<br><br>&nbsp;&nbsp;Проверьте стоимость.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            player.destroyItemByItemId("pwhero", Config.PWHERO_COIN, total, player, true);
            player.setHero(true);
            player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
            player.broadcastUserInfo();
            player.setHero(expire);
            tb.append("<br><br>&nbsp;&nbsp;Поздравляем! Вы теперь герой.</body></html>");
            Log.addDonate(player, "status hero; days: " + days, total);
        } else if (command.startsWith("_bbspwcustomskills")) {
            int skillId = Integer.parseInt(command.substring(19).trim());
            int total = 999;
            if (Config.PWCSKILLS.isEmpty()) {
                tb.append("<br><br>&nbsp;&nbsp;Не активно.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            Integer level = Config.PWCSKILLS.get(skillId);
            if (level == null) {
                tb.append("<br><br>&nbsp;&nbsp;Скилл не найден.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            L2Skill skill = SkillTable.getInstance().getInfo(skillId, level);
            if (skill == null) {
                tb.append("<br><br>&nbsp;&nbsp;Скилл не найден.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.PWCSKILLS_COIN);
            if (coin == null || coin.getCount() < Config.PWCSKILLS_PRICE) {
                tb.append("<br><br>&nbsp;&nbsp;Проверьте стоимость.</body></html>");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            player.destroyItemByItemId("pwcustomskills", Config.PWCSKILLS_COIN, Config.PWCSKILLS_PRICE, player, true);
            player.addSkill(skill, false);
            player.sendSkillList();
            player.addDonateSkill(0, skillId, level, -1);

            tb.append("<br><br>&nbsp;&nbsp;Куплен скилл: <font color=bef574>" + skill.getName() + " (" + level + " уровень)</font>.</body></html>");
            Log.addDonate(player, "skill: " + skill.getName(), Config.PWCSKILLS_PRICE);
        } else if (command.equalsIgnoreCase("_bbspwtittlecolor")) {
            tb.append("&nbsp;&nbsp;Стоимость смены цвета титула: ");
            if (Config.PWTCOLOR_PAYMENT && player.getAppearance().getTitleColor() != 16777079) {
                tb.append("для вас бесплатно!<br>");
            } else {
                tb.append(Config.PWTCOLOR_PRICE + " " + Config.PWTCOLOR_COINNAME + ".<br>");
            }

            tb.append("<table width=600><tr><td>Выберите цвет: <br></td></tr>");
            String title = player.getTitle();
            if (title == null || title.equals("") || title.equals(" ")) {
                title = "######";
            }

            int step = 0;
            for (int i = 0, n = 5; i < n; i++) {
                String[] paint = _colours.get(i);
                if (paint == null) {
                    continue;
                }

                tb.append("<tr>");
                for (int k = 0; k <= 5; k++) {
                    tb.append("<td><a action=\"bypass _bbspwtittlecolr " + paint[k] + "\"><font color=" + paint[k] + "># " + title + " #</font></td>");
                }
                tb.append("</tr>");
                step++;
                if (step == 5) {
                    tb.append("<tr>");
                    tb.append("<td><br><br><a action=\"bypass _bbspwtittlecolor 5_10\"><font color=" + _colours.get(5)[0] + ">#Вперед</font></a></td>");
                    tb.append("<td width=50></td>");
                    for (int k = 0; k <= 5; k++) {
                        tb.append("<td>&nbsp;</td>");
                    }
                    tb.append("</tr>");
                    break;
                }
            }
            tb.append("</table><br><br>&nbsp;&nbsp;Свой цвет, введите оттенок:<br1> <edit var=\"color\" width=100 length=\"6\"><br>");
            tb.append("&nbsp;&nbsp;<a action=\"bypass _bbspwtittlecolr $color\">Далее</a><br>");
            tb.append("</body></html>");
        } else if (command.startsWith("_bbspwtittlecolor")) {
            String[] tmp = command.substring(18).trim().split("_");
            int start = Integer.parseInt(tmp[0]);
            int stop = Integer.parseInt(tmp[1]);
            tb.append("&nbsp;&nbsp;Стоимость смены цвета титула: ");
            if (Config.PWTCOLOR_PAYMENT && player.getAppearance().getTitleColor() != 16777079) {
                tb.append("для вас бесплатно!<br>");
            } else {
                tb.append(Config.PWTCOLOR_PRICE + " " + Config.PWTCOLOR_COINNAME + ".<br>");
            }

            tb.append("<table width=600><tr><td>Выберите цвет: <br></td></tr>");
            String title = player.getTitle();
            if (title == null || title.equals("") || title.equals(" ")) {
                title = "######";
            }
            int step = 0;
            int finish = stop + 1;
            for (int i = start, n = finish; i < n; i++) {
                String[] paint = _colours.get(i);
                if (paint == null) {
                    continue;
                }

                if (step == 0) {
                    tb.append("<tr>");
                    if (start == 0) {
                        tb.append("<td></td>");
                    } else {
                        tb.append("<td><a action=\"bypass _bbspwtittlecolor " + (start - 5) + "_" + (stop - 5) + "\"><font color=" + _colours.get((start - 1))[0] + ">#Назад</font></a><br><br></td>");
                    }
                    tb.append("<td width=50></td>");
                    for (int k = 0; k <= 5; k++) {
                        tb.append("<td>&nbsp;</td>");
                    }
                    tb.append("</tr>");
                    step++;
                    continue;
                }

                tb.append("<tr>");
                for (int k = 0; k <= 5; k++) {
                    tb.append("<td><a action=\"bypass _bbspwtittlecolr " + paint[k] + "\"><font color=" + paint[k] + "># " + title + " #</font></td>");
                }
                tb.append("</tr>");
                step++;
                if (step == 5) {
                    if (finish < _colours.size()) {
                        tb.append("<tr>");
                        tb.append("<td><br><br><a action=\"bypass _bbspwtittlecolor " + (start + 5) + "_" + (stop + 5) + "\"><font color=" + _colours.get(finish)[0] + ">#Вперед</font></a></td>");
                        tb.append("<td width=50></td>");
                        for (int k = 0; k <= 5; k++) {
                            tb.append("<td>&nbsp;</td>");
                        }
                        tb.append("</tr>");
                    } else {
                        tb.append("<tr>");
                        for (int k = 0; k <= 5; k++) {
                            tb.append("<td><a action=\"bypass _bbspwtittlecolr " + _colours.getLast()[k] + "\"><font color=" + _colours.getLast()[k] + "># " + title + " #</font></a></td>");
                        }
                        tb.append("</tr>");
                    }
                    break;
                }
                /*if (step == 5)
                {
                tb.append("<tr>");
                tb.append((finish < _colours.size() ? "<td><br><br><a action=\"bypass _bbspwtittlecolor " + (start+5) + "_" + (stop+5) + "\"><font color=" + _colours.get(finish)[0] + ">#Вперед</font></a></td>" : ""));
                //tb.append((finish < _colours.size() ? "<a action=\"bypass _bbsl24tittlecolor " + (start+5) + "_" + (stop+5) + "\"><font color=" + _colours.get(finish)[0] + ">######</font></a>" : ""));
                tb.append("<td width=50></td>");
                for (int k = 0; k <= 5; k++)
                tb.append("<td>&nbsp;</td>");
                tb.append("</tr>");
                break;
                }*/
            }
            tb.append("</table><br><br>&nbsp;&nbsp;Свой цвет, введите оттенок:<br1> <edit var=\"color\" width=100 length=\"6\"><br>");
            tb.append("&nbsp;&nbsp;<a action=\"bypass _bbspwtittlecolr $color\">Далее</a><br>");
            tb.append("</body></html>");
            separateAndSend(tb.toString(), player);
        } else if (command.startsWith("_bbspwtittlecolr")) {
            int color = 0;
            String tmp = "";
            try {
                tmp = command.substring(17).trim();
                color = Integer.decode("0x" + convertColor(tmp));
            } catch (Exception e) {
                color = -1;
            }

            if (color == -1) {
                tb.append("Неправильно введен оттенок; yandex.ru - html цвета");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            int coinId = 0;
            int price = 0;

            if (!Config.PWTCOLOR_PAYMENT || player.getAppearance().getTitleColor() == 16777079) {
                coinId = Config.PWTCOLOR_COIN;
                price = Config.PWTCOLOR_PRICE;
            }

            if (coinId > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);
                if (coin == null || coin.getCount() < price) {
                    tb.append("Проверьте стоимость.");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }
                player.destroyItemByItemId("_bbspwcngcolor", coinId, price, player, true);
            }
            player.getAppearance().setTitleColor(color);
            player.broadcastUserInfo();
            player.store();
            tb.append("<br><br>&nbsp;<font color=" + tmp + ">Надеемся, новый цвет вам понравится!<font><br><br>&nbsp;&nbsp;<a action=\"bypass _bbspwtittlecolor\">Хочу другой!</a></body></html>");
        } else if (command.equalsIgnoreCase("_bbspwnamecolor")) {
            tb.append("&nbsp;&nbsp;Стоимость смены цвета ника: ");
            if (Config.PWTCOLOR_PAYMENT && player.getAppearance().getTitleColor() != 16777079) {
                tb.append("для вас бесплатно!<br>");
            } else {
                tb.append(Config.PWNCOLOR_PRICE + " " + Config.PWNCOLOR_COINNAME + ".<br>");
            }

            tb.append("<table width=600><tr><td>Выберите цвет: <br></td></tr>");
            String title = player.getName();
            if (title == null || title.equals("") || title.equals(" ")) {
                title = "######";
            }

            int step = 0;
            for (int i = 0, n = 5; i < n; i++) {
                String[] paint = _ncolours.get(i);
                if (paint == null) {
                    continue;
                }

                tb.append("<tr>");
                for (int k = 0; k <= 5; k++) {
                    tb.append("<td><a action=\"bypass _bbspwnamecolr " + paint[k] + "\"><font color=" + paint[k] + "># " + title + " #</font></td>");
                }
                tb.append("</tr>");
                step++;
                if (step == 5) {
                    tb.append("<tr>");
                    tb.append("<td><br><br><a action=\"bypass _bbspwnamecolor 5_10\"><font color=" + _ncolours.get(5)[0] + ">#Вперед</font></a></td>");
                    tb.append("<td width=50></td>");
                    for (int k = 0; k <= 5; k++) {
                        tb.append("<td>&nbsp;</td>");
                    }
                    tb.append("</tr>");
                    break;
                }
            }
            tb.append("</table><br></body></html>");
        } else if (command.startsWith("_bbspwnamecolor")) {
            String[] tmp = command.substring(16).trim().split("_");
            int start = Integer.parseInt(tmp[0]);
            int stop = Integer.parseInt(tmp[1]);
            tb.append("&nbsp;&nbsp;Стоимость смены цвета нику: ");
            if (Config.PWTCOLOR_PAYMENT && player.getAppearance().getNameColor() != 16777215) {
                tb.append("для вас бесплатно!<br>");
            } else {
                tb.append(Config.PWNCOLOR_PRICE + " " + Config.PWNCOLOR_COINNAME + ".<br>");
            }

            tb.append("<table width=600><tr><td>Выберите цвет: <br></td></tr>");
            String title = player.getName();
            if (title == null || title.equals("") || title.equals(" ")) {
                title = "######";
            }
            int step = 0;
            int finish = stop + 1;
            for (int i = start, n = finish; i < n; i++) {
                String[] paint = _ncolours.get(i);
                if (paint == null) {
                    continue;
                }

                if (step == 0) {
                    tb.append("<tr>");
                    if (start == 0) {
                        tb.append("<td></td>");
                    } else {
                        tb.append("<td><a action=\"bypass _bbspwnamecolor " + (start - 5) + "_" + (stop - 5) + "\"><font color=" + _ncolours.get((start - 1))[0] + ">#Назад</font></a><br><br></td>");
                    }
                    tb.append("<td width=50></td>");
                    for (int k = 0; k <= 5; k++) {
                        tb.append("<td>&nbsp;</td>");
                    }
                    tb.append("</tr>");
                    step++;
                    continue;
                }

                tb.append("<tr>");
                for (int k = 0; k <= 5; k++) {
                    tb.append("<td><a action=\"bypass _bbspwnamecolr " + paint[k] + "\"><font color=" + paint[k] + "># " + title + " #</font></td>");
                }
                tb.append("</tr>");
                step++;
                if (step == 5) {
                    if (finish < _ncolours.size()) {
                        tb.append("<tr>");
                        tb.append("<td><br><br><a action=\"bypass _bbspwnamecolor " + (start + 5) + "_" + (stop + 5) + "\"><font color=" + _ncolours.get(finish)[0] + ">#Вперед</font></a></td>");
                        tb.append("<td width=50></td>");
                        for (int k = 0; k <= 5; k++) {
                            tb.append("<td>&nbsp;</td>");
                        }
                        tb.append("</tr>");
                    }
                    /*else
                    {
                    tb.append("<tr>");
                    for (int k = 0; k <= 5; k++)
                    tb.append("<td><a action=\"bypass _bbspwnamecolr " + _ncolours.getLast()[k] + "\"><font color=" + _ncolours.getLast()[k] + "># " + title + " #</font></td>");
                    tb.append("</tr>");
                    }*/
                    break;
                }
                /*if (step == 5)
                {
                tb.append("<tr>");
                tb.append((finish < _colours.size() ? "<td><br><br><a action=\"bypass _bbspwnamecolor " + (start+5) + "_" + (stop+5) + "\"><font color=" + _colours.get(finish)[0] + ">#Вперед</font></a></td>" : ""));
                //tb.append((finish < _colours.size() ? "<a action=\"bypass _bbsl24tittlecolor " + (start+5) + "_" + (stop+5) + "\"><font color=" + _colours.get(finish)[0] + ">######</font></a>" : ""));
                tb.append("<td width=50></td>");
                for (int k = 0; k <= 5; k++)
                tb.append("<td>&nbsp;</td>");
                tb.append("</tr>");
                break;
                }*/
            }
            tb.append("</table><br></body></html>");
        } else if (command.startsWith("_bbspwnamecolr")) {
            String tmp = command.substring(15).trim();
            int coinId = 0;
            int price = 0;

            if (!Config.PWTCOLOR_PAYMENT || player.getAppearance().getNameColor() == 16777215) {
                coinId = Config.PWNCOLOR_COIN;
                price = Config.PWNCOLOR_PRICE;
            }

            if (coinId > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(coinId);
                if (coin == null || coin.getCount() < price) {
                    tb.append("Проверьте стоимость.");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }
                player.destroyItemByItemId("_bbspwcngcolor", coinId, price, player, true);
            }
            player.getAppearance().setNameColor(Integer.decode("0x" + convertColor(tmp)));
            player.broadcastUserInfo();
            player.store();
            tb.append("<br><br>&nbsp;<font color=" + tmp + ">Надеемся, новый цвет вам понравится!<font><br><br>&nbsp;&nbsp;<a action=\"bypass _bbspwnamecolor\">Хочу другой!</a></body></html>");
        } else if (command.startsWith("_bbstransaug")) {
            tb.clear();
            tb = null;
            AugmentBBSManager.getInstance().parsecmd(command, player);
            return;
        } else if (command.startsWith("_bbschangenick")) {
            String name = command.substring(15).trim();
            if (name.isEmpty()) {
                tb.append("Вы не ввели желаемый ник.");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            if (!Util.isValidName(player, name)) {
                tb.append("Попробуйте другой ник.");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            if (Config.BBS_CNAME_COIN > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(Config.BBS_CNAME_COIN);
                if (coin == null || coin.getCount() < Config.BBS_CNAME_PRICE) {
                    tb.append("Проверьте стоимость.<br> " + Config.BBS_CNAME_PRICE + " " + Config.BBS_CNAME_VAL + ".");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }
            }

            if (Util.isExistsName(name)) {
                tb.append("Данный ник уже занят.");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            if (Config.BBS_CNAME_COIN > 0) {
                player.destroyItemByItemId("_bbspwcngname", Config.BBS_CNAME_COIN, Config.BBS_CNAME_PRICE, player, true);
            }
            player.changeName(name);
            Log.addDonate(player, "Change name.", Config.BBS_CNAME_PRICE);
            tb.append("Поздравляем, теперь ваш новый ник - " + name + ".<br>Мы не кому не расскажем.");
        } else if (command.startsWith("_bbsnobless")) {
            if (!Config.NOBLES_ENABLE) {
                tb.append("Сервис выдачи ноблесса отключен.");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }
            if (player.isNoble()) {
                tb.append("Вы уже ноблесс.");
                separateAndSend(tb.toString(), player);
                tb.clear();
                tb = null;
                return;
            }

            if (Config.SNOBLE_COIN > 0) {
                L2ItemInstance coins = player.getInventory().getItemByItemId(Config.SNOBLE_COIN);
                if (coins == null || coins.getCount() < Config.SNOBLE_PRICE) {
                    tb.append("Стоимость ноблесса " + Config.SNOBLE_PRICE + " " + Config.SNOBLE_COIN_NAME + ".");
                    separateAndSend(tb.toString(), player);
                    tb.clear();
                    tb = null;
                    return;
                }

                player.destroyItemByItemId("Donate Shop", Config.SNOBLE_COIN, Config.SNOBLE_PRICE, player, true);
            }

            player.setNoble(true);
            player.addItem("rewardNoble", 7694, 1, player, true);
            player.sendUserPacket(new PlaySound("ItemSound.quest_finish"));

            if (!Config.ACADEMY_CLASSIC) {
                player.rewardAcademy(0);
            }
            Log.addDonate(player, "Noblesse.", Config.SNOBLE_PRICE);
            tb.append("Поздравляем, теперь вы ноблесс.");
        }
        separateAndSend(tb.toString(), player);
        tb.clear();
        tb = null;
    }

    private String convertColor(String color) {
        //TextBuilder tb = new TextBuilder(color);
        return new TextBuilder(color).reverse().toString();
    }

    private void cacheColours() {
        _colours.add(new String[]{"FFFFCC", "FFFF99", "FFFF66", "FFFF33", "FFFF00", "CCCC00"});
        _colours.add(new String[]{"FFCC66", "FFCC00", "FFCC33", "CC9900", "CC9933", "996600"});
        _colours.add(new String[]{"FF9900", "FF9933", "CC9966", "CC6600", "996633", "663300"});
        _colours.add(new String[]{"FFCC99", "FF9966", "FF6600", "CC6633", "993300", "660000"});
        _colours.add(new String[]{"FF6633", "CC3300", "FF3300", "FF0000", "CC0000", "990000"});
        _colours.add(new String[]{"FFCCCC", "FF9999", "FF6666", "FF3333", "FF0033", "CC0033"});
        _colours.add(new String[]{"CC9999", "CC6666", "CC3333", "993333", "990033", "330000"});
        _colours.add(new String[]{"FF6699", "FF3366", "FF0066", "CC3366", "996666", "663333"});
        _colours.add(new String[]{"FF99CC", "FF3399", "FF0099", "CC0066", "993366", "660033"});
        _colours.add(new String[]{"FF66CC", "FF00CC", "FF33CC", "CC6699", "CC0099", "990066"});
        _colours.add(new String[]{"FFCCFF", "FF99FF", "FF66FF", "FF33FF", "FF00FF", "CC3399"});
        _colours.add(new String[]{"CC99CC", "CC66CC", "CC00CC", "CC33CC", "990099", "993399"});
        _colours.add(new String[]{"CC66FF", "CC33FF", "CC00FF", "9900CC", "996699", "660066"});
        _colours.add(new String[]{"CC99FF", "9933CC", "9933FF", "9900FF", "660099", "663366"});
        _colours.add(new String[]{"9966CC", "9966FF", "6600CC", "6633CC", "663399", "330033"});
        _colours.add(new String[]{"CCCCFF", "9999FF", "6633FF", "6600FF", "330099", "330066"});
        _colours.add(new String[]{"9999CC", "6666FF", "6666CC", "666699", "333399", "333366"});
        _colours.add(new String[]{"3333FF", "3300FF", "3300CC", "3333CC", "000099", "000066"});
        _colours.add(new String[]{"6699FF", "3366FF", "0000FF", "0000CC", "0033CC", "000033"});
        _colours.add(new String[]{"0066FF", "0066CC", "3366CC", "0033FF", "003399", "003366"});
        _colours.add(new String[]{"99CCFF", "3399FF", "0099FF", "6699CC", "336699", "006699"});
        _colours.add(new String[]{"66CCFF", "33CCFF", "00CCFF", "3399CC", "0099CC", "003333"});
        _colours.add(new String[]{"99CCCC", "66CCCC", "339999", "669999", "006666", "336666"});
        _colours.add(new String[]{"CCFFFF", "99FFFF", "66FFFF", "33FFFF", "00FFFF", "00CCCC"});
        _colours.add(new String[]{"99FFCC", "66FFCC", "33FFCC", "00FFCC", "33CCCC", "009999"});
        _colours.add(new String[]{"66CC99", "33CC99", "00CC99", "339966", "009966", "006633"});
        _colours.add(new String[]{"66FF99", "33FF99", "00FF99", "33CC66", "00CC66", "009933"});
        _colours.add(new String[]{"99FF99", "66FF66", "33FF66", "00FF66", "339933", "006600"});
        _colours.add(new String[]{"CCFFCC", "99CC99", "66CC66", "669966", "336633", "003300"});
        _colours.add(new String[]{"33FF33", "00FF33", "00FF00", "00CC00", "33CC33", "00CC33"});
        _colours.add(new String[]{"66FF00", "66FF33", "33FF00", "33CC00", "339900", "009900"});
        _colours.add(new String[]{"CCFF99", "99FF66", "66CC00", "66CC33", "669933", "336600"});
        _colours.add(new String[]{"99FF00", "99FF33", "99CC66", "99CC00", "99CC33", "669900"});
        _colours.add(new String[]{"CCFF66", "CCFF00", "CCFF33", "CCCC99", "666633", "333300"});
        _colours.add(new String[]{"CCCC66", "CCCC33", "999966", "999933", "999900", "666600"});
        _colours.add(new String[]{"CCCCCC", "CCCCCC", "999999", "666666", "333333", "000000"});

        _ncolours.add(new String[]{"FFFFCC", "FFFF99", "FFFF66", "FFFF33", "FFFF00", "CCCC00"});
        _ncolours.add(new String[]{"FFCC66", "FFCC00", "FFCC33", "CC9900", "CC9933", "996600"});
        _ncolours.add(new String[]{"FF9900", "FF9933", "CC9966", "CC6600", "996633", "CC9900"});
        _ncolours.add(new String[]{"FFCC66", "FFCC00", "FFCC33", "CC9900", "993300", "660000"});
        _ncolours.add(new String[]{"9966CC", "9966FF", "6600CC", "6633CC", "663399", "330033"});
        _ncolours.add(new String[]{"CCCCFF", "9999FF", "6633FF", "6600FF", "330099", "330066"});
        _ncolours.add(new String[]{"9966CC", "9966FF", "6600CC", "6633CC", "663399", "330033"});
        _ncolours.add(new String[]{"CCCCFF", "9999FF", "6633FF", "6600FF", "330099", "330066"});
        _ncolours.add(new String[]{"9999CC", "6666FF", "6666CC", "666699", "333399", "333366"});
        _ncolours.add(new String[]{"3333FF", "3300FF", "3300CC", "3333CC", "000099", "000066"});
        _ncolours.add(new String[]{"6699FF", "3366FF", "0000FF", "0000CC", "0033CC", "000033"});
        _ncolours.add(new String[]{"0066FF", "0066CC", "3366CC", "0033FF", "003399", "003366"});
        _ncolours.add(new String[]{"99CCFF", "3399FF", "0099FF", "6699CC", "336699", "006699"});
        _ncolours.add(new String[]{"66CCFF", "33CCFF", "00CCFF", "3399CC", "0099CC", "003333"});
        _ncolours.add(new String[]{"99CCCC", "66CCCC", "339999", "669999", "006666", "336666"});
        _ncolours.add(new String[]{"CCFFFF", "99FFFF", "66FFFF", "33FFFF", "00FFFF", "00CCCC"});
        _ncolours.add(new String[]{"99FFCC", "66FFCC", "33FFCC", "00FFCC", "33CCCC", "009999"});
        _ncolours.add(new String[]{"66CC99", "33CC99", "00CC99", "339966", "009966", "006633"});
        _ncolours.add(new String[]{"66FF99", "33FF99", "00FF99", "33CC66", "00CC66", "009933"});
        _ncolours.add(new String[]{"99FF99", "66FF66", "33FF66", "00FF66", "339933", "006600"});
        _ncolours.add(new String[]{"CCFFCC", "99CC99", "66CC66", "669966", "336633", "003300"});
        _ncolours.add(new String[]{"33FF33", "00FF33", "00FF00", "00CC00", "33CC33", "00CC33"});
        _ncolours.add(new String[]{"66FF00", "66FF33", "33FF00", "33CC00", "339900", "009900"});
        _ncolours.add(new String[]{"CCFF99", "99FF66", "66CC00", "66CC33", "669933", "336600"});
        _ncolours.add(new String[]{"99FF00", "99FF33", "99CC66", "99CC00", "99CC33", "669900"});
        _ncolours.add(new String[]{"CCFF66", "CCFF00", "CCFF33", "CCCC99", "666633", "333300"});
        _ncolours.add(new String[]{"99FF00", "99FF33", "99CC66", "99CC00", "99CC33", "669900"});
        _ncolours.add(new String[]{"CCFF66", "CCFF00", "CCFF33", "CCCC99", "666633", "333300"});
        _ncolours.add(new String[]{"CCCC66", "CCCC33", "999966", "999933", "999900", "666600"});
        _ncolours.add(new String[]{"CCCCCC", "CCCCCC", "999999", "666666", "333333", "000000"});
    }

    @Override
    public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance player) {
        // TODO Auto-generated method stub
    }
}
