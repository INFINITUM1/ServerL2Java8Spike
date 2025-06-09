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
package scripts.items.itemhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import scripts.items.IItemHandler;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
* This class ...
*
* @version $Revision: 1.1.6.4 $ $Date: 2005/04/06 18:25:18 $
*/

public class Scrolls implements IItemHandler
{
	private static final int[] ITEM_IDS = { 3926, 3927, 3928, 3929, 3930, 3931, 3932,
											3933, 3934, 3935, 4218, 5593, 5594, 5595, 6037,
											5703, 5803, 5804, 5805, 5806, 5807, // lucky charm
											8515, 8516, 8517, 8518, 8519, 8520, // charm of courage
											8594, 8595, 8596, 8597, 8598, 8599, // scrolls of recovery
											8954, 8955, 8956,                   // primeval crystal
											9146, 9147, 9148, 9149, 9150, 9151, 9152, 9153, 9154, 9155,
											6652, 6653, 6654, 6655, // valakas
											8192// стрела фринты
                                   		  };

	public void useItem(L2PlayableInstance playable, L2ItemInstance item, boolean ctrl)
   	{
		L2PcInstance activeChar;
		if (playable.isPlayer())
			activeChar = (L2PcInstance)playable;
		else if (playable.isPet())
			activeChar = ((L2PetInstance)playable).getOwner();
		else
			return;

		if (activeChar.isAllSkillsDisabled())
		{
			//activeChar.setNextScroll(item.getObjectId());
			activeChar.sendActionFailed();
			return;
		}

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(Static.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return;
		}

		int itemId = item.getItemId();

		if (itemId >= 8594 && itemId <= 8599) //Scrolls of recovery XML: 2286
		{
			if (activeChar.getKarma() > 0) return; // Chaotic can not use it

			if ((itemId == 8594 && activeChar.getExpertiseIndex()==0) || // Scroll: Recovery (No Grade)
       	        (itemId == 8595 && activeChar.getExpertiseIndex()==1) || // Scroll: Recovery (D Grade)
       	        (itemId == 8596 && activeChar.getExpertiseIndex()==2) || // Scroll: Recovery (C Grade)
       	        (itemId == 8597 && activeChar.getExpertiseIndex()==3) || // Scroll: Recovery (B Grade)
       	        (itemId == 8598 && activeChar.getExpertiseIndex()==4) || // Scroll: Recovery (A Grade)
       	        (itemId == 8599 && activeChar.getExpertiseIndex()==5))   // Scroll: Recovery (S Grade)
       	 	{
				if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;
				showAnim(activeChar, 2286, 1, 1, 0);
				activeChar.reduceDeathPenaltyBuffLevel();
				useScroll(activeChar, 2286, itemId - 8593);
       	 	}
		   	else
		   		activeChar.sendPacket(Static.INCOMPATIBLE_ITEM_GRADE);
          	return;
		}
		else if (itemId == 5703 || itemId >= 5803 && itemId <= 5807)
		{
			if ((itemId == 5703 && activeChar.getExpertiseIndex()==0) ||     // Lucky Charm (No Grade)
					(itemId == 5803 && activeChar.getExpertiseIndex()==1) || // Lucky Charm (D Grade)
					(itemId == 5804 && activeChar.getExpertiseIndex()==2) || // Lucky Charm (C Grade)
					(itemId == 5805 && activeChar.getExpertiseIndex()==3) || // Lucky Charm (B Grade)
					(itemId == 5806 && activeChar.getExpertiseIndex()==4) || // Lucky Charm (A Grade)
					(itemId == 5807 && activeChar.getExpertiseIndex()==5))   // Lucky Charm (S Grade)
			{
				if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;
				showAnim(activeChar, 2168, activeChar.getExpertiseIndex()+1, 1, 0);
				useScroll(activeChar, 2168, activeChar.getExpertiseIndex()+1);
				activeChar.setCharmOfLuck(true);
			}
			else
				activeChar.sendPacket(Static.INCOMPATIBLE_ITEM_GRADE);
			return;
		}
	   	else if (itemId >= 8515 && itemId <= 8520) // Charm of Courage XML: 5041
	   	{
	   		if ((itemId == 8515 && activeChar.getExpertiseIndex()==0) || // Charm of Courage (No Grade)
			    (itemId == 8516 && activeChar.getExpertiseIndex()==1) || // Charm of Courage (D Grade)
       	        (itemId == 8517 && activeChar.getExpertiseIndex()==2) || // Charm of Courage (C Grade)
       	        (itemId == 8518 && activeChar.getExpertiseIndex()==3) || // Charm of Courage (B Grade)
       	        (itemId == 8519 && activeChar.getExpertiseIndex()==4) || // Charm of Courage (A Grade)
       	        (itemId == 8520 && activeChar.getExpertiseIndex()==5))   // Charm of Courage (S Grade)
       	  	{
	   			if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
	   				return;
      		 	showAnim(activeChar, 5041, 1, 1, 0);
      		  	useScroll(activeChar, 5041, 1);
      		  	activeChar.setCharmOfCourage(true);
       	  	}
	   		else
	   			activeChar.sendPacket(Static.INCOMPATIBLE_ITEM_GRADE);
	   		return;
	   	}
	   	else if (itemId >= 8954 && itemId <= 8956)
	   	{
	   		if (activeChar.getLevel() < 76) return;
	   		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
				return;
	   		switch(itemId)
			{
				case 8954: // Blue Primeval Crystal XML: 2306
					showAnim(activeChar, 2306, 1, 1, 0);
					activeChar.addExpAndSp(0, 50000);
					break;
				case 8955: // Green Primeval Crystal XML: 2306
					showAnim(activeChar, 2306, 2, 1, 0);
					activeChar.addExpAndSp(0, 100000);
					break;
				case 8956: // Red Primeval Crystal XML: 2306
					showAnim(activeChar, 2306, 3, 1, 0);
					activeChar.addExpAndSp(0, 200000);
					break;
				default:
					break;
			}
	   		return;
	   	}

		// for the rest, there are no extra conditions
		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;

		switch(itemId)
		{
			case 3926: // Scroll of Guidance XML:2050
				showAnim(activeChar, 2050, 1, 1, 0);
         		useScroll(activeChar, 2050, 1);
         		break;
         	case 3927: // Scroll of Death Whipser XML:2051
         		showAnim(activeChar, 2051, 1, 1, 0);
         		useScroll(activeChar, 2051, 1);
         		break;
         	case 3928: // Scroll of Focus XML:2052
         		showAnim(activeChar, 2052, 1, 1, 0);
         		useScroll(activeChar, 2052, 1);
         		break;
         	case 3929: // Scroll of Greater Acumen XML:2053
         		showAnim(activeChar, 2053, 1, 1, 0);
         		useScroll(activeChar, 2053, 1);
         		break;
         	case 3930: // Scroll of Haste XML:2054
         		showAnim(activeChar, 2054, 1, 1, 0);
         		useScroll(activeChar, 2054, 1);
         		break;
         	case 3931: // Scroll of Agility XML:2055
         		showAnim(activeChar, 2055, 1, 1, 0);
         		useScroll(activeChar, 2055, 1);
         		break;
         	case 3932: // Scroll of Mystic Enpower XML:2056
         		showAnim(activeChar, 2056, 1, 1, 0);
         		useScroll(activeChar, 2056, 1);
         		break;
         	case 3933: // Scroll of Might XML:2057
         		showAnim(activeChar, 2057, 1, 1, 0);
         		useScroll(activeChar, 2057, 1);
         		break;
         	case 3934: // Scroll of Wind Walk XML:2058
         		showAnim(activeChar, 2058, 1, 1, 0);
         		useScroll(activeChar, 2058, 1);
         		break;
         	case 3935: // Scroll of Shield XML:2059
         		showAnim(activeChar, 2059, 1, 1, 0);
         		useScroll(activeChar, 2059, 1);
         		break;
         	case 4218: // Scroll of Mana Regeneration XML:2064
         		showAnim(activeChar, 2064, 1, 1, 0);
         		useScroll(activeChar, 2064, 1);
         		break;
         	case 5593: // SP Scroll Low Grade XML:2167
         		showAnim(activeChar, 2167, 1, 1, 0);
         		activeChar.addExpAndSp(0, 500);
         		break;
         	case 5594: // SP Scroll Medium Grade XML:2167
         		showAnim(activeChar, 2167, 1, 1, 0);
         		activeChar.addExpAndSp(0, 5000);
         		break;
         	case 5595: // SP Scroll High Grade XML:2167
         		showAnim(activeChar, 2167, 1, 1, 0);
         		activeChar.addExpAndSp(0, 100000);
         		break;
         	case 6037: // Scroll of Waking XML:2170
         		showAnim(activeChar, 2170, 1, 1, 0);
         		useScroll(activeChar, 2170, 1);
         		break;
         	case 9146: // Scroll of Guidance - For Event XML:2050
         		showAnim(activeChar, 2050, 1, 1, 0);
         		useScroll(activeChar, 2050, 1);
         		break;
         	case 9147: // Scroll of Death Whipser - For Event XML:2051
         		showAnim(activeChar, 2051, 1, 1, 0);
         		useScroll(activeChar, 2051, 1);
         		break;
         	case 9148: // Scroll of Focus - For Event XML:2052
         		showAnim(activeChar, 2052, 1, 1, 0);
         		useScroll(activeChar, 2052, 1);
         		break;
         	case 9149: // Scroll of Acumen - For Event XML:2053
         		showAnim(activeChar, 2053, 1, 1, 0);
         		useScroll(activeChar, 2053, 1);
         		break;
         	case 9150: // Scroll of Haste - For Event XML:2054
         		showAnim(activeChar, 2054, 1, 1, 0);
         		useScroll(activeChar, 2054, 1);
         		break;
         	case 9151: // Scroll of Agility - For Event XML:2055
         		showAnim(activeChar, 2055, 1, 1, 0);
         		useScroll(activeChar, 2055, 1);
         		break;
         	case 9152: // Scroll of Enpower - For Event XML:2056
         		showAnim(activeChar, 2056, 1, 1, 0);
         		useScroll(activeChar, 2056, 1);
         		break;
         	case 9153: // Scroll of Might - For Event XML:2057
         		showAnim(activeChar, 2057, 1, 1, 0);
         		useScroll(activeChar, 2057, 1);
         		break;
         	case 9154: // Scroll of Wind Walk - For Event XML:2058
         		showAnim(activeChar, 2058, 1, 1, 0);
         		useScroll(activeChar, 2058, 1);
         		break;
         	case 9155: // Scroll of Shield - For Event XML:2059
         		showAnim(activeChar, 2059, 1, 1, 0);
         		useScroll(activeChar, 2059, 1);
         		break;
			// VALAKAS AMULETS
			case 6652: // Amulet Protection of Valakas
         		showAnim(activeChar, 2057, 1, 1000, 0);
				useScroll(activeChar, 2231, 1);
				break;
			case 6653: // Amulet Flames of Valakas
         		showAnim(activeChar, 2057, 1, 1000, 0);
				useScroll(activeChar, 2233, 1);
				break;
			case 6654: // Amulet Flames of Valakas
         		showAnim(activeChar, 2057, 1, 1000, 0);
				useScroll(activeChar, 2233, 1);
				break;
			case 6655: // Amulet Slay Valakas
         		showAnim(activeChar, 2057, 1, 1000, 0);
				useScroll(activeChar, 2232, 1);
				break;
			case 8192: // breaking_arrow
				useScroll(activeChar, 2234, 1);
				break;
         	default:
         		break;
		}
   	}
	
	private void showAnim(L2PcInstance cha, int skillId, int skillLevel, int hitTime, int reuseDelay)
	{
        cha.broadcastPacket(new MagicSkillUser(cha, cha, skillId, skillLevel, hitTime, reuseDelay));
	}

	public void useScroll(L2PcInstance activeChar, int magicId,int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId,level);
		if (skill != null)
			activeChar.doCast(skill);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
