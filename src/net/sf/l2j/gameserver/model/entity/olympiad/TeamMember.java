package net.sf.l2j.gameserver.model.entity.olympiad;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Log;

public class TeamMember {

    private OlympiadGame _game;
    private L2PcInstance _player;
    private int _objId;
    private String _name = "";
    private CompType _type;
    private int _side;
    private Location _returnLoc;
    private boolean _isDead;

    public boolean isDead() {
        return _isDead;
    }

    public void doDie() {
        _isDead = true;
    }

    public TeamMember(int obj_id, String name, OlympiadGame game, int side) {
        _objId = obj_id;
        _name = name;
        _game = game;
        _type = game.getType();
        _side = side;

        L2PcInstance player = L2World.getInstance().getPlayer(obj_id);
        if (player == null) {
            return;
        }

        _player = player;

        try {
            if (player.inObserverMode()) {
                if (player.getOlympiadObserveId() > 0) {
                    player.leaveOlympiadObserverMode();
                } else {
                    player.leaveObserverMode();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        player.setOlympiadSide(side);
        player.setOlympiadGameId(game.getId());
    }

    public StatsSet getStat() {
        return Olympiad._nobles.get(_objId);
    }

    public void takePointsForCrash() {
        if (!checkPlayer()) {
            try {
                StatsSet stat = getStat();
                int points = stat.getInteger(Olympiad.POINTS);
                int diff = Math.min(OlympiadGame.MAX_POINTS_LOOSE, points / _type.getLooseMult());
                stat.set(Olympiad.POINTS, points - diff);
                Log.add("Olympiad Result: " + _name + " lost " + diff + " points for crash", "olympiad");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkPlayer() {
        L2PcInstance player = _player;
        if (player == null || player.isDeleting() || player.isOnline() == 0 || player.getOlympiadGameId() == -1 || player.getOlympiadObserveId() > 0) {
            return false;
        }

        if (!player.isConnected()) {
            return false;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquiped()) {
            return false;
        }

        /*
         * L2GameClient client = player.getNetConnection(); if(client == null)
         * return false; MMOConnection conn = client.getConnection(); if(conn ==
         * null || conn.isClosed()) return false;
         */
        return true;
    }

    public void portPlayerToArena() {
        L2PcInstance player = _player;
        if (!checkPlayer() || player == null || player.isTeleporting()) {
            _player = null;
            return;
        }

        try {
            _returnLoc = player.getLoc();

            if (player.isDead()) {
                player.setIsPendingRevive(true);
            }
            if (player.isSitting()) {
                player.standUp();
            }

            player.setChannel(2);
            player.setTarget(null);
            player.setIsInOlympiadMode(true);

            if (Config.OLY_RELOAD_SKILLS_BEGIN) {
                player.reloadSkills();
            }

            if (player.getParty() != null) {
                player.getParty().oustPartyMember(player);
            }
            int diff = 550;
            Location tele = Olympiad.STADIUMS[_game.getId()].getTele1();
            if (_side == 2) {
                diff = -550;
                tele = Olympiad.STADIUMS[_game.getId()].getTele2();
            }

            player.teleToLocationEvent(tele.x + diff, tele.y, tele.z);
            player.sendPacket(new ExOlympiadMode(_side));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void portPlayerBack() {
        L2PcInstance player = _player;
        if (player == null) {
            return;
        }

        try {
            player.setChannel(1);
            player.setIsInOlympiadMode(false);
            player.setOlympiadSide(-1);
            player.setOlympiadGameId(-1);

            if (Config.OLY_RELOAD_SKILLS_END) {
                player.reloadSkills();
            }

            player.stopAllEffects();

            player.setCurrentCp(player.getMaxCp());
            player.setCurrentMp(player.getMaxMp());

            if (player.isDead()) {
                player.setCurrentHp(player.getMaxHp());
                player.broadcastPacket(new Revive(player));
            } else {
                player.setCurrentHp(player.getMaxHp());
            }

            // Add clan skill
            if (player.getClan() != null) {
                for (L2Skill skill : player.getClan().getAllSkills()) {
                    if (skill == null) {
                        continue;
                    }
                    player.addSkill(skill, false);
                }
            }

            // Add Hero Skills
            if (player.isHero()) {
                Hero.addSkills(player);
            }

            for (L2Skill s : player.getAllSkills()) {
                if (s == null) {
                    continue;
                }
                if (s.isForbidOly()) {
                    player.addStatFuncs(s.getStatFuncs(null, player));
                }
            }

            // Обновляем скилл лист, после добавления скилов
            player.sendSkillList();
            player.sendPacket(new ExOlympiadMode(0));
            player.sendPacket(new ExOlympiadMatchEnd());

            Olympiad.removeNobleIp(player, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (_returnLoc != null) {
                player.teleToLocationEvent(_returnLoc.x, _returnLoc.y, _returnLoc.z);
            } else {
                player.teleToClosestTown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPvpArena(boolean f) {
        L2PcInstance player = _player;
        if (player == null) {
            return;
        }
        player.setPVPArena(f);
    }

    public void reloadSkills() {
        L2PcInstance player = _player;
        if (player == null) {
            return;
        }
        player.reloadSkills(false);
    }

    public void preFightRestore() {
        L2PcInstance player = _player;
        if (player == null) {
            return;
        }

        /*
         * SkillTable.getInstance().getInfo(1204, 2).getEffects(player, player);
         *
         * int buff = 1086; if (player.isMageClass()) buff = 1085;
         *
         * if (player.getFirstEffect(buff) == null)
         * SkillTable.getInstance().getInfo(buff, 1).getEffects(player, player);
         */
        FastMap<Integer, Integer> buffs = Config.OLY_FIGHTER_BUFFS;
        if (player.isMageClass()) {
            buffs = Config.OLY_MAGE_BUFFS;
        }

        SkillTable _st = SkillTable.getInstance();
        for (FastMap.Entry<Integer, Integer> e = buffs.head(), end = buffs.tail(); (e = e.getNext()) != end;) {
            Integer id = e.getKey(); // No typecast necessary.
            Integer lvl = e.getValue(); // No typecast necessary.
            if (id == null || lvl == null) {
                continue;
            }

            _st.getInfo(id, lvl).getEffects(player, player);
        }

        player.setCurrentCp(player.getMaxCp());
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMp(player.getMaxMp());
    }

    public void preparePlayer() {
        L2PcInstance player = _player;
        if (player == null) {
            return;
        }

        try {
            // Remove Buffs
            player.stopAllEffects();

            // Remove clan skill
            if (player.getClan() != null) {
                for (L2Skill skill : player.getClan().getAllSkills()) {
                    player.removeSkill(skill, false);
                }
            }

            // Remove Hero Skills
            if (player.isHero()) {
                Hero.removeSkills(player);
            }

            if (Config.SOB_ID != 1 && Config.PROTECT_OLY_SOB) {
                if (Config.SOB_ID == 1) {
                    player.removeSkill(player.getKnownSkill(7077), false);
                    player.removeSkill(player.getKnownSkill(7078), false);
                    player.removeSkill(player.getKnownSkill(7079), false);
                    player.removeSkill(player.getKnownSkill(7080), false);
                } else {
                    player.removeSkill(player.getKnownSkill(Config.SOB_ID), false);
                }
            }

            for (L2Skill s : player.getAllSkills()) {
                if (s == null) {
                    continue;
                }
                if (s.isForbidOly()) {
                    player.removeStatsOwner(s);
                }
            }

            // Abort casting if player casting
            if (player.isCastingNow()) {
                player.abortCast();
            }

            if (player.getCubics() != null) {
                for (L2CubicInstance cubic : player.getCubics().values()) {
                    if (cubic == null) {
                        continue;
                    }

                    cubic.stopAction();
                    player.delCubic(cubic.getId());
                }
                player.getCubics().clear();
            }

            // Remove Summon's Buffs
            if (player.getPet() != null) {
                L2Summon summon = player.getPet();
                summon.stopAllEffects();

                if (summon.isPet()) {
                    summon.unSummon(player);
                }
            }
            if (player.getTrainedBeast() != null) {
                player.getTrainedBeast().doDespawn();
            }

            player.sendSkillList();

            // Remove Hero weapons
            L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
            if (wpn == null) {
                wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
            }
            if (wpn != null && wpn.isHeroItem()) {
                player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
                player.abortCast();
                player.abortAttack();
            } else if (wpn != null && wpn.isAugmented() && !Config.ALT_ALLOW_AUGMENT_ON_OLYMP) {
                wpn.getAugmentation().removeBoni(player);
            }

            // remove bsps/sps/ss automation
            if (player.getAutoSoulShot() != null) {
                for (int itemId : player.getAutoSoulShot().values()) {
                    player.removeAutoSoulShot(itemId);
                    player.sendPacket(new ExAutoSoulShot(itemId, 0));
                }
            }

            for (L2ItemInstance item : player.getInventory().getItems()) {
                if (item == null) {
                    continue;
                }

                if (item.isEquipped()
                        && item.notForOly()) {
                    player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
                }
            }

            if (player.getActiveWeaponInstance() != null) {
                player.getActiveWeaponInstance().setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                player.getActiveWeaponInstance().setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }

            player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
            player.setCurrentCp(player.getMaxCp());

            player.sendItems(false);
            player.sendChanges();
            player.broadcastUserInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveNobleData() {
        OlympiadDatabase.saveNobleData(_objId);
    }

    public void logout() {
        _player = null;
    }

    public L2PcInstance getPlayer() {
        return _player;
    }

    public int getObjId() {
        return _objId;
    }

    public String getName() {
        return _name;
    }
}
