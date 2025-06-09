package net.sf.l2j.gameserver.model.actor.instance;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

/**
 *
 * @author foxtrot
 */
public class L2PcNpcInstance extends L2PcInstance {

    private Bypasser bp;

    private static class Bypasser {

        private L2NpcInstance npc;

        public void bypass(L2PcInstance player, String command) {
        }

        public Bypasser(L2NpcInstance npc, L2PcNpcInstance pc) {
            this.npc = npc;
        }

        public void onAction(L2PcInstance target, L2PcNpcInstance self) {
            if (!npc.canTarget(target)) {
                return;
            }

            // Check if the L2PcInstance already target the L2NpcInstance
            if (self != target.getTarget()) {
                //if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());

                // Set the target of the L2PcInstance target
                target.setTarget(self);
                target.sendPacket(new MyTargetSelected(self.getObjectId(), 0));
                if (npc.getTemplate().getEventQuests(Quest.QuestEventType.ONFOCUS) != null) {
                    for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ONFOCUS)) {
                        quest.notifyFocus(npc, target);
                    }
                }

                // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
                target.sendPacket(new ValidateLocation(npc));
            } else {
                //player.sendPacket(new ValidateLocation(this));
                // Check if the target is attackable (without a forced attack) and isn't dead

                // Calculate the distance between the L2PcInstance and the L2NpcInstance
                if (!Util.checkIfInRange(150, target, self, false)) {
                    // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                    target.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, npc);
                } else {
                    // Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
                    // to display a social action of the L2NpcInstance on their client
                    npc.broadcastPacket(new SocialAction(npc.getObjectId(), Rnd.get(8)));

                    // Open a chat window on client with the text of the L2NpcInstance
                    if (npc.isEventMob) {
                        L2Event.showEventHtml(target, String.valueOf(npc.getObjectId()));
                    } else {
                        Quest[] qlst = npc.getTemplate().getEventQuests(Quest.QuestEventType.NPC_FIRST_TALK);
                        if ((qlst != null) && qlst.length == 1) {
                            qlst[0].notifyFirstTalk(npc, target);
                        } else {
                            npc.showChatWindow(target, 0);
                        }
                    }
                }
                target.sendActionFailed();
            }
        }
    }

    private static class BypasserDefault extends Bypasser {

        private L2NpcInstance npc;

        public BypasserDefault(L2NpcInstance npc, L2PcNpcInstance pc) {
            super(npc, pc);
            npc.setXYZ(pc.getX(), pc.getY(), pc.getZ());
            this.npc = npc;
            L2World.getInstance().storeObject(npc);
        }

        @Override
        public void bypass(L2PcInstance player, String command) {
            npc.onBypassFeedback(player, command);
        }

        @Override
        public void onAction(L2PcInstance player, L2PcNpcInstance pc) {
            super.onAction(player, pc);
        }
    }

    private L2PcNpcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app, boolean aggro) {
        super(objectId, template);
        setPcAppearance(app);
    }

    private L2PcNpcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app, boolean aggro, boolean summon) {
        super(objectId, template);
        setPcAppearance(app);
    }

    public static L2PcNpcInstance restorePcNpc(int objectId, int classId, boolean hero, boolean male, L2NpcTemplate _template, int npcId) {
        L2PcNpcInstance player;
        int activeClassId = Rnd.get(89, 112);
        if (classId > 0) {
            activeClassId = classId;
        }

        final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
        byte abc = (byte) Rnd.get(3);
        PcAppearance app = new PcAppearance(abc, abc, abc, male);

        player = new L2PcNpcInstance(objectId, template, "fake_qwerty", app, true);

        player.setNpcId(npcId);
        player.setFantome(true);
        player.getStat().setSp(2147483647);
        long pXp = player.getExp();
        long tXp = Experience.LEVEL[80];
        player.addExpAndSp(tXp - pXp, 0);
        player.setHeading(Rnd.get(1, 65535));

        player.setHero(hero);

        player.setCurrentHp(player.getMaxHp());
        player.setCurrentCp(player.getMaxCp());
        player.setCurrentMp(player.getMaxMp());

        player._classIndex = 0;
        try {
            player.setBaseClass(2);
        } catch (Exception e) {
            player.setBaseClass(activeClassId);
        }

        player._activeClass = activeClassId;

        try {
            player.stopAllTimers();
        } catch (Throwable t) {
            _log.log(Level.SEVERE, "deleteMe()", t);
        }
        player.getStatus().stopHpMpRegeneration();

        Constructor<?> _constructor = null;
        try {
            Class[] parameters = {int.class, Class.forName("net.sf.l2j.gameserver.templates.L2NpcTemplate")};
            _constructor = Class.forName("net.sf.l2j.gameserver.model.actor.instance." + _template.type + "Instance").getConstructor(parameters);
        } catch (Exception ex) {
            _log.log(Level.WARNING, "Class not found", ex);
        }
        Object[] parameters = {IdFactory.getInstance().getNextId(), _template};
        try {
            Object tmp = _constructor.newInstance(parameters);
            player.setBypass(new BypasserDefault((L2NpcInstance) tmp, player));
        } catch (Exception e) {
            _log.log(Level.WARNING, "NPC " + _template.npcId + " class not found", e);
        }
        return player;
    }

    @Override
    public void onAction(L2PcInstance player) {
        bp.onAction(player, this);
    }

    private void setBypass(Bypasser bp) {
        this.bp = bp;
    }

    /*
     * @Override public boolean isPlayer() { return false; }
     */
    @Override
    public boolean isPcNpc() {
        return true;
    }

    @Override
    public boolean isRealPlayer() {
        return false;
    }

    @Override
    public boolean isShop() {
        return true;
    }
    private String _tittle;

    @Override
    public void setTitle(String tittle) {
        _tittle = tittle;
    }

    @Override
    public final String getTitle() {
        return _tittle;
    }
    private int _npcId;

    public void setNpcId(int npcId) {
        _npcId = npcId;
    }

    @Override
    public int getNpcId() {
        return _npcId;
    }

    @Override
    public L2NpcInstance getNpcShop() {
        return bp.npc;
    }
}
