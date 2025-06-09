/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.l2j.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javolution.util.FastTable;

/**
 *
 * @author Администратор
 */
public class L2EffectList {

    private L2Character _owner;
    private final ConcurrentLinkedQueue<L2Effect> _buffs = new ConcurrentLinkedQueue<L2Effect>();
    private final ConcurrentLinkedQueue<L2Effect> _dances = new ConcurrentLinkedQueue<L2Effect>();
    private final ConcurrentLinkedQueue<L2Effect> _debuffs = new ConcurrentLinkedQueue<L2Effect>();
    private final ConcurrentLinkedQueue<L2Effect> _augments = new ConcurrentLinkedQueue<L2Effect>();
    private final Map<String, L2Effect> _stacks = new ConcurrentHashMap<String, L2Effect>();
    private static L2Effect[] _emptyArray = new L2Effect[0];
    private static FastTable<L2Effect> _emptyTable = new FastTable<L2Effect>();

    public L2EffectList(L2Character owner) {
        _owner = owner;
    }

    public synchronized void addEffect(L2Effect newEffect) {
        if (newEffect == null) {
            return;
        }

        if (!_owner.updateSkillEffects(newEffect.getId(), newEffect.getLevel())) {
            return;
        }

        if (!checkBuffCount(newEffect.getSkill())) {
            return;
        }

        if (!checkAlreadyStacked(newEffect, _stacks.get(newEffect.getStackType()))) {
            return;
        }

        //System.out.println("###" + newEffect.getEffectType().);

        newEffect.setInUse(true);
        _owner.addStatFuncs(newEffect.getStatFuncs());
        insertEffect(newEffect);
        _owner.updateEffectIcons();
    }

    private boolean checkBuffCount(L2Skill tempskill) {
        if (tempskill == null) {
            return false;
        }

        if (tempskill.isAugment() && _owner.isMounted()) {
            return false;
        }

        if (tempskill.checkFirstBuff() && replaceFirstBuff()) {
            removeFirstBuff(tempskill.getId());
        }
        return true;
    }

    private boolean checkAlreadyStacked(L2Effect newEffect, L2Effect old) {
        //if (newEffect.getStackType().equals("none")) {
        System.out.println("###-1#");
        if (newEffect.hasEmptyStack()) {
            return true;
        }
        System.out.println("###0#");
        if (old == null) {
            System.out.println("###1#");
            if (newEffect.hasMoreStacks()) {
                System.out.println("###2#");
                for (L2Effect buff : _buffs) {
                    if (buff == null) {
                        continue;
                    }

                    if (newEffect.containsStack(buff.getStackType())) {
                        System.out.println("###3#");
                        removeEffect(buff);
                    }
                }
            }
            _stacks.put(newEffect.getStackType(), newEffect);
            return true;
        }

        if (!newEffect.hasMoreStacks() && newEffect.getStackOrder() < old.getStackOrder()) {
            return false;
        }

        _owner.removeStatsOwner(old);
        old.setInUse(false);
        old.exit();
        _stacks.put(newEffect.getStackType(), newEffect);
        return true;
    }

    private void insertEffect(L2Effect newEffect) {
        if (newEffect.getSkill().isLikeDebuff()) {
            _debuffs.add(newEffect);
            return;
        }
        if (newEffect.getSkill().isDance()) {
            _dances.add(newEffect);
            return;
        }
        if (newEffect.getSkill().isAugment()) {
            _augments.add(newEffect);
            return;
        }
        _buffs.add(newEffect);
    }

    public synchronized void removeEffect(L2Effect effect) {
        if (effect == null) {
            return;
        }

        //System.out.println("####");

        _owner.removeStatsOwner(effect);
        effect.setInUse(false);
        _stacks.remove(effect.getStackType());

        if (effect.getSkill().isLikeDebuff()) {
            _debuffs.remove(effect);
        } else if (effect.getSkill().isDance()) {
            _dances.remove(effect);
        } else if (effect.getSkill().isAugment()) {
            _augments.remove(effect);
        } else {
            _buffs.remove(effect);
        }

        _owner.updateEffectIcons();
    }

    public boolean replaceFirstBuff() {
        return _owner.replaceFirstBuff();
    }

    public int getBuffCount() {
        return (_buffs.size() + _dances.size() + _augments.size());
    }

    public int getDebuffCount() {
        return _debuffs.size();
    }

    public int getDanceCount() {
        return _dances.size();
    }

    public int getAugmentCount() {
        return _augments.size();
    }

    public void removeFirstBuff(int preferSkill) {
        FastTable<L2Effect> effects = getAllBuffsTable();
        if (effects.isEmpty()) {
            return;
        }

        L2Effect e = null;
        L2Effect removeMe = null;
        for (int i = 0, n = effects.size(); i < n; i++) {
            e = effects.get(i);
            if (e == null) {
                continue;
            }

            if (preferSkill == 0) {
                removeMe = e;
                break;
            } else if (e.getSkill().getId() == preferSkill) {
                removeMe = e;
                break;
            } else if (removeMe == null) {
                removeMe = e;
            }
        }
        if (removeMe != null) {
            removeMe.exit();
        }
    }

    public final FastTable<L2Effect> getAllBuffsTable() {
        if (_buffs.isEmpty() && _dances.isEmpty() && _augments.isEmpty()) {
            return _emptyTable;
        }

        FastTable<L2Effect> effects = new FastTable<L2Effect>();
        effects.addAll(_buffs);
        effects.addAll(_dances);
        effects.addAll(_augments);

        return effects;
    }

    public final FastTable<L2Effect> getAllDebuffsTable() {
        // Create a copy of the effects set
        if (_debuffs.isEmpty()) {
            return _emptyTable;
        }

        FastTable<L2Effect> effects = new FastTable<L2Effect>();
        effects.addAll(_debuffs);

        return effects;
    }

    public final FastTable<L2Effect> getAllEffectsTable() {
        // Create a copy of the effects set
        if (_buffs.isEmpty() && _debuffs.isEmpty() && _dances.isEmpty() && _augments.isEmpty()) {
            return _emptyTable;
        }

        FastTable<L2Effect> effects = new FastTable<L2Effect>();
        effects.addAll(_buffs);
        effects.addAll(_dances);
        effects.addAll(_augments);
        effects.addAll(_debuffs);

        return effects;
    }

    public final L2Effect[] getAllEffects() {
        // Create a copy of the effects set
        FastTable<L2Effect> effects = getAllEffectsTable();
        // If no effect found, return EMPTY_EFFECTS
        if (effects == null || effects.isEmpty()) {
            return _emptyArray;
        }

        return effects.toArray(new L2Effect[effects.size()]);
        // Return all effects in progress in a table
        /*int ArraySize = effects.size();
        L2Effect[] effectArray = new L2Effect[ArraySize];
        for (int i = 0; i < ArraySize; i++) {
        if (i >= effects.size() || effects.get(i) == null) {
        break;
        }
        effectArray[i] = effects.get(i);
        }
        return effectArray;*/
    }
}
