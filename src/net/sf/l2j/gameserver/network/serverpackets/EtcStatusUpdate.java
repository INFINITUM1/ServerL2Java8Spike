package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author Luca Baldi
 */
public class EtcStatusUpdate extends L2GameServerPacket {

    private int IncreasedForce, WeightPenalty, MessageRefusal, DangerArea;
    private int _expertisePenalty, CharmOfCourage, DeathPenaltyLevel;
    private boolean can_writeImpl = false;

    public EtcStatusUpdate(L2PcInstance player) {
        if (player == null) {
            return;
        }
        IncreasedForce = player.getCharges(); // 1-7 increase force, lvl
        WeightPenalty = player.getWeightPenalty();
        MessageRefusal = (player.getMessageRefusal() || player.isChatBanned()) ? 1 : 0;
        DangerArea = player.isInDangerArea() ? 1 : 0;
        _expertisePenalty = player.getExpertisePenalty();
        CharmOfCourage = player.getCharmOfCourage() ? 1 : 0;
        DeathPenaltyLevel = player.getDeathPenaltyBuffLevel();
        can_writeImpl = true;
    }

    @Override
    protected final void writeImpl() {
        if (!can_writeImpl) {
            return;
        }

        // dddddddd
        writeC(0xF3); //Packet type
        writeD(IncreasedForce); // skill id 4271, 7 lvl
        writeD(WeightPenalty); // skill id 4270, 4 lvl
        writeD(MessageRefusal); //skill id 4269, 1 lvl
        writeD(DangerArea); // skill id 4268, 1 lvl
        writeD(_expertisePenalty); // skill id 4267, 1 lvl at off c4 server scripts
        writeD(CharmOfCourage); //Charm of Courage, "Prevents experience value decreasing if killed during a siege war".
        writeD(DeathPenaltyLevel); //Death Penalty max lvl 15, "Combat ability is decreased due to death."
    }
}
