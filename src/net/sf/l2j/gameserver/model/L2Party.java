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

import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.DimensionalRift;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowAdd;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowAll;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowDelete;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Log;
import net.sf.l2j.util.Rnd;

/**
 * This class ...
 *
 * @author nuocnam
 * @version $Revision: 1.6.2.2.2.6 $ $Date: 2005/04/11 19:12:16 $
 */
public class L2Party {

    private static final double[] BONUS_EXP_SP = {1, 1.30, 1.39, 1.50, 1.54, 1.58, 1.63, 1.67, 1.71};
    //private static Logger _log = Logger.getLogger(L2Party.class.getName());
    private FastTable<L2PcInstance> _members = new FastTable<L2PcInstance>();
    private int _pendingInvitation = 0;       // Number of players that already have been invited (but not replied yet)
    private int _partyLvl = 0;
    private int _itemDistribution = 0;
    private int _itemLastLoot = 0;
    private L2CommandChannel _commandChannel = null;
    private DimensionalRift _dr;
    public static final int ITEM_LOOTER = 0;
    public static final int ITEM_RANDOM = 1;
    public static final int ITEM_RANDOM_SPOIL = 2;
    public static final int ITEM_ORDER = 3;
    public static final int ITEM_ORDER_SPOIL = 4;
    private ScheduledFuture<?> positionsUpdate = null;
    private FastMap<Integer, Location> positions = new FastMap<Integer, Location>();

    /**
     * constructor ensures party has always one member - leader
     * @param leader
     * @param itemDistributionMode
     */
    public L2Party(L2PcInstance leader, int itemDistribution) {
        _itemDistribution = itemDistribution;
        _members.add(leader);
        _partyLvl = leader.getLevel();
        positionsUpdate = ThreadPoolManager.getInstance().scheduleGeneral(new PositionsUpdate(), 5000);
    }

    class PositionsUpdate implements Runnable {

        public PositionsUpdate() {
        }

        public void run() {
            if (_members == null || _members.size() < 2) {
                destroyParty();
                return;
            }

            positions.clear();
            for (L2PcInstance member : getPartyMembers()) {
                if (member == null) {
                    continue;
                }

                positions.put(member.getObjectId(), new Location(member.getX(), member.getY(), member.getZ()));
            }

            //PartyMemberPosition pmp = new PartyMemberPosition(positions);
            for (L2PcInstance member : getPartyMembers()) {
                if (member == null) {
                    continue;
                }

                member.sendPacket(new PartyMemberPosition(positions));
                //broadcastToPartyMembers(member, new PartyMemberPosition(member));
            }
            //positions.clear();
            //pmp = null;
            positionsUpdate = ThreadPoolManager.getInstance().scheduleGeneral(new PositionsUpdate(), 5000);
        }
    }

    /**
     * returns number of party members
     * @return
     */
    public int getMemberCount() {
        return getPartyMembers().size();
    }

    /**
     * returns number of players that already been invited, but not replied yet
     * @return
     */
    public int getPendingInvitationNumber() {
        return _pendingInvitation;
    }

    /**
     * decrease number of players that already been invited but not replied yet
     * happens when: player join party or player decline to join
     */
    public void decreasePendingInvitationNumber() {
        _pendingInvitation--;
    }

    /**
     * increase number of players that already been invite but not replied yet
     */
    public void increasePendingInvitationNumber() {
        _pendingInvitation++;
    }

    /**
     * returns all party members
     * @return
     */
    public FastTable<L2PcInstance> getPartyMembers() {
        //if (_members == null) _members = new FastList<L2PcInstance>();
        return _members;
    }

    /**
     * get random member from party
     * @return
     */
    //private L2PcInstance getRandomMember() { return getPartyMembers().get(Rnd.get(getPartyMembers().size())); }
    private L2PcInstance getCheckedRandomMember(int ItemId, L2Character target) {
        FastTable<L2PcInstance> availableMembers = new FastTable<L2PcInstance>();
        for (L2PcInstance member : getPartyMembers()) {
            if (member.getInventory().validateCapacityByItemId(ItemId)
                    && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                availableMembers.add(member);
            }
        }
        if (availableMembers.size() > 0) {
            return availableMembers.get(Rnd.get(availableMembers.size()));
        } else {
            return null;
        }
    }

    /**
     * get next item looter
     * @return
     */
    /*private L2PcInstance getNextLooter()
    {
    _itemLastLoot++;
    if (_itemLastLoot > getPartyMembers().size() -1) _itemLastLoot = 0;
    
    return (getPartyMembers().size() > 0) ? getPartyMembers().get(_itemLastLoot) : null;
    }*/
    private L2PcInstance getCheckedNextLooter(int ItemId, L2Character target) {
        for (int i = 0; i < getMemberCount(); i++) {
            _itemLastLoot++;
            if (_itemLastLoot >= getMemberCount()) {
                _itemLastLoot = 0;
            }
            L2PcInstance member;
            try {
                member = getPartyMembers().get(_itemLastLoot);
                if (member.getInventory().validateCapacityByItemId(ItemId)
                        && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                    return member;
                }
            } catch (Exception e) {
                // continue, take another member if this just logged off
            }
        }

        return null;
    }

    /**
     * get next item looter
     * @return
     */
    private L2PcInstance getActualLooter(L2PcInstance player, int ItemId, boolean spoil, L2Character target) {
        L2PcInstance looter = player;

        switch (_itemDistribution) {
            case ITEM_RANDOM:
                if (!spoil) {
                    looter = getCheckedRandomMember(ItemId, target);
                }
                break;
            case ITEM_RANDOM_SPOIL:
                looter = getCheckedRandomMember(ItemId, target);
                break;
            case ITEM_ORDER:
                if (!spoil) {
                    looter = getCheckedNextLooter(ItemId, target);
                }
                break;
            case ITEM_ORDER_SPOIL:
                looter = getCheckedNextLooter(ItemId, target);
                break;
        }

        if (looter == null) {
            looter = player;
        }
        return looter;
    }

    /**
     * true if player is party leader
     * @param player
     * @return
     */
    public boolean isLeader(L2PcInstance player) {
        return (getLeader().equals(player));
    }

    /**
     * Returns the Object ID for the party leader to be used as a unique identifier of this party
     * @return int
     */
    public int getPartyLeaderOID() {
        return getLeader().getObjectId();
    }

    /**
     * Broadcasts packet to every party member
     * @param msg
     */
    public void broadcastToPartyMembers(L2GameServerPacket msg) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member == null) {
                continue;
            }

            member.sendPacket(msg);
        }
    }

    public void broadcastHtmlToPartyMembers(String text) {
        NpcHtmlMessage html = NpcHtmlMessage.id(0);
        html.setHtml("<html><body>" + text + "<br></body></html>");
        broadcastToPartyMembers(html);
    }

    /**
     * Send a Server->Client packet to all other L2PcInstance of the Party.<BR><BR>
     */
    public void broadcastToPartyMembers(L2PcInstance player, L2GameServerPacket msg) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member != null && !member.equals(player)) {
                member.sendPacket(msg);
            }
        }
    }

    /**
     * adds new member to party
     * @param player
     */
    public void addPartyMember(L2PcInstance player) {
        player.sendPacket(new PartySmallWindowAll(this, player));
        player.sendPacket(SystemMessage.id(SystemMessageId.YOU_JOINED_S1_PARTY).addString(getLeader().getName()));
        broadcastToPartyMembers(SystemMessage.id(SystemMessageId.S1_JOINED_PARTY).addString(player.getName()));
        broadcastToPartyMembers(new PartySmallWindowAdd(player));

        synchronized (_members) {
            _members.add(player);
        }

        recalculatePartyLevel();

        for (L2PcInstance member : getPartyMembers()) {
            member.updateEffectIcons(true); // update party icons only
        }
        if (isInDimensionalRift()) {
            _dr.partyMemberInvited();
        }
    }

    /**
     * removes player from party
     * @param player
     */
    public void removePartyMember(L2PcInstance player) {
        removePartyMember(player, true);
    }

    public void removePartyMember(L2PcInstance player, boolean ingame) {
        if (player == null || !_members.contains(player)) {
            return;
        }

        boolean leader = isLeader(player);

        synchronized (_members) {
            _members.remove(player);
        }

        recalculatePartyLevel();

        if (player.isFestivalParticipant()) {
            SevenSignsFestival.getInstance().updateParticipants(player, this);
        }

        player.sendPacket(Static.YOU_LEFT_PARTY);
        player.sendPacket(Static.PartySmallWindowDeleteAll);
        player.setParty(null);
        if (player.hasExitPenalty()) {
            player.teleToClosestTown();
        }

        SystemMessage msg = null;
        if (ingame) {
            msg = SystemMessage.id(SystemMessageId.S1_LEFT_PARTY).addString(player.getName());
        } else {
            msg = SystemMessage.id(SystemMessageId.S1_S2).addString(player.getName() + " вышел из игры.");
        }

        broadcastToPartyMembers(msg);
        broadcastToPartyMembers(new PartySmallWindowDelete(player));

        if (leader) {
            if (ingame) {
                msg = null;
                destroyParty();
                broadcastToPartyMembers(Static.PARTY_DISPERSED);
                return;
            } else if (_members.size() > 1) {
                /*SystemMessage msg = SystemMessage.id(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER);
                msg.addString(_members.get(0).getName());
                broadcastToPartyMembers(msg);
                broadcastToPartyMembers(new PartySmallWindowUpdate(_members.get(0)));*/

                //обновляем инфо о пати у всех пати мемберов и сообщаем о новом лидере
                L2GameServerPacket del_pkt = new PartySmallWindowDeleteAll();
                msg = SystemMessage.id(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(_members.get(0).getName());
                for (L2PcInstance member : _members) {
                    member.sendPacket(msg);
                    member.sendPacket(del_pkt);
                    member.sendPacket(new PartySmallWindowAll(this, member));
                }
            }
        }

        if (_members.size() == 1) {
            L2PcInstance lastMember = _members.get(0);

            if (lastMember.getDuel() != null) {
                lastMember.getDuel().onRemoveFromParty(lastMember);
            }

            lastMember.setParty(null);
            lastMember.sendPacket(Static.PARTY_DISPERSED);
        }

        if (isInDimensionalRift()) {
            _dr.partyMemberExited(player);
        }

        if (player.getDuel() != null) {
            player.getDuel().onRemoveFromParty(player);
        }

        msg = null;
        /*//if (getPartyMembers().contains(player))
        //{
        //getPartyMembers().remove(player);
        recalculatePartyLevel();
        
        if (player.isFestivalParticipant())
        SevenSignsFestival.getInstance().updateParticipants(player, this);
        
        if(player.getDuel() != null)
        player.getDuel().onRemoveFromParty(player);
        
        try
        {
        if (player.getForceBuff() != null)
        player.abortCast();
        
        for (L2Character character : player.getKnownList().getKnownCharacters())
        if (character.getForceBuff() != null && character.getForceBuff().getTarget() == player)
        character.abortCast();
        }
        catch (Exception e){}
        
        player.sendPacket(Static.YOU_LEFT_PARTY);
        player.sendPacket(Static.PartySmallWindowDeleteAll);
        player.setParty(null);
        
        SystemMessage msg = SystemMessage.id(SystemMessageId.S1_LEFT_PARTY);
        msg.addString(player.getName());
        broadcastToPartyMembers(msg);
        broadcastToPartyMembers(new PartySmallWindowDelete(player));
        
        if (isInDimensionalRift())
        _dr.partyMemberExited(player);
        
        if (getPartyMembers().size() == 1)
        destroyParty();
        //}*/
    }

    /**
     * Change party leader (used for string arguments)
     * @param name
     */
    public void changePartyLeader(String name) {
        L2PcInstance new_leader = getPlayerByName(name);
        L2PcInstance current_leader = _members.get(0);

        if (new_leader == null || current_leader == null) {
            return;
        }

        if (current_leader.equals(new_leader)) {
            current_leader.sendPacket(Static.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF);
            return;
        }

        synchronized (_members) {
            if (!_members.contains(new_leader)) {
                current_leader.sendPacket(Static.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER);
                return;
            }

            //меняем местами нового и текущего лидера
            int idx = _members.indexOf(new_leader);
            _members.set(0, new_leader);
            _members.set(idx, current_leader);

            //обновляем инфо о пати у всех пати мемберов и сообщаем о новом лидере
            L2GameServerPacket del_pkt = new PartySmallWindowDeleteAll();
            SystemMessage msg = SystemMessage.id(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(name);
            for (L2PcInstance member : _members) {
                member.sendPacket(del_pkt);
                member.sendPacket(new PartySmallWindowAll(this, member));
                member.sendPacket(msg);
            }
            msg = null;
        }

        if (isInCommandChannel()) {
            _commandChannel.setChannelLeader(new_leader);
        }
        /*L2PcInstance new_leader = getPlayerByName(name);
        L2PcInstance current_leader = _members.get(0);
        
        if(new_leader == null || current_leader == null)
        return;
        
        if(current_leader.equals(new_leader))
        {
        current_leader.sendPacket(Static.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF);
        return;
        }
        
        synchronized (_members)
        {
        if(!_members.contains(new_leader))
        {
        current_leader.sendPacket(Static.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER);
        return;
        }
        
        //меняем местами нового и текущего лидера
        int idx = _members.indexOf(new_leader);
        _members.set(0, new_leader);
        _members.set(idx, current_leader);
        updateLeaderInfo();
        }
        
        if(isInCommandChannel())
        _commandChannel.setChannelLeader(new_leader);*/
    }

    /**
     * finds a player in the party by name
     * @param name
     * @return
     */
    private L2PcInstance getPlayerByName(String name) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member.getName().equals(name)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Oust player from party
     * @param player
     */
    public void oustPartyMember(L2PcInstance player) {
        if (!_members.contains(player)) {
            return;
        }

        if (isLeader(player)) {
            broadcastToPartyMembers(Static.PARTY_DISPERSED);
            destroyParty();
            /*removePartyMember(player);
            if(_members.size() > 1)
            {
            SystemMessage msg = SystemMessage.id(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER);
            msg.addString(_members.get(0).getName());
            broadcastToPartyMembers(msg);
            broadcastToPartyMembers(new PartySmallWindowUpdate(_members.get(0)));
            }
            else
            {
            broadcastToPartyMembers(Static.PARTY_DISPERSED);
            destroyParty();
            }*/
        } else {
            removePartyMember(player);
        }
        /*if(!_members.contains(player))
        return;
        
        if(isLeader(player))
        {
        removePartyMember(player);
        if(_members.size() > 0)
        {
        //SystemMessage msg = SystemMessage.id(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER);
        //msg.addString(_members.get(0).getName());
        //broadcastToPartyMembers(msg);
        //broadcastToPartyMembers(new PartySmallWindowUpdate(_members.get(0)));
        synchronized (_members)
        {
        //меняем местами нового и текущего лидера
        //int idx = _members.indexOf(new_leader);
        //_members.set(0, _members.get(0));
        //_members.set(idx, current_leader);
        //int idx = _members.indexOf(new_leader);
        //_members.set(0, new_leader);
        //_members.set(idx, current_leader);
        updateLeaderInfo();
        }
        }
        }
        else
        removePartyMember(player);*/
    }

    /**
     * Oust player from party
     * Overloaded method that takes player's name as parameter
     * @param name
     */
    public void oustPartyMember(String name) {
        oustPartyMember(getPlayerByName(name));
    }

    /**
     * dissolves entire party
     *
     */
    /*  [DEPRECATED]
    private void dissolveParty()
    {
    SystemMessage msg = SystemMessage.id(SystemMessageId.PARTY_DISPERSED);
    for(int i = 0; i < _members.size(); i++)
    {
    L2PcInstance temp = _members.get(i);
    temp.sendPacket(msg);
    temp.sendPacket(new PartySmallWindowDeleteAll());
    temp.setParty(null);
    }
    }
     */
    private void destroyParty() {
        if (getLeader() != null) {
            getLeader().setParty(null);
            if (getLeader().getDuel() != null) {
                getLeader().getDuel().onRemoveFromParty(getLeader());
            }
        }

        synchronized (_members) {
            for (L2PcInstance temp : _members) {
                if (temp == null) {
                    continue;
                }
                temp.sendPacket(new PartySmallWindowDeleteAll());
                temp.setParty(null);
                if (temp.hasExitPenalty()) {
                    temp.teleToClosestTown();
                }
            }
            _members.clear();
        }

        if (positionsUpdate != null) {
            positionsUpdate.cancel(true);
            positionsUpdate = null;
        }
        positions.clear();
    }

    /**
     * distribute item(s) to party members
     * @param player
     * @param item
     */
    public void distributeItem(L2PcInstance player, L2ItemInstance item) {
        if (item.getItemId() == 57) {
            distributeAdena(player, item.getCount(), player);
            ItemTable.getInstance().destroyItem("Party", item, player, null);
            return;
        }

        L2PcInstance target = getActualLooter(player, item.getItemId(), false, player);
        target.addItem("Party", item, player, true);

        // Send messages to other party members about reward
        SystemMessage msg = null;
        if (item.getCount() > 1) {
            msg = SystemMessage.id(SystemMessageId.S1_PICKED_UP_S2_S3).addString(target.getName()).addItemName(item.getItemId()).addNumber(item.getCount());
        } else {
            msg = SystemMessage.id(SystemMessageId.S1_PICKED_UP_S2).addString(target.getName()).addItemName(item.getItemId());
        }

        broadcastToPartyMembers(target, msg);
        msg = null;

        player.sendChanges();
        if (Config.LOG_ITEMS) {
            String act = Log.getTime() + "PICKUP " + item.getItemName() + "(" + item.getCount() + ")(" + item.getEnchantLevel() + ")(" + item.getObjectId() + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")" + "\n";
            Log.item(act, Log.PICKUP);
        }
    }

    /**
     * distribute item(s) to party members
     * @param player
     * @param item
     */
    public void distributeItem(L2PcInstance player, L2Attackable.RewardItem item, boolean spoil, L2Attackable target) {
        if (item == null) {
            return;
        }

        if (item.getItemId() == 57) {
            distributeAdena(player, item.getCount(), target);
            return;
        }

        L2PcInstance looter = getActualLooter(player, item.getItemId(), spoil, target);

        looter.addItem(spoil ? "Sweep" : "Party", item.getItemId(), item.getCount(), player, true);

        // Send messages to other aprty members about reward
        SystemMessage msg = null;
        if (item.getCount() > 1) {
            msg = spoil ? SystemMessage.id(SystemMessageId.S1_SWEEPED_UP_S2_S3) : SystemMessage.id(SystemMessageId.S1_PICKED_UP_S2_S3);
            msg.addString(looter.getName()).addItemName(item.getItemId()).addNumber(item.getCount());
        } else {
            msg = spoil ? SystemMessage.id(SystemMessageId.S1_SWEEPED_UP_S2) : SystemMessage.id(SystemMessageId.S1_PICKED_UP_S2);
            msg.addString(looter.getName()).addItemName(item.getItemId());
        }
        broadcastToPartyMembers(looter, msg);

        if (Config.LOG_ITEMS) {
            String act = Log.getTime() + "PICKUP(" + (spoil ? "Sweep" : "Party") + ") itemId: " + item.getItemId() + "(" + item.getCount() + ") #(player " + player.getName() + ", account: " + player.getAccountName() + ", ip: " + player.getIP() + ", hwid: " + player.getHWID() + ")" + "\n";
            Log.item(act, Log.PICKUP);
        }
    }

    /**
     * distribute adena to party members
     * @param adena
     */
    public void distributeAdena(L2PcInstance player, int adena, L2Character target) {
        // Get all the party members
        FastTable<L2PcInstance> membersList = getPartyMembers();

        // Check the number of party members that must be rewarded
        // (The party member must be in range to receive its reward)
        FastTable<L2PcInstance> ToReward = new FastTable<L2PcInstance>();
        for (L2PcInstance member : membersList) {
            if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true)) {
                continue;
            }
            ToReward.add(member);
        }

        // Avoid null exceptions, if any
        if (ToReward == null || ToReward.isEmpty()) {
            return;
        }

        // Now we can actually distribute the adena reward
        // (Total adena splitted by the number of party members that are in range and must be rewarded)
        int count = adena / ToReward.size();
        for (L2PcInstance member : ToReward) {
            member.addAdena("Party", count, player, true);
        }

        ToReward.clear();
        ToReward = null;
    }

    /**
     * Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Get the L2PcInstance owner of the L2SummonInstance (if necessary) </li>
     * <li>Calculate the Experience and SP reward distribution rate </li>
     * <li>Add Experience and SP to the L2PcInstance </li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR><BR>
     * Exception are L2PetInstances that leech from the owner's XP; they get the exp indirectly, via the owner's exp gain<BR>
     *
     * @param xpReward The Experience reward to distribute
     * @param spReward The SP reward to distribute
     * @param rewardedMembers The list of L2PcInstance to reward
     *
     */
    public void distributeXpAndSp(long xpReward, int spReward, FastTable<L2PlayableInstance> rewardedMembers, int topLvl) {
        L2SummonInstance summon = null;
        FastTable<L2PlayableInstance> validMembers = getValidMembers(rewardedMembers, topLvl);

        float penalty;
        double sqLevel;
        double preCalculation;

        xpReward *= getExpBonus(validMembers.size());
        spReward *= getSpBonus(validMembers.size());

        double sqLevelSum = 0;
        for (L2PlayableInstance character : validMembers) {
            sqLevelSum += (character.getLevel() * character.getLevel());
        }

        // Go through the L2PcInstances and L2PetInstances (not L2SummonInstances) that must be rewarded
        synchronized (rewardedMembers) {
            for (L2Character member : rewardedMembers) {
                if (member.isDead()) {
                    continue;
                }

                penalty = 0;

                // The L2SummonInstance penalty
                if (member.getPet() != null && member.getPet().isSummon()) {
                    summon = (L2SummonInstance) member.getPet();
                    penalty = summon.getExpPenalty();
                }
                // Pets that leech xp from the owner (like babypets) do not get rewarded directly
                if (member.isPet()) {
                    if (((L2PetInstance) member).getPetData().getOwnerExpTaken() > 0) {
                        continue;
                    } else // TODO: This is a temporary fix while correct pet xp in party is figured out
                    {
                        penalty = (float) 0.85;
                    }
                }


                // Calculate and add the EXP and SP reward to the member
                if (validMembers.contains(member)) {
                    sqLevel = member.getLevel() * member.getLevel();
                    preCalculation = (sqLevel / sqLevelSum) * (1 - penalty);

                    // Add the XP/SP points to the requested party member
                    if (!member.isDead()) {
                        member.addExpAndSp(Math.round(member.calcStat(Stats.EXPSP_RATE, xpReward * preCalculation, null, null)),
                                (int) member.calcStat(Stats.EXPSP_RATE, spReward * preCalculation, null, null));
                    }
                } else {
                    member.addExpAndSp(0, 0);
                }
            }
        }
    }

    /**
     * Calculates and gives final XP and SP rewards to the party member.<BR>
     * This method takes in consideration number of members, members' levels, rewarder's level and bonus modifier for the actual party.<BR><BR>
     *
     * @param member is the L2Character to be rewarded
     * @param xpReward is the total amount of XP to be "splited" and given to the member
     * @param spReward is the total amount of SP to be "splited" and given to the member
     * @param penalty is the penalty that must be applied to the XP rewards of the requested member
     */
    /**
     * refresh party level
     *
     */
    public void recalculatePartyLevel() {
        int newLevel = 0;
        for (L2PcInstance member : getPartyMembers()) {
            if (member == null) {
                continue;
            }

            if (member.getLevel() > newLevel) {
                newLevel = member.getLevel();
            }
        }
        _partyLvl = newLevel;
    }

    //телепорт всей пати
    public void teleTo(int x, int y, int z) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member == null) {
                continue;
            }

            member.teleToLocation(x, y, z);
        }
    }

    private FastTable<L2PlayableInstance> getValidMembers(FastTable<L2PlayableInstance> members, int topLvl) {
        FastTable<L2PlayableInstance> validMembers = new FastTable<L2PlayableInstance>();

//		Fixed LevelDiff cutoff point
        if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level")) {
            for (L2PlayableInstance member : members) {
                if (topLvl - member.getLevel() <= Config.PARTY_XP_CUTOFF_LEVEL) {
                    validMembers.add(member);
                }
            }
        } //		Fixed MinPercentage cutoff point
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage")) {
            int sqLevelSum = 0;
            for (L2PlayableInstance member : members) {
                sqLevelSum += (member.getLevel() * member.getLevel());
            }

            for (L2PlayableInstance member : members) {
                int sqLevel = member.getLevel() * member.getLevel();
                if (sqLevel * 100 >= sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT) {
                    validMembers.add(member);
                }
            }
        } //		Automatic cutoff method
        else if (Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto")) {
            int sqLevelSum = 0;
            for (L2PlayableInstance member : members) {
                sqLevelSum += (member.getLevel() * member.getLevel());
            }

            int i = members.size() - 1;
            if (i < 1) {
                return members;
            }
            if (i >= BONUS_EXP_SP.length) {
                i = BONUS_EXP_SP.length - 1;
            }

            for (L2PlayableInstance member : members) {
                int sqLevel = member.getLevel() * member.getLevel();
                if (sqLevel >= sqLevelSum * (1 - 1 / (1 + BONUS_EXP_SP[i] - BONUS_EXP_SP[i - 1]))) {
                    validMembers.add(member);
                }
            }
        }
        return validMembers;
    }

    private double getBaseExpSpBonus(int membersCount) {
        int i = membersCount - 1;
        if (i < 1) {
            return 1;
        }
        if (i >= BONUS_EXP_SP.length) {
            i = BONUS_EXP_SP.length - 1;
        }

        return BONUS_EXP_SP[i];
    }

    private double getExpBonus(int membersCount) {
        if (membersCount < 2) {
            //not is a valid party
            return getBaseExpSpBonus(membersCount);
        } else {
            return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP;
        }
    }

    private double getSpBonus(int membersCount) {
        if (membersCount < 2) {
            //not is a valid party
            return getBaseExpSpBonus(membersCount);
        } else {
            return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP;
        }
    }

    public int getLevel() {
        return _partyLvl;
    }

    public int getLootDistribution() {
        return _itemDistribution;
    }

    public boolean isInCommandChannel() {
        return _commandChannel != null;
    }

    public L2CommandChannel getCommandChannel() {
        return _commandChannel;
    }

    public void setCommandChannel(L2CommandChannel channel) {
        _commandChannel = channel;
    }

    public boolean isInDimensionalRift() {
        return _dr != null;
    }

    public void setDimensionalRift(DimensionalRift dr) {
        _dr = dr;
    }

    public DimensionalRift getDimensionalRift() {
        return _dr;
    }

    public L2PcInstance getLeader() {
        return getPartyMembers().get(0);
    }

    public void updateMembers() {
        L2GameServerPacket del_pkt = new PartySmallWindowDeleteAll();
        for (L2PcInstance member : _members) {
            if (member == null) {
                continue;
            }

            member.sendPacket(del_pkt);
            member.sendPacket(new PartySmallWindowAll(this, member));
        }
        del_pkt = null;
    }

    public void teleToEpic(int x, int y, int z, int x1, int y2, int z2) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member == null) {
                continue;
            }

            GrandBossManager.getInstance().getZone(x1, y2, z2).allowPlayerEntry(member, 3000000);
            member.teleToLocation(x, y, z);
        }
    }

    public void addEffect(int id, int level, int rnd) {
        for (L2PcInstance member : getPartyMembers()) {
            if (member == null) {
                continue;
            }

            if (Rnd.get(100) > rnd) {
                continue;
            }

            member.getEffect(id, level);
        }
    }
}
