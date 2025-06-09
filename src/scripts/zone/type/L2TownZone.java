package scripts.zone.type;

import java.awt.Polygon;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.util.Location;
import net.sf.l2j.util.Rnd;
import javolution.util.FastList;
import net.sf.l2j.Config.EventReward;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import scripts.autoevents.anarchy.Anarchy;
import scripts.zone.L2ZoneType;

/**
 * A Town zone
 *
 * @author  durgus
 */
public class L2TownZone extends L2ZoneType {

    private String _townName;
    private int _townId;
    private int _redirectTownId;
    private int _taxById;
    private boolean _noPeace;
    private int _pvpArena;
    private boolean _saveBuff;
    //private boolean _isPeaceZone;
    private FastList<Location> _points = new FastList<Location>();
    private FastList<Location> _pkPoints = new FastList<Location>();
    private FastList<Polygon> _tradeZones = new FastList<Polygon>();
    private FastList<Polygon> _pvpRange = new FastList<Polygon>();
    private FastList<EventReward> _pvpReward = new FastList<EventReward>();

    public L2TownZone(int id) {
        super(id);

        _taxById = 0;

        // точки спауна
        _points.clear();
        _pkPoints.clear();
        // Default to Giran
        _redirectTownId = 9;

        // Default peace zone
        _noPeace = false;
        _pvpArena = 0;
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("name")) {
            _townName = value;
        } else if (name.equals("townId")) {
            _townId = Integer.parseInt(value);
        } else if (name.equals("redirectTownId")) {
            _redirectTownId = Integer.parseInt(value);
        } else if (name.equals("taxById")) {
            _taxById = Integer.parseInt(value);
        } else if (name.equals("pvpType")) {
            _pvpArena = Integer.parseInt(value);
            if (_pvpArena == 7) {
                _noPeace = true;
            }
        } else if (name.equals("noPeace")) {
            _noPeace = Boolean.parseBoolean(value);
        } else if (name.equals("saveBuff")) {
            _saveBuff = Boolean.parseBoolean(value);
        } else if (name.equals("restartPoints")) {
            String[] token = value.split(";");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }

                String[] loc = point.split(",");
                Integer x = Integer.valueOf(loc[0]);
                Integer y = Integer.valueOf(loc[1]);
                Integer z = Integer.valueOf(loc[2]);
                if (x == null || y == null || z == null) {
                    continue;
                }

                _points.add(new Location(x, y, z));
            }
        } else if (name.equals("restartPointsPk")) {
            String[] token = value.split(";");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }

                String[] loc = point.split(",");
                Integer x = Integer.valueOf(loc[0]);
                Integer y = Integer.valueOf(loc[1]);
                Integer z = Integer.valueOf(loc[2]);
                if (x == null || y == null || z == null) {
                    continue;
                }

                _pkPoints.add(new Location(x, y, z));
            }
        } else if (name.equals("tradeRange")) {
            String[] token = value.split("#");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }

                Polygon _tradePoly = new Polygon();
                String[] token2 = point.split(";");
                for (String point2 : token2) {
                    if (point2.equals("")) {
                        continue;
                    }

                    String[] loc = point2.split(",");
                    Integer x = Integer.valueOf(loc[0]);
                    Integer y = Integer.valueOf(loc[1]);
                    if (x == null || y == null) {
                        continue;
                    }

                    _tradePoly.addPoint(x, y);
                }
                _tradeZones.add(_tradePoly);
            }
        } else if (name.equals("pvpRange")) {
            String[] token = value.split("#");
            for (String point : token) {
                if (point.equals("")) {
                    continue;
                }

                Polygon _pvpPoly = new Polygon();
                String[] token2 = point.split(";");
                for (String point2 : token2) {
                    if (point2.equals("")) {
                        continue;
                    }

                    String[] loc = point2.split(",");
                    Integer x = Integer.valueOf(loc[0]);
                    Integer y = Integer.valueOf(loc[1]);
                    if (x == null || y == null) {
                        continue;
                    }

                    _pvpPoly.addPoint(x, y);
                }
                _pvpRange.add(_pvpPoly);
            }
        } else if (name.equals("pvpRewards")) {
            String[] token = value.split(";");
            for (String item : token) {
                if (item.isEmpty()) {
                    continue;
                }

                String[] id = item.split(",");
                try {
                    _pvpReward.add(new EventReward(Integer.parseInt(id[0]), Integer.parseInt(id[1]), Integer.parseInt(id[2])));
                } catch (NumberFormatException nfe) {
                }
            }
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(L2Character character) {
        if (_townId == 18) {
            character.setInDino(true);
        }
        checkPvpType(character, true);
    }

    @Override
    protected void onExit(L2Character character) {
        if (_townId == 18) {
            character.setInDino(false);
        }
        checkPvpType(character, false);
    }

    private void checkPvpType(L2Character character, boolean in) {
        if (Config.ANARCHY_ENABLE && Anarchy.getEvent().isInBattle(_townId)) {
            character.setPVPArena(in);
        }

        if (_saveBuff) {
            character.setSaveBuff(in);
        }
        switch (_pvpArena) {
            case 46:
                character.setFreePvp(in);
                character.setPVPArena(in);
                break;
            case 14:
                character.setPVPArena(in);
                character.setFreeArena(in);
                break;
            case 7:
                character.setFreePk(in);
                break;
        }
    }

    @Override
    protected void onDieInside(L2Character character) {
    }

    @Override
    protected void onReviveInside(L2Character character) {
    }

    /**
     * Returns this town zones name
     * @return
     */
    @Deprecated
    public String getName() {
        return _townName;
    }

    /**
     * Returns this zones town id (if any)
     * @return
     */
    @Override
    public int getTownId() {
        return _townId;
    }

    /**
     * Gets the id for this town zones redir town
     * @return
     */
    @Deprecated
    public int getRedirectTownId() {
        return _redirectTownId;
    }

    /**
     * Returns this zones spawn location
     * @return
     */
    public Location getSpawnLoc() {
        return _points.get(Rnd.get(_points.size() - 1));
    }

    public Location getSpawnLocPk() {
        return _pkPoints.get(Rnd.get(_pkPoints.size() - 1));
    }

    /**
     * Returns this town zones castle id
     * @return
     */
    public final int getTaxById() {
        return _taxById;
    }

    public final boolean isNoPeace() {
        return _noPeace;
    }

    public boolean isPeaceZone() {
        return false;
    }

    public void reValidateZone() {
        for (L2Character temp : _characterList.values()) {
            if (temp == null) {
                continue;
            }

            onEnter(temp);
        }
    }

    @Override
    public boolean isPvP(final int x, final int y) {
        if (_noPeace) {
            return true;
        }

        Polygon poly = null;
        for (FastList.Node<Polygon> n = _pvpRange.head(), end = _pvpRange.tail(); (n = n.getNext()) != end;) {
            poly = n.getValue();
            if (poly == null) {
                continue;
            }

            if (poly.contains(x, y)) {
                return true;
            }
        }
        poly = null;
        return false;
    }

    @Override
    public boolean isArena() {
        return (_pvpArena == 1 || _pvpArena == 46);
    }

    @Override
    public boolean isInsideTradeZone(final int x, final int y) {
        if (_tradeZones.isEmpty()) {
            return false;
        }

        Polygon poly = null;
        for (FastList.Node<Polygon> n = _tradeZones.head(), end = _tradeZones.tail(); (n = n.getNext()) != end;) {
            poly = n.getValue();
            if (poly == null) {
                continue;
            }

            if (poly.contains(x, y)) {
                return true;
            }
        }
        poly = null;
        return false;
    }

    @Override
    public boolean isTownZone() {
        return true;
    }

    @Override
    public L2TownZone getTownZone() {
        return this;
    }

    @Override
    public void givePvpRewards(L2PcInstance player) {
        System.out.println("###7#");
        if (_pvpReward.isEmpty()) {
            return;
        }
        System.out.println("###8#");
        EventReward reward = null;
        for (FastList.Node<EventReward> k = _pvpReward.head(), endk = _pvpReward.tail(); (k = k.getNext()) != endk;) {
            reward = k.getValue();
            if (reward == null) {
                continue;
            }

            if (reward.chance == 100 || Rnd.get(100) < reward.chance) {
                player.addItem("pvp_bonus_town", reward.id, reward.count, player, true);
            }
        }
        reward = null;
    }
}
