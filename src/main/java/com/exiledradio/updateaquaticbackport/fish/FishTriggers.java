package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.common.Content;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The two advancement triggers 1.13 added for fish, which 1.12.2 lacks: {@code filled_bucket} (a mob
 * scooped into a bucket) and {@code fishing_rod_hooked} (an item reeled in). Both take 1.12.2's item
 * predicate ({@code {"item": "...", "data": n}}) under {@code "item"}, as 1.13's do.
 */
public final class FishTriggers {

    public static final ItemTrigger FILLED_BUCKET = new ItemTrigger(Content.id("filled_bucket"));
    public static final ItemTrigger FISHING_ROD_HOOKED = new ItemTrigger(Content.id("fishing_rod_hooked"));

    private static boolean registered;

    private FishTriggers() {
    }

    /** Registers both triggers with the advancement system; before any server loads advancements. */
    static void register() {
        if (!registered) {
            registered = true;
            CriteriaTriggers.register(FILLED_BUCKET);
            CriteriaTriggers.register(FISHING_ROD_HOOKED);
        }
    }

    /** A trigger that fires with an item and passes when the criterion's item predicate matches it. */
    public static final class ItemTrigger implements ICriterionTrigger<ItemTrigger.Instance> {

        private final ResourceLocation id;
        private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<PlayerAdvancements, Set<Listener<Instance>>>();

        ItemTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public void addListener(PlayerAdvancements advancements, Listener<Instance> listener) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                set = new HashSet<Listener<Instance>>();
                this.listeners.put(advancements, set);
            }
            set.add(listener);
        }

        @Override
        public void removeListener(PlayerAdvancements advancements, Listener<Instance> listener) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    this.listeners.remove(advancements);
                }
            }
        }

        @Override
        public void removeAllListeners(PlayerAdvancements advancements) {
            this.listeners.remove(advancements);
        }

        @Override
        public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
            return new Instance(this.id, ItemPredicate.deserialize(json.get("item")));
        }

        public void trigger(EntityPlayerMP player, ItemStack stack) {
            PlayerAdvancements advancements = player.getAdvancements();
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                return;
            }
            List<Listener<Instance>> passed = new ArrayList<Listener<Instance>>();
            for (Listener<Instance> listener : set) {
                if (listener.getCriterionInstance().test(stack)) {
                    passed.add(listener);
                }
            }
            for (Listener<Instance> listener : passed) {
                listener.grantCriterion(advancements);
            }
        }

        /** One criterion: an item predicate. */
        public static final class Instance extends AbstractCriterionInstance {
            private final ItemPredicate item;

            Instance(ResourceLocation id, ItemPredicate item) {
                super(id);
                this.item = item;
            }

            public boolean test(ItemStack stack) {
                return this.item.test(stack);
            }
        }
    }
}
