package net.sf.l2j.gameserver.model.entity;

import java.awt.Polygon;

public class EventTerritoryRound extends EventTerritory
{
	private int _minZ = 0;
	private int _maxZ = 0;
	private Polygon _p = null;

	public EventTerritoryRound()
	{
		super(0);
		_p = new Polygon();
	}

	public void addPoint(int x, int y)
	{
		_p.addPoint(x, y);
	}

	@Override
	public void setZ(int min, int max)
	{
		_minZ = min;
		_maxZ = max;
	}

	@Override
	public boolean contains(int x, int y, int z)
	{
		if (z < _minZ || z > _maxZ)
			return false;
		
		return (_p.contains(x, y));
	}

	@Override
	public boolean intersectsLine(int x, int y, int z, int tx, int ty, int tz)
	{
		/*if (z < _minZ || z > _maxZ)
			return false;*/

		if ((z < _minZ || z > _maxZ) && (tz < _minZ || tz > _maxZ))
			return false;
		
		boolean point1 = contains(x, y, z);
		boolean point2 = contains(tx, ty, z);
		return (point1 != point2);
	}
}