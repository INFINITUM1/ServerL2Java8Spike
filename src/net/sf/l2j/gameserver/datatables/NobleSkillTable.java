package net.sf.l2j.gameserver.datatables;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.model.L2Skill;

/**
 *
 * @author -Nemesiss-
 */
public class NobleSkillTable
{
	private static NobleSkillTable _instance;
	private static FastTable<L2Skill> _ns;

	private NobleSkillTable()
	{
	}

	public static NobleSkillTable getInstance()
	{
		return _instance;
	}
	
	public static void init()
	{
		_instance = new NobleSkillTable();
		load();
	}
	
	private static void load()
	{
		_ns = new FastTable<L2Skill>();
		SkillTable sk = SkillTable.getInstance();
		_ns.add(sk.getInfo(1323, 1));
		_ns.add(sk.getInfo(325, 1));
		_ns.add(sk.getInfo(326, 1));
		_ns.add(sk.getInfo(327, 1));
		_ns.add(sk.getInfo(1324, 1));
		_ns.add(sk.getInfo(1325, 1));
		_ns.add(sk.getInfo(1326, 1));
		_ns.add(sk.getInfo(1327, 1));
	}

	public FastTable<L2Skill> getNobleSkills()
	{
		return _ns;
	}
}
