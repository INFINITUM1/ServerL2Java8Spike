package net.sf.l2j.gameserver.util;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;

public class PeaceZone {

    private static final PeaceZone _instance = new PeaceZone();
    private static final ZoneManager _zm = ZoneManager.getInstance();

    private PeaceZone() {
        //
    }

    public static PeaceZone getInstance() {
        return _instance;
    }

    public boolean inPeace(L2Object attacker, L2Object attacked) {
        if (attacker.isMonster() && (attacked != null && attacked.isMonster())) {
            return true;
        }

        if (attacker.isMonster() || attacker.isL2Npc()) {
            return false;
        }

        if (attacked != null) {
            if (attacked.isMonster()) {
                return false;
            }
            if (attacked.isPcNpc()) {
                return true;
            }
            if (attacked.isL2Npc()) {
                return !Config.ALLOW_HIT_NPC;
            }
        }

        if (attacker.isPlayer() || attacker.isL2Summon()) {
            L2PcInstance player = attacker.getPlayer();
            if (player.isEventWait()) {
                return true;
            }

            if (attacked != null && (attacked.isPlayer() || attacked.isL2Summon())) {
                L2PcInstance target = attacked.getPlayer();
                if (target.isTeleporting()) {
                    return true;
                }

                if (target.isProtected()) {
                    return true;
                }

                if (player.isInsideZone(16)
                        && target.isInsideZone(16)) {
                    if (target.getClan() != null
                            && target.getClan().isMember(player.getObjectId())
                            && target.getKarma() > 0) {
                        return false;
                    }
                }

                if (player.isInPVPArena()
                        && target.isInPVPArena()) {
                    return false;
                }

                if (_zm.inSafe(attacked)) {
                    return true;
                }

                if (target.isInOlympiadMode()
                        && !target.isOlympiadStart()) {
                    return true;
                }

                if (target.isEventWait()) {
                    return true;
                }

                if (Config.PROTECT_GRADE_PVP
                        && player.getExpertiseIndex() != target.getExpertiseIndex()) {
                    return true;
                }

                if (Config.TVT_EVENT_ENABLED
                        && Config.TVT_TEAM_PROTECT && TvTEvent.isInSameTeam(player.getObjectId(), target.getObjectId())) {
                    return true;
                }
            }

            if (player.isInPVPArena()) {
                return false;
            }

            if (player.isInOlympiadMode()
                    && !player.isOlympiadStart()) {
                return true;
            }

            if (player.isEventWait()) {
                return true;
            }
        }

        return _zm.inSafe(attacker);
    }

    public boolean inPeace(L2Object object) {
        return inPeace(object, null);
    }

    public boolean outGate(L2Object attacker, L2Object attacked, int skillId) {
        if (skillId == 1403) {
            return false;
        }

        return attacker.isPlayer()
                && attacked.isPlayer()
                && _zm.inSafe(attacker)
                && !_zm.inSafe(attacked);
    }
    /*public boolean inPeace(L2Object attacker, L2Object attacked)
     {
     if (attacker.isMonster() || attacker.isL2Npc())
     return false;
    
     if (attacked != null && (attacked.isMonster() || attacked.isL2Npc()))
     return false;
    
     if (attacker.isPlayer() || attacker instanceof L2Summon)
     {
     L2PcInstance player;
     if (attacker instanceof L2Summon)
     player=((L2Summon)attacker).getOwner();
     else
     player=(L2PcInstance)attacker;
    
     if(player.isInOlympiadMode() && !player.isOlympiadStart())
     return true;
    
     if (player.inEventBattle())
     return false;
    
     if (attacked != null && (attacked.isPlayer() || attacked instanceof L2Summon))
     {
     L2PcInstance target;
     if (attacked instanceof L2Summon)
     target=((L2Summon)attacked).getOwner();
     else
     target=(L2PcInstance)attacked;
    
     if (player.isInsideZone(16) && target.isInsideZone(16))
     if (target.getClan() != null && target.getClan().isMember(player.getObjectId()) && target.getKarma() > 0)
     return false;
    
     if(target.isInOlympiadMode() && !target.isOlympiadStart())
     return true;
    
     if (target.inEventBattle())
     return false;
     }
     }
    
     if (_zm.inSafe(attacker) || _zm.inSafe(attacked))
     return true;
    
     return false;
     }*/
}
