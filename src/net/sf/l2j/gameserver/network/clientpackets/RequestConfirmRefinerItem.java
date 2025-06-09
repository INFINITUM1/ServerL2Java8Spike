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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExConfirmVariationRefiner;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Fromat(ch) dd
 * @author  -Wooden-
 */
public class RequestConfirmRefinerItem extends L2GameClientPacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
	}

	/**
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
		L2ItemInstance refinerItem = (L2ItemInstance)L2World.getInstance().findObject(_refinerItemObjId);

		if (targetItem == null || refinerItem == null) return;

		int itemGrade = targetItem.getItem().getItemGrade();
		int refinerItemId = refinerItem.getItem().getItemId();

		// is the item a life stone?
		if (refinerItemId < 8723 || refinerItemId > 8762)
		{
			player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
			return;
		}

		int gemstoneCount=0;
		int gemstoneItemId=0;
		int lifeStoneLevel = getLifeStoneLevel(refinerItemId);
		SystemMessage sm = SystemMessage.id(SystemMessageId.REQUIRES_S1_S2);
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				gemstoneCount = 20;
				gemstoneItemId = 2130;
				sm.addNumber(gemstoneCount).addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_B:
				if (lifeStoneLevel < 3)
				{
					player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
					return;
				}
				gemstoneCount = 30;
				gemstoneItemId = 2130;
				sm.addNumber(gemstoneCount).addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_A:
				if (lifeStoneLevel < 6)
				{
					player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
					return;
				}
				gemstoneCount = 20;
				gemstoneItemId = 2131;
				sm.addNumber(gemstoneCount).addString("Gemstone C");
				break;
			case L2Item.CRYSTAL_S:
				if (lifeStoneLevel != 10)
				{
					player.sendPacket(Static.THIS_IS_NOT_A_SUITABLE_ITEM);
					return;
				}
				gemstoneCount = 25;
				gemstoneItemId = 2131;
				sm.addNumber(gemstoneCount).addString("Gemstone C");
				break;
		}

		player.sendPacket(new ExConfirmVariationRefiner(_refinerItemObjId, refinerItemId, gemstoneItemId, gemstoneCount));
		player.sendPacket(sm);
		sm = null;
	}

	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if (itemId < 10) return 0; // normal grade
		if (itemId < 20) return 1; // mid grade
		if (itemId < 30) return 2; // high grade
		return 3; // top grade
	}

	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		return itemId;
	}
}
