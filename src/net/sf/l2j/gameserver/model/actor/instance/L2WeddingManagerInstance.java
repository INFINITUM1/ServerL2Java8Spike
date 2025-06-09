package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.entity.Wedding;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

//трахни себя [s]пропеллером[/s] декомпиллером; декомпил твой потолок.
public class L2WeddingManagerInstance extends L2NpcInstance {

    private boolean _active = false;
    private Wedding wed = null;

    public L2WeddingManagerInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (!Config.L2JMOD_ALLOW_WEDDING) {
            player.sendHtmlMessage("В данный момент я в отпуске.");
            return;
        }

        if (_active) {
            player.sendHtmlMessage("В данный момент я занята.<br> Интервал проведения свадеб: " + (Config.L2JMOD_WEDDING_INTERVAL / 1000) + " секунд.");
            return;
        }

        if (command.equalsIgnoreCase("engage")) {
            L2Party party = player.getParty();
            if (party == null) {
                player.sendHtmlMessage("Вы должны быть вдвоем в пати со своим партнером.");
                return;
            }

            if (player.getAppearance().getSex()) {
                party.broadcastHtmlToPartyMembers("Я буду разговаривать только с женихом.");
                return;
            }

            if (party.getMemberCount() != 2) {
                party.broadcastHtmlToPartyMembers("Только жених и невеста должны быть в пати.");
                return;
            }

            if (!party.isLeader(player)) {
                party.broadcastHtmlToPartyMembers("Жених должен быть лидером пати.");
                return;
            }

            if (player.getAppearance().getSex() == party.getPartyMembers().get(1).getAppearance().getSex()) {
                party.broadcastHtmlToPartyMembers("Однополые браки запрещены!");
                return;
            }

            if (player.isMarried() || party.getPartyMembers().get(1).isMarried()) {
                String married = party.getPartyMembers().get(1).getName();
                if (player.isMarried()) {
                    married = player.getName();
                }

                party.broadcastHtmlToPartyMembers(married + " уже в браке!!");
                return;
            }

            if (!isWearedFormal(player) || !isWearedFormal(party.getPartyMembers().get(1))) {
                party.broadcastHtmlToPartyMembers("Вы должны быть в свадебных нарядах.");
                return;
            }

            if (Config.L2JMOD_WEDDING_COIN > 0) {
                L2ItemInstance coin = player.getInventory().getItemByItemId(Config.L2JMOD_WEDDING_COIN);
                if (coin == null || coin.getCount() < Config.L2JMOD_WEDDING_PRICE) {
                    party.broadcastHtmlToPartyMembers("Жених должен оплатить: " + Config.L2JMOD_WEDDING_PRICE + " " + Config.L2JMOD_WEDDING_COINNAME + ".");
                    return;
                }

                if (!player.destroyItemByItemId("WEDDING", Config.L2JMOD_WEDDING_COIN, Config.L2JMOD_WEDDING_PRICE, player, true)) {
                    party.broadcastHtmlToPartyMembers("Жених должен оплатить: " + Config.L2JMOD_WEDDING_PRICE + " " + Config.L2JMOD_WEDDING_COINNAME + ".");
                    return;
                }
            }

            _active = true;
            wed = new Wedding(player, party.getPartyMembers().get(1), this);
            CoupleManager.getInstance().regWedding(player.getObjectId(), wed);

            //wed.broadcastHtml("Ну чтож, начнем. <br><br>(Это окно можно закрыть)");
            ThreadPoolManager.getInstance().scheduleAi(new Finish(), Config.L2JMOD_WEDDING_INTERVAL, false);
        } else if (command.equalsIgnoreCase("divorce")) {
            if (!player.isMarried() || player.getPartnerId() == 0) {
                player.sendHtmlMessage("Вы не состоите в браке.");
                return;
            }

            L2ItemInstance coin = player.getInventory().getItemByItemId(Config.L2JMOD_WEDDING_DIVORCE_COIN);
            if (coin == null || coin.getCount() < Config.L2JMOD_WEDDING_DIVORCE_PRICE) {
                player.sendHtmlMessage("Стоимость развода: " + Config.L2JMOD_WEDDING_DIVORCE_PRICE + " " + Config.L2JMOD_WEDDING_DIVORCE_COINNAME + ".");
                return;
            }

            if (!player.destroyItemByItemId("WEDDING", Config.L2JMOD_WEDDING_DIVORCE_COIN, Config.L2JMOD_WEDDING_DIVORCE_PRICE, player, true)) {
                player.sendHtmlMessage("Стоимость развода: " + Config.L2JMOD_WEDDING_DIVORCE_PRICE + " " + Config.L2JMOD_WEDDING_DIVORCE_COINNAME + ".");
                return;
            }

            CoupleManager.getInstance().deleteCouple(player.getCoupleId());
        }
    }

    private boolean isWearedFormal(L2PcInstance player) {
        if (player == null) {
            return false;
        }

        if (!Config.L2JMOD_WEDDING_FORMALWEAR) {
            return true;
        }

        return hasFormalWear(player.getInventory().getItemByItemId(6408));
    }

    private boolean hasFormalWear(L2ItemInstance formal) {
        if (formal == null) {
            return false;
        }

        return formal.isEquipped();
    }

    public void finish(boolean no) {
        if (wed != null) {
            if (no && !wed.married) {
                wed.broadcastHtml("Свадьба отменена.");
                sayString("Свадьба отменена!!!", 18);
                if (Config.L2JMOD_WEDDING_COIN > 0) {
                    L2PcInstance groom = wed.getGroom();
                    if (groom != null) {
                        giveItem(groom, Config.L2JMOD_WEDDING_COIN, Config.L2JMOD_WEDDING_PRICE);
                    }
                }
            }
            wed.clear();
        }
        wed = null;
        _active = false;
        sayString("Прошу следующую пару подойти ко мне.", 18);
    }

    private class Finish implements Runnable {

        Finish() {
        }

        public void run() {
            finish(true);
        }
    }
}
