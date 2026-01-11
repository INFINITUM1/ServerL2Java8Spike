package net.sf.l2j.gameserver.model.base;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoPlaySettingsHolder {
	private final AtomicInteger _options = new AtomicInteger();
	private final AtomicBoolean _pickup = new AtomicBoolean(true);
	private final AtomicInteger _nextTargetMode = new AtomicInteger(1);
	private final AtomicBoolean _shortRange = new AtomicBoolean();
	private final AtomicBoolean _respectfulHunting = new AtomicBoolean(true);
	private final AtomicInteger _autoPotionPercent = new AtomicInteger(50);
	
	public AutoPlaySettingsHolder()
	{
	}
	
	public int getOptions()
	{
		return _options.get();
	}
	
	public void setOptions(int options)
	{
		_options.set(options);
	}
	
	public boolean doPickup()
	{
		return _pickup.get();
	}
	
	public void setPickup(boolean value)
	{
		_pickup.set(value);
	}
	
	public int getNextTargetMode()
	{
		return _nextTargetMode.get();
	}
	
	public void setNextTargetMode(int nextTargetMode)
	{
		_nextTargetMode.set(nextTargetMode);
	}
	
	public boolean isShortRange()
	{
		return _shortRange.get();
	}
	
	public void setShortRange(boolean value)
	{
		_shortRange.set(value);
	}
	
	public boolean isRespectfulHunting()
	{
		return _respectfulHunting.get();
	}
	
	public void setRespectfulHunting(boolean value)
	{
		_respectfulHunting.set(value);
	}
	
	public int getAutoPotionPercent()
	{
		return _autoPotionPercent.get();
	}
	
	public void setAutoPotionPercent(int value)
	{
		_autoPotionPercent.set(value);
	}
}
