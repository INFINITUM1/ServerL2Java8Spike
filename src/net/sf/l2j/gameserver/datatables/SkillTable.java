package net.sf.l2j.gameserver.datatables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.SkillsEngine;
import net.sf.l2j.gameserver.templates.L2WeaponType;

public class SkillTable {
    //private static Logger _log = Logger.getLogger(SkillTable.class.getName());

    private static SkillTable _instance;
    private HashMap<Integer, L2Skill> _skills;
    private boolean _initialized = true;

    public static SkillTable getInstance() {
        if (_instance == null) {
            _instance = new SkillTable();
        }
        return _instance;
    }

    private SkillTable() {
        _skills = new HashMap<Integer, L2Skill>();
        SkillsEngine.getInstance().loadAllSkills(_skills);
        parceAugmentsInfo();
    }

    public void reload() {
        _instance = new SkillTable();
    }

    public boolean isInitialized() {
        return _initialized;
    }

    /**
     * Provides the skill hash
     * @param skill The L2Skill to be hashed
     * @return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel())
     */
    public static int getSkillHashCode(L2Skill skill) {
        return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel());
    }

    /**
     * Centralized method for easier change of the hashing sys
     * @param skillId The Skill Id
     * @param skillLevel The Skill Level
     * @return The Skill hash number
     */
    public static int getSkillHashCode(int skillId, int skillLevel) {
        return skillId * 256 + skillLevel;
    }

    public L2Skill getInfo(int skillId, int level) {
        return _skills.get(SkillTable.getSkillHashCode(skillId, level));
    }

    public int getMaxLevel(int magicId, int level) {
        L2Skill temp;

        while (level < 100) {
            level++;
            temp = _skills.get(SkillTable.getSkillHashCode(magicId, level));

            if (temp == null) {
                return level - 1;
            }
        }

        return level;
    }
    private static final L2WeaponType[] weaponDbMasks = {
        L2WeaponType.ETC,
        L2WeaponType.BOW,
        L2WeaponType.POLE,
        L2WeaponType.DUALFIST,
        L2WeaponType.DUAL,
        L2WeaponType.BLUNT,
        L2WeaponType.SWORD,
        L2WeaponType.DAGGER,
        L2WeaponType.BIGSWORD,
        L2WeaponType.ROD,
        L2WeaponType.BIGBLUNT
    };

    public int calcWeaponsAllowed(int mask) {
        if (mask == 0) {
            return 0;
        }

        int weaponsAllowed = 0;

        for (int i = 0; i < weaponDbMasks.length; i++) {
            if ((mask & (1 << i)) != 0) {
                weaponsAllowed |= weaponDbMasks[i].mask();
            }
        }

        return weaponsAllowed;
    }
    private static FastMap<Integer, String> _augInfo = new FastMap<Integer, String>().shared("SkillTable._augInfo");

    private static void parceAugmentsInfo() {
        _augInfo.clear();
        LineNumberReader lnr = null;
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File Data = new File("./data/augmentation_info.txt");
            if (!Data.exists()) {
                //System.out.println("[ERROR] SkillTable, parceAugmentsInfo() '/data/augmentation_info.txt' not founded. ");
                return;
            }

            fr = new FileReader(Data);
            br = new BufferedReader(fr);
            lnr = new LineNumberReader(br);
            //#номер_сообщения,текст
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }

                String[] msgs = line.split("	");
                try {
                    _augInfo.put(Integer.parseInt(msgs[0]), msgs[3]);
                } catch (Exception ignrd) {
                }
            }
        } catch (final Exception e) {
            System.out.println("[ERROR] SkillTable, parceAugmentsInfo() error: " + e);
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
                if (br != null) {
                    br.close();
                }
                if (lnr != null) {
                    lnr.close();
                }
            } catch (Exception e1) {
            }
        }
    }

    public static String getAugmentInfo(int id) {
        return findAugInfo(_augInfo.get(id));
    }

    private static String findAugInfo(String info) {
        if (info == null || info.isEmpty()) {
            return "Нет информации.";
        }

        return info;
    }
}
