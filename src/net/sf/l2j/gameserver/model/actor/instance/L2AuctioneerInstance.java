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
package net.sf.l2j.gameserver.model.actor.instance;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.AuctionManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.Auction;
import net.sf.l2j.gameserver.model.entity.Auction.Bidder;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Util;

public final class L2AuctioneerInstance extends L2FolkInstance {

    private static String _adenaName;
    private static final int COND_ALL_FALSE = 0;
    private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
    private static final int COND_REGULAR = 3;
    private Map<Integer, Auction> _pendingAuctions = new FastMap<Integer, Auction>();

    public L2AuctioneerInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
        _adenaName = ItemTable.getInstance().getItemName(Config.CLANHALL_PAYMENT);
    }

    @Override
    public void onAction(L2PcInstance player) {
        if (!Config.ALT_ALLOW_AUC) {
            player.sendActionFailed();
            return;
        }

        if (!canTarget(player)) {
            return;
        }

        player.setLastFolkNPC(this);

        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget()) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            player.sendPacket(new MyTargetSelected(getObjectId(), 0));

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        } else {
            // Calculate the distance between the L2PcInstance and the L2NpcInstance
            if (!canInteract(player)) {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            } else {
                if (!Config.ALT_ALLOW_AUC) {
                    player.sendMessage("Аукцион будет запущен через пару дней");
                    return;
                }
                showMessageWindow(player);
            }
        }
        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendActionFailed();
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (!Config.ALT_ALLOW_AUC) {
            player.sendActionFailed();
            return;
        }

        int condition = validateCondition(player);

        if (condition == COND_ALL_FALSE) {
            //TODO: html
            player.sendMessage("В другое время");
            return;
        }
        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) {
            //TODO: html
            player.sendMessage("Осада!");
            return;
        } else if (condition == COND_REGULAR) {
            StringTokenizer st = new StringTokenizer(command, " ");
            String actualCommand = st.nextToken(); // Get actual command

            String val = "";
            if (st.countTokens() >= 1) {
                val = st.nextToken();
            }

            if (actualCommand.equalsIgnoreCase("auction")) {
                if (val == "") {
                    return;
                }

                try {
                    int days = Integer.parseInt(val);
                    try {
                        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                        int bid = 0;
                        if (st.countTokens() >= 1) {
                            bid = Integer.parseInt(st.nextToken());
                        }

                        Auction a = new Auction(player.getClan().getHasHideout(), player.getClan(), days * 86400000L, bid, ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getName());
                        if (_pendingAuctions.get(a.getId()) != null) {
                            _pendingAuctions.remove(a.getId());
                        }

                        _pendingAuctions.put(a.getId(), a);

                        String filename = "data/html/auction/AgitSale3.htm";
                        NpcHtmlMessage html = NpcHtmlMessage.id(1);
                        html.setFile(filename);
                        html.replace("%x%", val);
                        html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getDesc());
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale2");
                        html.replace("adena", _adenaName);
                        html.replace("%objectId%", String.valueOf((getObjectId())));
                        player.sendPacket(html);
                    } catch (Exception e) {
                        player.sendMessage("Неправильная ставка!");
                    }
                } catch (Exception e) {
                    player.sendMessage("Неверное время аукциона!");
                }
                return;
            }
            if (actualCommand.equalsIgnoreCase("confirmAuction")) {
                try {
                    Auction a = _pendingAuctions.get(player.getClan().getHasHideout());
                    a.confirmAuction();
                    _pendingAuctions.remove(player.getClan().getHasHideout());
                } catch (Exception e) {
                    player.sendMessage("Аукцион не найден.");
                }
                return;
            } else if (actualCommand.equalsIgnoreCase("bidding")) {
                if (val == "") {
                    return;
                }
                //if(Config.DEBUG) player.sendMessage("bidding show successful");

                try {
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    int auctionId = Integer.parseInt(val);
                    //if(Config.DEBUG) player.sendMessage("auction test started");
                    String filename = "data/html/auction/AgitAuctionInfo.htm";
                    Auction a = AuctionManager.getInstance().getAuction(auctionId);

                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    if (a != null) {
                        html.replace("%AGIT_NAME%", a.getItemName());
                        html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
                        html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
                        html.replace("%AGIT_SIZE%", "30 ");
                        html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
                        html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
                        html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
                        html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
                        html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
                        html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
                        html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
                        html.replace("adena", _adenaName);
                    } else {
                        _log.warning("Auctioneer Auction null for AuctionId : " + auctionId);
                    }
                    player.sendPacket(html);
                } catch (Exception e) {
                    player.sendMessage("Аукцион не найден!");
                }

                return;
            } else if (actualCommand.equalsIgnoreCase("bid")) {
                if (val == "") {
                    return;
                }

                try {
                    int auctionId = Integer.parseInt(val);
                    try {
                        int bid = 0;
                        if (st.countTokens() >= 1) {
                            bid = Integer.parseInt(st.nextToken());
                        }

                        AuctionManager.getInstance().getAuction(auctionId).setBid(player, bid);
                    } catch (Exception e) {
                        player.sendMessage("Неправильная ставка!");
                    }
                } catch (Exception e) {
                    player.sendMessage("Аукцион не найден!");
                }

                return;
            } else if (actualCommand.equalsIgnoreCase("bid1")) {
                if (player.getClan() == null || player.getClan().getLevel() < 2) {
                    player.sendMessage("Ваш клан должен быть выше 2 уровня");
                    return;
                }

                if (val == "") {
                    return;
                }
                if ((player.getClan().getAuctionBiddedAt() > 0 && player.getClan().getAuctionBiddedAt() != Integer.parseInt(val)) || player.getClan().getHasHideout() > 0) {
                    player.sendMessage("Делать ставки можно только на один кланхолл");
                    return;
                }

                try {
                    String filename = "data/html/auction/AgitBid1.htm";

                    int minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
                    if (minimumBid == 0) {
                        minimumBid = AuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();
                    }

                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
                    html.replace("%PLEDGE_ADENA%", Util.formatAdena(player.getClan().getWarehouse().getItemCount(Config.CLANHALL_PAYMENT)));
                    html.replace("%AGIT_AUCTION_MINBID%", Util.formatAdena(minimumBid));
                    html.replace("adena", _adenaName);
                    html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
                    player.sendPacket(html);
                    return;
                } catch (Exception e) {
                    player.sendMessage("Аукцион не найден!");
                }
                return;
            } else if (actualCommand.equalsIgnoreCase("list")) {
                List<Auction> auctions = AuctionManager.getInstance().getAuctions();
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                //Limit for make new page, prevent client crash 
                int limit = 15;
                int start;
                int i = 1;
                double npage = Math.ceil((float) auctions.size() / limit);
                if (val == "") {
                    start = 1;
                } else {
                    start = limit * (Integer.parseInt(val) - 1) + 1;
                    limit *= Integer.parseInt(val);
                }
                //if (Config.DEBUG) player.sendMessage("cmd list: auction test started");
                TextBuilder items = new TextBuilder("<table width=280 border=0><tr>");
                for (int j = 1; j <= npage; j++) {
                    items.append("<td><center><a action=\"bypass -h npc_" + getObjectId() + "_list " + j + "\"> Page " + j + " </a></center></td>");
                }

                items.append("</tr></table> <table width=280 border=0>");
                for (Auction a : auctions) {
                    if (i > limit) {
                        break;
                    } else if (i < start) {
                        i++;
                        continue;
                    } else {
                        i++;
                    }
                    items.append("<tr>"
                            + "<td>" + ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation() + "</td>"
                            + "<td><a action=\"bypass -h npc_" + getObjectId() + "_bidding " + a.getId() + "\">" + a.getItemName() + "</a></td>"
                            + "<td>" + format.format(a.getEndDate()) + "</td>"
                            + "<td>" + a.getStartingBid() + "</td>"
                            + "</tr>");

                }
                items.append("</table>");
                String filename = "data/html/auction/AgitAuctionList.htm";

                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
                html.replace("%itemsField%", items.toString());
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("bidlist")) {
                int auctionId = 0;
                if (val == "") {
                    if (player.getClan().getAuctionBiddedAt() <= 0) {
                        return;
                    } else {
                        auctionId = player.getClan().getAuctionBiddedAt();
                    }
                } else {
                    auctionId = Integer.parseInt(val);
                }
                //if (Config.DEBUG) player.sendMessage("cmd bidlist: auction test started");
                TextBuilder biders = new TextBuilder();
                Map<Integer, Bidder> bidders = AuctionManager.getInstance().getAuction(auctionId).getBidders();
                for (Bidder b : bidders.values()) {
                    biders.append("<tr>"
                            + "<td>" + b.getClanName() + "</td><td>" + Util.htmlSpecialChars(b.getName()) + "</td><td>" + b.getTimeBid().get(Calendar.YEAR) + "/" + (b.getTimeBid().get(Calendar.MONTH) + 1) + "/" + b.getTimeBid().get(Calendar.DATE) + "</td><td>" + b.getBid() + "</td>"
                            + "</tr>");
                }
                String filename = "data/html/auction/AgitBidderList.htm";

                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_LIST%", biders.toString());
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
                html.replace("%x%", val);
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                biders.clear();
                biders = null;
                return;
            } else if (actualCommand.equalsIgnoreCase("selectedItems")) {
                if (player.getClan() != null && player.getClan().getHasHideout() == 0 && player.getClan().getAuctionBiddedAt() > 0) {
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    String filename = "data/html/auction/AgitBidInfo.htm";
                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
                    if (a != null) {
                        html.replace("%AGIT_NAME%", a.getItemName());
                        html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
                        html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
                        html.replace("%AGIT_SIZE%", "30 ");
                        html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
                        html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
                        html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
                        html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
                        html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
                        html.replace("%objectId%", String.valueOf(getObjectId()));
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
                        html.replace("adena", _adenaName);
                    } else {
                        _log.warning("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
                    }
                    player.sendPacket(html);
                    return;
                } else if (player.getClan() != null && AuctionManager.getInstance().getAuction(player.getClan().getHasHideout()) != null) {
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    String filename = "data/html/auction/AgitSaleInfo.htm";
                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    Auction a = AuctionManager.getInstance().getAuction(player.getClan().getHasHideout());
                    if (a != null) {
                        html.replace("%AGIT_NAME%", a.getItemName());
                        html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
                        html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
                        html.replace("%AGIT_SIZE%", "30 ");
                        html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLease()));
                        html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getLocation());
                        html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
                        html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + String.valueOf((((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60)) + " minutes");
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
                        html.replace("%AGIT_AUCTION_DESC%", ClanHallManager.getInstance().getClanHallById(a.getItemId()).getDesc());
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
                        html.replace("%id%", String.valueOf(a.getId()));
                        html.replace("%objectId%", String.valueOf(getObjectId()));
                        html.replace("adena", _adenaName);
                    } else {
                        _log.warning("Auctioneer Auction null for getHasHideout : " + player.getClan().getHasHideout());
                    }
                    player.sendPacket(html);
                    return;
                } else if (player.getClan() != null && player.getClan().getHasHideout() != 0) {
                    int ItemId = player.getClan().getHasHideout();
                    String filename = "data/html/auction/AgitInfo.htm";
                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    if (ClanHallManager.getInstance().getClanHallById(ItemId) != null) {
                        html.replace("%AGIT_NAME%", ClanHallManager.getInstance().getClanHallById(ItemId).getName());
                        html.replace("%AGIT_OWNER_PLEDGE_NAME%", player.getClan().getName());
                        html.replace("%OWNER_PLEDGE_MASTER%", Util.htmlSpecialChars(player.getClan().getLeaderName()));
                        html.replace("%AGIT_SIZE%", "30 ");
                        html.replace("%AGIT_LEASE%", String.valueOf(ClanHallManager.getInstance().getClanHallById(ItemId).getLease()));
                        html.replace("%AGIT_LOCATION%", ClanHallManager.getInstance().getClanHallById(ItemId).getLocation());
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
                        html.replace("%objectId%", String.valueOf(getObjectId()));
                        html.replace("adena", _adenaName);
                    } else {
                        _log.warning("Clan Hall ID NULL : " + ItemId + " Can be caused by concurent write in ClanHallManager");
                    }
                    player.sendPacket(html);
                    return;
                }
            } else if (actualCommand.equalsIgnoreCase("cancelBid")) {
                int bid = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
                String filename = "data/html/auction/AgitBidCancel.htm";
                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_BID%", String.valueOf(bid));
                html.replace("%AGIT_BID_REMAIN%", String.valueOf((int) (bid * 0.9)));
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("doCancelBid")) {
                if (AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()) != null) {
                    AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).cancelBid(player.getClanId());
                    player.sendMessage("Ставка отменена");
                }
                return;
            } else if (actualCommand.equalsIgnoreCase("cancelAuction")) {
                if (!((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) == L2Clan.CP_CH_AUCTION)) {
                    player.sendMessage("Недостаточно прав");
                    return;
                }
                String filename = "data/html/auction/AgitSaleCancel.htm";
                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("doCancelAuction")) {
                if (AuctionManager.getInstance().getAuction(player.getClan().getHasHideout()) != null) {
                    AuctionManager.getInstance().getAuction(player.getClan().getHasHideout()).cancelAuction();
                    player.sendMessage("Аукцион отменен");
                }
                return;
            } else if (actualCommand.equalsIgnoreCase("sale2")) {
                String filename = "data/html/auction/AgitSale2.htm";
                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale");
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("sale")) {
                if (!((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) == L2Clan.CP_CH_AUCTION)) {
                    player.sendMessage("Недостаточно прав");
                    return;
                }
                String filename = "data/html/auction/AgitSale1.htm";
                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile(filename);
                html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallManager.getInstance().getClanHallByOwner(player.getClan()).getLease()));
                html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getItemCount(Config.CLANHALL_PAYMENT)));
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
                html.replace("%objectId%", String.valueOf(getObjectId()));
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("rebid")) {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                if (!((player.getClanPrivileges() & L2Clan.CP_CH_AUCTION) == L2Clan.CP_CH_AUCTION)) {
                    player.sendMessage("Недостаточно прав");
                    return;
                }
                try {
                    String filename = "data/html/auction/AgitBid2.htm";
                    NpcHtmlMessage html = NpcHtmlMessage.id(1);
                    html.setFile(filename);
                    Auction a = AuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
                    if (a != null) {
                        html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
                        html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
                        html.replace("%AGIT_AUCTION_END%", String.valueOf(format.format(a.getEndDate())));
                        html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
                        html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
                        html.replace("adena", _adenaName);
                    } else {
                        _log.warning("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
                    }
                    player.sendPacket(html);
                } catch (Exception e) {
                    player.sendMessage("Аукцион не найден!");
                }
                return;
            } else if (actualCommand.equalsIgnoreCase("location")) {
                NpcHtmlMessage html = NpcHtmlMessage.id(1);
                html.setFile("data/html/auction/location.htm");
                html.replace("%location%", MapRegionTable.getInstance().getClosestTownName(player));
                html.replace("%LOCATION%", getPictureName(player));
                html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
                html.replace("adena", _adenaName);
                player.sendPacket(html);
                return;
            } else if (actualCommand.equalsIgnoreCase("start")) {
                showMessageWindow(player);
                return;
            }
        }
        super.onBypassFeedback(player, command);
    }

    public void showMessageWindow(L2PcInstance player) {
        String filename = "data/html/auction/auction-no.htm";

        int condition = validateCondition(player);
        if (condition == COND_BUSY_BECAUSE_OF_SIEGE) {
            filename = "data/html/auction/auction-busy.htm"; // Busy because of siege
        } else {
            filename = "data/html/auction/auction.htm";
        }

        NpcHtmlMessage html = NpcHtmlMessage.id(1);
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcId%", String.valueOf(getNpcId()));
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    private int validateCondition(L2PcInstance player) {
        if (getCastle() != null && getCastle().getCastleId() > 0) {
            if (getCastle().getSiege().getIsInProgress()) {
                return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
            } else {
                return COND_REGULAR;
            }
        }

        return COND_ALL_FALSE;
    }

    private String getPictureName(L2PcInstance plyr) {
        switch (MapRegionTable.getInstance().getMapRegion(plyr.getX(), plyr.getY())) {
            case 5:
                return "GLUDIO";
            case 6:
                return "GLUDIN";
            case 7:
                return "DION";
            case 8:
                return "GIRAN";
            case 14:
                return "RUNE";
            case 15:
                return "GODARD";
            case 16:
                return "SCHUTTGART";
            default:
                return "ADEN";
        }
    }
}