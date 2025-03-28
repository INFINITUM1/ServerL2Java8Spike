package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.util.FastList;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ArmorSet;
import net.sf.l2j.mysql.Close;
import net.sf.l2j.mysql.Connect;
import net.sf.l2j.util.log.AbstractLogger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ArmorSetsTable {

    private static Logger _log = AbstractLogger.getLogger(ArmorSetsTable.class.getName());
    private static ArmorSetsTable _instance;

    private static final FastMap<Integer, L2ArmorSet> _armorSets = new FastMap<Integer, L2ArmorSet>().shared("ArmorSetsTable._armorSets");

    public static ArmorSetsTable getInstance() {
        if (_instance == null) {
            _instance = new ArmorSetsTable();
        }
        return _instance;
    }

    private ArmorSetsTable() {
        //_armorSets = new FastMap<Integer,L2ArmorSet>().shared();
        loadData();
    }

    private void loadData() {
        Connect con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);
            st = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill FROM armorsets");
            rs = st.executeQuery();
            rs.setFetchSize(50);

            while (rs.next()) {
                int chest = rs.getInt("chest");
                int legs = rs.getInt("legs");
                int head = rs.getInt("head");
                int gloves = rs.getInt("gloves");
                int feet = rs.getInt("feet");
                int skill_id = rs.getInt("skill_id");
                int shield = rs.getInt("shield");
                int shield_skill_id = rs.getInt("shield_skill_id");
                int enchant6skill = rs.getInt("enchant6skill");
                _armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill));
            }

        } catch (Exception e) {
            _log.severe("ArmorSetsTable: Error reading ArmorSets table: " + e);
        } finally {
            Close.CSR(con, st, rs);
        }

        if (Config.ARMORSETS_XML) {
            loadXML();
        }
        _log.config("Loading ArmorSetsTable... total " + _armorSets.size() + " armor sets.");
    }

    public boolean setExists(int chestId) {
        return _armorSets.containsKey(chestId);
    }

    public L2ArmorSet getSet(int chestId) {
        return _armorSets.get(chestId);
    }

    private void loadXML() {
        try {
            File file = new File(Config.DATAPACK_ROOT, "data/armorsets.xml");
            if (!file.exists()) {
                _log.config("ArmorSetsTable [ERROR]: data/armorsets.xml doesn't exist");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            Document doc = factory.newDocumentBuilder().parse(file);

            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if ("list".equalsIgnoreCase(n.getNodeName())) {
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                        if ("set".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs;// = d.getAttributes();
                            //String name = attrs.getNamedItem("name").getNodeValue();
                            int skill_id = 0, enchant6skill = 0, chest = 0;

                            FastList<Integer> items = new FastList<>();
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("bonus".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    skill_id = Integer.parseInt(attrs.getNamedItem("skill").getNodeValue());
                                    enchant6skill = Integer.parseInt(attrs.getNamedItem("enchant6skill").getNodeValue());
                                } else if ("items".equalsIgnoreCase(cd.getNodeName())) { //<skill id='1062' lvl='2' animation='1015' chance='90' delay='3'/>
                                    attrs = cd.getAttributes();

                                    chest = Integer.parseInt(attrs.getNamedItem("chest").getNodeValue());
                                    String[] list = attrs.getNamedItem("other").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        items.add(Integer.parseInt(list_id));
                                    }
                                }
                            }

                            _armorSets.put(chest, new L2ArmorSet(chest, items, skill_id, enchant6skill));
                        } else if ("set_special".equalsIgnoreCase(d.getNodeName())) {
                            NamedNodeMap attrs;// = d.getAttributes();
                            //String name = attrs.getNamedItem("name").getNodeValue();
                            int skill_id = 0, enchant6skill = 0;

                            FastMap<Integer, FastList<Integer>> items = new FastMap<>();
                            FastList<Integer> chests = new FastList<>();
                            /*FastList<Integer> legs = new FastList<>();
                             FastList<Integer> head = new FastList<>();
                             FastList<Integer> gloves = new FastList<>();
                             FastList<Integer> feet = new FastList<>();*/
                            for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                                if ("bonus".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    skill_id = Integer.parseInt(attrs.getNamedItem("skill").getNodeValue());
                                    enchant6skill = Integer.parseInt(attrs.getNamedItem("enchant6skill").getNodeValue());
                                } else if ("chest".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    String[] list = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        chests.add(Integer.parseInt(list_id));
                                    }
                                } else if ("legs".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<Integer> legs = new FastList<>();
                                    String[] list = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        legs.add(Integer.parseInt(list_id));
                                    }
                                    items.put(0, legs);
                                } else if ("head".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<Integer> head = new FastList<>();
                                    String[] list = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        head.add(Integer.parseInt(list_id));
                                    }
                                    items.put(1, head);
                                } else if ("gloves".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<Integer> gloves = new FastList<>();
                                    String[] list = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        gloves.add(Integer.parseInt(list_id));
                                    }
                                    items.put(2, gloves);
                                } else if ("feet".equalsIgnoreCase(cd.getNodeName())) {
                                    attrs = cd.getAttributes();

                                    FastList<Integer> feet = new FastList<>();
                                    String[] list = attrs.getNamedItem("items").getNodeValue().split(",");
                                    for (String list_id : list) {
                                        if (list_id.isEmpty()) {
                                            continue;
                                        }
                                        feet.add(Integer.parseInt(list_id));
                                    }
                                    items.put(3, feet);
                                }
                            }

                            for (Integer chest : chests) {
                                if (chest == null) {
                                    continue;
                                }
                                _armorSets.put(chest, new L2ArmorSet(chest, items, skill_id, enchant6skill, true));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.warning("ArmorSetsTable [ERROR]: loadXML() " + e.toString());
        }
    }
}
