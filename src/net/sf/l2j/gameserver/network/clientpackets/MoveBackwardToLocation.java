package net.sf.l2j.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Location;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.4.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class MoveBackwardToLocation extends L2GameClientPacket {
    //private static Logger _log = Logger.getLogger(MoveBackwardToLocation.class.getName());
    // cdddddd

    private int _targetX;
    private int _targetY;
    private int _targetZ;
    private int _moveMovement;
    private int _originX;
    private int _originY;
    private int _originZ;

    @Override
    protected void readImpl() {
        _targetX = readD();
        _targetY = readD();
        _targetZ = readD();
        _originX = readD();
        _originY = readD();
        _originZ = readD();
        try {
            _moveMovement = readD(); // is 0 if cursor keys are used  1 if mouse is used
        } catch (BufferUnderflowException e) {
            // ignore for now
            /*if (Config.KICK_L2WALKER) {
             L2PcInstance player = getClient().getActiveChar();
             //player.sendPacket(SystemMessageId.HACKING_TOOL);
             player.kick();
             //Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " trying to use l2walker!", IllegalPlayerAction.PUNISH_KICK);
             }*/
        }
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.gCPBJ() < 111) {
            player.sendActionFailed();
            return;
        }
        player.sCPBJ();

        if (_moveMovement == 0)// || (!player.isInWater() && Math.abs(player.getZ() - _targetZ) > 3000))// && Config.GEODATA < 1) // cursor movement without geodata is disabled
        {
            player.sendActionFailed();
            return;
        }

        //player.sendAdmResultMessage("##runImpl#" + _moveMovement);
        //double dx = player.getX() - _targetX;
        //double dy = player.getY() - _targetY;
        //double dy = Util.calculateDistance(player.getX(), player.getY(), 0, _targetX, _targetY);
        //player.sendMessage("#2#" + dy);
        // Can't move if character is confused, or trying to move a huge distance
        if (player.isOutOfControl() || player.isUltimate() || player.isParalyzed())// || dy < 15)// || ((dx*dx+dy*dy) > 98010000)) // 9900*9900
        {
            player.sendActionFailed();
            return;
        }

        if (player.isInBoat()) {
            player.setInBoat(false);
        }

        if (player.getTeleMode() > 0) {
            if (player.getTeleMode() == 1) {
                player.setTeleMode(0);
            }
            player.sendActionFailed();
            player.teleToLocation(_targetX, _targetY, _targetZ, false);
            return;
        }

        if (_targetZ > _originZ && Math.abs(_originZ - _targetZ) > 700) {
            if (player.isFlying() || player.isInWater()) {
                _targetZ += 700;
            } else if (Config.WALK_FAR) {
                _targetZ = GeoData.getInstance().getSpawnHeight(_targetX, _targetY, _targetZ, _targetZ, null);
            } else {
                player.sendActionFailed();
                return;
            }
        }

        /*
         * if (player.isAttackingNow() && player.getActiveWeaponItem() != null
         * && (player.getActiveWeaponItem().getItemType() == L2WeaponType.BOW))
         * { //player.abortAttack(); player.sendActionFailed(); return;
         }
         */
        //System.out.println("x:" + _targetX + ";y" + _targetY + ";z" + _targetZ);
        player.updateLastTeleport(false);
        //player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));//player.calcHeading(_targetX, _targetY)));
        //player.moveToLocation(_targetX, _targetY, _targetZ);
        //player.revalidateZone(true);
        //player.sendMessage("#1#" + player.getHeading());
        //if(player.getParty() != null)
        //	player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
        //player.moveToLocationm(_targetX, _targetY, _targetZ, 0);

        //ThreadPoolManager.getInstance().executePathfind(new StartMoveTask(player, new L2CharPosition(_targetX, _targetY, _targetZ, 0)));
        if (Config.NPC_DEWALKED_ZONE && ZoneManager.getInstance().inSafe(player)) {
            checkDeWalkedZone(ZoneManager.getCoordInDwZone(player.getX(), player.getY(), player.getZ(), _targetX, _targetY, _targetZ));
        }

        player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));
    }

    private void checkDeWalkedZone(Location npc_el) {
        if (npc_el == null) {
            return;
        }
        _targetX = npc_el.getX();
        _targetY = npc_el.getY();
        _targetZ = npc_el.getZ();
    }

    /*public static class StartMoveTask implements Runnable {
    
     private L2PcInstance _player;
     private L2CharPosition _loc;
    
     public StartMoveTask(L2PcInstance player, L2CharPosition loc) {
     _player = player;
     _loc = loc;
     }
    
     public void run() {
     _player.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, _loc);
     }
     }*/
}
