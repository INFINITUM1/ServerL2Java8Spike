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

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestMagicSkillUse extends L2GameClientPacket {

    private int _magicId;
    private boolean _ctrlPressed;
    private boolean _shiftPressed;

    @Override
    protected void readImpl() {
        _magicId = readD();              // Identifier of the used skill
        _ctrlPressed = readD() != 0;         // True if it's a ForceAttack : Ctrl pressed
        _shiftPressed = readC() != 0;         // True if Shift pressed
    }

    @Override
    protected void runImpl() {
        // Get the current L2PcInstance of the player
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) {
            return;
        }

        if (System.currentTimeMillis() - player.getCPC() < 100) {
            player.sendActionFailed();
            return;
        }

        player.setCPC();

        if (player.isOutOfControl() || player.isAllSkillsDisabled()) {
            player.sendActionFailed();
            return;
        }

        if (player.isDead()) {
            player.sendActionFailed();
            return;
        }

        if (player.isFakeDeath()) {
            player.stopFakeDeath(null);
            return;
        }

        // Get the L2Skill template corresponding to the skillID received from the client
        L2Skill skill = SkillTable.getInstance().getInfo(_magicId, player.getSkillLevel(_magicId));
        // Check the validity of the skill
        if (skill == null) {
            player.sendActionFailed();
            return;
        }

        if (skill.isPassive() || skill.isChance()) {
            return;
        }

        // Check if the skill type is TOGGLE
        if (skill.isToggle() && player.getFirstEffect(_magicId) != null) {
            player.stopSkillEffects(_magicId);
            return;
        }

        try {
            // Check if all casting conditions are completed
            if (skill.isBattleForceSkill() || skill.isSpellForceSkill()) {
                player.setGroundSkillLoc(null);
                if (skill.checkForceCondition(player, _magicId)) {
                    player.useMagic(skill, _ctrlPressed, _shiftPressed);
                } else {
                    player.sendPacket(Static.NOT_ENOUGH_FORCES);
                    player.sendActionFailed();
                }
            } else if (skill.checkCondition(player, player, false)) {
                player.useMagic(skill, _ctrlPressed, _shiftPressed);
            } else {
                player.sendActionFailed();
            }
        } catch (Exception e) {
            player.sendActionFailed();
        }
    }
}
