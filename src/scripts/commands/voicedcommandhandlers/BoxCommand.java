package scripts.commands.voicedcommandhandlers;

import scripts.items.IItemHandler;
import scripts.items.ItemHandler;
import scripts.items.itemhandlers.ExtractableItems;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import scripts.commands.IVoicedCommandHandler;

public class BoxCommand implements IVoicedCommandHandler
{
    private static final String[] VOICED_COMMANDS = { "box" };
    private static final int BOX_ID = 12712;

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance player, String target)
    {
        String[] args = command.trim().split("\\s+");

        // просто .box
        if (args.length == 1)
        {
            player.sendMessage("Use .box all or .box <count>");
            return true;
        }

        int boxesCount = player.getItemCount(BOX_ID);

        // .box all
        if (args.length == 2 && args[1].equalsIgnoreCase("all"))
        {
            if (boxesCount == 0)
            {
                player.sendMessage("You have no boxes to open.");
                return true;
            }

            openBoxes(player, boxesCount);
            return true;
        }

        // .box <number>
        if (args.length == 2)
        {
            int count;

            try
            {
                count = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                player.sendMessage("Use .box all or .box <count>");
                return true;
            }

            if (count <= 0)
            {
                player.sendMessage("Count must be greater than zero.");
                return true;
            }

            if (boxesCount < count)
            {
                player.sendMessage("Not enough boxes.");
                return true;
            }

            openBoxes(player, count);
            return true;
        }

        // любой другой мусор
        player.sendMessage("Use .box all or .box <count>");
        return true;
    }
private void openBoxes(L2PcInstance player, int count)
{
    L2ItemInstance box = player.getInventory().getItemByItemId(BOX_ID);
    if (box == null)
        return;

    IItemHandler handler = ItemHandler.getInstance().getItemHandler(box.getItemId());
    if (!(handler instanceof ExtractableItems))
    {
        player.sendMessage("This item cannot be opened in bulk.");
        return;
    }

    ExtractableItems extract = (ExtractableItems) handler;

    extract.extractMultiple(player, box, count);
}
    // private void openBoxes(L2PcInstance player, long count)
    // {
    //     for (int i = 0; i < count; i++)
    //     {
    //         L2ItemInstance box = player.getInventory().getItemByItemId(BOX_ID);
    //         if (box == null)
    //             return;

    //         ItemHandler.getInstance().useItem(player, box, false);
    //     }
    // }

    @Override
    public String[] getVoicedCommandList()
    {
        return VOICED_COMMANDS;
    }
}
