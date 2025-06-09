
package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.Static;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.log.AbstractLogger;
import net.sf.l2j.util.Rnd;
import scripts.script.DateRange;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.1.2.7 $ $Date: 2005/03/29 23:15:14 $
 */
public class Announcements
{
	private static Logger _log = AbstractLogger.getLogger(Announcements.class.getName());

	private static Announcements _ains; //_instance
	private FastList<String> _an_a = new FastList<String>(); // _announcements
	private List<List<Object>> _an_b = new FastList<List<Object>>(); // _an_b
    public static FastMap<Integer, FastList<String>> _autoAn = new FastMap<Integer, FastList<String>>().shared("Announcements._autoAn");
	private static final int _an_delay = Config.AUTO_ANNOUNCE_DELAY * 60000;

	public Announcements()
	{
	}

	public static Announcements getInstance()
	{
		return _ains;
	}
	
	public static void init()
	{
		_ains = new Announcements();
		_ains.load();
	}

	public void load()
	{
		//_an_a.clear();
		//_autoAn.clear();
		
		File file = new File(Config.DATAPACK_ROOT, "data/announcements.txt");
		if (file.exists())
			readFromDisk(file);
		else
			_log.config("data/announcements.txt doesn't exist");
		_log.config("Announcements: Loaded " + _an_a.size() + " Announcements.");
		
		if (Config.AUTO_ANNOUNCE_ALLOW)
		{		
			readAnFromDisk();
			if (_autoAn.size() > 1)			
				ThreadPoolManager.getInstance().scheduleGeneral(new DoAnnounce(), _an_delay);
				
			_log.config("Announcements: Loaded " + _autoAn.size() + " Auto Announcements.");
		}
	}

	public void showAnnouncements(L2PcInstance activeChar)
	{
		for (FastList.Node<String> n = _an_a.head(), end = _an_a.tail(); (n = n.getNext()) != end;)  
			activeChar.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, activeChar.getName(), n.getValue()));
	}
	
	public void showWarnings(L2PcInstance activeChar)
	{
		activeChar.sendPacket(Static.AdmWarnings);
	}

	public void addEventAnnouncement(DateRange validDateRange, String[] msg)
	{
	    List<Object> entry = new FastList<Object>();
	    entry.add(validDateRange);
	    entry.add(msg);
	    _an_b.add(entry);
	}

	public void listAnnouncements(L2PcInstance activeChar)
	{
        String content = HtmCache.getInstance().getHtmForce("data/html/admin/announce.htm");
        NpcHtmlMessage adminReply = NpcHtmlMessage.id(5);
        adminReply.setHtml(content);
        TextBuilder replyMSG = new TextBuilder("<br>");
		for (FastList.Node<String> n = _an_a.head(), end = _an_a.tail(); (n = n.getNext()) != end;)
		{
			String value = n.getValue();
			replyMSG.append("<table width=260><tr><td width=220>" + value + "</td><td width=40>");
			replyMSG.append("<button value=\"Delete\" action=\"bypass -h admin_del_announcement " + _an_a.indexOf(value) + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		}
        adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void addAnnouncement(String text)
	{
		_an_a.add(text);
		saveToDisk();
	}

	public void delAnnouncement(int line)
	{
		_an_a.remove(line);
		saveToDisk();
	}

	private void readFromDisk(File file)
	{
		LineNumberReader lnr = null;
		try
		{
			int i=0;
			String line = null;
			lnr = new LineNumberReader(new FileReader(file));
			while ( (line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line,"\n\r");
				if (st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_an_a.add(announcement);

					i++;
				}
			}
		}
		catch (IOException e1)
		{
			_log.log(Level.SEVERE, "Error reading announcements", e1);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch (Exception e2)
			{
				// nothing
			}
		}
	}
	
	private void readAnFromDisk()
	{	
		try
		{
			File file = new File(Config.DATAPACK_ROOT, "data/auto_announcements.xml");
			if (!file.exists())
			{
				_log.config("data/auto_announcements.xml doesn't exist");
				return;
			}
				
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			Document doc = factory.newDocumentBuilder().parse(file);
			
			//FastList<String> strings = new FastList<String>();
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("announce".equalsIgnoreCase(d.getNodeName()))
						{
							FastList<String> strings = new FastList<String>();//strings.clear();
							NamedNodeMap attrs = d.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							//int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue()); может быть потом
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("str".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									String srt = attrs.getNamedItem("text").getNodeValue();
									strings.add(srt);
								}
							}
							_autoAn.put(id, strings);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error reading data/auto_announcements.xml", e);
		}
		/***
		<announce id='11013'>
			<str text='123'/>
			<str text='234'/>
		</announce>
		FastMap<Integer, FastList<String>> _autoAn = new FastMap<Integer, FastList<String>>().shared();
		*/
	}
	
	static class DoAnnounce implements Runnable
	{
		DoAnnounce()
		{
		}

		public void run()
		{			
			try
			{
				int idx = Rnd.get((_autoAn.size() - 1));
				//System.out.println("####" + idx);
				FastList<String> strings = _autoAn.get(idx);
				for (L2PcInstance player : L2World.getInstance().getAllPlayers())
				{
					for (FastList.Node<String> n = strings.head(), end = strings.tail(); (n = n.getNext()) != end;)
						player.sendPacket(new CreatureSay(0, Say2.ANNOUNCEMENT, player.getName(), n.getValue()));
				}
				//System.out.println("####1 " + _autoAn.size());
				//_autoAn.remove(idx);
				//System.out.println("####2 " + _autoAn.size());
				//_autoAn.recycle(_autoAn);
				//System.out.println("####3 " + _autoAn.size());
			}
			catch(Exception e)
			{}	
			//System.out.println("####4 " + _autoAn.size());
			//if(_autoAn.size() == 0)
			//	_ains.readAnFromDisk();
			
			ThreadPoolManager.getInstance().scheduleGeneral(new DoAnnounce(), _an_delay);
		}
	}

	private void saveToDisk()
	{
		File file = new File("data/announcements.txt");
		FileWriter save = null;
		try
		{
			save = new FileWriter(file);
			for (FastList.Node<String> n = _an_a.head(), end = _an_a.tail(); (n = n.getNext()) != end;)
			{
				save.write(n.getValue());
				save.write("\r\n");
			}
		}
		catch (IOException e)
		{
			_log.warning("saving the announcements file has failed: " + e);
		}
		finally
		{
			try { save.flush(); } catch (Exception e1) { }
			try { save.close(); } catch (Exception e1) { }
		}
	}

	public void announceToAll(String text) 
	{
		CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", text);

		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(cs);
	}
	public void announceToAll(SystemMessage sm) 
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			player.sendPacket(sm);
	}

	// Method fo handling announcements from admin
	public void handleAnnounce(String command, int lengthToTrim)
	{
		try
		{
			// Announce string to everyone on server
			String text = command.substring(lengthToTrim);
			Announcements.getInstance().announceToAll(text);
		}

		// No body cares!
		catch (StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
		}
	}
}
