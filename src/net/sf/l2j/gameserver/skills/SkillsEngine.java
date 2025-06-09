
package net.sf.l2j.gameserver.skills;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Item;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2EtcItem;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.util.log.AbstractLogger;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SkillsEngine {

    protected static final Logger _log = AbstractLogger.getLogger(SkillsEngine.class.getName());

	private static final SkillsEngine _instance = new SkillsEngine();

	private List<File> _armorFiles     = new LinkedList<File>();
	private List<File> _weaponFiles    = new LinkedList<File>();
	private List<File> _etcitemFiles   = new LinkedList<File>();
	private List<File> _skillFiles     = new LinkedList<File>();

    public static SkillsEngine getInstance()
	{
		return _instance;
	}

	private SkillsEngine()
	{
		//hashFiles("data/stats/etcitem", _etcitemFiles);
		hashFiles("data/stats/armor", _armorFiles);
		hashFiles("data/stats/weapon", _weaponFiles);
		hashFiles("data/stats/skills", _skillFiles);
	}

	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, dirname);
		if (!dir.exists())
		{
			_log.config("Dir "+dir.getAbsolutePath()+" not exists");
			return;
		}
		File[] files = dir.listFiles();
		for (File f : files)
		{
			if (f.getName().endsWith(".xml"))
			    if (!f.getName().startsWith("custom"))
				hash.add(f);
		}
		File customfile = new File(Config.DATAPACK_ROOT, dirname+"/custom.xml");
		if (customfile.exists())
		    hash.add(customfile);
	}

	public List<L2Skill> loadSkills(File file)
	{
		if (file == null)
		{
			_log.config("Skill file not found.");
			return null;
		}
		DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}

	public void loadAllSkills(HashMap<Integer, L2Skill> allSkills)
	{
		int count = 0;
		for (File file : _skillFiles)
		{
			List<L2Skill> s  = loadSkills(file);
			if (s == null)
				continue;
			for (L2Skill skill : s)
            {
				allSkills.put(SkillTable.getSkillHashCode(skill), skill);
				count++;
            }
		}
		_log.config("SkillsEngine: Loaded "+count+" Skill templates from XML files.");
	}

    public List<L2Armor> loadArmors(FastMap<Integer, Item> armorData)
    {
        List<L2Armor> list  = new LinkedList<L2Armor>();
        for (L2Item item : loadData2(armorData, _armorFiles))
        {
            list.add((L2Armor)item);
        }
        return list;
    }

    public List<L2Weapon> loadWeapons(FastMap<Integer, Item> weaponData)
    {
        List<L2Weapon> list  = new LinkedList<L2Weapon>();
        for (L2Item item : loadData2(weaponData, _weaponFiles))
        {
            list.add((L2Weapon)item);
        }
        return list;
    }

    public List<L2EtcItem> loadItems(FastMap<Integer, Item> itemData)
    {
        List<L2EtcItem> list  = new LinkedList<L2EtcItem>();
        for (L2Item item : loadData2(itemData, _etcitemFiles))
        {
            list.add((L2EtcItem)item);
        }
        if (list.size() == 0)
        {
            for (Item item : itemData.values())
            {
                list.add(new L2EtcItem((L2EtcItemType)item.type, item.set));
            }
        }
        return list;
    }
	
    public List<L2Item> loadData2(FastMap<Integer, Item> itemData, List<File> files)
    {
        List<L2Item> list  = new LinkedList<L2Item>();
        for (File f : files)
        {
            DocumentItem document   = new DocumentItem(itemData, f);
            document.parse();
            list.addAll(document.getItemList());
        }
        return list;
    }

    public List<L2Item> loadData(HashMap<Integer, Item> itemData, List<File> files)
    {
        List<L2Item> list  = new LinkedList<L2Item>();
        for (File f : files)
        {
            DocumentItem document   = new DocumentItem(itemData, f);
            document.parse();
            list.addAll(document.getItemList());
        }
        return list;
    }
}
