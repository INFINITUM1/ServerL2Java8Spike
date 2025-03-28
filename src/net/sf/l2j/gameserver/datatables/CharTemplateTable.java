package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javolution.util.FastMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.util.Rnd;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.log.AbstractLogger;

public class CharTemplateTable {

    private static Logger _log = AbstractLogger.getLogger(CharTemplateTable.class.getName());
    private static CharTemplateTable _instance;
    private static String[] CHAR_CLASSES = {
        "Human Fighter",
        "Warrior",
        "Gladiator",
        "Warlord",
        "Human Knight",
        "Paladin",
        "Dark Avenger",
        "Rogue",
        "Treasure Hunter",
        "Hawkeye",
        "Human Mystic",
        "Human Wizard",
        "Sorceror",
        "Necromancer",
        "Warlock",
        "Cleric",
        "Bishop",
        "Prophet",
        "Elven Fighter",
        "Elven Knight",
        "Temple Knight",
        "Swordsinger",
        "Elven Scout",
        "Plainswalker",
        "Silver Ranger",
        "Elven Mystic",
        "Elven Wizard",
        "Spellsinger",
        "Elemental Summoner",
        "Elven Oracle",
        "Elven Elder",
        "Dark Fighter",
        "Palus Knight",
        "Shillien Knight",
        "Bladedancer",
        "Assassin",
        "Abyss Walker",
        "Phantom Ranger",
        "Dark Elven Mystic",
        "Dark Elven Wizard",
        "Spellhowler",
        "Phantom Summoner",
        "Shillien Oracle",
        "Shillien Elder",
        "Orc Fighter",
        "Orc Raider",
        "Destroyer",
        "Orc Monk",
        "Tyrant",
        "Orc Mystic",
        "Orc Shaman",
        "Overlord",
        "Warcryer",
        "Dwarven Fighter",
        "Dwarven Scavenger",
        "Bounty Hunter",
        "Dwarven Artisan",
        "Warsmith",
        "dummyEntry1", "dummyEntry2", "dummyEntry3", "dummyEntry4", "dummyEntry5",
        "dummyEntry6", "dummyEntry7", "dummyEntry8", "dummyEntry9", "dummyEntry10",
        "dummyEntry11", "dummyEntry12", "dummyEntry13", "dummyEntry14", "dummyEntry15",
        "dummyEntry16", "dummyEntry17", "dummyEntry18", "dummyEntry19", "dummyEntry20",
        "dummyEntry21", "dummyEntry22", "dummyEntry23", "dummyEntry24", "dummyEntry25",
        "dummyEntry26", "dummyEntry27", "dummyEntry28", "dummyEntry29", "dummyEntry30",
        "Duelist",
        "DreadNought",
        "Phoenix Knight",
        "Hell Knight",
        "Sagittarius",
        "Adventurer",
        "Archmage",
        "Soultaker",
        "Arcana Lord",
        "Cardinal",
        "Hierophant",
        "Eva Templar",
        "Sword Muse",
        "Wind Rider",
        "Moonlight Sentinel",
        "Mystic Muse",
        "Elemental Master",
        "Eva's Saint",
        "Shillien Templar",
        "Spectral Dancer",
        "Ghost Hunter",
        "Ghost Sentinel",
        "Storm Screamer",
        "Spectral Master",
        "Shillien Saint",
        "Titan",
        "Grand Khauatari",
        "Dominator",
        "Doomcryer",
        "Fortune Seeker",
        "Maestro",
        "HellParent",
        "HellAssassin",
        "HellWarrior",
        "HellWizard",
        "HellTank",
        "HellHealer"
    };
    /*private static String[] CHAR_CLASSES = {
        "Human Fighter",
        "Warrior",
        "Gladiator",
        "Warlord",
        "Human Knight",
        "Paladin",
        "Dark Avenger",
        "Rogue",
        "Treasure Hunter",
        "Hawkeye",
        "Human Mystic",
        "Human Wizard",
        "Sorceror",
        "Necromancer",
        "Warlock",
        "Cleric",
        "Bishop",
        "Prophet",
        "Elven Fighter",
        "Elven Knight",
        "Temple Knight",
        "Swordsinger",
        "Elven Scout",
        "Plainswalker",
        "Silver Ranger",
        "Elven Mystic",
        "Elven Wizard",
        "Spellsinger",
        "Elemental Summoner",
        "Elven Oracle",
        "Elven Elder",
        "Dark Fighter",
        "Palus Knight",
        "Shillien Knight",
        "Bladedancer",
        "Assassin",
        "Abyss Walker",
        "Phantom Ranger",
        "Dark Elven Mystic",
        "Dark Elven Wizard",
        "Spellhowler",
        "Phantom Summoner",
        "Shillien Oracle",
        "Shillien Elder",
        "Orc Fighter",
        "Orc Raider",
        "Destroyer",
        "Orc Monk",
        "Tyrant",
        "Orc Mystic",
        "Orc Shaman",
        "Overlord",
        "Warcryer",
        "Dwarven Mystic",
        "Enchanter",
        "Exorcist",
        "Medic",
        "Witch",
        "dummyEntry1", "dummyEntry2", "dummyEntry3", "dummyEntry4", "dummyEntry5",
        "dummyEntry6", "dummyEntry7", "dummyEntry8", "dummyEntry9", "dummyEntry10",
        "dummyEntry11", "dummyEntry12", "dummyEntry13", "dummyEntry14", "dummyEntry15",
        "dummyEntry16", "dummyEntry17", "dummyEntry18", "dummyEntry19", "dummyEntry20",
        "dummyEntry21", "dummyEntry22", "dummyEntry23", "dummyEntry24", "dummyEntry25",
        "dummyEntry26", "dummyEntry27", "dummyEntry28", "dummyEntry29", "dummyEntry30",
        "Duelist",
        "DreadNought",
        "Phoenix Knight",
        "Hell Knight",
        "Sagittarius",
        "Adventurer",
        "Archmage",
        "Soultaker",
        "Arcana Lord",
        "Cardinal",
        "Hierophant",
        "Eva Templar",
        "Sword Muse",
        "Wind Rider",
        "Moonlight Sentinel",
        "Mystic Muse",
        "Elemental Master",
        "Eva's Saint",
        "Shillien Templar",
        "Spectral Dancer",
        "Ghost Hunter",
        "Ghost Sentinel",
        "Storm Screamer",
        "Spectral Master",
        "Shillien Saint",
        "Titan",
        "Grand Khauatari",
        "Dominator",
        "Doomcryer",
        "Druid",
        "Physician",
        "HellParent",
        "HellAssassin",
        "HellWarrior",
        "HellWizard",
        "HellTank",
        "HellHealer"
    };*/
    private static FastMap<Integer, L2PcTemplate> _templates = new FastMap<Integer, L2PcTemplate>().shared("CharTemplateTable._templates");

    public static CharTemplateTable getInstance() {
        if (_instance == null) {
            _instance = new CharTemplateTable();
        }
        return _instance;
    }

    private CharTemplateTable() {
        //_templates = new FastMap<Integer, L2PcTemplate>().shared();
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("SELECT * FROM class_list, char_templates, lvlupgain WHERE class_list.id = char_templates.classId AND class_list.id = lvlupgain.classId ORDER BY class_list.id");
            rs = st.executeQuery();

            while (rs.next()) {
                StatsSet set = new StatsSet();
                //ClassId classId = ClassId.values()[rs.getInt("id")];
                set.set("classId", rs.getInt("id"));
                set.set("className", rs.getString("className"));
                set.set("raceId", rs.getInt("raceId"));
                set.set("baseSTR", rs.getInt("STR"));
                set.set("baseCON", rs.getInt("CON"));
                set.set("baseDEX", rs.getInt("DEX"));
                set.set("baseINT", rs.getInt("_INT"));
                set.set("baseWIT", rs.getInt("WIT"));
                set.set("baseMEN", rs.getInt("MEN"));
                set.set("baseHpMax", rs.getFloat("defaultHpBase"));
                set.set("lvlHpAdd", rs.getFloat("defaultHpAdd"));
                set.set("lvlHpMod", rs.getFloat("defaultHpMod"));
                set.set("baseMpMax", rs.getFloat("defaultMpBase"));
                set.set("baseCpMax", rs.getFloat("defaultCpBase"));
                set.set("lvlCpAdd", rs.getFloat("defaultCpAdd"));
                set.set("lvlCpMod", rs.getFloat("defaultCpMod"));
                set.set("lvlMpAdd", rs.getFloat("defaultMpAdd"));
                set.set("lvlMpMod", rs.getFloat("defaultMpMod"));
                set.set("baseHpReg", 1.5);
                set.set("baseMpReg", 0.9);
                set.set("basePAtk", rs.getInt("p_atk"));
                set.set("basePDef", /*classId.isMage()? 77 : 129*/ rs.getInt("p_def"));
                set.set("baseMAtk", rs.getInt("m_atk"));
                set.set("baseMDef", rs.getInt("char_templates.m_def"));
                set.set("classBaseLevel", rs.getInt("class_lvl"));
                set.set("basePAtkSpd", rs.getInt("p_spd"));
                set.set("baseMAtkSpd", /*classId.isMage()? 166 : 333*/ rs.getInt("char_templates.m_spd"));
                set.set("baseCritRate", rs.getInt("char_templates.critical") / 10);
                set.set("baseRunSpd", rs.getInt("move_spd"));
                set.set("baseWalkSpd", 0);
                set.set("baseShldDef", 0);
                set.set("baseShldRate", 0);
                set.set("baseAtkRange", 40);
                set.set("parentClassId", rs.getInt("parent_id"));

                L2PcTemplate ct;

                set.set("collision_radius", rs.getDouble("m_col_r"));
                set.set("collision_height", rs.getDouble("m_col_h"));
                ct = new L2PcTemplate(set);
                /*//5items must go here
                for (int x=1; x < 6 ;x++)
                {
                if (rs.getInt("items"+x) != 0)
                {
                ct.addItem(rs.getInt("items"+x));
                }
                }*/
                _templates.put(ct.classId.getId(), ct);
            }
        } catch (SQLException e) {
            _log.warning("error while loading char templates " + e.getMessage());
        } finally {
            Close.CSR(con, st, rs);
        }
        loadAdditions();
        _log.config("CharTemplateTable: Loaded " + _templates.size() + " Character Templates.");
    }

    public L2PcTemplate getTemplate(ClassId classId) {
        return getTemplate(classId.getId());
    }

    public L2PcTemplate getTemplate(int classId) {
        int key = classId;
        return _templates.get(key);
    }

    public static String getClassNameById(int classId) {
        return CHAR_CLASSES[classId];
    }

    public static int getClassIdByName(String className) {
        int currId = 1;

        for (String name : CHAR_CLASSES) {
            if (name.equalsIgnoreCase(className)) {
                break;
            }

            currId++;
        }

        return currId;
    }

    private void loadAdditions() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/pc_settings.xml");
            if (!file.exists()) {
                _log.config("CharTemplateTable [ERROR]: data/pc_settings.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            //FastList<String> strings = new FastList<String>();
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("class".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs = d.getAttributes();
                            int classId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                            L2PcTemplate ct = getTemplate(classId);
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("equipment".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] items = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String item : items) {
                                        if (item.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        ct.addItem(Integer.parseInt(item));
                                    }
                                }
                                if ("spawn".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();
                                    String[] coords = attrs.getNamedItem("points").getNodeValue().split(";");
                                    for (String xyz : coords) {
                                        if (xyz.equalsIgnoreCase("")) {
                                            continue;
                                        }

                                        String[] point = xyz.split(",");
                                        Integer x = Integer.valueOf(point[0]);
                                        Integer y = Integer.valueOf(point[1]);
                                        Integer z = Integer.valueOf(point[2]);
                                        if (x == null || y == null || z == null) {
                                            continue;
                                        }

                                        ct.addSpawnPoint(new Location(x  + Rnd.get(300), y  + Rnd.get(300), z));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("CharTemplateTable [ERROR]: loadAdditions() " + e.toString());
        }
    }
//	public L2CharTemplate[] getAllTemplates()
//	{
//		return _templates.values().toArray(new L2CharTemplate[_templates.size()]);
//	}
}
