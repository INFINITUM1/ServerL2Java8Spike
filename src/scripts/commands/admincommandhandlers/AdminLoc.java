package scripts.commands.admincommandhandlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.commands.IAdminCommandHandler;

public class AdminLoc implements IAdminCommandHandler
{
    private static final String[] ADMIN_COMMANDS = {"admin_logloc","admin_logloc_begin","admin_logloc_end","admin_logloc_minz","admin_logloc_maxz","admin_townis"};
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{		
		if (command.equals("admin_logloc"))
		{  
			int locX = activeChar.getX();
			int locY = activeChar.getY();
			int locZ = activeChar.getZ();
			writeLoc(activeChar, locX, locY, locZ);
		}
		else if (command.startsWith("admin_logloc_begin"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				String locName = st.nextToken();
				beginLoc(activeChar, locName);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.startsWith("admin_logloc_end"))
		{  
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				String closelocName = st.nextToken();
				closeLoc(activeChar, closelocName);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.equals("admin_logloc_minz"))
		{  
			int minZ = activeChar.getZ();
			minZwrite(activeChar, minZ);
		}
		else if (command.equals("admin_logloc_maxz"))
		{  
			int maxZ = activeChar.getZ();
			maxZwrite(activeChar, maxZ);
		}
		return true;
	}
	
	private void writeLoc(L2PcInstance activeChar, int posX, int posY, int posZ)
	{
		String name = activeChar.getName();
		new File("log/game").mkdirs();

		try
		{
			File file 		= new File("log/game/logloc.txt");
			FileWriter save = new FileWriter(file, true);
			String out = ""+name+": "+posX+" "+posY+" "+posZ+"\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			activeChar.sendAdmResultMessage(""+posX+" "+posY+" "+posZ+"");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void beginLoc(L2PcInstance activeChar, String nameLoc)
	{
		String name = activeChar.getName();
		new File("log/game").mkdirs();

		try
		{
			File file 		= new File("log/game/logloc.txt");
			FileWriter save = new FileWriter(file, true);
			String out = ""+name+": begin "+nameLoc+"{\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			activeChar.sendAdmResultMessage(""+name+": begin "+nameLoc+"");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void closeLoc(L2PcInstance activeChar, String nameLoc)
	{
		String name = activeChar.getName();
		new File("log/game").mkdirs();

		try
		{
			File file 		= new File("log/game/logloc.txt");
			FileWriter save = new FileWriter(file, true);
			String out = "}"+name+": close "+nameLoc+"\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			activeChar.sendAdmResultMessage(""+name+": close "+nameLoc+"");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void minZwrite(L2PcInstance activeChar, int Zmin)
	{
		String name = activeChar.getName();
		new File("log/game").mkdirs();

		try
		{
			File file 		= new File("log/game/logloc.txt");
			FileWriter save = new FileWriter(file, true);
			String out = ""+name+": minZ:"+Zmin+"\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			activeChar.sendAdmResultMessage("MinZ: "+Zmin+"");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void maxZwrite(L2PcInstance activeChar, int Zmax)
	{
		String name = activeChar.getName();
		new File("log/game").mkdirs();

		try
		{
			File file 		= new File("log/game/logloc.txt");
			FileWriter save = new FileWriter(file, true);
			String out = ""+name+": maxZ:"+Zmax+"\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			activeChar.sendAdmResultMessage("MaxZ: "+Zmax+"");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
}
