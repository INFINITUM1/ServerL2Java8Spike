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
package net.sf.l2j.gameserver.model;

import java.util.concurrent.ScheduledFuture;
import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.util.PeaceZone;

public abstract class L2Summon extends L2PlayableInstance {
    //private static Logger _log = Logger.getLogger(L2Summon.class.getName());

    protected int _pkKills;
    private byte _pvpFlag;
    private L2PcInstance _owner;
    private int _karma = 0;
    private int _attackRange = 36; //Melee range
    private boolean _follow = true;
    private boolean _previousFollowStatus = true;
    private int _maxLoad;
    private int _chargedSoulShot;
    private int _chargedSpiritShot;
    // TODO: currently, all servitors use 1 shot.  However, this value
    // should vary depending on the servitor template (id and level)!
    private int _soulShotsPerHit = 1;
    private int _spiritShotsPerHit = 1;
    protected boolean _isAgation = false;
    protected boolean _showSumAnim;

    public class AIAccessor extends L2Character.AIAccessor {

        protected AIAccessor() {
        }

        public L2Summon getSummon() {
            return L2Summon.this;
        }

        public boolean isAutoFollow() {
            return L2Summon.this.getFollowStatus();
        }

        public void doPickupItem(L2Object object) {
            L2Summon.this.doPickupItem(object);
        }
    }

    public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner) {
        super(objectId, template);
        init(owner);
    }

    private void init(L2PcInstance owner) {
        getKnownList();	// init knownlist
        getStat();			// init stats
        getStatus();		// init status

        _showSumAnim = true;
        _owner = owner;
        _ai = new L2SummonAI(new L2Summon.AIAccessor());
        setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
        if (getNpcId() == Config.SOB_NPC) {
            _isAgation = true;
        }
    }

    @Override
    public final SummonKnownList getKnownList() {
        if (super.getKnownList() == null || !(super.getKnownList() instanceof SummonKnownList)) {
            setKnownList(new SummonKnownList(this));
        }
        return (SummonKnownList) super.getKnownList();
    }

    @Override
    public SummonStat getStat() {
        if (super.getStat() == null || !(super.getStat() instanceof SummonStat)) {
            setStat(new SummonStat(this));
        }
        return (SummonStat) super.getStat();
    }

    @Override
    public SummonStatus getStatus() {
        if (super.getStatus() == null || !(super.getStatus() instanceof SummonStatus)) {
            setStatus(new SummonStatus(this));
        }
        return (SummonStatus) super.getStatus();
    }

    @Override
    public L2CharacterAI getAI() {
        //synchronized(this)
        //{
        if (_ai == null) {
            _ai = new L2SummonAI(new L2Summon.AIAccessor());
        }
        //}

        return _ai;
    }

    @Override
    public L2NpcTemplate getTemplate() {
        return (L2NpcTemplate) super.getTemplate();
    }

    // this defines the action buttons, 1 for Summon, 2 for Pets
    public abstract int getSummonType();

    @Override
    public void updateAbnormalEffect() {
        FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            pc.sendPacket(new NpcInfo(this, pc, 1));
        }
        players.clear();
        players = null;
        pc = null;
    }

    /**
     * @return Returns the mountable.
     */
    public boolean isMountable() {
        return false;
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (isAgathion()) {
            player.sendActionFailed();
            return;
        }

        if (player == _owner && player.getTarget() == this) {
            player.sendPacket(new PetStatusShow(this));
            player.sendActionFailed();
        } else if (player.getTarget() != this) {
            player.setTarget(this);
            player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

            //sends HP/MP status of the summon to other characters
            StatusUpdate su = new StatusUpdate(getObjectId());
            su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
            su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
            //su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
            //su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
            player.sendPacket(su);
        } else if (player.getTarget() == this) {
            if (isAutoAttackable(player) || player.isOlympiadStart()) {
                if (canSeeTarget(player)) {
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
                }
            } else {
                // This Action Failed packet avoids player getting stuck when clicking three or more times
                player.sendActionFailed();
                if (canSeeTarget(player)) {
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
                }
            }
        }
    }

    @Override
    public void onActionShift(L2GameClient client) {
        if (isAgathion()) {
            L2PcInstance player = client.getActiveChar();
            if (player == null) {
                return;
            }
            player.sendActionFailed();
            return;
        }
        super.onActionShift(client);
    }

    public long getExpForThisLevel() {
        if (getLevel() >= Experience.LEVEL.length) {
            return 0;
        }
        return Experience.LEVEL[getLevel()];
    }

    public long getExpForNextLevel() {
        if (getLevel() >= Experience.LEVEL.length - 1) {
            return 0;
        }
        return Experience.LEVEL[getLevel() + 1];
    }

    @Override
    public final int getKarma() {
        return _karma;
    }

    public void setKarma(int karma) {
        _karma = karma;
    }

    @Override
    public final L2PcInstance getOwner() {
        return _owner;
    }

    @Override
    public final int getNpcId() {
        return getTemplate().npcId;
    }

    public void setPvpFlag(byte pvpFlag) {
        _pvpFlag = pvpFlag;
    }

    @Override
    public byte getPvpFlag() {
        return _pvpFlag;
    }

    public void setPkKills(int pkKills) {
        _pkKills = pkKills;
    }

    public final int getPkKills() {
        return _pkKills;
    }

    public final int getMaxLoad() {
        return _maxLoad;
    }

    public final int getSoulShotsPerHit() {
        return _soulShotsPerHit;
    }

    public final int getSpiritShotsPerHit() {
        return _spiritShotsPerHit;
    }

    public void setMaxLoad(int maxLoad) {
        _maxLoad = maxLoad;
    }

    @Override
    public void setChargedSoulShot(int shotType) {
        _chargedSoulShot = shotType;
    }

    @Override
    public void setChargedSpiritShot(int shotType) {
        _chargedSpiritShot = shotType;
    }

    public void followOwner() {
        setFollowStatus(true);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }
        DecayTaskManager.getInstance().addDecayTask(this);
        return true;
    }

    public boolean doDie(L2Character killer, boolean decayed) {
        if (!super.doDie(killer)) {
            return false;
        }
        if (!decayed) {
            DecayTaskManager.getInstance().addDecayTask(this);
        }
        return true;
    }

    public void stopDecay() {
        DecayTaskManager.getInstance().cancelDecayTask(this);
    }

    @Override
    public void onDecay() {
        deleteMe(_owner);
    }

    @Override
    public void updateAndBroadcastStatus(int val) {
        if (!isAgathion()) {
            getOwner().sendPacket(new PetInfo(this, val));
            getOwner().sendPacket(new PetStatusUpdate(this));
        }

        if (isVisible()) {
            broadcastNpcInfo(val);
        }

        updateEffectIcons(true);
    }

    public void broadcastNpcInfo(int val) {
        FastList<L2PcInstance> players = getKnownList().getListKnownPlayers();
        L2PcInstance pc = null;
        for (FastList.Node<L2PcInstance> n = players.head(), end = players.tail(); (n = n.getNext()) != end;) {
            pc = n.getValue();
            if (pc == null) {
                continue;
            }

            if (pc == getOwner()) {
                continue;
            }
            pc.sendPacket(new NpcInfo(this, pc, val));
        }
        players.clear();
        players = null;
        pc = null;
    }

    @Override
    public void broadcastStatusUpdate() {
        super.broadcastStatusUpdate();

        if (getOwner() != null && isVisible()) {
            getOwner().sendPacket(new PetStatusUpdate(this));
        }
    }

    @Override
    public void updateEffectIcons(boolean partyOnly) {
        PartySpelled ps = new PartySpelled(this);

        // Go through all effects if any
        L2Effect[] effects = getAllEffects();
        if (effects != null && effects.length > 0) {
            for (L2Effect effect : effects) {
                if (effect == null) {
                    continue;
                }

                if (effect.getInUse()) {
                    effect.addPartySpelledIcon(ps);
                }
            }
        }

        this.getOwner().sendPacket(ps);
    }

    public void deleteMe(L2PcInstance owner) {
        getAI().stopFollow();
        owner.sendPacket(new PetDelete(getObjectId(), 2));

        //FIXME: I think it should really drop items to ground and only owner can take for a while
        giveAllToOwner();
        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setPet(null);
    }

    public synchronized void unSummon(L2PcInstance owner) {
        if (isVisible() && !isDead()) {
            getAI().stopFollow();
            owner.sendPacket(new PetDelete(getObjectId(), 2));
            if (getWorldRegion() != null) {
                getWorldRegion().removeFromZones(this);
            }
            store();

            giveAllToOwner();
            decayMe();
            getKnownList().removeAllKnownObjects();
            owner.setPet(null);
            setTarget(null);
        }
    }

    public int getAttackRange() {
        return _attackRange;
    }

    public void setAttackRange(int range) {
        if (range < 36) {
            range = 36;
        }
        _attackRange = range;
    }

    @Override
    public void setFollowStatus(boolean state) {
        _follow = state;
        if (_follow) {
            getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
        } else {
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
        }
    }

    @Override
    public boolean getFollowStatus() {
        return _follow;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return _owner.isAutoAttackable(attacker);
    }

    @Override
    public int getChargedSoulShot() {
        return _chargedSoulShot;
    }

    @Override
    public int getChargedSpiritShot() {
        return _chargedSpiritShot;
    }

    public int getControlItemId() {
        return 0;
    }

    public L2Weapon getActiveWeapon() {
        return null;
    }

    public PetInventory getInventory() {
        return null;
    }

    protected void doPickupItem(L2Object object) {
        return;
    }

    public void giveAllToOwner() {
        return;
    }

    public void store() {
        return;
    }

    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return null;
    }

    @Override
    public L2Weapon getActiveWeaponItem() {
        return null;
    }

    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return null;
    }

    @Override
    public L2Weapon getSecondaryWeaponItem() {
        return null;
    }

    /**
     * Return the L2Party object of its L2PcInstance owner or null.<BR><BR>
     */
    @Override
    public L2Party getParty() {
        if (_owner == null) {
            return null;
        }

        return _owner.getParty();
    }

    /**
     * Return True if the L2Character has a Party in progress.<BR><BR>
     */
    @Override
    public boolean isInParty() {
        if (_owner == null) {
            return false;
        }

        return _owner.getParty() != null;
    }

    /**
     * Check if the active L2Skill can be casted.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR> <li>Check if the target is correct </li>
     * <li>Check if the target is in the skill cast range </li> <li>Check if the
     * summon owns enough HP and MP to cast the skill </li> <li>Check if all
     * skills are enabled and this skill is enabled </li><BR><BR> <li>Check if
     * the skill is active </li><BR><BR> <li>Notify the AI with
     * AI_INTENTION_CAST and target</li><BR><BR>
     *
     * @param skill The L2Skill to use
     * @param forceUse used to force ATTACK on players
     * @param dontMove used to prevent movement, if not in range
     *
     */
    public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove) {
        if (skill == null || isDead()) {
            return;
        }

        // Check if the skill is active
        if (skill.isPassive()) {
            return;
        }

        //************************************* Check Casting in Progress *******************************************

        // If a skill is currently being used
        if (isCastingNow()) {
            return;
        }

        //************************************* Check Target *******************************************

        // Get the target for the skill
        L2Object target = null;

        switch (skill.getTargetType()) {
            // OWNER_PET should be cast even if no target has been found
            case TARGET_OWNER_PET:
                target = getOwner();
                break;
            // PARTY, AURA, SELF should be cast even if no target has been found
            case TARGET_PARTY:
            case TARGET_AURA:
            case TARGET_SELF:
                target = this;
                break;
            default:
                // Get the first target of the list
                target = skill.getFirstOfTargetList(this);
                break;
        }

        // Check the validity of the target
        if (target == null) {
            if (getOwner() == null) {
                return;
            } else {
                target = getOwner().getTarget();
                if (target == null) {
                    getOwner().sendPacket(Static.TARGET_CANT_FOUND);
                    return;
                }
            }
        }

        //************************************* Check skill availability *******************************************

        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill.getId()) && getOwner() != null) {
            getOwner().sendPacket(SystemMessage.id(SystemMessageId.SKILL_NOT_AVAILABLE).addString(skill.getName()));
            return;
        }
        //************************************* Check Consumables *******************************************

        // Check if the summon has enough MP
        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)) {
            // Send a System Message to the caster
            if (getOwner() != null) {
                getOwner().sendPacket(Static.NOT_ENOUGH_MP);
            }
            return;
        }

        // Check if the summon has enough HP
        if (getCurrentHp() <= skill.getHpConsume()) {
            // Send a System Message to the caster
            if (getOwner() != null) {
                getOwner().sendPacket(Static.NOT_ENOUGH_HP);
            }
            return;
        }

        //************************************* Check Summon State *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive()) {
            if (getOwner() != null) {
                if (PeaceZone.getInstance().inPeace(getOwner(), target)) {
                    // If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
                    //sendPacket(SystemMessage.id(SystemMessageId.TARGET_IN_PEACEZONE));
                    getOwner().sendActionFailed();
                    return;
                }

                // if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
                if (getOwner().isInOlympiadMode() && !getOwner().isOlympiadCompStart()) {
                    getOwner().sendActionFailed();
                    return;
                }
            }

            // Check if the target is attackable
			/*
             * if (target.isL2Door()) {
             * if(!((L2DoorInstance)target).isAttackable(getOwner())) return; }
             * else {
             */
            /*
             * if (!target.isAttackable() && getOwner() != null &&
             * (getOwner().getAccessLevel() < Config.GM_PEACEATTACK)) { return;
             * }
             */

            // Check if a Forced ATTACK is in progress on non-attackable target
            if (!target.isAutoAttackable(this) && !forceUse
                    && skill.getTargetType() != SkillTargetType.TARGET_AURA
                    && skill.getTargetType() != SkillTargetType.TARGET_CLAN
                    && skill.getTargetType() != SkillTargetType.TARGET_ALLY
                    && skill.getTargetType() != SkillTargetType.TARGET_PARTY
                    && skill.getTargetType() != SkillTargetType.TARGET_SELF) {
                return;
            }
            //}
        }
        // Notify the AI with AI_INTENTION_CAST and target
        getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
    }

    @Override
    public void setIsImobilised(boolean value) {
        super.setIsImobilised(value);

        if (value) {
            _previousFollowStatus = getFollowStatus();
            // if imobilized temporarly disable follow mode
            if (_previousFollowStatus) {
                setFollowStatus(false);
            }
        } else {
            // if not more imobilized restore previous follow mode
            setFollowStatus(_previousFollowStatus);
        }
    }

    public void setOwner(L2PcInstance newOwner) {
        _owner = newOwner;
    }

    /**
     * @return Returns the showSummonAnimation.
     */
    @Override
    public boolean isShowSummonAnimation() {
        return _showSumAnim;
    }

    /**
     * @param showSummonAnimation The showSummonAnimation to set.
     */
    @Override
    public void setShowSummonAnimation(boolean showSummonAnimation) {
        _showSumAnim = showSummonAnimation;
    }

    public int getWeapon() {
        return 0;
    }

    public int getArmor() {
        return 0;
    }

    /**
     * Servitors' skills automatically change their level based on the
     * servitor's level. Until level 70, the servitor gets 1 lv of skill per 10
     * levels. After that, it is 1 skill level per 5 servitor levels. If the
     * resulting skill level doesn't exist use the max that does exist!
     *
     * @see
     * net.sf.l2j.gameserver.model.L2Character#doCast(net.sf.l2j.gameserver.model.L2Skill)
     */
    @Override
    public void doCast(L2Skill skill) {
        int petLevel = getLevel();
        int skillLevel = petLevel / 10;
        if (petLevel >= 70) {
            skillLevel += (petLevel - 65) / 10;
        }

        // adjust the level for servitors less than lv 10
        if (skillLevel < 1) {
            skillLevel = 1;
        }

        L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(), skillLevel);

        if (skillToCast != null) {
            super.doCast(skillToCast);
        } else {
            super.doCast(skill);
        }
    }

    public int getPetSpeed() {
        return getTemplate().baseRunSpd;
    }

    public void broadcastPetInfo() {
        // После PetInfo нужно обязательно обновлять иконки бафов (они затираются).
        // Поэтому броадкаст для удобства совмещен с updateEffectIcons()
        updateEffectIcons();
    }

    /**
     * Делает броадкаст для пета
     */
    @Override
    public void broadcastUserInfo() {
        broadcastPetInfo();
    }

    @Override
    public boolean isEnemyForMob(L2Attackable mob) {
        if (_owner.isGM() || isAgathion()) {
            return false;
        }

        return (_owner.isEnemyForMob(mob) || mob.isAggressive());
    }

    @Override
    public boolean isInsidePvpZone() {
        if (ZoneManager.getInstance().inPvpZone(this)) {
            return true;
        }

        return super.isInsidePvpZone();
    }

    @Override
    public boolean replaceFirstBuff() {
        return (getBuffCount() >= Config.BUFFS_PET_MAX_AMOUNT);
    }

    @Override
    public void rechargeAutoSoulShot(boolean a, boolean b, boolean c) {
        if (_owner == null) {
            return;
        }

        _owner.rechargeAutoSoulShot(a, b, c);
    }

    @Override
    public boolean isL2Summon() {
        return true;
    }

    /**
     * formulas
     */
    @Override
    public double calcAtkAccuracy(double value) {
        value += (getLevel() < 60) ? 4 : 5;
        return value;
    }

    @Override
    public double calcAtkCritical(double value, double dex) {
        return 40;
    }

    @Override
    public double calcMAtkCritical(double value, double wit) {
        return 8;
    }

    @Override
    public L2PcInstance getPlayer() {
        return _owner;
    }

    @Override
    public L2Summon getL2Summon() {
        return this;
    }
    //

    @Override
    public boolean isAgathion() {
        return _isAgation;
    }

    @Override
    public void updatePvPStatus(L2Character target) {
        if (_owner == null) {
            return;
        }

        _owner.updatePvPStatus(target);
    }

    @Override
    public void onKillUpdatePvPKarma(L2Character target) {
        if (_owner == null) {
            return;
        }

        _owner.onKillUpdatePvPKarma(target);
    }

    @Override
    public boolean hasFarmPenalty() {
        if (_owner == null) {
            return false;
        }

        return _owner.hasFarmPenalty();
    }

    @Override
    public Duel getDuel() {
        if (_owner == null) {
            return null;
        }
        return _owner.getDuel();
    }

    @Override
    public boolean isAttackable() {
        if (isAgathion()) {
            return false;
        }
        return super.isAttackable();
    }

    @Override
    public boolean isInvul() {
        if (isAgathion()) {
            return true;
        }
        return super.isInvul();
    }

    @Override
    public void increasePvpKills(int count) {
        if (_owner == null) {
            return;
        }
        _owner.increasePvpKills(count);
    }
}
