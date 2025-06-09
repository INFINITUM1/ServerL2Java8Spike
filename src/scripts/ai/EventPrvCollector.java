package scripts.ai;

import javolution.util.FastList;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;

public class EventPrvCollector extends L2NpcInstance
{
	private static String htmPath = "data/html/events/";
	private FastList<Location> _points = new FastList<Location>();
	
	public EventPrvCollector(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
    @Override
	public void onSpawn()
	{
		super.onSpawn();
		if (getSpawn() != null)
			return;
		
		_points.add(new Location(-82220, 241607, -3728));
		_points.add(new Location(47424, 51784, -2992));
		_points.add(new Location(7634, 18001, -4376));
		_points.add(new Location(-46536, -117242, -240));
		_points.add(new Location(116597, -184242, -1560));
		_points.add(new Location(90501, 147230, -3528));
		_points.add(new Location(-83003, 149232, -3112));
		_points.add(new Location(-14688, 121121, -2984));
		_points.add(new Location(18877, 142497, -3048));
		_points.add(new Location(80907, 53103, -1560));
		_points.add(new Location(118322, 74090, -2376));
		_points.add(new Location(152018, 25312, -2128));
		_points.add(new Location(115733, 219367, -3624));
		_points.add(new Location(18517, 170288, -3496));
		_points.add(new Location(147596, -59044, -2979));
		_points.add(new Location(41284, -52018, -853));
		_points.add(new Location(-46536, -117242, -240));
		ThreadPoolManager.getInstance().scheduleAi(new Teleport(1), 600000, false);
	}
	
	private class Teleport implements Runnable
	{
		int action;
		Teleport(int action)
		{
			this.action = action;
		}

		public void run()
		{		
			switch(action)
			{
				case 1:
					broadcastPacket(new CreatureSay(getObjectId(), 0, getName(), "“ра-л€-л€! —егодн€ € собираюсь в еще одно увлекательное путешествие! ¬ этот раз мне попадетс€ что-нибудь удивительное..."));
					ThreadPoolManager.getInstance().scheduleAi(new Teleport(2), 5000, false);
					break;
				case 2:
					Location loc = _points.get(Rnd.get(_points.size() -1));
					teleToLocation(loc.x, loc.y, loc.z, false);
					ThreadPoolManager.getInstance().scheduleAi(new Teleport(1), 600000, false);
					break;
			}
		}
	}
}
