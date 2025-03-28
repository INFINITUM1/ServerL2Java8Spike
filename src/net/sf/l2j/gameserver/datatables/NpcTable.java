package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javolution.text.TextBuilder;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastTable;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2DropCategory;
import net.sf.l2j.gameserver.model.L2DropData;
import net.sf.l2j.gameserver.model.L2MinionData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.6.2.9 $ $Date: 2005/04/06 16:13:25 $
 */
public class NpcTable {

    private static final Logger _log = AbstractLogger.getLogger(NpcTable.class.getName());
    private static NpcTable _instance;
    private static FastMap<Integer, L2NpcTemplate> _npcs = new FastMap<Integer, L2NpcTemplate>().shared("NpcTable._npcs");
    private boolean _initialized = false;

    public static NpcTable getInstance() {
        if (_instance == null) {
            _instance = new NpcTable();
        }

        return _instance;
    }

    private NpcTable() {
        restoreNpcData(false);
    }

    private void restoreNpcData(boolean reload) {
        _npcs.clear();

        Connect con = null;
        PreparedStatement st = null;
        PreparedStatement st3 = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            try {
                st = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[]{"id", "idTemplate", "name", "serverSideName", "title", "serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type", "attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk", "pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor", "walkspd", "runspd", "faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type"}) + " FROM npc");
                rs = st.executeQuery();
                rs.setFetchSize(50);

                fillNpcTable(rs);
            } catch (Exception e) {
                _log.severe("NpcTable [ERROR]: Error creating NPC table: " + e);
            } finally {
                Close.SR(st, rs);
            }

            try {
                st = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
                rs = st.executeQuery();
                rs.setFetchSize(50);
                L2NpcTemplate npcDat = null;
                L2Skill npcSkill = null;

                while (rs.next()) {
                    int mobId = rs.getInt("npcid");
                    npcDat = _npcs.get(mobId);

                    if (npcDat == null) {
                        continue;
                    }

                    int skillId = rs.getInt("skillid");
                    int level = rs.getInt("level");

                    if (npcDat.race == null && skillId == 4416) {
                        npcDat.setRace(level);
                        continue;
                    }

                    npcSkill = SkillTable.getInstance().getInfo(skillId, level);

                    if (npcSkill == null) {
                        continue;
                    }

                    npcDat.addSkill(npcSkill);
                }
            } catch (Exception e) {
                _log.severe("NPCTable: Error reading NPC skills table: " + e);
            } finally {
                Close.SR(st, rs);
            }

            try {
                st = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[]{"mobId", "itemId", "min", "max", "category", "chance"}) + " FROM droplist ORDER BY mobId, chance DESC");
                rs = st.executeQuery();
                rs.setFetchSize(50);
                L2Item itemDat = null;
                L2DropData dropDat = null;
                L2NpcTemplate npcDat = null;
                while (rs.next()) {
                    int mobId = rs.getInt("mobId");
                    npcDat = _npcs.get(mobId);
                    if (npcDat == null) {
                        _log.severe("NpcTable [ERROR]: No npc correlating with id : " + mobId);
                        continue;
                    }
                    int itemId = rs.getInt("itemId");
                    itemDat = ItemTable.getInstance().getTemplate(itemId);
                    if (itemDat == null) {
                        st3 = con.prepareStatement("DELETE FROM `droplist` WHERE `itemId`=?");
                        st3.setInt(1, itemId);
                        st3.execute();
                        Close.S(st3);
                        _log.severe("NpcTable [ERROR]: No item correlating with id: " + itemId + "; mobId: " + mobId);
                        continue;
                    }

                    dropDat = new L2DropData();
                    dropDat.setItemId(itemId);
                    dropDat.setMinDrop(rs.getInt("min"));
                    dropDat.setMaxDrop(rs.getInt("max"));
                    dropDat.setChance(rs.getInt("chance"));
                    int category = rs.getInt("category");
                    npcDat.addDropData(dropDat, category);
                }
            } catch (Exception e) {
                _log.severe("NpcTable [ERROR]: reading NPC drop data: " + e);
            } finally {
                Close.SR(st, rs);
            }

            try {
                st = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[]{"npc_id", "class_id"}) + " FROM skill_learn");
                rs = st.executeQuery();
                rs.setFetchSize(50);

                while (rs.next()) {
                    int npcId = rs.getInt("npc_id");
                    int classId = rs.getInt("class_id");
                    L2NpcTemplate npc = getTemplate(npcId);

                    if (npc == null) {
                        _log.warning("NpcTable [ERROR]: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
                        continue;
                    }

                    npc.addTeachInfo(ClassId.values()[classId]);
                }
            } catch (Exception e) {
                _log.severe("NpcTable [ERROR]: reading NPC trainer data: " + e);
            } finally {
                Close.SR(st, rs);
            }

            try {
                st = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[]{"boss_id", "minion_id", "amount_min", "amount_max"}) + " FROM minions");
                rs = st.executeQuery();
                rs.setFetchSize(50);
                L2MinionData minionDat = null;
                L2NpcTemplate npcDat = null;
                int cnt = 0;

                while (rs.next()) {
                    int raidId = rs.getInt("boss_id");
                    npcDat = _npcs.get(raidId);
                    if (npcDat == null) {
                        _log.severe("NpcTable [ERROR]: No boss correlating with id : " + raidId);
                        continue;
                    }
                    minionDat = new L2MinionData();
                    minionDat.setMinionId(rs.getInt("minion_id"));
                    minionDat.setAmountMin(rs.getInt("amount_min"));
                    minionDat.setAmountMax(rs.getInt("amount_max"));
                    npcDat.addRaidData(minionDat);
                    cnt++;
                }
                _log.config("Loading NpcTable... total " + cnt + " Minions.");
            } catch (Exception e) {
                _log.severe("NpcTable [ERROR]: loading minion data: " + e);
            } finally {
                Close.SR(st, rs);
            }
        } catch (Exception e) {
            _log.warning("NpcTable [ERROR]: loading npc data: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }

        _initialized = true;
        if (reload) {
            L2World.getInstance().deleteVisibleNpcSpawns(true);
        }
    }

    private void fillNpcTable(ResultSet NpcData) throws Exception {
        fillNpcTable(NpcData, false);
    }

    private void fillNpcTable(ResultSet NpcData, boolean one) throws Exception {
        while (NpcData.next()) {
            StatsSet npcDat = new StatsSet();
            int id = NpcData.getInt("id");

            if (Config.ASSERT) {
                assert id < 1000000;
            }

            npcDat.set("npcId", id);
            npcDat.set("idTemplate", NpcData.getInt("idTemplate"));
            int level = NpcData.getInt("level");
            npcDat.set("level", level);
            npcDat.set("jClass", NpcData.getString("class"));

            npcDat.set("baseShldDef", 0);
            npcDat.set("baseShldRate", 0);
            npcDat.set("baseCritRate", 38);

            npcDat.set("name", NpcData.getString("name"));
            npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
            //npcDat.set("name", "");
            npcDat.set("title", NpcData.getString("title"));
            npcDat.set("serverSideTitle", NpcData.getBoolean("serverSideTitle"));
            npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
            npcDat.set("collision_height", NpcData.getDouble("collision_height"));
            npcDat.set("sex", NpcData.getString("sex"));
            npcDat.set("type", NpcData.getString("type"));
            npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
            npcDat.set("rewardExp", NpcData.getInt("exp"));
            npcDat.set("rewardSp", NpcData.getInt("sp"));
            npcDat.set("basePAtkSpd", NpcData.getInt("atkspd"));
            npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd"));
            npcDat.set("aggroRange", NpcData.getInt("aggro"));
            npcDat.set("rhand", NpcData.getInt("rhand"));
            npcDat.set("lhand", NpcData.getInt("lhand"));
            npcDat.set("armor", NpcData.getInt("armor"));
            npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
            npcDat.set("baseRunSpd", NpcData.getInt("runspd"));

            // constants, until we have stats in DB
            npcDat.set("baseSTR", NpcData.getInt("str"));
            npcDat.set("baseCON", NpcData.getInt("con"));
            npcDat.set("baseDEX", NpcData.getInt("dex"));
            npcDat.set("baseINT", NpcData.getInt("int"));
            npcDat.set("baseWIT", NpcData.getInt("wit"));
            npcDat.set("baseMEN", NpcData.getInt("men"));

            npcDat.set("baseHpMax", NpcData.getInt("hp"));
            npcDat.set("baseCpMax", 0);
            npcDat.set("baseMpMax", NpcData.getInt("mp"));
            npcDat.set("baseHpReg", NpcData.getFloat("hpreg") > 0 ? NpcData.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
            npcDat.set("baseMpReg", NpcData.getFloat("mpreg") > 0 ? NpcData.getFloat("mpreg") : 0.9 + 0.3 * ((level - 1) / 10.0));
            npcDat.set("basePAtk", NpcData.getInt("patk"));
            npcDat.set("basePDef", NpcData.getInt("pdef"));
            npcDat.set("baseMAtk", NpcData.getInt("matk"));
            npcDat.set("baseMDef", NpcData.getInt("mdef"));

            npcDat.set("factionId", NpcData.getString("faction_id"));
            npcDat.set("factionRange", NpcData.getInt("faction_range"));

            npcDat.set("isUndead", NpcData.getString("isUndead"));

            npcDat.set("absorb_level", NpcData.getString("absorb_level"));
            npcDat.set("absorb_type", NpcData.getString("absorb_type"));

            L2NpcTemplate template = new L2NpcTemplate(npcDat);
            template.addVulnerability(Stats.BOW_WPN_VULN, 1);
            template.addVulnerability(Stats.BLUNT_WPN_VULN, 1);
            template.addVulnerability(Stats.DAGGER_WPN_VULN, 1);

            template.addNpcChat(CustomServerData.getInstance().getNpcChat(id));
            template.applyChaseRange(Config.NPC_CHASE_RANGES.get(id));
            CustomServerData.getInstance().setPenaltyItems(id, template);

            _npcs.put(id, template);
            if (one) {
                _log.config("NpcTable: NpcId " + id + " reloaded.");
            }
        }
        if (!one) {
            _log.config("Loading NpcTable... total " + _npcs.size() + " Npc Templates.");
        }
    }

    public void reloadNpc(int id) {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            // save a copy of the old data
            L2NpcTemplate old = getTemplate(id);
            FastMap<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>().shared("NpcTable.skills");

            if (old.getSkills() != null) {
                skills.putAll(old.getSkills());
            }

            FastTable<L2DropCategory> categories = new FastTable<L2DropCategory>();

            if (old.getDropData() != null) {
                categories.addAll(old.getDropData());
            }

            ClassId[] classIds = null;

            if (old.getTeachInfo() != null) {
                classIds = old.getTeachInfo().clone();
            }

            FastTable<L2MinionData> minions = new FastTable<L2MinionData>();

            if (old.getMinionData() != null) {
                minions.addAll(old.getMinionData());
            }

            // reload the NPC base data
            con = L2DatabaseFactory.get();
            st = con.prepareStatement("SELECT " + L2DatabaseFactory.safetyString(new String[]{"id", "idTemplate", "name", "serverSideName", "title", "serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type", "attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk", "pdef", "matk", "mdef", "atkspd", "aggro", "matkspd", "rhand", "lhand", "armor", "walkspd", "runspd", "faction_id", "faction_range", "isUndead", "absorb_level", "absorb_type"}) + " FROM npc WHERE id=?");
            st.setInt(1, id);
            rs = st.executeQuery();
            fillNpcTable(rs, true);

            // restore additional data from saved copy
            L2NpcTemplate created = getTemplate(id);

            for (L2Skill skill : skills.values()) {
                created.addSkill(skill);
            }

            for (FastMap.Entry<Integer, L2Skill> e = skills.head(), end = skills.tail(); (e = e.getNext()) != end;) {
                //String key = e.getKey(); // No typecast necessary.
                L2Skill skill = e.getValue(); // No typecast necessary.
                if (skill == null) {
                    continue;
                }

                created.addSkill(skill);
            }

            if (classIds != null) {
                for (ClassId classId : classIds) {
                    created.addTeachInfo(classId);
                }
            }

            for (L2MinionData minion : minions) {
                created.addRaidData(minion);
            }

            //
            L2World.getInstance().respawnVisibleNpcSpawns(id);
        } catch (Exception e) {
            _log.warning("NpcTable [ERROR]: Could not reload data for NPC " + id + ": " + e);
        } finally {
            Close.CSR(con, st, rs);
        }
    }

    // just wrapper
    public void reloadAllNpc() {
        restoreNpcData(true);
    }

    public void saveNpc(StatsSet npc) {
        Connect con = null;
        PreparedStatement st = null;
        String query = "";
        try {
            con = L2DatabaseFactory.get();
            FastMap<String, Object> set = npc.getSet();

            String name = "";
            TextBuilder values = new TextBuilder("");

            for (Object obj : set.keySet()) {
                name = (String) obj;

                if (!name.equalsIgnoreCase("npcId")) {
                    if (!values.toString().equalsIgnoreCase("")) {
                        values.append(", ");
                    }

                    values.append(name + " = '" + set.get(name) + "'");
                }
            }

            query = "UPDATE npc SET " + values.toString() + " WHERE id = ?";
            st = con.prepareStatement(query);
            st.setInt(1, npc.getInteger("npcId"));
            st.execute();
        } catch (Exception e) {
            _log.warning("NpcTable [ERROR]: Could not store new NPC data in database: " + e);
        } finally {
            Close.CS(con, st);
        }
    }

    public boolean isInitialized() {
        return _initialized;
    }

    public void replaceTemplate(L2NpcTemplate npc) {
        _npcs.put(npc.npcId, npc);
    }

    public L2NpcTemplate getTemplate(int id) {
        return _npcs.get(id);
    }

    public L2NpcTemplate getTemplateByName(String name) {
        for (L2NpcTemplate npcTemplate : _npcs.values()) {
            if (npcTemplate.name.equalsIgnoreCase(name)) {
                return npcTemplate;
            }
        }

        return null;
    }

    public L2NpcTemplate[] getAllOfLevel(int lvl) {
        List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();

        for (L2NpcTemplate t : _npcs.values()) {
            if (t.level == lvl) {
                list.add(t);
            }
        }

        return list.toArray(new L2NpcTemplate[list.size()]);
    }

    public L2NpcTemplate[] getAllMonstersOfLevel(int lvl) {
        List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();

        for (L2NpcTemplate t : _npcs.values()) {
            if (t.level == lvl && "L2Monster".equals(t.type)) {
                list.add(t);
            }
        }

        return list.toArray(new L2NpcTemplate[list.size()]);
    }

    public L2NpcTemplate[] getAllNpcStartingWith(String letter) {
        List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();

        for (L2NpcTemplate t : _npcs.values()) {
            if (t.name.startsWith(letter) && "L2Npc".equals(t.type)) {
                list.add(t);
            }
        }

        return list.toArray(new L2NpcTemplate[list.size()]);
    }

    /**
     * @param classType
     * @return
     */
    public Set<Integer> getAllNpcOfClassType(String classType) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param class1
     * @return
     */
    public Set<Integer> getAllNpcOfL2jClass(Class<?> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param aiType
     * @return
     */
    public Set<Integer> getAllNpcOfAiType(String aiType) {
        // TODO Auto-generated method stub
        return null;
    }
}
