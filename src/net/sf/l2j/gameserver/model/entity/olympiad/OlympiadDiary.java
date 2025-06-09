package net.sf.l2j.gameserver.model.entity.olympiad;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastTable;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.CustomServerData;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

public class OlympiadDiary
{
	private static Logger _log = AbstractLogger.getLogger(OlympiadDiary.class.getName());
	
	private static FastMap<Integer, ClassInfo> _diaries = new FastMap<Integer, ClassInfo>().shared("OlympiadDiary._diaries"); // класс_ид, инфо о текущем герое в этом классе
	private static SimpleDateFormat datef = new SimpleDateFormat("'Год:' yyyy 'Месяц:' M 'День:' d 'Время:' H:m");
	
	private static class ClassInfo
	{
		public int leader;
		public String name;
		FastMap<Integer, FastTable<Record>> pages = new FastMap<Integer, FastTable<Record>>().shared("OlympiadDiary.pages"); // страница, записи
		//public int _charId;
		
		public ClassInfo(int leader, String name)
		{
			this.leader = leader;
			this.name = name;
		}
		
		public void putRecords(FastMap<Integer, FastTable<Record>> pages)
		{
			if (pages == null || pages.isEmpty())
				return;
			
			this.pages.putAll(pages);
		}
		
		public void updateRecords(int page, FastTable<Record> records)
		{
			if (records.isEmpty())
				return;
			
			this.pages.put(page, records);
		}
		
		public FastTable<Record> getPage(int page)
		{
			return pages.get(page);
		}
		
		public int getPages()
		{
			return pages.size();
		}
	}
	
	private static class Record
	{
		public String date;
		public String action;
		
		public Record(String date, String action)
		{
			this.date = date;
			this.action = action;
		}
	}
	
	public static void open()
	{
		_diaries.clear();
	}
	
	public static void clear()
	{
		for (FastMap.Entry<Integer, ClassInfo> e = _diaries.head(), end = _diaries.tail(); (e = e.getNext()) != end;) 
		{
			Integer key = e.getKey();
			ClassInfo value = e.getValue();
			if (key == null || value == null)
				continue;

			value = null;
		}
	}
	
	public static void close()
	{
        _log.info("Hero System: Loaded " + _diaries.size() + " Diaries.");
	}
	
	public static void write(int charId)
	{
		String charName = CustomServerData.getInstance().getCharName(charId);
		if (charName.equalsIgnoreCase("n?f"))
			return;
		
		ClassInfo info = new ClassInfo(charId, charName);
		Connect con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			int classId = -1;
			con = L2DatabaseFactory.get();
			con.setTransactionIsolation(1);
			st = con.prepareStatement("SELECT class_id, char_id, page, records FROM olympiad_diaries WHERE char_id = ?");
            st.setInt(1, charId);
			rs = st.executeQuery();
			rs.setFetchSize(50);
			
			FastTable<Record> recs = new FastTable<Record>();
			FastMap<Integer, FastTable<Record>> recPages = new FastMap<Integer, FastTable<Record>>();
			while (rs.next())
			{
				recs.clear();
				recPages.clear();
				
				classId = rs.getInt("class_id");
				int page = rs.getInt("page");
				String records = rs.getString("records");
				
				String[] frecs = records.split(";");
                for (String rec : frecs)
                {
					if (rec.equals(""))
						continue;
						
                    String[] recrd = rec.split(",");
					
                    String date = recrd[0];
					String action = recrd[1];
                    if (date == null || action == null)
						continue;
						
					recs.add(new Record(date, action));
                }
				recPages.put(page, recs);
				info.putRecords(recPages);
			}
			if (classId != -1)
				_diaries.put(classId, info);
		}
		catch (SQLException e)
		{
			_log.warning("OlympiadDiary: Could not load olympiad_diaries table.");
			e.getMessage();
		}
		finally
		{
			Close.CSR(con, st, rs);
		}
	}
	
	public static void addRecord(L2PcInstance player, String rec)
	{
		if (!Hero.getInstance().isHero(player.getObjectId()))
			return;
		
		ClassInfo info = _diaries.get(player.getClassId().getId());
		if (info != null)
		{
			int freePage = 0;
			int leader = info.leader;
			if (player.getObjectId() != leader)
				return;
			
			if (!info.name.equalsIgnoreCase(player.getName()))
				info.name = player.getName();
			
			FastTable<Record> records = null;//new FastTable<Record>;
			FastMap<Integer, FastTable<Record>> pages = info.pages;
			if (pages.size() > 1)
			{
				freePage = pages.size() - 1;
				records = pages.get(freePage);
			}
			else
				records = pages.get(0);
			
			if (records.size() > 15)
			{
				freePage += 1;
				records = new FastTable<Record>();
			}
			
			records.add(new Record(datef.format(new Date()).toString(), rec));

			info.updateRecords(freePage, records);
			
			TextBuilder tb = new TextBuilder();
			for (int i = 0, n = records.size(); i < n; i++) 
			{
				Record rc = records.get(i);
				tb.append(rc.date + "," + rc.action + ";");
			}
			rec = tb.toString();
			updateDatabase(player.getClassId().getId(), leader, freePage, rec);
			tb.clear();
			tb = null;
		}
		else
			putNewHero(rec, player, 0);
	}
	
	private static void putNewHero(String rec, L2PcInstance player, int page)
	{
		FastTable<Record> nRecs = new FastTable<Record>();
		FastMap<Integer, FastTable<Record>> nRecPages = new FastMap<Integer, FastTable<Record>>();
				
		Date date = new Date();
		Record rc = new Record(datef.format(date).toString(), rec);
		rec = rc.date + "," + rc.action + ";";
		nRecs.add(rc);
		nRecPages.put(page, nRecs);
		
		ClassInfo info = new ClassInfo(player.getObjectId(), player.getName());
		info.putRecords(nRecPages);
		
		_diaries.put(player.getClassId().getId(), info);
		updateDatabase(player.getClassId().getId(), player.getObjectId(), 0, rec);
	}
	
	private static void updateDatabase(int classId, int charId, int page, String record)
	{
		Connect con = null;
		PreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.get();
			st = con.prepareStatement("REPLACE INTO `olympiad_diaries` (`class_id`, `char_id`, `page`, `records`) VALUES (?,?,?,?)");
			st.setInt(1, classId);
			st.setInt(2, charId);
			st.setInt(3, page);
			st.setString(4, record);
			st.execute();
		}
		catch(final SQLException e)
		{
			_log.warning("OlyDiary: updateDatabase() error: " + e);
		}
		finally
		{
			Close.CS(con, st);
		}
	}

	public static void show(L2PcInstance player, String query) // _diary?class=88&page=1
	{
		int classId = 0;
		int page = 0;
		try
		{
			String[] cmd = query.split("&");
			classId = Integer.parseInt(cmd[0]);
			page = Integer.parseInt(cmd[1].substring(5));
			page -= 1;
		}
		catch(Exception e)
		{
			return;
		}
			
		ClassInfo info = _diaries.get(player.getClassId().getId());
		if (info != null)
		{
			NpcHtmlMessage reply = NpcHtmlMessage.id(0);
			TextBuilder replyMSG = new TextBuilder("<html><body>");
		
			FastTable<Record> records = info.getPage(page);
			if (records == null || records.isEmpty())
				return;
			
			if (!info.name.equalsIgnoreCase(player.getName()))
				info.name = player.getName();
			
			replyMSG.append("<table width=280><tr><td>Дневник героя " + info.name + "<br></td></tr>");
			//for (int i = 0, n = records.size(); i < n; i++) 
			for (int i = (records.size() -1); i > -1; i--)
			{
				Record rec = records.get(i);
				if (rec == null)
					continue;
				
				replyMSG.append("<tr><td><font color=LEVEL>" + rec.date + "</font><br1>" + rec.action + "<br></td></tr>");
			}
			replyMSG.append("</table><br>");
			if (info.getPages() > 1)
			{
				for (int p = 0, k = info.getPages(); p < k; p++)
				{
					if (p == page)
						replyMSG.append(" " + p + "&nbsp;");
					else
						replyMSG.append(" <a action=\"bypass -h _diary?class=" + classId + "&page= " + p + "\">").append(p).append("</a>&nbsp;");
				}
			}
			replyMSG.append("</body></html>");
			reply.setHtml(replyMSG.toString());
			player.sendPacket(reply);
		}
	}
	
	/*
CREATE TABLE `olympiad_diaries` (
  `class_id` smallint(3) NOT NULL default '0',
  `page` smallint(2) NOT NULL default '0',
  `char_id` int(10) unsigned NOT NULL default '0',
  `records` varchar(2000) NOT NULL default ' ',
  `active` smallint(2) NOT NULL default '1',
  PRIMARY KEY  (`class_id`,`page`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
	*/
}
