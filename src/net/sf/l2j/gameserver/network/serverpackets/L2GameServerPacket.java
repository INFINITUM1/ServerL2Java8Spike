package net.sf.l2j.gameserver.network.serverpackets;

//import java.util.logging.Logger;
import net.sf.l2j.gameserver.network.L2GameClient;
import org.mmocore.network.nio.impl.SendablePacket;

/**
 *
 * @author KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient> {
    //private static final Logger _log = Logger.getLogger(L2GameServerPacket.class.getName());

    /**
     * @see org.mmocore.network.SendablePacket#write()
     */
    @Override
    public final boolean write() {
        /*System.out.println(getClient().toString() + "Server: " + getType());
         L2PcInstance activeChar = client.getActiveChar();
         if (activeChar != null && activeChar.isSpy()) {
         Log.add(TimeLogger.getLogTime() + " Player: " + activeChar.getName() + "// " + client.toString() + "Server: " + getType(), "packet_spy");
         }*/

        try {
            writeImpl();
            gc();
            return true;
        } catch (Throwable t) {
            System.out.println("Client: " + getClient() + " - Failed writing: " + getType());
            t.printStackTrace();
        }
        //System.out.println("Server-WriteImple: "+getType());
        return false;
    }

    protected abstract void writeImpl();

    /**
     * @return A String with this packet name for debuging purposes
     */
    public String getType() {
        return "S." + getClass().getSimpleName();
    }

    // очищаем и выводим из памяти массивы с данными (если есть) после отправки одиночного пакета
    public void gc() {
    }
    // очищаем и выводим из памяти массивы с данными (если есть) после отправки пакета окружающим

    public void gcb() {
    }

    public boolean isCharInfo() {
        return false;
    }
}
