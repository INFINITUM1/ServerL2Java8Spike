/*
 * This program is free software; you can redistribute it and/or modify
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
package net.sf.l2j.gameserver;

import java.nio.ByteBuffer;


/**
 * This class ...
 *
 * @version $Revision: 1.3.4.3 $ $Date: 2005/03/27 15:29:18 $
 */
public class Crypt
{
	private final byte[] _k = new byte[16];
	private boolean _f;

	public void setKey(byte[] k)
	{
		System.arraycopy(k,0, _k, 0, k.length);
		_f = true;
	}

	public void decrypt(ByteBuffer b)
	{
		if (!_f)
			return;

		final int sz = b.remaining();
		int temp = 0;
		for (int i = 0; i < sz; i++)
		{
			int temp2 = b.get(i);
			b.put(i, (byte)(temp2 ^ _k[i&15] ^ temp));
			temp = temp2;
		}

		int old = _k[8] &0xff;
		old |= _k[9] << 8 &0xff00;
		old |= _k[10] << 0x10 &0xff0000;
		old |= _k[11] << 0x18 &0xff000000;

		old += sz;

		_k[8] = (byte)(old &0xff);
		_k[9] = (byte)(old >> 0x08 &0xff);
		_k[10] = (byte)(old >> 0x10 &0xff);
		_k[11] = (byte)(old >> 0x18 &0xff);
	}

	public void encrypt(ByteBuffer b)
	{
		if (!_f)
			return;

		int temp = 0;
		final int sz = b.remaining();
		for (int i = 0; i < sz; i++)
		{
			int temp2 = b.get(i);
			temp = temp2 ^ _k[i&15] ^ temp;
			b.put(i, (byte) temp);
		}

		int old = _k[8] &0xff;
		old |= _k[9] << 8 &0xff00;
		old |= _k[10] << 0x10 &0xff0000;
		old |= _k[11] << 0x18 &0xff000000;

		old += sz;

		_k[8] = (byte)(old &0xff);
		_k[9] = (byte)(old >> 0x08 &0xff);
		_k[10] = (byte)(old >> 0x10 &0xff);
		_k[11] = (byte)(old >> 0x18 &0xff);
	}
}
