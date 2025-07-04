/* This program is free software; you can redistribute it and/or modify
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
package scripts.commands.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import scripts.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.commands.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExRedSky;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SignsSky;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.network.serverpackets.SunRise;
import net.sf.l2j.gameserver.network.serverpackets.SunSet;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

/**
 * This class handles following admin commands:
 *   <li> invis/invisible/vis/visible = makes yourself invisible or visible
 *   <li> earthquake = causes an earthquake of a given intensity and duration around you
 *   <li> bighead/shrinkhead = changes head size
 *   <li> gmspeed = temporary Super Haste effect.
 *   <li> para/unpara = paralyze/remove paralysis from target
 *   <li> para_all/unpara_all = same as para/unpara, affects the whole world.
 *   <li> polyself/unpolyself = makes you look as a specified mob.
 *   <li> changename = temporary change name
 *   <li> clearteams/setteam_close/setteam = team related commands
 *   <li> social = forces an L2Character instance to broadcast social action packets.
 *   <li> effect = forces an L2Character instance to broadcast MSU packets.
 *   <li> abnormal = force changes over an L2Character instance's abnormal state.
 *   <li> play_sound/play_sounds = Music broadcasting related commands
 *   <li> atmosphere = sky change related commands.
 */
public class AdminEffects implements IAdminCommandHandler {

    private static final String[] ADMIN_COMMANDS = {"admin_invis", "admin_invisible", "admin_vis", "admin_visible", "admin_invis_menu",
        "admin_earthquake", "admin_earthquake_menu",
        "admin_bighead", "admin_shrinkhead",
        "admin_gmspeed", "admin_gmspeed_menu",
        "admin_unpara_all", "admin_para_all", "admin_unpara", "admin_para", "admin_unpara_all_menu", "admin_para_all_menu", "admin_unpara_menu", "admin_para_menu",
        "admin_polyself", "admin_unpolyself", "admin_polyself_menu", "admin_unpolyself_menu",
        "admin_changename", "admin_changename_menu",
        "admin_clearteams", "admin_setteam_close", "admin_setteam",
        "admin_social", "admin_effect", "admin_social_menu", "admin_effect_menu",
        "admin_abnormal", "admin_abnormal_menu",
        "admin_play_sounds", "admin_play_sound",
        "admin_atmosphere", "admin_atmosphere_menu", "admin_setchannel", "admin_dbfsdbfs", "admin_hshs"};
    private static final int REQUIRED_LEVEL = Config.GM_GODMODE;

    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        if (!Config.ALT_PRIVILEGES_ADMIN) {
            if (!(checkLevel(activeChar.getAccessLevel()) && activeChar.isGM())) {
                return false;
            }
        }

        GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), "");

        StringTokenizer st = new StringTokenizer(command);
        st.nextToken();

        if (command.equals("admin_invis_menu")) {
            if (!activeChar.isInvisible()) {
                activeChar.setChannel(0);
                activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ());
                activeChar.sendAdmResultMessage("Вас теперь не видно");
            } else {
                activeChar.setChannel(1);
                activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ());
                activeChar.sendAdmResultMessage("Вас теперь видно");
            }
            RegionBBSManager.getInstance().changeCommunityBoard();
        } else if (command.startsWith("admin_invis")) {
            activeChar.setChannel(0);
            //activeChar.broadcastUserInfo();
            //activeChar.decayMe();
            //activeChar.spawnMe();
            activeChar.teleToLocationEvent(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
            activeChar.sendAdmResultMessage("Вас теперь не видно");
        } else if (command.startsWith("admin_vis")) {
            activeChar.setChannel(1);
            //activeChar.broadcastUserInfo();
            activeChar.teleToLocationEvent(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
            activeChar.sendAdmResultMessage("Вас теперь видно");
            //RegionBBSManager.getInstance().changeCommunityBoard();
        } else if (command.startsWith("admin_earthquake")) {
            try {
                String val1 = st.nextToken();
                int intensity = Integer.parseInt(val1);
                String val2 = st.nextToken();
                int duration = Integer.parseInt(val2);
                Earthquake eq = new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration);
                activeChar.broadcastPacket(eq);
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Use: //earthquake <intensity> <duration>");
            }
        } else if (command.startsWith("admin_atmosphere")) {
            try {
                String type = st.nextToken();
                String state = st.nextToken();
                adminAtmosphere(type, state, activeChar);
            } catch (Exception ex) {
            }
        } else if (command.equals("admin_play_sounds")) {
            AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
        } else if (command.startsWith("admin_play_sounds")) {
            try {
                AdminHelpPage.showHelpPage(activeChar, "songs/songs" + command.substring(17) + ".htm");
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.startsWith("admin_play_sound")) {
            try {
                playAdminSound(activeChar, command.substring(17));
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.startsWith("admin_para") || command.startsWith("admin_para_menu")) {
            String type = "1";
            try {
                type = st.nextToken();
            } catch (Exception e) {
            }
            try {
                L2Object target = activeChar.getTarget();
                L2Character player = null;
                if (target.isL2Character()) {
                    player = (L2Character) target;
                    if (type.equals("1")) {
                        player.startAbnormalEffect(0x0400);
                    } else {
                        player.startAbnormalEffect(0x0800);
                    }
                    player.setIsParalyzed(true);
                    StopMove sm = new StopMove(player);
                    player.sendPacket(sm);
                    player.broadcastPacket(sm);
                }
            } catch (Exception e) {
            }
        } else if (command.equals("admin_unpara") || command.equals("admin_unpara_menu")) {
            try {
                L2Object target = activeChar.getTarget();
                L2Character player = null;
                if (target.isL2Character()) {
                    player = (L2Character) target;
                    player.stopAbnormalEffect((short) 0x0400);
                    player.setIsParalyzed(false);
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_para_all")) {
            try {
                for (L2PcInstance player : activeChar.getKnownList().getKnownPlayersInRadius(2000)) {
                    if (!player.isGM()) {
                        player.startAbnormalEffect(0x0400);
                        player.setIsParalyzed(true);
                    }
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_unpara_all")) {
            try {
                for (L2PcInstance player : activeChar.getKnownList().getKnownPlayersInRadius(2000)) {
                    player.stopAbnormalEffect(0x0400);
                    player.setIsParalyzed(false);
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_bighead")) {
            try {
                L2Object target = activeChar.getTarget();
                L2Character player = null;
                if (target.isL2Character()) {
                    player = (L2Character) target;
                    player.startAbnormalEffect(0x2000);
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_shrinkhead")) {
            try {
                L2Object target = activeChar.getTarget();
                L2Character player = null;
                if (target.isL2Character()) {
                    player = (L2Character) target;
                    player.stopAbnormalEffect((short) 0x2000);
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_gmspeed")) {
            try {
                int val = Integer.parseInt(st.nextToken());
                boolean sendMessage = activeChar.getFirstEffect(7029) != null;
                activeChar.stopSkillEffects(7029);
                if (val == 0 && sendMessage) {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.EFFECT_S1_DISAPPEARED).addSkillName(7029));
                } else if ((val >= 1) && (val <= 4)) {
                    L2Skill gmSpeedSkill = SkillTable.getInstance().getInfo(7029, val);
                    activeChar.doCast(gmSpeedSkill);
                }
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Use //gmspeed value (0=off...4=max).");
            } finally {
                activeChar.updateEffectIcons();
            }
        } else if (command.startsWith("admin_polyself")) {
            try {
                String id = st.nextToken();
                activeChar.getPoly().setPolyInfo("npc", id);
                activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
                CharInfo info1 = new CharInfo(activeChar);
                activeChar.broadcastPacket(info1);
                UserInfo info2 = new UserInfo(activeChar);
                activeChar.sendPacket(info2);
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_unpolyself")) {
            activeChar.getPoly().setPolyInfo(null, "1");
            activeChar.decayMe();
            activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
            CharInfo info1 = new CharInfo(activeChar);
            activeChar.broadcastPacket(info1);
            UserInfo info2 = new UserInfo(activeChar);
            activeChar.sendPacket(info2);
        } else if (command.startsWith("admin_changename")) {
            try {
                String name = st.nextToken();
                String oldName = "null";
                try {
                    L2Object target = activeChar.getTarget();
                    L2Character player = null;
                    if (target.isL2Character()) {
                        player = (L2Character) target;
                        oldName = player.getName();
                    } else if (target == null) {
                        player = activeChar;
                        oldName = activeChar.getName();
                    }
                    if (player.isPlayer()) {
                        L2World.getInstance().removeFromAllPlayers((L2PcInstance) player);
                    }
                    player.setName(name);
                    if (player.isPlayer()) {
                        L2World.getInstance().addVisibleObject(player, null, null);
                    }
                    if (player.isPlayer()) {
                        CharInfo info1 = new CharInfo((L2PcInstance) player);
                        player.broadcastPacket(info1);
                        UserInfo info2 = new UserInfo((L2PcInstance) player);
                        player.sendPacket(info2);
                    } else if (player instanceof L2NpcInstance) {
                        NpcInfo info1 = new NpcInfo((L2NpcInstance) player, null);
                        player.broadcastPacket(info1);
                    }
                    activeChar.sendAdmResultMessage("Changed name from " + oldName + " to " + name + ".");
                } catch (Exception e) {
                }
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (command.equals("admin_clear_teams")) {
            try {
                for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values()) {
                    player.setTeam(0);
                    player.broadcastUserInfo();
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_setteam_close")) {
            try {
                String val = st.nextToken();
                int teamVal = Integer.parseInt(val);
                for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values()) {
                    if (activeChar.isInsideRadius(player, 400, false, true)) {
                        player.setTeam(0);
                        if (teamVal != 0) {
                            SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
                            sm.addString("You have joined team " + teamVal);
                            player.sendPacket(sm);
                        }
                        player.broadcastUserInfo();
                    }
                }
            } catch (Exception e) {
            }
        } else if (command.startsWith("admin_setteam")) {
            String val = command.substring(14);
            int teamVal = Integer.parseInt(val);
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (target.isPlayer()) {
                player = (L2PcInstance) target;
            } else {
                return false;
            }
            player.setTeam(teamVal);
            if (teamVal != 0) {
                SystemMessage sm = SystemMessage.id(SystemMessageId.S1_S2);
                sm.addString("You have joined team " + teamVal);
                player.sendPacket(sm);
            }
            player.broadcastUserInfo();
        } else if (command.startsWith("admin_social")) {
            try {
                String target = null;
                L2Object obj = activeChar.getTarget();
                if (st.countTokens() == 2) {
                    int social = Integer.parseInt(st.nextToken());
                    target = st.nextToken();
                    if (target != null) {
                        L2PcInstance player = L2World.getInstance().getPlayer(target);
                        if (player != null) {
                            if (performSocial(social, player, activeChar)) {
                                activeChar.sendMessage(player.getName() + " was affected by your request.");
                            }
                        } else {
                            try {
                                int radius = Integer.parseInt(target);
                                for (L2Object object : activeChar.getKnownList().getKnownObjects().values()) {
                                    if (activeChar.isInsideRadius(object, radius, false, false)) {
                                        performSocial(social, object, activeChar);
                                    }
                                }
                                activeChar.sendMessage(radius + " units radius affected by your request.");
                            } catch (NumberFormatException nbe) {
                                activeChar.sendAdmResultMessage("Incorrect parameter");
                            }
                        }
                    }
                } else if (st.countTokens() == 1) {
                    int social = Integer.parseInt(st.nextToken());
                    if (obj == null) {
                        obj = activeChar;
                    }
                    if (obj != null) {
                        if (performSocial(social, obj, activeChar)) {
                            activeChar.sendMessage(obj.getName() + " was affected by your request.");
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                        }
                    } else {
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
                    }
                } else if (!command.contains("menu")) {
                    activeChar.sendAdmResultMessage("Usage: //social <social_id> [player_name|radius]");
                }
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_abnormal")) {
            try {
                String target = null;
                L2Object obj = activeChar.getTarget();
                if (st.countTokens() == 2) {
                    String parm = st.nextToken();
                    int abnormal = Integer.decode("0x" + parm);
                    target = st.nextToken();
                    if (target != null) {
                        L2PcInstance player = L2World.getInstance().getPlayer(target);
                        if (player != null) {
                            if (performAbnormal(abnormal, player)) {
                                activeChar.sendMessage(player.getName() + "'s abnormal status was affected by your request.");
                            } else {
                                activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                            }
                        } else {
                            try {
                                int radius = Integer.parseInt(target);
                                for (L2Object object : activeChar.getKnownList().getKnownObjects().values()) {
                                    if (activeChar.isInsideRadius(object, radius, false, false)) {
                                        performAbnormal(abnormal, object);
                                    }
                                }
                                activeChar.sendMessage(radius + " units radius affected by your request.");
                            } catch (NumberFormatException nbe) {
                                activeChar.sendAdmResultMessage("Usage: //abnormal <hex_abnormal_mask> [player|radius]");
                            }
                        }
                    }
                } else if (st.countTokens() == 1) {
                    int abnormal = Integer.decode("0x" + st.nextToken());
                    if (obj == null) {
                        obj = activeChar;
                    }
                    if (obj != null) {
                        if (performAbnormal(abnormal, obj)) {
                            activeChar.sendMessage(obj.getName() + "'s abnormal status was affected by your request.");
                        } else {
                            activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                        }
                    } else {
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
                    }
                } else if (!command.contains("menu")) {
                    activeChar.sendAdmResultMessage("Usage: //abnormal <abnormal_mask> [player_name|radius]");
                }
            } catch (Exception e) {
                if (Config.DEBUG) {
                    e.printStackTrace();
                }
            }
        } else if (command.startsWith("admin_effect")) {
            try {
                L2Object obj = activeChar.getTarget();
                int level = 1, hittime = 1;
                int skill = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) {
                    level = Integer.parseInt(st.nextToken());
                }
                if (st.hasMoreTokens()) {
                    hittime = Integer.parseInt(st.nextToken());
                }
                if (obj == null) {
                    obj = activeChar;
                }
                if (obj != null) {
                    if (!(obj.isL2Character())) {
                        activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
                    } else {
                        L2Character target = (L2Character) obj;
                        target.broadcastPacket(new MagicSkillUser(target, activeChar, skill, level, hittime, 0));
                        activeChar.sendMessage(obj.getName() + " performs MSU " + skill + "/" + level + " by your request.");
                    }
                } else {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.INCORRECT_TARGET));
                }
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Usage: //effect skill [level | level hittime]");
            }
        } else if (command.startsWith("admin_setchannel")) {
            try {
                int val = Integer.parseInt(st.nextToken());
                L2Object obj = activeChar.getTarget();
                if (obj == null || !obj.isPlayer()) {
                    activeChar.sendAdmResultMessage("Цель не найдена.");
                    return false;
                }

                L2PcInstance ptarg = (L2PcInstance) obj;

                ptarg.setEventChannel(val);
                ptarg.teleToLocation(ptarg.getX(), ptarg.getY(), ptarg.getZ());
                activeChar.sendAdmResultMessage("Игрок " + ptarg.getName() + "; канал " + val);
            } catch (Exception e) {
                activeChar.sendAdmResultMessage("Use //setchannel value");
            }
        } /*else if (command.startsWith("admin_dbfsdbfs")) {
        try {
        activeChar.getEffect(95, 20);
        activeChar.getEffect(97, 11);
        activeChar.getEffect(102, 16);
        activeChar.getEffect(105, 24);
        activeChar.getEffect(115, 17);
        activeChar.getEffect(116, 14);
        activeChar.getEffect(122, 15);
        activeChar.getEffect(127, 14);
        activeChar.getEffect(400, 10);
        activeChar.getEffect(401, 10);
        activeChar.getEffect(407, 10);
        activeChar.getEffect(408, 10);
        activeChar.getEffect(412, 10);
        activeChar.getEffect(1096, 16);
        activeChar.getEffect(1099, 15);
        } catch (Exception e) {
        activeChar.sendAdmResultMessage("Use //dbfsdbfs");
        }
        } else if (command.startsWith("admin_hshs")) {
        try {
        activeChar.getEffect(4552, 10);
        activeChar.getEffect(4553, 10);
        activeChar.getEffect(4554, 10);
        } catch (Exception e) {
        activeChar.sendAdmResultMessage("Use //dbfsdbfs");
        }
        }*/
        if (command.contains("menu")) {
            showMainPage(activeChar, command);
        }
        return true;
    }

    /**
     * @param action bitmask that should be applied over target's abnormal
     * @param target
     * @return <i>true</i> if target's abnormal state was affected , <i>false</i> otherwise.
     */
    private boolean performAbnormal(int action, L2Object target) {
        if (target.isL2Character()) {
            L2Character character = (L2Character) target;
            if ((character.getAbnormalEffect() & action) == action) {
                character.stopAbnormalEffect(action);
            } else {
                character.startAbnormalEffect(action);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean performSocial(int action, L2Object target, L2PcInstance activeChar) {
        try {
            if (target.isL2Character()) {
                if ((target instanceof L2Summon) || (target instanceof L2ChestInstance)) {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                    return false;
                }
                if ((target instanceof L2NpcInstance) && (action < 1 || action > 3)) {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                    return false;
                }
                if ((target.isPlayer()) && (action < 2 || action > 16)) {
                    activeChar.sendPacket(SystemMessage.id(SystemMessageId.NOTHING_HAPPENED));
                    return false;
                }
                L2Character character = (L2Character) target;
                character.broadcastPacket(new SocialAction(target.getObjectId(), action));
            } else {
                return false;
            }
        } catch (Exception e) {
        }
        return true;
    }

    /**
     *
     * @param type - atmosphere type (signssky,sky)
     * @param state - atmosphere state(night,day)
     */
    private void adminAtmosphere(String type, String state, L2PcInstance activeChar) {
        L2GameServerPacket packet = null;

        if (type.equals("signsky")) {
            if (state.equals("dawn")) {
                packet = new SignsSky(2);
            } else if (state.equals("dusk")) {
                packet = new SignsSky(1);
            }
        } else if (type.equals("sky")) {
            if (state.equals("night")) {
                packet = new SunSet();
            } else if (state.equals("day")) {
                packet = new SunRise();
            } else if (state.equals("red")) {
                packet = new ExRedSky(10);
            }
        } else {
            activeChar.sendAdmResultMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red>");
        }
        if (packet != null) {
            for (L2PcInstance player : L2World.getInstance().getAllPlayers()) {
                player.sendPacket(packet);
            }
        }
    }

    private void playAdminSound(L2PcInstance activeChar, String sound) {
        PlaySound _snd = new PlaySound(1, sound, 0, 0, 0, 0, 0);
        activeChar.sendPacket(_snd);
        activeChar.broadcastPacket(_snd);
        activeChar.sendAdmResultMessage("Playing " + sound + ".");
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private boolean checkLevel(int level) {
        return (level >= REQUIRED_LEVEL);
    }

    private void showMainPage(L2PcInstance activeChar, String command) {
        String filename = "effects_menu";
        if (command.contains("abnormal")) {
            filename = "abnormal";
        } else if (command.contains("social")) {
            filename = "social";
        }
        AdminHelpPage.showHelpPage(activeChar, filename + ".htm");
    }
}
