
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.BeginRotation;
import net.sf.l2j.gameserver.network.serverpackets.StopRotation;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.util.Rnd;

/**
 * @author decad
 *
 * Implementation of the Bluff Effect
 */
final class EffectBluff extends L2Effect {

    public EffectBluff(Env env, EffectTemplate template)
    {
        super(env, template);
    }

    @Override
	public EffectType getEffectType()
    {
        return EffectType.BLUFF; //test for bluff effect
    }

    /** Notify started */
    @Override
	public void onStart()
    {
    	if(getEffected().isRaid())
			return;

		getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		getEffected().startStunning();

		/* жалобы от игроков
		boolean eff = true;
		// разворот и сбивка таргета
		if (Rnd.get(100) < (getEffected().calcStat(Stats.SLEEP_VULN, getEffected().getTemplate().baseSleepVuln, getEffected(), null) * 100))
		{
			getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
			getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
			getEffected().setHeading(getEffector().getHeading());
			if (Rnd.get(100) < 50)
				getEffected().setTarget(null);
		}

		//стун
		if (Rnd.get(100) < (getEffected().calcStat(Stats.STUN_VULN, getEffected().getTemplate().baseStunVuln, getEffected(), null) * 100))
		{
			eff = false;
			getEffected().startStunning();
		}

		if (eff)
			onActionTime();*/
    }

    @Override
	public void onExit()
    {
		getEffected().stopStunning(this);
    }

    @Override
	public boolean onActionTime()
    {
		onExit();
        return false;
    }
}
