package net.sf.l2j.gameserver.model.entity.olympiad;

import net.sf.l2j.util.Location;

public class Stadia
{
	private boolean _freeToUse = true;
	private Location _tele1;
	private Location _tele2;

	public boolean isFreeToUse()
	{
		return _freeToUse;
	}

	public void setStadiaBusy()
	{
		_freeToUse = false;
	}

	public void setStadiaFree()
	{
		_freeToUse = true;
	}

	public void setTele1(Location tele1)
	{
		_tele1 = tele1;
	}

	public void setTele2(Location tele2)
	{
		_tele2 = tele2;
	}

	public Location getTele1()
	{
		return _tele1;
	}

	public Location getTele2()
	{
		return _tele2;
	}
}