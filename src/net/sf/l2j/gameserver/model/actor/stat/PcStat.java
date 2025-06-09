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
package net.sf.l2j.gameserver.model.actor.stat;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.util.log.AbstractLogger;

public class PcStat extends PlayableStat {

    private static final Logger _log = AbstractLogger.getLogger(L2PcInstance.class.getName());
    // =========================================================
    // Data Field
    private int _oldMaxHp;      // stats watch
    private int _oldMaxMp;      // stats watch
    private int _oldMaxCp;      // stats watch
    L2PcInstance activeChar;

    // =========================================================
    // Constructor
    public PcStat(L2PcInstance activeChar) {
        super(activeChar);
        this.activeChar = activeChar;
    }

    // =========================================================
    // Method - Public
    @Override
    public boolean addExp(long value) {
        return addExp(value, false);
    }

    @Override
    public boolean addExp(long value, boolean restore) {
        // Set new karma
        if (!activeChar.isCursedWeaponEquiped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Character.ZONE_PVP))) {
            //System.out.println("###??#");
            int karmaLost = activeChar.calculateKarmaLost(value);
            if (karmaLost > 0) {
                activeChar.setKarma(activeChar.getKarma() - karmaLost);
            }
        }

        if (!super.addExp(value, restore)) {
            return false;
        }

        /* Micht : Use of UserInfo for C5
        StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
        su.addAttribute(StatusUpdate.EXP, getExp());
        activeChar.sendPacket(su);
         */
        activeChar.sendPacket(new UserInfo(activeChar));

        return true;
    }

    /**
     * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.<BR><BR>
     *
     * <B><U> Actions </U> :</B><BR><BR>
     * <li>Remove Karma when the player kills L2MonsterInstance</li>
     * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance</li>
     * <li>Send a Server->Client System Message to the L2PcInstance </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet SocialAction (broadcast) </li>
     * <li>If the L2PcInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommandation, Expertise and beginner skills...) </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet UserInfo to the L2PcInstance </li><BR><BR>
     *
     * @param addToExp The Experience value to add
     * @param addToSp The SP value to add
     */
    @Override
    public boolean addExpAndSp(long addToExp, int addToSp) {
        if (activeChar.isNoExp()) {
            return false;
        }

        float ratioTakenByPet = 0;

        // if this player has a pet that takes from the owner's Exp, give the pet Exp now
        if (activeChar.getPet() != null && activeChar.getPet().isPet()) {
            L2PetInstance pet = (L2PetInstance) activeChar.getPet();
            ratioTakenByPet = pet.getPetData().getOwnerExpTaken();

            // only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
            // allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
            if (ratioTakenByPet > 0 && !pet.isDead()) {
                pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
            }
            // now adjust the max ratio to avoid the owner earning negative exp/sp
            if (ratioTakenByPet > 1) {
                ratioTakenByPet = 1;
            }
            addToExp = (long) (addToExp * (1 - ratioTakenByPet));
            addToSp = (int) (addToSp * (1 - ratioTakenByPet));
        }

        addToExp *= activeChar.calcStat(Stats.EXP_RATE_BONUS, 1);
        addToSp *= activeChar.calcStat(Stats.SP_RATE_BONUS, 1);

        if (!super.addExpAndSp(addToExp, addToSp)) {
            return false;
        }

        // Send a Server->Client System Message to the L2PcInstance
        activeChar.sendChanges();
        activeChar.sendPacket(SystemMessage.id(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP).addNumber((int) addToExp).addNumber(addToSp));
        return true;
    }

    @Override
    public boolean removeExpAndSp(long addToExp, int addToSp) {
        if (!super.removeExpAndSp(addToExp, addToSp)) {
            return false;
        }

        // Send a Server->Client System Message to the L2PcInstance
        activeChar.sendPacket(SystemMessage.id(SystemMessageId.EXP_DECREASED_BY_S1).addNumber((int) addToExp));
        activeChar.sendPacket(SystemMessage.id(SystemMessageId.SP_DECREASED_S1).addNumber(addToSp));
        return true;
    }

    @Override
    public boolean addLevel(byte value) {
        return addLevel(value, false);
    }

    @Override
    public final boolean addLevel(byte value, boolean restore) {
        if (getLevel() + value > Experience.MAX_LEVEL - 1) {
            return false;
        }

        boolean levelIncreased = super.addLevel(value, restore);

        if (levelIncreased) {
            activeChar.setCurrentCp(getMaxCp());
            activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 15));
            activeChar.sendPacket(Static.YOU_INCREASED_YOUR_LEVEL);
        }

        activeChar.rewardSkills(); // Give Expertise skill of this level
        if (activeChar.getClan() != null) {
            activeChar.getClan().updateClanMember(activeChar);
            activeChar.getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(activeChar));
        }
        if (activeChar.isInParty()) {
            activeChar.getParty().recalculatePartyLevel(); // Recalculate the party level
        }
        StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
        su.addAttribute(StatusUpdate.LEVEL, getLevel());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
        su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
        activeChar.sendPacket(su);

        // Update the overloaded status of the L2PcInstance
        activeChar.refreshOverloaded();
        // Update the expertise status of the L2PcInstance
        activeChar.refreshExpertisePenalty();
        // Send a Server->Client packet UserInfo to the L2PcInstance
        activeChar.sendPacket(new UserInfo(activeChar));

        if (!restore && Config.LVLUP_RELOAD_SKILLS) {
            activeChar.reloadSkills(false);
        }

        return levelIncreased;
    }

    @Override
    public boolean addSp(int value) {
        if (!super.addSp(value)) {
            return false;
        }

        StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
        su.addAttribute(StatusUpdate.SP, getSp());
        activeChar.sendPacket(su);

        return true;
    }

    @Override
    public final long getExpForLevel(int level) {
        return Experience.LEVEL[level];
    }

    // =========================================================
    // Method - Private
    // =========================================================
    // Property - Public
    @Override
    public final L2PcInstance getActiveChar() {
        return activeChar;
    }

    @Override
    public final long getExp() {
        if (activeChar.isSubClassActive()) {
            return activeChar.getSubClasses().get(activeChar.getClassIndex()).getExp();
        }

        return super.getExp();
    }

    @Override
    public final void setExp(long value) {
        if (activeChar.isSubClassActive()) {
            activeChar.getSubClasses().get(activeChar.getClassIndex()).setExp(value);
        } else {
            super.setExp(value);
        }
    }

    @Override
    public final byte getLevel() {
        if (activeChar.isSubClassActive()) {
            return activeChar.getSubClasses().get(activeChar.getClassIndex()).getLevel();
        }

        return super.getLevel();
    }

    @Override
    public final void setLevel(byte value) {
        if (value > Experience.MAX_LEVEL - 1) {
            value = Experience.MAX_LEVEL - 1;
        }

        if (activeChar.isSubClassActive()) {
            activeChar.getSubClasses().get(activeChar.getClassIndex()).setLevel(value);
        } else {
            super.setLevel(value);
        }
    }

    @Override
    public final int getMaxCp() {
        int val = super.getMaxCp();
        if (val != _oldMaxCp) {
            _oldMaxCp = val;
            if (activeChar.getStatus().getCurrentCp() != val) {
                activeChar.getStatus().setCurrentCp(activeChar.getStatus().getCurrentCp());
            }
        }
        return val;
    }

    @Override
    public final int getMaxHp() {
        // Get the Max HP (base+modifier) of the L2PcInstance
        int val = super.getMaxHp();
        if (val != _oldMaxHp) {
            _oldMaxHp = val;

            // Launch a regen task if the new Max HP is higher than the old one
            if (activeChar.getStatus().getCurrentHp() != val) {
                activeChar.getStatus().setCurrentHp(activeChar.getStatus().getCurrentHp()); // trigger start of regeneration
            }
        }

        return val;
    }

    @Override
    public final int getMaxMp() {
        // Get the Max MP (base+modifier) of the L2PcInstance
        int val = super.getMaxMp();

        if (val != _oldMaxMp) {
            _oldMaxMp = val;

            // Launch a regen task if the new Max MP is higher than the old one
            if (activeChar.getStatus().getCurrentMp() != val) {
                activeChar.getStatus().setCurrentMp(activeChar.getStatus().getCurrentMp()); // trigger start of regeneration
            }
        }

        return val;
    }

    @Override
    public final int getSp() {
        if (activeChar.isSubClassActive()) {
            return activeChar.getSubClasses().get(activeChar.getClassIndex()).getSp();
        }

        return super.getSp();
    }

    @Override
    public final void setSp(int value) {
        if (activeChar.isSubClassActive()) {
            activeChar.getSubClasses().get(activeChar.getClassIndex()).setSp(value);
        } else {
            super.setSp(value);
        }
    }

    @Override
    public double getSkillMastery() {
        double val = calcStat(Stats.SKILL_MASTERY, 0, null, null);

        if (activeChar.isMageClass()) {
            val *= Formulas.getINTBonus(activeChar);
        } else {
            val *= Formulas.getSTRBonus(activeChar);
        }

        return val;
    }

    @Override
    public int getWalkSpeed() {
        if (activeChar.isInWater()) {
            return Config.WATER_SPEED;
        }

        return super.getWalkSpeed();
    }

    @Override
    public int getRunSpeed() {
        if (activeChar.isInWater()) {
            return Config.WATER_SPEED;
        }

        return super.getRunSpeed();
    }
}
