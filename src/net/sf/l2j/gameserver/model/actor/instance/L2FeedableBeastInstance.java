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

import javolution.util.FastMap;
import javolution.util.FastTable;

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.util.Rnd;

// This class is here mostly for convinience and for avoidance of hardcoded IDs.
// It refers to Beast (mobs) that can be attacked but can also be fed
// For example, the Beast Farm's Alpen Buffalo.
// This class is only trully used by the handlers in order to check the correctness
// of the target.  However, no additional tasks are needed, since they are all
// handled by scripted AI.
public class L2FeedableBeastInstance extends L2MonsterInstance {

    public static FastMap<Integer, growthInfo> growthCapableMobs = new FastMap<Integer, growthInfo>().shared("L2FeedableBeastInstance.growthCapableMobs");
    public static FastMap<Integer, Integer> madCowPolymorph = new FastMap<Integer, Integer>().shared("L2FeedableBeastInstance.madCowPolymorph");
    static FastTable<Integer> tamedBeasts = new FastTable<Integer>();
    static FastTable<Integer> feedableBeasts = new FastTable<Integer>();
    public static FastMap<Integer, Integer> feedInfo = new FastMap<Integer, Integer>();
    private static int GOLDEN_SPICE = 0;
    private static int CRYSTAL_SPICE = 1;
    private static int SKILL_GOLDEN_SPICE = 2188;
    private static int SKILL_CRYSTAL_SPICE = 2189;
    private static String[][] text = new String[][]{
        {
            "What did you just do to me?",
            "You want to tame me, huh?",
            "Do not give me this. Perhaps you will be in danger.",
            "Bah bah. What is this unpalatable thing?",
            "My belly has been complaining. This hit the spot.",
            "What is this? Can I eat it?",
            "You don't need to worry about me.",
            "Delicious food, thanks.",
            "I am starting to like you!",
            "Gulp"},
        {
            "I do not think you have given up on the idea of taming me.",
            "That is just food to me.  Perhaps I can eat your hand too.",
            "Will eating this make me fat? Ha ha",
            "Why do you always feed me?",
            "Do not trust me. I may betray you"},
        {
            "Destroy",
            "Look what you have done!",
            "Strange feeling...! Evil intentions grow in my heart...!",
            "It is happenning!",
            "This is sad...Good is sad...!"}};

    public L2FeedableBeastInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);

        // Alpen Kookabura

        growthCapableMobs.put(21451, new growthInfo(0, new int[][][]{{{21452, 21453, 21454, 21455}}, {{21456, 21457, 21458, 21459}}}, 100));

        growthCapableMobs.put(21452, new growthInfo(1, new int[][][]{{{21460, 21462}}, {}}, 40));
        growthCapableMobs.put(21453, new growthInfo(1, new int[][][]{{{21461, 21463}}, {}}, 40));
        growthCapableMobs.put(21454, new growthInfo(1, new int[][][]{{{21460, 21462}}, {}}, 40));
        growthCapableMobs.put(21455, new growthInfo(1, new int[][][]{{{21461, 21463}}, {}}, 40));

        growthCapableMobs.put(21456, new growthInfo(1, new int[][][]{{}, {{21464, 21466}}}, 40));
        growthCapableMobs.put(21457, new growthInfo(1, new int[][][]{{}, {{21465, 21467}}}, 40));
        growthCapableMobs.put(21458, new growthInfo(1, new int[][][]{{}, {{21464, 21466}}}, 40));
        growthCapableMobs.put(21459, new growthInfo(1, new int[][][]{{}, {{21465, 21467}}}, 40));

        growthCapableMobs.put(21460, new growthInfo(2, new int[][][]{{{21468, 21824}, {16017, 16018}}, {}}, 25));
        growthCapableMobs.put(21461, new growthInfo(2, new int[][][]{{{21469, 21825}, {16017, 16018}}, {}}, 25));
        growthCapableMobs.put(21462, new growthInfo(2, new int[][][]{{{21468, 21824}, {16017, 16018}}, {}}, 25));
        growthCapableMobs.put(21463, new growthInfo(2, new int[][][]{{{21469, 21825}, {16017, 16018}}, {}}, 25));

        growthCapableMobs.put(21464, new growthInfo(2, new int[][][]{{}, {{21468, 21824}, {16017, 16018}}}, 25));
        growthCapableMobs.put(21465, new growthInfo(2, new int[][][]{{}, {{21469, 21825}, {16017, 16018}}}, 25));
        growthCapableMobs.put(21466, new growthInfo(2, new int[][][]{{}, {{21468, 21824}, {16017, 16018}}}, 25));
        growthCapableMobs.put(21467, new growthInfo(2, new int[][][]{{}, {{21469, 21825}, {16017, 16018}}}, 25));

        // Alpen Buffalo

        growthCapableMobs.put(21470, new growthInfo(0, new int[][][]{{{21471, 21472, 21473, 21474}}, {{21475, 21476, 21477, 21478}}}, 100));

        growthCapableMobs.put(21471, new growthInfo(1, new int[][][]{{{21479, 21481}}, {}}, 40));
        growthCapableMobs.put(21472, new growthInfo(1, new int[][][]{{{21481, 21482}}, {}}, 40));
        growthCapableMobs.put(21473, new growthInfo(1, new int[][][]{{{21479, 21481}}, {}}, 40));
        growthCapableMobs.put(21474, new growthInfo(1, new int[][][]{{{21480, 21482}}, {}}, 40));

        growthCapableMobs.put(21475, new growthInfo(1, new int[][][]{{}, {{21483, 21485}}}, 40));
        growthCapableMobs.put(21476, new growthInfo(1, new int[][][]{{}, {{21484, 21486}}}, 40));
        growthCapableMobs.put(21477, new growthInfo(1, new int[][][]{{}, {{21483, 21485}}}, 40));
        growthCapableMobs.put(21478, new growthInfo(1, new int[][][]{{}, {{21484, 21486}}}, 40));

        growthCapableMobs.put(21479, new growthInfo(2, new int[][][]{{{21487, 21826}, {16013, 16014}}, {}}, 25));
        growthCapableMobs.put(21480, new growthInfo(2, new int[][][]{{{21488, 21827}, {16013, 16014}}, {}}, 25));
        growthCapableMobs.put(21481, new growthInfo(2, new int[][][]{{{21487, 21826}, {16013, 16014}}, {}}, 25));
        growthCapableMobs.put(21482, new growthInfo(2, new int[][][]{{{21488, 21827}, {16013, 16014}}, {}}, 25));

        growthCapableMobs.put(21483, new growthInfo(2, new int[][][]{{}, {{21487, 21826}, {16013, 16014}}}, 25));
        growthCapableMobs.put(21484, new growthInfo(2, new int[][][]{{}, {{21488, 21827}, {16013, 16014}}}, 25));
        growthCapableMobs.put(21485, new growthInfo(2, new int[][][]{{}, {{21487, 21826}, {16013, 16014}}}, 25));
        growthCapableMobs.put(21486, new growthInfo(2, new int[][][]{{}, {{21488, 21827}, {16013, 16014}}}, 25));

        // Alpen Cougar

        growthCapableMobs.put(21489, new growthInfo(0, new int[][][]{{{21490, 21491, 21492, 21493}}, {{21494, 21495, 21496, 21497}}}, 100));

        growthCapableMobs.put(21490, new growthInfo(1, new int[][][]{{{21498, 21500}}, {}}, 40));
        growthCapableMobs.put(21491, new growthInfo(1, new int[][][]{{{21499, 21501}}, {}}, 40));
        growthCapableMobs.put(21492, new growthInfo(1, new int[][][]{{{21498, 21500}}, {}}, 40));
        growthCapableMobs.put(21493, new growthInfo(1, new int[][][]{{{21499, 21501}}, {}}, 40));

        growthCapableMobs.put(21494, new growthInfo(1, new int[][][]{{}, {{21502, 21504}}}, 40));
        growthCapableMobs.put(21495, new growthInfo(1, new int[][][]{{}, {{21503, 21505}}}, 40));
        growthCapableMobs.put(21496, new growthInfo(1, new int[][][]{{}, {{21502, 21504}}}, 40));
        growthCapableMobs.put(21497, new growthInfo(1, new int[][][]{{}, {{21503, 21505}}}, 40));

        growthCapableMobs.put(21498, new growthInfo(2, new int[][][]{{{21506, 21828}, {16015, 16016}}, {}}, 25));
        growthCapableMobs.put(21499, new growthInfo(2, new int[][][]{{{21507, 21829}, {16015, 16016}}, {}}, 25));
        growthCapableMobs.put(21500, new growthInfo(2, new int[][][]{{{21506, 21828}, {16015, 16016}}, {}}, 25));
        growthCapableMobs.put(21501, new growthInfo(2, new int[][][]{{{21507, 21829}, {16015, 16016}}, {}}, 25));

        growthCapableMobs.put(21502, new growthInfo(2, new int[][][]{{}, {{21506, 21828}, {16015, 16016}}}, 25));
        growthCapableMobs.put(21503, new growthInfo(2, new int[][][]{{}, {{21507, 21829}, {16015, 16016}}}, 25));
        growthCapableMobs.put(21504, new growthInfo(2, new int[][][]{{}, {{21506, 21828}, {16015, 16016}}}, 25));
        growthCapableMobs.put(21505, new growthInfo(2, new int[][][]{{}, {{21507, 21829}, {16015, 16016}}}, 25));

        madCowPolymorph.put(21824, 21468);
        madCowPolymorph.put(21825, 21469);
        madCowPolymorph.put(21826, 21487);
        madCowPolymorph.put(21827, 21488);
        madCowPolymorph.put(21828, 21506);
        madCowPolymorph.put(21829, 21507);

        for (Integer i = 16013; i <= 16018; i++) {
            tamedBeasts.add(i);
        }
        for (Integer i = 16013; i <= 16019; i++) {
            feedableBeasts.add(i);
        }
        for (Integer i = 21451; i <= 21507; i++) {
            feedableBeasts.add(i);
        }
        for (Integer i = 21824; i <= 21829; i++) {
            feedableBeasts.add(i);
        }
    }

    private void spawnNext(final L2PcInstance player, final int growthLevel, final int food) {
        final int npcId = getNpcId();
        int nextNpcId = 0;

        if (growthLevel == 2) {
            // if tamed, the mob that will spawn depends on the class type (fighter/mage) of the player!
            if (Rnd.chance(50)) {
                if (player.getClassId().isMage()) {
                    nextNpcId = growthCapableMobs.get(npcId).spice[food][1][1];
                } else {
                    nextNpcId = growthCapableMobs.get(npcId).spice[food][1][0];
                }
            } // if not tamed, there is a small chance that have "mad cow" disease. That is a stronger-than-normal animal that attacks its feeder
            else if (player.getClassId().isMage()) {
                nextNpcId = growthCapableMobs.get(npcId).spice[food][0][1];
            } else {
                nextNpcId = growthCapableMobs.get(npcId).spice[food][0][0];
            }
        } else // all other levels of growth are straight-forward
        {
            nextNpcId = growthCapableMobs.get(npcId).spice[food][0][Rnd.get(growthCapableMobs.get(npcId).spice[food][0].length)];
        }

        // remove the feedinfo of the mob that got despawned, if any
        feedInfo.remove(getObjectId());

        if (growthCapableMobs.get(npcId).growth_level == 0) {
            onDecay();
        } else {
            deleteMe();
        }

        // if this is finally a trained mob, then despawn any other trained mobs that the player might have and initialize the Tamed Beast.
        if (!player.isPetSummoned() && tamedBeasts.contains(nextNpcId)) {
            final L2TamedBeastInstance oldTrained = player.getTrainedBeast();
            if (oldTrained != null) {
                oldTrained.doDespawn();
            }

            final L2NpcTemplate template = NpcTable.getInstance().getTemplate(nextNpcId);
            final L2TamedBeastInstance nextNpc = new L2TamedBeastInstance(IdFactory.getInstance().getNextId(), template);
            nextNpc.setCurrentHp(nextNpc.getMaxHp());
            nextNpc.setCurrentMp(nextNpc.getMaxMp());
            nextNpc.setFoodType(food == 0 ? SKILL_GOLDEN_SPICE : SKILL_CRYSTAL_SPICE);
            nextNpc.setHome(getX(), getY(), getZ());
            nextNpc.setRunning();
            nextNpc.spawnMe(getX(), getY(), getZ());
            nextNpc.setOwner(player);
            nextNpc.setRunning();

            final int objectId = nextNpc.getObjectId();

            /*final QuestState st = player.getQuestEngine().getQuestState("_020_BringUpWithLove");
            if(st != null && Rnd.chance(5) && st.getQuestItemsCount(7185) == 0)
            {
            st.giveItems(7185, 1);
            st.set("cond", "2");
            }*/

            // also, perform a rare random chat
            String text = "";
            switch (Rnd.get(10)) {
                case 0:
                    text = player.getName() + ", will you show me your hideaway?";
                    break;
                case 1:
                    text = player.getName() + ", whenever I look at spice, I think about you.";
                    break;
                case 2:
                    text = player.getName() + ", you do not need to return to the village.  I will give you strength.";
                    break;
                case 3:
                    text = "Thanks, " + player.getName() + ".  I hope I can help you";
                    break;
                case 4:
                    text = player.getName() + ", what can I do to help you?";
                    break;
            }
            broadcastPacket(new CreatureSay(objectId, 0, nextNpc.getName(), text));
        } // if not trained, the newly spawned mob will automatically be agro against its feeder (what happened to "never bite the hand that feeds you" anyway?!)
        else {
            // spawn the new mob
            final L2Spawn spawn = spawn(nextNpcId, getX(), getY(), getZ());
            if (spawn == null) {
                return;
            }

            final L2MonsterInstance nextNpc = (L2MonsterInstance) spawn.getLastSpawn();

            // register the player in the feedinfo for the mob that just spawned
            feedInfo.put(nextNpc.getObjectId(), player.getObjectId());
            broadcastPacket(new CreatureSay(nextNpc.getObjectId(), 0, nextNpc.getName(), text[growthLevel][Rnd.get(text[growthLevel].length)]));
            nextNpc.setRunning();
            nextNpc.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, player, 99999);
        }
    }

    @Override
    public boolean doDie(final L2Character killer) {
        if (!super.doDie(killer)) {
            return false;
        }

        feedInfo.remove(getObjectId());
        return true;
    }

    private class growthInfo {

        public int growth_level;
        public int growth_chance;
        public int[][][] spice;

        public growthInfo(final int level, final int[][][] sp, final int chance) {
            growth_level = level;
            spice = sp;
            growth_chance = chance;
        }
    }

    public L2Spawn spawn(final int npcId, final int x, final int y, final int z) {
        try {
            final L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
            if (template == null) {
                return null;
            }
            final L2Spawn spawn = new L2Spawn(template);
            spawn.setId(npcId);
            //if(getPlayer() != null)
            //	spawn.setHeading(getPlayer().getHeading());
            //else
            spawn.setHeading(0);
            spawn.setLocx(x);
            spawn.setLocy(y);
            spawn.setLocz(z + 20);
            spawn.stopRespawn();
            spawn.spawnOne();
            return spawn;
        } catch (final Exception e1) {
            _log.warning("Could not spawn Npc " + npcId);
        }
        return null;
    }

    public void onSkillUse(final L2PcInstance player, final int skill_id) {
        // gather some values on local variables
        final int npcId = getNpcId();
        // check if the npc and skills used are valid
        if (!feedableBeasts.contains(npcId)) {
            return;
        }
        if (skill_id != SKILL_GOLDEN_SPICE && skill_id != SKILL_CRYSTAL_SPICE) {
            return;
        }

        int food = GOLDEN_SPICE;
        if (skill_id == SKILL_CRYSTAL_SPICE) {
            food = CRYSTAL_SPICE;
        }

        final int objectId = getObjectId();
        // display the social action of the beast eating the food.
        broadcastPacket(new SocialAction(objectId, 2));

        // if this pet can't grow, it's all done.
        if (growthCapableMobs.containsKey(npcId)) {
            // do nothing if this mob doesn't eat the specified food (food gets consumed but has no effect).
            if (growthCapableMobs.get(npcId).spice[food].length == 0) {
                return;
            }

            // more value gathering on local variables
            final int growthLevel = growthCapableMobs.get(npcId).growth_level;

            if (growthLevel > 0) // check if this is the same player as the one who raised it from growth 0.
            // if no, then do not allow a chance to raise the pet (food gets consumed but has no effect).
            {
                if (feedInfo.get(objectId) != null && feedInfo.get(objectId) != player.getObjectId()) {
                    return;
                }
            }

            // Polymorph the mob, with a certain chance, given its current growth level
            if (Rnd.chance(growthCapableMobs.get(npcId).growth_chance)) {
                spawnNext(player, growthLevel, food);
            }
        } else if (tamedBeasts.contains(npcId)) {
            if (skill_id == ((L2TamedBeastInstance) this).getFoodType()) {
                ((L2TamedBeastInstance) this).onReceiveFood();
                final String[] mytext = new String[]{
                    "Refills! Yeah!",
                    "I am such a gluttonous beast, it is embarrassing! Ha ha",
                    "Your cooperative feeling has been getting better and better.",
                    "I will help you!",
                    "The weather is really good.  Wanna go for a picnic?",
                    "I really like you! This is tasty...",
                    "If you do not have to leave this place, then I can help you.",
                    "What can I helped you with?",
                    "I am not here only for food!",
                    "Yam, yam, yam, yam, yam!"};
                broadcastPacket(new CreatureSay(objectId, 0, getName(), mytext[Rnd.get(mytext.length)]));
            }
        }
    }

    /*@Override
    public boolean canMoveToHome()
    {
    return false;
    }*/
}
