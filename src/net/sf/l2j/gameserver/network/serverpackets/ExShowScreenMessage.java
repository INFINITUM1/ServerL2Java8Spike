package net.sf.l2j.gameserver.network.serverpackets;

public class ExShowScreenMessage extends L2GameServerPacket
{
	public static enum ScreenMessageAlign
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		MIDDLE_LEFT,
		MIDDLE_CENTER,
		MIDDLE_RIGHT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT,
	}

	private final String text;
	private final int type;
	private final int sysMessageId;
	private final boolean big_font;
	private final boolean effect;
	private final ScreenMessageAlign text_align;
	private final int time;

	public ExShowScreenMessage(final String text, final int time, final ScreenMessageAlign text_align, final boolean big_font)
	{
		this(text, time, text_align, big_font, 1, 0, false);
	}

	public ExShowScreenMessage(final String text, final int time, final ScreenMessageAlign text_align, final boolean big_font, final int type, final int messageId, final boolean showEffect)
	{
		this.type = type;
		sysMessageId = messageId;
		this.text = text;
		this.time = time;
		this.text_align = text_align;
		this.big_font = big_font;
		effect = showEffect;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x38);
		writeD(type); // 0 - system messages, 1 - your defined text
		writeD(sysMessageId); // system message id (_type must be 0 otherwise no effect)
		writeD(text_align.ordinal() + 1); // размещение текста
		writeD(0x00); // ?
		writeD(big_font ? 0 : 1); // размер текста
		writeD(0x00); // ?
		writeD(0x00); // ?
		writeD(effect ? 1 : 0); // upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
		writeD(time); // время отображения сообщения в милисекундах
		writeD(0x01); // ?
		writeS(text);
	}
}
