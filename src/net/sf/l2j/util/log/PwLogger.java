package net.sf.l2j.util.log;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.awt.*;
import javax.swing.*;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World; 

public class PwLogger extends DefaultLogger
{
	private static ConsoleFrame frame = null;
	private static PwLogger log;// = new PwLogger("pwLogger");

	private static String _startTime;
	private static long _memory;
	private static int _online = 0;
	private static int _traders = 0;

	private static boolean _loaded;
	
	public static PwLogger init()
	{
		log = new PwLogger("pwLogger");
		return log;
	}

	public PwLogger(String name)
	{		
		super(name);
		SimpleDateFormat datef = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		_startTime = datef.format(new Date()).toString();				
		
		Runtime r = Runtime.getRuntime();
		_memory = ((r.totalMemory() - r.freeMemory()) / 1024 / 1024);

		frame = new ConsoleFrame();
	}	

	static class RefreshInfo implements Runnable
	{
		RefreshInfo()
		{
		}

		public void run()
		{			
			try
			{
				if (_loaded)
				{
					_online = L2World.getInstance().getAllPlayersCount();
					_traders = L2World.getInstance().getAllOfflineCount();
				}

				_memory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
				refreshInfo();
			}
			catch (Throwable e){}
			ThreadPoolManager.getInstance().scheduleGeneral(new RefreshInfo(), 5000);
		}
	}

	public static void startRefresTask()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new RefreshInfo(), 5000);
	}

	public static void setLoaded()
	{
		_loaded = true;
	}

	@Override
	public Logger get(String name)
	{
		return log;
	}

    static class ConsoleFrame extends JFrame 
	{
		private static JTextPane logArea = null;
		private static JTextArea preText = null;
		private static JTextPane infoArea = null;
		private static JScrollPane areaScrollPane = null;
		public ConsoleFrame()
		{
			super("pwConsole");
			/*setSize(640, 480);
			logArea = new JTextArea();
			pane = new JScrollPane(logArea);
			getContentPane().add(pane);*/
            setSize(780, 480);
            //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            preText = new JTextArea();
            preText.setEditable(false);
            preText.setFocusable(true);

            /*logArea = new JTextPane();
            logArea.setContentType("text/html");
            logArea.setText(preText.getText());
            logArea.setLayout(new BoxLayout(logArea, BoxLayout.PAGE_AXIS));
            logArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Лог сервера"),
            BorderFactory.createEmptyBorder(10,10,10,10)));*/
            //logArea.setPreferredSize(new Dimension(600, 300));
            //logArea.setMinimumSize(new Dimension(600, 300));

            areaScrollPane = new JScrollPane(preText);
            //areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            areaScrollPane.setWheelScrollingEnabled(true);
            //areaScrollPane.setLayout(new BoxLayout(areaScrollPane, BoxLayout.PAGE_AXIS));
            areaScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Лог сервера"),
            BorderFactory.createEmptyBorder(1,1,1,1)));
            areaScrollPane.setPreferredSize(new Dimension(640, 480));
            areaScrollPane.setMinimumSize(new Dimension(640, 480));  
            getContentPane().add(areaScrollPane, BorderLayout.WEST);

			infoArea = new JTextPane();
			infoArea.setContentType("text/html"); 
			infoArea.setText("Время запуска: <br>" + _startTime + "<br>Онлайн: 0 <br>Торговцы: 0<br>Память: " + _memory);
			infoArea.setLayout(new BoxLayout(infoArea, BoxLayout.PAGE_AXIS));
			infoArea.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("Информация"),
			BorderFactory.createEmptyBorder(10,10,10,10)));
			infoArea.setPreferredSize(new Dimension(130, 480));
			infoArea.setMinimumSize(new Dimension(130, 480));
			getContentPane().add(infoArea, BorderLayout.CENTER);
            /*logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setFocusable(true);
            pane = new JScrollPane(logArea);
            pane.setPreferredSize(new Dimension(600, 300));
            pane.setMinimumSize(new Dimension(600, 300));
            pane.setWheelScrollingEnabled(true);
            pane.setAutoscrolls(true);
            getContentPane().add(pane, BorderLayout.WEST);*/

            /*infoArea = new JEditorPane();
            infoArea.setEditable(false);
            infoArea.setText("Время запуска: \n" + _startTime + "\nОнлайн: 0 \nТорговцы: 0\nПамять: \n" + _memory);
            infoArea.setPreferredSize(new Dimension(100, 300));
            infoArea.setMinimumSize(new Dimension(100, 300));
			getContentPane().add(infoArea, BorderLayout.CENTER);*/
			pack();
			setVisible(true);
		}

		public void validateConsoleText(String msg)
		{
			preText.append("" + PwLogger.getTime() + "" + msg + "\n");
			preText.setCaretPosition(preText.getText().length());
			//logArea.setText(preText.getText());
			getContentPane().validate();
			//areaScrollPane.scrollRectToVisible(new Rectangle(0, preText.getHeight()-2, 1, 1));
			//getContentPane().validate();
			//repaint();
            //show();
		}

		public void refreshInfo()
		{
            infoArea.setText("Время запуска: <br>" + _startTime + "<br>Онлайн: " + _online + " <br>Торговцы: " + _traders + "<br>Память: " + _memory);
			getContentPane().validate();
			//repaint();
            //show();
		}
	}

	public static void refreshInfo()
	{
		frame.refreshInfo();
	}

  public void config(String msg)
  {
	  frame.validateConsoleText(msg);
  }
  

  public void severe(String msg)
  {
	  frame.validateConsoleText(msg);
  }

  private static final String F_WARNING = "<font color=#e15b21>%msg%</font>";
  public void warning(String msg)
  {
	  //frame.validateConsoleText(F_WARNING.replaceAll("%msg%", msg));
	  frame.validateConsoleText(msg);
  }
  public void error(String msg)
  {
	  frame.validateConsoleText(msg);
  }

  public void info(String msg)
  {
	  frame.validateConsoleText(msg);
  }

  public void finest(String msg)
  {
	  //frame.validateConsoleText(msg);
  }

  public void fine(String msg)
  {
	  //frame.validateConsoleText(msg);
  }

  public void finer(String msg)
  {
	  //frame.validateConsoleText(msg);
  }
  
  /*public void log(LogRecord record)
  {
  }*/
  public void log(Level level, String msg)
  {
	  switch(level.intValue())
	  {
		case 1000:
			severe(msg);
			break;
		case 900:
			warning(msg);
			break;
		case 800:
			info(msg);
			break;
		case 700:
			config(msg);
			break;
		case 500:
			fine(msg);
			break;
		case 300:
			finest(msg);
			break;
	  }
  }
  
 /* public void log(Level level, String msg, Object param1)
  {
  }*/
  
  public void log(Level level, String msg, Throwable thrown)
  {
	  log(level, msg + " [" + thrown + "]");
  }
	
	public static String getTime()
	{
		Date date = new Date();
		SimpleDateFormat datef = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SS ");
		return datef.format(date).toString();
	}
}