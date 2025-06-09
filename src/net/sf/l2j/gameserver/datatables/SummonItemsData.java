package net.sf.l2j.gameserver.datatables;

import java.util.logging.Logger;
import java.io.File;
import javolution.util.FastMap;
import java.util.Scanner;

import net.sf.l2j.gameserver.model.L2SummonItem;
import net.sf.l2j.util.log.AbstractLogger;

public class SummonItemsData {

    private static final Logger _log = AbstractLogger.getLogger(SummonItemsData.class.getName());
    private static FastMap<Integer, L2SummonItem> _summonitems = new FastMap<Integer, L2SummonItem>().shared("SummonItemsData._summonitems");
    private static SummonItemsData _instance;

    public static SummonItemsData getInstance() {
        if (_instance == null) {
            _instance = new SummonItemsData();
        }

        return _instance;
    }

    public SummonItemsData() {
        //_summonitems = new FastMap<Integer, L2SummonItem>().shared();

        Scanner s;

        try {
            s = new Scanner(new File("./data/summon_items.csv"));
        } catch (Exception e) {
            _log.warning("Summon items data: Can not find './data/summon_items.csv'");
            return;
        }

        int lineCount = 0,
                commentLinesCount = 0;

        while (s.hasNextLine()) {
            lineCount++;

            String line = s.nextLine();

            if (line.startsWith("#")) {
                commentLinesCount++;
                continue;
            } else if (line.equals("")) {
                continue;
            }

            String[] lineSplit = line.split(";");
            boolean ok = true;
            int itemID = 0,
                    npcID = 0;
            byte summonType = 0;

            try {
                itemID = Integer.parseInt(lineSplit[0]);
                npcID = Integer.parseInt(lineSplit[1]);
                summonType = Byte.parseByte(lineSplit[2]);
            } catch (Exception e) {
                _log.warning("Summon items data: Error in line " + lineCount + " -> incomplete/invalid data or wrong seperator!");
                _log.warning("		" + line);
                ok = false;
            }

            if (!ok) {
                continue;
            }

            L2SummonItem summonitem = new L2SummonItem(itemID, npcID, summonType);
            _summonitems.put(itemID, summonitem);
        }

        _log.info("Loading Summon items data... total " + _summonitems.size() + " items.");
    }

    public L2SummonItem getSummonItem(int itemId) {
        return _summonitems.get(itemId);
    }

    public int[] itemIDs() {
        int size = _summonitems.size();
        int[] result = new int[size];
        int i = 0;
        for (L2SummonItem si : _summonitems.values()) {
            result[i] = si.getItemId();
            i++;
        }
        return result;
    }
}
