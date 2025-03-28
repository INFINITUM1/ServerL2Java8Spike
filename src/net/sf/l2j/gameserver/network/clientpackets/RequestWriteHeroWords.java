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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.util.TimeLogger;
import net.sf.l2j.util.Log;
/**
 * Format chS
 * c (id) 0xD0
 * h (subid) 0x0C
 * S the hero's words :)
 * @author -Wooden-
 *
 */
public final class RequestWriteHeroWords extends L2GameClientPacket
{
	private String _text;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_text = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
			
		if (!player.isHero())
			return;
			
		if(!player.isGM() && System.currentTimeMillis() - player.gCPBH() < 5000)
		{
			player.sendPacket(Static.HERO_DELAY);
			return;
		}
		player.sCPBH();
			
        if (_text.length() > Config.MAX_CHAT_LENGTH) 
     	 	_text = _text.substring(0, Config.MAX_CHAT_LENGTH); 
                
		_text = _text.replaceAll("\n", "");
		_text = _text.replace("\n", "");
        _text = _text.replace("n\\", "");
        _text = _text.replace("\r", "");
        _text = _text.replace("r\\", "");

		_text = ltrim(_text);
		_text = rtrim(_text);
		_text = itrim(_text);
		_text = lrtrim(_text);
		
		if (_text.isEmpty())
			return;
			
        if (Config.USE_CHAT_FILTER && !_text.startsWith("."))
        {
			String[] badwords = 
			{
				"хуй",
				"куй",
				"пизд",
				"бл€т",
				"бл€д",
				"шлюх",
				"хуе",
				"хуи",
				"ху€",
				"писд",
				"ебат",
				"ебла",
				"ебал",
				"ебут",
				"ебл",
				"пидо",
				"пида",
				"ганд",
				"eba",
				"ebu",
				"гнида",
				"залупа",
				"мудил",
				"муда",
				"выеб"
			};
				
			String wordn = "";
			String wordf = "";
			String newTextt = "";
			String delims = "[ ]+";
			String[] tokens = _text.split(delims);
			for (int i = 0; i < tokens.length; i++)
			{	
				wordn = tokens[i];
				String word = wordn.toLowerCase();
				word = word.replace("a","а");
				word = word.replace("c","с");
				word = word.replace("s","с");
				word = word.replace("e","е");
				word = word.replace("k","к");
				word = word.replace("m","м");
				word = word.replace("o","о");
				word = word.replace("0","о");
				word = word.replace("x","х");
				word = word.replace("uy","уй");
				word = word.replace("y","у");
				word = word.replace("u","у");
				word = word.replace("Є","е");
				word = word.replace("9","€");
				word = word.replace("3","з");
				word = word.replace("z","з");
				word = word.replace("d","д");
				word = word.replace("p","п");
				word = word.replace("i","и");
				word = word.replace("ya","€");
				word = word.replace("ja","€");
				for(String pattern : badwords)
				{
					if(word.matches(".*" + pattern + ".*"))
					{
						newTextt = word.replace(pattern, "-_-");
						break;
					}
					else
						newTextt = wordn;
				}
				wordf += newTextt + " ";
			}
			_text = wordf.replace("null","");
        }
		
		CreatureSay cs = new CreatureSay(player.getObjectId(), 17, player.getName(), _text);		
		for (L2PcInstance pchar : L2World.getInstance().getAllPlayers())
			if (!BlockList.isBlocked(pchar, player))
				pchar.sendPacket(cs);
							
		Log.add(TimeLogger.getTime() + player.getName() +": " + _text, "hero_chat");	
	}

    public static String ltrim(String source) 
	{
        return source.replaceAll("^\\s+", "");
    }

    public static String rtrim(String source) 
	{
        return source.replaceAll("\\s+$", "");
    }

    public static String itrim(String source) 
	{
        return source.replaceAll("\\b\\s{2,}\\b", " ");
    }


    public static String trim(String source) 
	{
        return itrim(ltrim(rtrim(source)));
    }

    public static String lrtrim(String source){
        return ltrim(rtrim(source));
    }

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "C.WriteHeroWords";
	}

}