/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.EventManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2ClassMasterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.templates.L2Henna;
import net.sf.l2j.gameserver.util.Online;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import scripts.events.custom.CustomPlayerEventList;

/**
 * Enter World Packet Handler<p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev656 cbdddd
 * <p>
 *
 * @version $Revision: 1.16.2.1.2.7 $ $Date: 2005/03/29 23:15:33 $
 */
public class EnterWorld extends L2GameClientPacket {

    private static final Logger _log = Logger.getLogger(EnterWorld.class.getName());

    //public TaskPriority getPriority() { return TaskPriority.PR_URGENT; }
    @Override
    protected void readImpl() {
        // this is just a trigger packet. it has no content
    }

    @Override
    protected void runImpl() {
        //System.out.println("#####1");
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            _log.warning("EnterWorld failed! player is null...");
            getClient().closeNow();
            return;
        }

        if (L2World.getInstance().getLagPlayer(player.getObjectId())) {
            player.kick();
            return;
        }

        if (player.isInGame()) {
            player.closeNetConnection();
            return;
        }
        player.setChannel(1);

        if (player.isGM()) {
            if (Config.GM_STARTUP_INVISIBLE) {
                player.setChannel(0);
            }

            if (Config.GM_STARTUP_INVULNERABLE) {
                player.setIsInvul(true);
            }

            if (Config.GM_STARTUP_SILENCE) {
                player.setMessageRefusal(true);
            }

            if (Config.GM_STARTUP_AUTO_LIST) {
                GmListTable.getInstance().addGm(player, false);
            } else {
                GmListTable.getInstance().addGm(player, true);
            }
        }
        //System.out.println("#####2");
        try {
            player.spawnMe(player.getX(), player.getY(), player.getZ());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("#####3");

        sendPacket(new HennaInfo(player));
        player.sendItems(false);
        sendPacket(new ShortCutInit(player));
        player.getMacroses().sendUpdate();
        sendPacket(new ClientSetTime());
        if (SevenSigns.getInstance().isSealValidationPeriod()) {
            sendPacket(new SignsSky());
        }
        sendPacket(Static.WELCOME_TO_LINEAGE);
        //System.out.println("#####4");

        SevenSigns.getInstance().sendCurrentPeriodMsg(player);

        Announcements.getInstance().showAnnouncements(player);
        if (Config.SHOW_ENTER_WARNINGS) {
            Announcements.getInstance().showWarnings(player);
        }
        //System.out.println("#####5");

        //add char to online characters
        player.setOnlineStatus(true);
        //System.out.println("#####6");

        //if (player.getHeroType() == 1)
        //   player.setHero(true);
        //System.out.println("#####7");
        checkPledgeClass(player);
        //System.out.println("#####8");

        if (player.getClanId() != 0 && player.getClan() != null) {
            notifyClanMembers(player);
            notifySponsorOrApprentice(player);
            sendPacket(new PledgeShowMemberListAll(player.getClan(), player));
            sendPacket(new PledgeStatusChanged(player.getClan()));
            player.sendPacket(new PledgeSkillList(player.getClan()));

            /*
             * if (player.getClan().getReputationScore() >= 0) // дубликат
             * notifyClanMembers -
             * clan.getClanMember(player.getName()).setPlayerInstance(player); {
             * L2Skill[] skills = player.getClan().getAllSkills(); for (L2Skill
             * sk : skills) { if(sk.getMinPledgeClass() <=
             * player.getPledgeClass()) player.addSkill(sk, false); } }
             */
            for (Siege siege : SiegeManager.getInstance().getSieges()) {
                if (!siege.getIsInProgress()) {
                    continue;
                }
                if (siege.checkIsAttacker(player.getClan())) {
                    player.setSiegeState((byte) 1);
                } else if (siege.checkIsDefender(player.getClan())) {
                    player.setSiegeState((byte) 2);
                }
            }
        }
        //System.out.println("#####9");
        //Expand Skill
        sendPacket(new ExStorageMaxCount(player));
        //System.out.println("#####10");

        Quest.playerEnter(player);
        sendPacket(new QuestList(player));

        //System.out.println("#####11");
        if (player.isInNoLogoutArea() || (!player.isGM() && player.getSiegeState() < 2 && player.isInsideZone(L2Character.ZONE_SIEGE))) {
            // Attacker or spectator logging in to a siege zone. Actually should be checked for inside castle only?
            player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
            //player.sendMessage("You have been teleported to the nearest town due to you being in siege zone");
        }
        //System.out.println("#####12");

        if (player.isDead() || player.isAlikeDead()) {
            sendPacket(new UserInfo(player));
            // no broadcast needed since the player will already spawn dead to others
            sendPacket(new Die(player));
        }
        //System.out.println("#####13");

        player.restoreEffects(null);
        player.updateEffectIcons();
        //System.out.println("#####14");

        CursedWeaponsManager.getInstance().checkPlayer(player);

        player.sendEtcStatusUpdate();
        //System.out.println("#####15");

        player.checkHpMessages(player.getMaxHp(), player.getCurrentHp());
        player.checkDayNightMessages();
        //System.out.println("#####16");

        if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false)) {
            DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
        }
        //System.out.println("#####17");

        //System.out.println("#####18");
        player.checkBanChat(false);

        CrownManager.getInstance().checkCrowns(player);

        L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null) {
            wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        }
        if (wpn != null) {
            if (wpn.isAugmented()) {
                wpn.getAugmentation().applyBoni(player);
            }

            if (wpn.isShadowItem()) {
                player.sendCritMessage(wpn.getItemName() + ": осталось " + wpn.getMana() + " минут.");
            }
        }

        Quest.playerEnter(player);
        //System.out.println("#####19");

        PetitionManager.getInstance().checkPetitionMessages(player);

        player.onPlayerEnter();

        //player.sendPacket(new SkillCoolTime(player));
        EventManager.getInstance().onLogin(player);

        player.sendSkillList();
        player.checkAllowedSkills();
        player.sendItems(false);
        //System.out.println("#####");
        player.setCurrentHp(player.getEnterWorldHp());
        player.setCurrentMp(player.getEnterWorldMp());
        player.setCurrentCp(player.getEnterWorldCp());
        //System.out.println("#####20");
        if (player.isInOlumpiadStadium()) {
            if (player.isDead() || player.isAlikeDead()) {
                player.doRevive();
            }
            player.teleToClosestTown();
            player.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium");
        }

        TvTEvent.onLogin(player);
        //System.out.println("#####21");

        Online _online = Online.getInstance();
        _online.checkMaxOnline();
        //System.out.println("#####22");

        if (Config.SONLINE_LOGIN_ONLINE) {
            TextBuilder onlineWelcome = new TextBuilder();
            onlineWelcome.append(Static.SHO_ONLINE_ALL + " ").append(_online.getCurrentOnline()).append("; ");
            if (Config.SONLINE_LOGIN_OFFLINE) {
                onlineWelcome.append(Static.SHO_ONLINE_TRD + " ").append(_online.getOfflineTradersOnline()).append(" ");
            }
            player.sendMessage(onlineWelcome.toString());
            onlineWelcome.clear();
            onlineWelcome = null;
            if (Config.SONLINE_LOGIN_MAX) {
                TextBuilder gWelcome = new TextBuilder();
                gWelcome.append(Static.SHO_ONLINE_REC + " ").append(_online.getMaxOnline()).append("; ");
                if (Config.SONLINE_LOGIN_DATE) {
                    gWelcome.append(Static.SHO_ONLINE_WAS + " ").append(_online.getMaxOnlineDate()).append(" ");
                }
                player.sendMessage(gWelcome.toString());
                gWelcome.clear();
                gWelcome = null;
            }
        }

        //System.out.println("#####23");
        //player.restoreProfileBuffs(); минус лишний коннект к мускулу
        player.checkDonateSkills();
        //System.out.println("#####24");

        // engage and notify Partner
        if (Config.L2JMOD_ALLOW_WEDDING) {
            engage(player);
            notifyPartner(player, player.getPartnerId());
        }
        notifyFriends(player, true);
        if (Config.PREMIUM_ANOOUNCE && player.isPremium()) {
            Announcements.getInstance().announceToAll(Config.PREMIUM_ANNOUNCE_PHRASE.replace("%player%", player.getName()));
        }
        //System.out.println("#####25");

        if (Config.PC_CAFE_ENABLED && player.getPcPoints() > 0) {
            sendPacket(new ExPCCafePointInfo(player, 0, false, false));
        }

        player.setInGame(true);
        player.sendTempMessages();
        StartClass(player);

        CustomPlayerEventList.notifyPlayerLogin(player);
        //test
        //player.sendAdmResultMessage("Email: " + getClient().hasEmail());
        //
        player.sendActionFailed();
        //System.out.println("#####finish");
    }

    /**
     * @param player
     */
    private void engage(L2PcInstance cha) {
        int _chaid = cha.getObjectId();
        for (Couple cl : CoupleManager.getInstance().getCouples()) {
            if (cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid) {
                if (cl.getMaried()) {
                    cha.setMarried(true);
                }

                cha.setCoupleId(cl.getId());
                if (cl.getPlayer1Id() == _chaid) {
                    cha.setPartnerId(cl.getPlayer2Id());
                } else {
                    cha.setPartnerId(cl.getPlayer1Id());
                }

                //tp skill
                L2Skill wedTP = SkillTable.getInstance().getInfo(7073, 1);
                if (wedTP != null) {
                    cha.addSkill(wedTP, false);
                }
            }
        }
    }

    /**
     * @param player partnerid
     */
    private void notifyPartner(L2PcInstance cha, int partnerId) {
        if (cha.getPartnerId() != 0) {
            L2PcInstance partner = L2World.getInstance().getPlayer(cha.getPartnerId());
            if (partner != null) {
                if (!partner.getAppearance().getSex()) {
                    partner.sendPacket(Static.WIFE_LOGIN);
                } else {
                    partner.sendPacket(Static.AUNT_LOGIN);
                }
            }
            partner = null;
        }
    }

    /**
     * @param player
     */
    public static void notifyFriends(L2PcInstance cha, boolean login) {
        if (login) {
            cha.sendPacket(new FriendList(cha));
        }

        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT friend_id, friend_name FROM character_friends WHERE char_id=?");
            st.setInt(1, cha.getObjectId());
            rs = st.executeQuery();

            L2PcInstance friend;
            int friendId;
            String friendName;
            while (rs.next()) {
                friendId = rs.getInt("friend_id");
                friendName = rs.getString("friend_name");
                cha.storeFriend(friendId, friendName);
                friend = L2World.getInstance().getPlayer(friendId);
                if (friend == null) {
                    continue;
                }

                if (login) {
                    friend.sendPacket(SystemMessage.id(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addString(cha.getName()));
                    friend.sendPacket(new FriendStatus(cha, true));
                } else {
                    friend.sendPacket(new FriendStatus(cha, false));
                    cha.sendPacket(new FriendList(cha));
                }
            }
            Close.SR(st, rs);
            checkMail(con, cha);
        } catch (Exception e) {
            _log.warning("could not restore friend data:" + e);
        } finally {
            Close.CSR(con, st, rs);
        }
    }

    private static void checkMail(Connect con, L2PcInstance player) {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = con.prepareStatement("SELECT id FROM `z_bbs_mail` WHERE `to` = ? AND `read` = ? LIMIT 1");
            st.setInt(1, player.getObjectId());
            st.setInt(2, 0);
            rs = st.executeQuery();
            if (rs.next()) {
                player.sendPacket(Static.ExMailArrived);
                player.sendPacket(Static.UNREAD_MAIL);
            }
        } catch (Exception e) {
            _log.warning("EnterWorld: checkMail() error: " + e);
        } finally {
            Close.SR(st, rs);
        }
    }

    /**
     * @param player
     */
    private void notifyClanMembers(L2PcInstance player) {
        L2Clan clan = player.getClan();
        if (clan != null && clan.getClanMember(player.getName()) != null) {
            clan.getClanMember(player.getName()).setPlayerInstance(player);
            clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
            clan.broadcastToOtherOnlineMembers(SystemMessage.id(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addString(player.getName()), player);
            clan.addBonusEffects(player, true);
        }
    }

    /**
     * @param player
     */
    private void notifySponsorOrApprentice(L2PcInstance player) {
        if (player.getSponsor() != 0) {
            L2PcInstance sponsor = L2World.getInstance().getPlayer(player.getSponsor());
            if (sponsor != null) {
                sponsor.sendPacket(SystemMessage.id(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addString(player.getName()));
            }
        } else if (player.getApprentice() != 0) {
            L2PcInstance apprentice = L2World.getInstance().getPlayer(player.getApprentice());
            if (apprentice != null) {
                apprentice.sendPacket(SystemMessage.id(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addString(player.getName()));
            }
        }
    }


    //Pledge
    private void checkPledgeClass(L2PcInstance player) {
        int pledgeClass = 0;
        if (player.getClan() != null && player.getClan().getClanMember(player.getObjectId()) != null) {
            pledgeClass = player.getClan().getClanMember(player.getObjectId()).calculatePledgeClass(player);
        }

        if (player.isNoble() && pledgeClass < 5) {
            pledgeClass = 5;
        }

        if (player.isHero()) {
            pledgeClass = 8;
        }

        player.setPledgeClass(pledgeClass);
    }

    private void StartClass (L2PcInstance player) {
        if (Config.ALTCLASS_MANAGER){
            if (player.getClassId().getId() == 0) {
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/HF.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 10){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/HM.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 18){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/EF.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 25){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/EM.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 31){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/EDF.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 38){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/EDM.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 44){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/OF.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 49){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/OM.htm");
                player.sendPacket(nhm);
            }
            if (player.getClassId().getId() == 53){
                NpcHtmlMessage nhm = NpcHtmlMessage.id(5);
                nhm.setFile("data/html/startclass/DF.htm");
                player.sendPacket(nhm);
            }
        }
    }

}
