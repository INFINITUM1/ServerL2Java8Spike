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
package scripts.items.itemhandlers;

import net.sf.l2j.gameserver.RecipeController;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.5.2.5 $ $Date: 2005/04/06 16:13:51 $
 */

public class Recipes implements IItemHandler
{
    private final int[] ITEM_IDS;

    public Recipes()
    {
        RecipeController rc = RecipeController.getInstance();
        ITEM_IDS = new int[rc.getRecipesCount()];
        for (int i = 0; i < rc.getRecipesCount(); i++)
        {
        	ITEM_IDS[i] = rc.getRecipeList(i).getRecipeId();
        }
    }

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
	{
		if (!(playable.isPlayer()))
			return;
		L2PcInstance activeChar = (L2PcInstance)playable;
        L2RecipeList rp = RecipeController.getInstance().getRecipeByItemId(item.getItemId());
     	if (activeChar.hasRecipeList(rp.getId()))
     		activeChar.sendPacket(Static.RECIPE_ALREADY_REGISTERED);
        else
        {
        	if (rp.isDwarvenRecipe())
        	{
        		if (activeChar.hasDwarvenCraft())
        		{
					if (rp.getLevel()>activeChar.getDwarvenCraft())
					{
					//can't add recipe, becouse create item level too low
						activeChar.sendPacket(Static.CREATE_LVL_TOO_LOW_TO_REGISTER);
					}
					else if (activeChar.getDwarvenRecipeBook().length >= activeChar.getDwarfRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						activeChar.sendPacket(SystemMessage.id(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getDwarfRecipeLimit()));
					}
					else
					{
						activeChar.registerDwarvenRecipeList(rp);
						activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_BEEN_ADDED).addString(item.getItem().getName()));
					}
        		}
        		else
        			activeChar.sendPacket(Static.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
        	}
        	else
        	{
        		if (activeChar.hasCommonCraft())
        		{
					if (rp.getLevel()>activeChar.getCommonCraft())
					{
					//can't add recipe, becouse create item level too low
						activeChar.sendPacket(Static.CREATE_LVL_TOO_LOW_TO_REGISTER);
					}
					else if (activeChar.getCommonRecipeBook().length >= activeChar.getCommonRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						activeChar.sendPacket(SystemMessage.id(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.getCommonRecipeLimit()));
					}
					else
					{
						activeChar.registerCommonRecipeList(rp);
						activeChar.sendPacket(SystemMessage.id(SystemMessageId.S1_HAS_BEEN_ADDED).addString(item.getItem().getName()));
					}
        		}
        		else
        			activeChar.sendPacket(Static.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
        	}
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
        }
    }

    public int[] getItemIds()
    {
        return ITEM_IDS;
    }
}
