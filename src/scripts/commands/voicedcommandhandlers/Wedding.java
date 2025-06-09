/* This program is free software; you can redistribute it and/or modify */
package scripts.commands.voicedcommandhandlers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUser;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import scripts.commands.IVoicedCommandHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Wedding implements IVoicedCommandHandler
{
    static final Log _log = LogFactory.getLog(Wedding.class);
    private static String[] _voicedCommands = { "divorce", "engage", "gotolove"};

    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
    {
        if(command.startsWith("gotolove"))
            return GoToLove(activeChar);
        return false;
    }

    public boolean GoToLove(L2PcInstance activeChar)
    {
        if(!activeChar.isMarried())
        {
			if (!activeChar.getAppearance().getSex())
				activeChar.sendMessage("Вы не женаты");
			else
				activeChar.sendMessage("Вы не замужем");
            return false;
        }

        if(activeChar.getPartnerId()==0)
        {
            activeChar.sendMessage("Упс, сообщите об этой ошибке Администратору");
            _log.error("Married but couldn't find parter for "+activeChar.getName());
            return false;
        }
		
        L2PcInstance partner;
        partner = (L2PcInstance)L2World.getInstance().findObject(activeChar.getPartnerId());
        if(partner == null)
        {
			activeChar.sendMessage("Ваш партнер не в игре");
            return false;
        }
		
		Castle castleac = CastleManager.getInstance().getCastle(activeChar);
		Castle castlepn = CastleManager.getInstance().getCastle(partner);
		
		String Sex = "";
		if (!partner.getAppearance().getSex())
			Sex = "Ваша жена";
		else
			Sex = "Ваш муж";
		
        if(partner.isInJail())
        {
			activeChar.sendMessage(""+Sex+" в тюрьме");
            return false;
        }
        else if(partner.isInOlympiadMode() || partner.getOlympiadSide() > 1)
        {
			activeChar.sendMessage(""+Sex+" на олимпиаде");
            return false;
        }
        else if(partner.atEvent)
        {
            activeChar.sendMessage(""+Sex+" на евенте");
            return false;
        }
        else  if (partner.isInDuel())
        {
            activeChar.sendMessage(""+Sex+" дуэлится");
            return false;
        }
        else if (partner.isFestivalParticipant())
        {
            activeChar.sendMessage(""+Sex+" на фестивале тьмы");
            return false;
        }
        else if (partner.isInParty() && partner.getParty().isInDimensionalRift())
        {
            activeChar.sendMessage(""+Sex+" в рифте");
            return false;
        }
        else if (partner.inObserverMode())
        {
        	activeChar.sendMessage(""+Sex+" наблюдает за олимпиадой");
            return false;
        }
       	if (partner.isEventWait())
       	{
			partner.sendMessage("Нельзя призывать на эвенте");
            return false;
        }
        else if(partner.getClan() != null
        		&& CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null
        		&& CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().getIsInProgress())
        {
        	activeChar.sendMessage(""+Sex+" на осаде");
        	return false;
        }
		else if (castlepn != null && castlepn.getSiege().getIsInProgress())
		{
			activeChar.sendMessage(""+Sex+" на осаде");
			return false;
		}

        else if(activeChar.isInJail())
        {
            activeChar.sendMessage("Вы в тюрьме");
            return false;
        }
        else if(activeChar.isInOlympiadMode() || activeChar.getOlympiadSide() > 1)
        {
            activeChar.sendMessage("Вы на олимпиаде");
            return false;
        }
        else if(activeChar.atEvent)
        {
            activeChar.sendMessage("Вы на евенте");
            return false;
        }
        else  if (activeChar.isInDuel())
        {
            activeChar.sendMessage("Дерись тряпка");
            return false;
        }
        else if (activeChar.inObserverMode())
        {
        	activeChar.sendMessage("Смотрите дальше вашу олимпиаду");
            return false;
        }
        else if(activeChar.getClan() != null
        		&& CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null
        		&& CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getSiege().getIsInProgress())
        {
        	activeChar.sendMessage("Вы на осаде");
        	return false;
        }
		else if (castleac != null && castleac.getSiege().getIsInProgress())
		{
			activeChar.sendMessage("Вы на осаде");
			return false;
		}
        else if (activeChar.isFestivalParticipant())
        {
            activeChar.sendMessage("Вы на фестивале");
            return false;
        }
        else if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
        {
            activeChar.sendMessage("Вы в рифте");
            return false;
        }
        // Thanks nbd
        else if (!TvTEvent.onEscapeUse(activeChar.getName()))
        {
        	activeChar.sendPacket(new ActionFailed());
			activeChar.sendMessage("Вы на твт");
        	return false;
        }
       	if (activeChar.isEventWait())
       	{
			activeChar.sendMessage("Нельзя призывать на эвенте");
            return false;
        }


        int teleportTimer = Config.L2JMOD_WEDDING_TELEPORT_DURATION*1000;

        activeChar.sendMessage("Вы встретитесь через "+ teleportTimer/60000 + " мин.");
        activeChar.getInventory().reduceAdena("Wedding", Config.L2JMOD_WEDDING_TELEPORT_PRICE, activeChar, null);

        activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        //SoE Animation section
        activeChar.setTarget(activeChar);
        activeChar.disableAllSkills();

        MagicSkillUser msk = new MagicSkillUser(activeChar, 1050, 1, teleportTimer, 0);
        Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 1200/*900*/);
        SetupGauge sg = new SetupGauge(0, teleportTimer);
        activeChar.sendPacket(sg);
        //End SoE Animation section

        EscapeFinalizer ef = new EscapeFinalizer(activeChar,partner.getX(),partner.getY(),partner.getZ(),partner.isIn7sDungeon());
        // continue execution later
        activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, teleportTimer));
        activeChar.setSkillCastEndTime(10+GameTimeController.getGameTicks()+teleportTimer/GameTimeController.MILLIS_IN_TICK);

        return true;
    }

    static class EscapeFinalizer implements Runnable
    {
        private L2PcInstance _activeChar;
        private int _partnerx;
        private int _partnery;
        private int _partnerz;
        private boolean _to7sDungeon;

        EscapeFinalizer(L2PcInstance activeChar, int x, int y, int z, boolean to7sDungeon)
        {
            _activeChar = activeChar;
            _partnerx = x;
            _partnery = y;
            _partnerz = z;
            _to7sDungeon = to7sDungeon;
        }

        public void run()
        {
            if (_activeChar.isDead())
                return;

            _activeChar.setIsIn7sDungeon(_to7sDungeon);

            _activeChar.enableAllSkills();

            try
            {
                _activeChar.teleToLocation(_partnerx, _partnery, _partnerz);
            } catch (Throwable e) { _log.error(e.getMessage(),e); }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public String[] getVoicedCommandList()
    {
        return _voicedCommands;
    }
}
