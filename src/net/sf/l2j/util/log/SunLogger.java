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

public class SunLogger extends DefaultLogger
{
	public SunLogger(String name)
	{		
		super(name);
	}

	@Override
	public Logger get(String name)
	{
		return getLogger(name);
	}
}