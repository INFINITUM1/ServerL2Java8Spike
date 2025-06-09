/*
 * $Header: Util.java, 14-Jul-2005 03:27:51 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 14-Jul-2005 03:27:51 $
 * $Revision: 1 $
 * $Log: Util.java,v $
 * Revision 1  14-Jul-2005 03:27:51  luisantonioa
 * Added copyright notice
 *
 *
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
package net.sf.l2j.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import net.sf.l2j.Base64;
import java.net.InetAddress;
import javolution.text.TextBuilder;

/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class Util
{
    public static boolean isInternalIP(String ipAddress)
    {
        return (ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("10.") ||
				//ipAddress.startsWith("172.16.") ||
                //Removed because there are some net IPs in this range.
                //TODO: Use regexp or something to only include 172.16.0.0 => 172.16.31.255
                ipAddress.startsWith("127.0.0.1"));
    }
	
	public static boolean isInMask(InetAddress address1, InetAddress address2, int mask)
	{
		byte[] addr1 = address1.getAddress();
		byte[] addr2 = address2.getAddress();
		
		if (((addr1[0] ^ addr2[0]) & ((mask >> 24) & 0xFF)) != 0)
			return false;
		if (((addr1[1] ^ addr2[1]) & ((mask >> 16) & 0xFF)) != 0)
			return false;
		if (((addr1[2] ^ addr2[2]) & ((mask >> 8) & 0xFF)) != 0)
			return false;
		if (((addr1[3] ^ addr2[3]) & (mask & 0xFF)) != 0)
			return false;
		
		return true;
	}

    public static String printData(byte[] data, int len)
	{
        TextBuilder result = new TextBuilder();

		int counter = 0;

		for (int i=0;i< len;i++)
		{
			if (counter % 16 == 0)
			{
				result.append(fillHex(i,4)+": ");
			}

			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");

				int charpoint = i-15;
				for (int a=0; a<16;a++)
				{
					int t1 = data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
					{
						result.append((char)t1);
					}
					else
					{
						result.append('.');
					}
				}

				result.append("\n");
				counter = 0;
			}
		}

		int rest = data.length % 16;
		if (rest > 0 )
		{
			for (int i=0; i<17-rest;i++ )
			{
				result.append("   ");
			}

			int charpoint = data.length-rest;
			for (int a=0; a<rest;a++)
			{
				int t1 = data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
				{
					result.append((char)t1);
				}
				else
				{
					result.append('.');
				}
			}

			result.append("\n");
		}


		return result.toString();
	}

	public static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);

		for (int i=number.length(); i< digits; i++)
		{
			number = "0" + number;
		}

		return number;
	}

	/**
	 * @param raw
	 * @return
	 */
	public static String printData(byte[] raw)
	{
		return printData(raw, raw.length);
	}

	public static String getSHA1(String pass)
	{
		String result = "";
		try
		{
			// Encode Password
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] newpass;
			newpass = pass.getBytes("UTF-8");
			newpass = md.digest(newpass);
			result = Base64.encodeBytes(newpass);
		}
		catch(Exception e)
		{
			//
		}
		return result;
	}
    
    public static String sha1(String input)
    {
        try 
        {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(input.getBytes()); 
			input = bytesToHex(md.digest());
        } 
		catch (NoSuchAlgorithmException ex) 
		{
			//
        }
        return input;
    }
    
    public static String md5(String input)
    {
        try 
        {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes()); 
			input = bytesToHex(md.digest());
        } 
		catch (NoSuchAlgorithmException ex) 
		{
			//
        }
        return input;
    }
    
    public static String bytesToHex(byte[] b) {
      char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
      StringBuilder buf = new StringBuilder();
      for (int j=0; j<b.length; j++) {
         buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
         buf.append(hexDigit[b[j] & 0x0f]);
      }
      return buf.toString();
   }

	public static String htmlSpecialChars(String word)
	{
		word = word.replaceAll("<","&lt;");
		word = word.replaceAll(">","&gt;");
		word = word.replaceAll("\\$","");
		return word;
	}
}
