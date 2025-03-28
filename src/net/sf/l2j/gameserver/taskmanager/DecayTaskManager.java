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
package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.util.log.AbstractLogger;
import scripts.ai.QueenAnt;

/**
 * @author la2
 * Lets drink to code!
 */
public class DecayTaskManager
{
    protected static final Logger _log = AbstractLogger.getLogger(DecayTaskManager.class.getName());
	protected Map<L2Character,Long> _decayTasks = new FastMap<L2Character,Long>().shared("DecayTaskManager._decayTasks");

    private static DecayTaskManager _instance;

    public DecayTaskManager()
    {
    }

	public static void init()
	{
		_instance = new DecayTaskManager();
		_instance.load();
	}
	
	private void load()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new DecayScheduler(),10000,5000);
	}
	
    public static DecayTaskManager getInstance()
    {
        return _instance;
    }

    public void addDecayTask(L2Character actor)
    {
        _decayTasks.put(actor,System.currentTimeMillis());
    }

    public void addDecayTask(L2Character actor, int interval)
    {
        _decayTasks.put(actor,System.currentTimeMillis()+interval);
    }

    public void cancelDecayTask(L2Character actor)
    {
    	try
    	{
    		_decayTasks.remove(actor);
    	}
    	catch(NoSuchElementException e){}
    }

    private class DecayScheduler implements Runnable
    {
    	protected DecayScheduler()
    	{
    		// Do nothing
    	}

        public void run()
        {
            Long current = System.currentTimeMillis();
            int delay;
            try
            {
            	if (_decayTasks != null)
            		for(L2Character actor : _decayTasks.keySet())
            		{
            			if(actor instanceof QueenAnt) 
							delay = 300000;
						else 
							delay = 8500;
            			if((current - _decayTasks.get(actor)) > delay)
            			{
            				actor.onDecay();
            				_decayTasks.remove(actor);
            			}
            		}
            } catch (Throwable e) {
				// TODO: Find out the reason for exception. Unless caught here, mob decay would stop.
            	_log.warning(e.toString());
			}
        }
    }

    @Override
	public String toString()
    {
        StringBuffer ret = new StringBuffer("============= DecayTask Manager Report ============\r\n");
        ret.append("Tasks count: "+_decayTasks.size()+"\r\n");
        ret.append("Tasks dump:\r\n");

        Long current = System.currentTimeMillis();
        for( L2Character actor : _decayTasks.keySet())
        {
            ret.append("Class/Name: "+actor.getClass().getSimpleName()+"/"+actor.getName()+" decay timer: "+(current - _decayTasks.get(actor))+"\r\n");
        }

        return ret.toString();
    }
}
