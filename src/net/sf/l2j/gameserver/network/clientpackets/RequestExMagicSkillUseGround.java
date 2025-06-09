
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Location;

/**
 * Fromat:(ch) dddddc
 * @author  -Wooden-
 */
public final class RequestExMagicSkillUseGround extends L2GameClientPacket
{
    private static Logger _log = Logger.getLogger(RequestExMagicSkillUseGround.class.getName());
    
    private int _x;
    private int _y;
    private int _z;
    private int _skillId;
    private boolean _ctrlPressed;
    private boolean _shiftPressed;
    
    @Override
    protected void readImpl()
    {
        _x = readD();
        _y = readD();
        _z = readD();
        _skillId = readD();
        _ctrlPressed = readD() != 0;
        _shiftPressed = readC() != 0;
    }
    
    /**
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
        // Get the current L2PcInstance of the player
        L2PcInstance player = getClient().getActiveChar();
        if (player == null)
            return;
			
		if (player.isInZonePeace() || !player.isInsideRadius(_x, _y, _z, 1000, false, false))
		{
			player.sendActionFailed();
			return;
		}
			
        // Get the level of the used skill
        int level = player.getSkillLevel(_skillId);
        if (level <= 0) 
        {
            player.sendActionFailed();
            return;
        }
        
        // Get the L2Skill template corresponding to the skillID received from the client
        L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
        
		if (skill != null)
		{
			// Check if all casting conditions are completed
			if (skill.isBattleForceSkill() || skill.isSpellForceSkill())
			{
				if (skill.checkForceCondition(player, _skillId))
				{
					player.setGroundSkillLoc(null);
					Location _loc = new Location(_x, _y, _z);
					player.setGroundSkillLoc(_loc);
					player.useMagic(skill, _ctrlPressed, _shiftPressed);
				}
				else
				{
					player.sendMessage("Недостаточно силы");
					player.sendActionFailed();
					return;
				}
			}
			else if (skill.checkCondition(player, player, false))
			{
				// player.stopMove();
				player.useMagic(skill, _ctrlPressed, _shiftPressed);
			}
			else
				player.sendActionFailed();
		}
		else
			player.sendActionFailed();
    }
}