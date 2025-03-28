package net.sf.l2j.gameserver.network.clientpackets;

public final class RequestEnchantItemDef extends RequestEnchantItem
{
    private int _objectId;

    @Override
	protected void readImpl()
    {
        _objectId = readD();
    }

    @Override
	protected void runImpl()
    {
		//
	}
}
