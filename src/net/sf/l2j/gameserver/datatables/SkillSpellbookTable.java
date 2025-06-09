
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javolution.util.FastMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class SkillSpellbookTable
{
	private static Logger _log = AbstractLogger.getLogger(SkillTreeTable.class.getName());
	private static SkillSpellbookTable _instance;

	private static FastMap<Integer, Integer> _skillSpellbooks;

	public static SkillSpellbookTable getInstance()
	{
        if (_instance == null)
            _instance = new SkillSpellbookTable();

		return _instance;
	}

	private SkillSpellbookTable()
	{
		_skillSpellbooks = new FastMap<Integer, Integer>();
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT skill_id, item_id FROM skill_spellbooks");
			rs = st.executeQuery();
			rs.setFetchSize(50);

			while (rs.next())
				_skillSpellbooks.put(rs.getInt("skill_id") , rs.getInt("item_id"));

			_log.config("Loading SkillSpellbookTable... total " + _skillSpellbooks.size() + " Spellbooks.");
		}
		catch (Exception e)
		{
			_log.warning("Error while loading spellbook data: " +  e);
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
	}

    public int getBookForSkill(int skillId)
    {
        if (!_skillSpellbooks.containsKey(skillId))
            return -1;

        return _skillSpellbooks.get(skillId);
    }

    public int getBookForSkill(L2Skill skill)
    {
        return getBookForSkill(skill.getId());
    }
}
