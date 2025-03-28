package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.log.AbstractLogger;

public class Runes
{
   private static final Logger _log = AbstractLogger.getLogger(Runes.class.getName());
   private static Runes _instance;
   public static FastList<RuneSkill> _runesList = new FastList<RuneSkill>();

   public static Runes getInstance()
   {
      return _instance;
   }

   public static void load()
   {
      _instance = new Runes();
      _instance.init();
   }

   private void init()
   {
      cacheRunes();
   }

   private void cacheRunes()
   {
      try
      {
         File var1 = new File(Config.DATAPACK_ROOT, "data/runes_mod.xml");
         if(!var1.exists())
         {
            _log.config("CustomServerData [ERROR]: data/runes_mod.xml doesn\'t exist");
            return;
         }

         DocumentBuilderFactory var2 = DocumentBuilderFactory.newInstance();
         var2.setValidating(false);
         var2.setIgnoringComments(true);
         Document var3 = var2.newDocumentBuilder().parse(var1);

         for(Node var4 = var3.getFirstChild(); var4 != null; var4 = var4.getNextSibling())
         {
            if("list".equalsIgnoreCase(var4.getNodeName()))
            {
               for(Node var5 = var4.getFirstChild(); var5 != null; var5 = var5.getNextSibling())
               {
                  if("rune".equalsIgnoreCase(var5.getNodeName()))
                  {
                     NamedNodeMap var6 = var5.getAttributes();
                     int var7 = Integer.parseInt(var6.getNamedItem("skillId").getNodeValue());
                     int var8 = Integer.parseInt(var6.getNamedItem("skillLvl").getNodeValue());
                     RuneSkill var9 = new RuneSkill(var7, var8);

                     for(Node var10 = var5.getFirstChild(); var10 != null; var10 = var10.getNextSibling())
                     {
                        if("items".equalsIgnoreCase(var10.getNodeName()))
                        {
                           var6 = var10.getAttributes();
                           FastList<Integer> var11 = new FastList<Integer>();
                           String[] var12 = var6.getNamedItem("list").getNodeValue().split(",");
                           String[] var13 = var12;
                           int var14 = var12.length;

                           for(int var15 = 0; var15 < var14; ++var15)
                           {
                              String var16 = var13[var15];
                              if(!var16.equalsIgnoreCase(""))
                                 var11.add(Integer.valueOf(Integer.parseInt(var16)));
                           }
                           var9.addRunes(var11);
                        }
                     }
                     _runesList.add(var9);
                  }
               }
            }
         }
      }
      catch (Exception var17)
      {
         _log.warning("CustomServerData [ERROR]: cacheRunes() " + var17.toString());
      }
      _log.config("CustomServerData: Runes Mod, loaded " + _runesList.size() + " rune skills.");
   }

   public void checkRuneSkills(L2PcInstance var1)
   {
      if(var1 != null)
      {
         RuneSkill var2 = null;
         FastList.Node<RuneSkill> var3 = _runesList.head();
         FastList.Node<RuneSkill> var4 = _runesList.tail();

         while((var3 = var3.getNext()) != var4)
         {
            var2 = (RuneSkill)var3.getValue();
            if(var2 != null)
            {
               applyRuneSkill(var1, var2, findRuneItems(var1, var2), var1.getKnownSkill(var2.skill.getId()));
            }
         }

         var2 = null;
      }
   }

   private void applyRuneSkill(L2PcInstance var1, RuneSkill var2, boolean var3, L2Skill var4)
   {
      if(var3)
      {
         if(var4 == null)
         {
            var1.addSkill(var2.skill, false);
            var1.sendSkillList();
         }
         else if(var4.getLevel() < var2.skill.getLevel())
         {
            var1.removeSkill(var4, false);
            var1.addSkill(var2.skill, false);
            var1.sendSkillList();
         }
      }
      else
      {
         if(var4 != null && var4.getLevel() == var2.skill.getLevel())
         {
            var1.removeSkill(var2.skill, false);
            var1.sendSkillList();
         }
      }
   }

   private boolean findRuneItems(L2PcInstance var1, RuneSkill var2)
   {
      FastList.Node<Integer> var3 = var2.runes.head();
      FastList.Node<Integer> var4 = var2.runes.tail();

      Integer var5;
      do
      {
         if((var3 = var3.getNext()) == var4)
            return false;

         var5 = (Integer)var3.getValue();
      }
      while(var5 == null || var1.getItemCount(var5.intValue()) <= 0);

      return true;
   }

   public FastList<RuneSkill> getRunes()
   {
      return _runesList;
   }

   public static class RuneSkill
   {
      public L2Skill skill;
      public FastList<Integer> runes = new FastList<Integer>();

      public RuneSkill(int var1, int var2)
      {
         skill = SkillTable.getInstance().getInfo(var1, var2);
      }

      public void addRunes(FastList<Integer> var1)
      {
         runes.addAll(var1);
      }
   }
}
