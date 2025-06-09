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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
//import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateFail;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateOk;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectInfo;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Location;
import scripts.events.custom.CustomPlayerEventList;

import static net.sf.l2j.Config.ALT_START_ITEMS;


/**
 * This class ...
 *
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:30 $
 */
@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket {

    //private static final Logger _log = Logger.getLogger(CharacterCreate.class.getName());
    // cSdddddddddddd
    private String _name;
    private int _race;
    private byte _sex;
    private int _classId;
    private int _int;
    private int _str;
    private int _con;
    private int _men;
    private int _dex;
    private int _wit;
    private byte _hairStyle;
    private byte _hairColor;
    private byte _face;
    private long _exp;

    @Override
    protected void readImpl() {
        _name = readS();
        _race = readD();
        _sex = (byte) readD();
        _classId = readD();
        _int = readD();
        _str = readD();
        _con = readD();
        _men = readD();
        _dex = readD();
        _wit = readD();
        _hairStyle = (byte) readD();
        _hairColor = (byte) readD();
        _face = (byte) readD();
        _exp = 6299994999L;
    }

    @Override
    protected void runImpl() {
        if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
            return;
        } else if (CharNameTable.getInstance().doesCharNameExist(_name)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
            return;
        } else if ((_name.length() < 3) || (_name.length() > 16) || !Util.isAlphaNumeric(_name) || !isValidName(_name)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
            return;
        }

        //if (Config.DEBUG)
        //	_log.fine("charname: " + _name + " classId: " + _classId);
        L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(_classId);
        if (template == null || template.classBaseLevel > 1) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
            return;
        }

        CharNameTable.getInstance().storeCharName(_name);

        int objectId = IdFactory.getInstance().getNextId();
        L2PcInstance newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
        //newChar.setMaxLoad(template.baseLoad);
        if (Config.ALT_START_LEVEL > 0) {
            newChar.fullRestore();
        } else {
            newChar.setCurrentHp(template.baseHpMax);
            newChar.setCurrentCp(template.baseCpMax);
            newChar.setCurrentMp(template.baseMpMax);
        }

        // send acknowledgement
        sendPacket(new CharCreateOk());

        initNewChar(getClient(), newChar);
    }
    private static final Pattern cnamePattern = Pattern.compile(Config.CNAME_TEMPLATE);//Pattern.compile("[\\w\\u005F\\u002E]+", Pattern.UNICODE_CASE);

    private boolean isValidName(String text) {
        /* Pattern pattern;
         try {
         pattern = Pattern.compile(Config.CNAME_TEMPLATE);
         } catch (PatternSyntaxException e) // case of illegal pattern
         {
         _log.warning("ERROR : Character name pattern of config is wrong!");
         pattern = Pattern.compile(".*");
         }*/

        return cnamePattern.matcher(text).matches();
    }

    private void initNewChar(L2GameClient client, L2PcInstance newChar) {
        //if (Config.DEBUG) _log.fine("Character init start");
        L2World.getInstance().storeObject(newChar);

        L2PcTemplate template = newChar.getTemplate();

        newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
      
        Location loc = template.getRandomSpawnPoint();
        newChar.setXYZInvisible(loc.x, loc.y, loc.z);

            L2ShortCut shortcut;

        if (ALT_START_ITEMS) {
            L2ItemInstance item4 = newChar.getInventory().addItem("Init", 728, 1, newChar, null);
            shortcut = new L2ShortCut(5, 0, 1, item4.getObjectId(), -1, 1);
            newChar.registerShortCut(shortcut);

            L2ItemInstance item2 = newChar.getInventory().addItem("Init", 1467, 1, newChar, null);
            shortcut = new L2ShortCut(8, 0, 1, item2.getObjectId(), -1, 1);
            newChar.registerShortCut(shortcut);

            L2ItemInstance item3 = newChar.getInventory().addItem("Init", 3952, 1, newChar, null);
            shortcut = new L2ShortCut(9, 0, 1, item3.getObjectId(), -1, 1);
            newChar.registerShortCut(shortcut);
        }

            //add attack shortcut
            shortcut = new L2ShortCut(0, 0, 3, 2, -1, 1);
            newChar.registerShortCut(shortcut);

            //add take shortcut
            shortcut = new L2ShortCut(3, 0, 3, 5, -1, 1);
            newChar.registerShortCut(shortcut);

            //add sit shortcut
            shortcut = new L2ShortCut(10, 0, 3, 0, -1, 1);
            newChar.registerShortCut(shortcut);

            for (Integer itemId : template.getItems()) {
                L2ItemInstance item = newChar.getInventory().addItem("Init", itemId, 1, newChar, null);
                if (item.getItemId() == 5588) {
                    //add tutbook shortcut
                    shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), -1, 1);
                    newChar.registerShortCut(shortcut);
                }
                if (item.isEquipable()) {
                    if (Config.CHAR_CREATE_ENCHANT > 0) {
                        item.setEnchantLevel(Config.CHAR_CREATE_ENCHANT);
                        //if (newChar.getActiveWeaponItem() != null && item.getItem().getType2() == L2Item.TYPE2_WEAPON)
                        //	continue;
                    }
                    newChar.getInventory().equipItemAndRecord(item);
                }
            }
            
            if ((Config.ON_BUFFS) && (Config.ON_BUFFS_LVL >= newChar.getLevel())) {
                FastMap<Integer, Integer> buffs = null;
                if (newChar.isMageClass()) {
                    buffs = Config.ON_ENTER_M_BUFFS;
                } else {
                    buffs = Config.ON_ENTER_F_BUFFS;
                }


                Integer id = null;
                Integer lvl = null;
                SkillTable _st = SkillTable.getInstance();
                for (FastMap.Entry<Integer, Integer> e = buffs.head(), end = buffs.tail(); (e = e.getNext()) != end; ) {
                    id = e.getKey(); // No typecast necessary.
                    lvl = e.getValue(); // No typecast necessary.
                    if (id == null || lvl == null) {
                        continue;
                    }

                    _st.getInfo(id, lvl).getEffects(newChar, newChar);
                }
            }



            L2SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());
            for (int i = 0; i < startSkills.length; i++) {
                newChar.addSkill(SkillTable.getInstance().getInfo(startSkills[i].getId(), startSkills[i].getLevel()), true);
                if (startSkills[i].getId() == 1001 || startSkills[i].getId() == 1177) {
                    shortcut = new L2ShortCut(1, 0, 2, startSkills[i].getId(), 1, 1);
                    newChar.registerShortCut(shortcut);
                }
                if (startSkills[i].getId() == 1216) {
                    shortcut = new L2ShortCut(10, 0, 2, startSkills[i].getId(), 1, 1);
                    newChar.registerShortCut(shortcut);
                }
                //if (Config.DEBUG)
                //	_log.fine("adding starter skill:" + startSkills[i].getId()+ " / "+ startSkills[i].getLevel());
            }
            L2GameClient.saveCharToDisk(newChar);
            newChar.deleteMe(); // release the world of this character and it's inventory

            CustomPlayerEventList.notifyPlayerCreate(newChar);

            // send char list
            CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
            client.getConnection().sendPacket(cl);
            client.setCharSelection(cl.getCharInfo());
            //if (Config.DEBUG) _log.fine("Character init end");
        }

    }
