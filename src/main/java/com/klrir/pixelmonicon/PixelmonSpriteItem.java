package com.klrir.pixelmonicon;

import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IVStore;
import com.pixelmonmod.pixelmon.api.util.ITranslatable;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import io.papermc.paper.adventure.PaperAdventure;
import lombok.Builder;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PixelmonSpriteItem {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int MAX_IV_TOTAL = 186;
    private static final int MAX_EV_TOTAL = 510;

    private static final Map<BattleStatsType, TextColor> STAT_COLORS = Map.of(
            BattleStatsType.HP, NamedTextColor.RED,
            BattleStatsType.ATTACK, NamedTextColor.GOLD,
            BattleStatsType.DEFENSE, NamedTextColor.YELLOW,
            BattleStatsType.SPECIAL_ATTACK, NamedTextColor.BLUE,
            BattleStatsType.SPECIAL_DEFENSE, NamedTextColor.GREEN,
            BattleStatsType.SPEED, NamedTextColor.LIGHT_PURPLE
    );

    @Builder(builderClassName = "Build")
    private static ItemStack build(
            @NonNull Pokemon pokemon,
            boolean hasNickname,
            boolean hasLevel,
            boolean hasNature,
            Nature[] natures,
            boolean hasGrowth,
            boolean hasAbility,
            Ability[] abilities,
            boolean hasOT,
            boolean hasPokeball,
            boolean hasEV,
            boolean hasIV,
            boolean hasMoveset,
            boolean isTrainer
    ) {
        ItemStack item = CraftItemStack.asBukkitCopy(SpriteItemHelper.getPhoto(pokemon));
        boolean shiny = pokemon.isShiny();

        item.editMeta(meta -> {
            meta.displayName(buildDisplayName(pokemon, shiny, hasNickname));
            meta.lore(buildLore(pokemon, hasLevel, hasNature, natures, hasGrowth, hasAbility, abilities, hasOT, hasPokeball, hasEV, hasIV, hasMoveset, isTrainer));
            if (shiny) meta.setEnchantmentGlintOverride(true);
        });

        return item;
    }

    /* ---------------------------------------------------------------------- */
    /* Lore                                                                    */
    /* ---------------------------------------------------------------------- */

    private static List<Component> buildLore(Pokemon pokemon,
                                             boolean hasLevel,
                                             boolean hasNature,
                                             Nature[] natures,
                                             boolean hasGrowth,
                                             boolean hasAbility,
                                             Ability[] abilities,
                                             boolean hasOT,
                                             boolean hasPokeball,
                                             boolean hasEV,
                                             boolean hasIV,
                                             boolean hasMoveset,
                                             boolean isTrainer) {

        List<Component> lore = new ArrayList<>();

        if (hasLevel) lore.add(buildLevelLine(pokemon.getPokemonLevel(), isTrainer ? ">= " : ""));
        if (hasNature) lore.addAll(buildNatureLine(isTrainer ? natures :
                List.of(pokemon.getNature()).toArray(Nature[]::new)));
        if (hasGrowth) lore.add(buildGrowthLine(pokemon));
        if (hasAbility) lore.addAll(buildAbilityLine(isTrainer ? abilities :
                List.of(pokemon.getAbility()).toArray(Ability[]::new)));
        if (hasOT) lore.add(buildOTLine(pokemon.getOriginalTrainer()));
        if (hasPokeball) lore.add(buildPokeballLine(pokemon.getBall()));

        if (hasMoveset) {
            lore.add(Component.empty());
            lore.addAll(buildMoveSetLine(pokemon.getMoveset().attacks));
        }

        if (hasEV || hasIV) {
            lore.add(Component.empty());
        }

        if (hasIV) {
            lore.add(buildStatSummaryLine(
                    "IVS",
                    getTotalIV(pokemon.getIVs()),
                    MAX_IV_TOTAL
            ));
            lore.addAll(buildStatDetailLines(pokemon, true));
        }

        if (hasEV) {
            lore.add(buildStatSummaryLine(
                    "EVS",
                    pokemon.getEVs().getTotal(),
                    MAX_EV_TOTAL
            ));
            lore.addAll(buildStatDetailLines(pokemon, false));
        }

        return lore;
    }

    private static Component buildLevelLine(int level, String... prefix) {
        String prefx = Arrays.stream(prefix).findFirst().orElse("");
        return deserialize("<green>Level<opt></green> <light_purple><prefix><level></light_purple>",
                Placeholder.parsed("opt", prefx.isEmpty() ? ":" : ""),
                Placeholder.parsed("level", String.valueOf(level)),
                Placeholder.parsed("prefix", prefx));
    }

    private static List<Component> buildNatureLine(Nature... natures) {
        List<Component> lines = new ArrayList<>();
        final String JOIN = "<dark_gray>,</dark_gray> ";

        if (natures == null || natures.length == 0) {
            return lines;
        }

        if (natures.length == 1) {
            Nature nature = natures[0];
            lines.add(deserialize(
                    "<green>Nature:</green> <light_purple><nature></light_purple><suffix>",
                    Placeholder.parsed("nature", nature.name()),
                    Placeholder.component("suffix", buildNatureSuffix(nature))
            ));
            return lines;
        }

        int wrapAfter = 3;
        for (int i = 0; i < natures.length; i += wrapAfter) {
            int end = Math.min(i + wrapAfter, natures.length);
            Nature[] chunk = Arrays.copyOfRange(natures, i, end);

            String content = Arrays.stream(chunk)
                    .map(Nature::name)
                    .collect(Collectors.joining(JOIN));

            final String format = getFormatted(natures.length, i, end);

            lines.add(deserialize(
                    format,
                    Placeholder.parsed("name", "Nature"),
                    Placeholder.parsed("value", content)
            ));
        }

        return lines;
    }

    private static @NonNull String getFormatted(int length, int i, int end) {
        boolean first = (i == 0);
        boolean last = end == length;

        String format;
        if (first && last) {
            format = "<green><name>:</green> <dark_gray>[<light_purple><value></light_purple>]</dark_gray>";
        } else if (first) {
            format = "<green><name>:</green> <dark_gray>[<light_purple><value></light_purple>";
        } else if (last) {
            format = "<dark_gray><light_purple><value></light_purple>]</dark_gray>";
        } else {
            format = "<gray>        </gray><dark_gray><light_purple><value></light_purple>";
        }
        return format;
    }

    private static Component buildNatureSuffix(Nature nature) {
        BattleStatsType inc = nature.getIncreasedStat();
        BattleStatsType dec = nature.getDecreasedStat();

        if (inc == BattleStatsType.NONE && dec == BattleStatsType.NONE) {
            return Component.empty();
        }

        Component suffix = Component.text(" (", NamedTextColor.DARK_GRAY);

        if (inc != BattleStatsType.NONE) {
            suffix = suffix.append(deserialize("+<buff>",
                    Placeholder.component("buff", Component.translatable(inc.getTranslationKey())))
                    .color(NamedTextColor.GREEN)
            );
        }

        if (inc != BattleStatsType.NONE && dec != BattleStatsType.NONE) {
            suffix = suffix.append(deserialize("<dark_gray>/</dark_gray>"));
        }

        if (dec != BattleStatsType.NONE) {
            suffix = suffix.append(deserialize("-<debuff>",
                            Placeholder.component("debuff", Component.translatable(dec.getTranslationKey())))
                            .color(NamedTextColor.RED)
            );
        }

        return suffix.append(Component.text(")", NamedTextColor.DARK_GRAY));
    }

    private static Component buildGrowthLine(Pokemon pokemon) {
        return deserialize(
                "<green>Growth:</green> <light_purple><growth></light_purple>",
                Placeholder.component(
                        "growth",
                        PaperAdventure.asAdventure(pokemon.getGrowth().value().getName())
                )
        );
    }

    private static List<Component> buildAbilityLine(Ability... abilities) {
        List<Component> lines = new ArrayList<>();
        final String JOIN = "<dark_gray>,</dark_gray> ";

        if (abilities == null || abilities.length == 0) {
            return lines;
        }

        int wrapAfter = 3;

        for (int i = 0; i < abilities.length; i += wrapAfter) {
            int end = Math.min(i + wrapAfter, abilities.length);
            Ability[] chunk = Arrays.copyOfRange(abilities, i, end);

            String content = Arrays.stream(chunk)
                    .map(ability -> Component.translatable(ability.getTranslationKey()).fallback())
                    .collect(Collectors.joining(JOIN));

            final String format = getFormatted(abilities.length, i, end);

            lines.add(deserialize(
                    format,
                    Placeholder.parsed("name", "Ability"),
                    Placeholder.parsed("value", content)
            ));
        }

        return lines;
    }

    private static Component buildOTLine(String OT) {
        return deserialize("<green>OT:</green> <light_purple><ot></light_purple>",
                Placeholder.parsed("ot", OT));
    }

    private static Component buildPokeballLine(PokeBall pokeBall) {
        return deserialize("<green>Pokeball:</green> <light_purple><pokeball></light_purple>",
                Placeholder.parsed("pokeball", pokeBall.getName()));
    }

    private static List<Component> buildMoveSetLine(Attack... attacks) {
        List<Component> lines = new ArrayList<>();
        final Component MOVE_SEPARATOR = Component.text(", ", NamedTextColor.DARK_GRAY);

        if (attacks == null || attacks.length == 0) {
            return lines;
        }

        if (attacks.length == 1) {
            Attack attack = attacks[0];
            lines.add(deserialize(
                    "<green>Moveset:</green> <light_purple><move></light_purple><suffix>",
                    Placeholder.component("move", Component.translatable(attack.getMove().getTranslationKey()))
            ));
            return lines;
        }

        int wrapAfter = 3;
        for (int i = 0; i < attacks.length; i += wrapAfter) {
            int end = Math.min(i + wrapAfter, attacks.length);
            Attack[] chunk = Arrays.copyOfRange(attacks, i, end);

            Component content = Arrays.stream(chunk)
                    .filter(a -> a != null && a.getMove() != null)
                    .map(a -> Component.translatable(a.getMove().getTranslationKey()))
                    .reduce((a, b) -> a.append(MOVE_SEPARATOR).append(b))
                    .orElse(null);

            if (content == null) continue;

            final String format = getFormatted(attacks.length, i, end);

            lines.add(deserialize(
                    format,
                    Placeholder.parsed("name", "Moveset"),
                    Placeholder.component("value", content)
            ));
        }

        return lines;
    }

    private static Component buildStatSummaryLine(String label, int current, int max) {
        return deserialize("<aqua><label>:</aqua> <light_purple><current>/<max> " +
                        "<green>(</green><percent><green>)</green></light_purple>",
                Placeholder.parsed("label", label),
                Placeholder.parsed("current", String.valueOf(current)),
                Placeholder.parsed("max", String.valueOf(max)),
                Placeholder.parsed("percent", formatPercent(current, max))
        );
    }

    private static List<Component> buildStatDetailLines(Pokemon pokemon, boolean iv) {
        List<Component> lines = new ArrayList<>();
        List<Component> buffer = new ArrayList<>(3);

        STAT_COLORS.forEach((stat, color) -> {
            int value = iv
                    ? pokemon.getIVs().getStat(stat)
                    : pokemon.getEVs().getStat(stat);

            buffer.add(deserialize("<stat>: <value>",
                    Placeholder.component("stat", Component.translatable(stat.getAbbreviatedTranslationKey())),
                    Placeholder.parsed("value", String.valueOf(value)))
                    .color(color)
            );

            if (buffer.size() == 3) {
                lines.add(joinWithSlash(buffer));
                buffer.clear();
            }
        });

        if (!buffer.isEmpty()) {
            lines.add(joinWithSlash(buffer));
        }

        return lines;
    }

    /* ---------------------------------------------------------------------- */
    /* Display Name                                                            */
    /* ---------------------------------------------------------------------- */

    private static Component buildDisplayName(Pokemon pokemon, boolean shiny, boolean showNickname) {
        Component baseName = getFullPokemonName(pokemon);
        Component heldName = Component.empty();
        if (!pokemon.getHeldItem().equals(net.minecraft.world.item.ItemStack.EMPTY)) {
            heldName = MM.deserialize(" @ <held>",
                    Placeholder.component("held", Component.translatable(pokemon.getHeldItemAsItemHeld()
                            .getTranslationKey())));
        }
        Component name = deserialize(
                "<green><name><bold><shiny></bold><held></green><yellow></yellow>",
                Placeholder.component("name", baseName),
                Placeholder.component("held", heldName),
                Placeholder.parsed("shiny", shiny ? "✨" : "")
        );

        if (!showNickname) return name;

        Component nickname = getNickname(pokemon);
        if (nickname == null) return name;

        return name.append(
                deserialize(
                        " <gold>[</gold><nickname><gold>]</gold>",
                        Placeholder.component("nickname", nickname)
                )
        );
    }

    private static Component getFullPokemonName(Pokemon pokemon) {
        Component name = deserialize(pokemon.getSpecies().getName());
        String form = pokemon.getFormName();

        return form.isEmpty() || form.equals("base")
                ? name
                : name.append(MM.deserialize("-" + form));
    }

    private static @Nullable Component getNickname(Pokemon pokemon) {
        return pokemon.hasNickname()
                ? PaperAdventure.asAdventure(pokemon.getNickname())
                : null;
    }

    /* ---------------------------------------------------------------------- */
    /* Utils                                                                   */
    /* ---------------------------------------------------------------------- */

    private static int getTotalIV(IVStore ivs) {
        int total = 0;
        for (int v : ivs.getArray()) total += v;
        return total;
    }

    private static String formatPercent(int value, int max) {
        return String.format("%.2f%%", (value * 100.0) / max);
    }

    private static Component joinWithSlash(List<Component> components) {
        Component line = Component.empty();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) line = line.append(Component.text("/", NamedTextColor.DARK_GRAY));
            line = line.append(components.get(i));
        }
        return line;
    }

    private static Component deserialize(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers)
                .decoration(TextDecoration.ITALIC, false);
    }

    /* ---------------------------------------------------------------------- */
    /* Builder flags                                                           */
    /* ---------------------------------------------------------------------- */

    public static class Build {
        private boolean hasNickname;
        private boolean hasLevel;
        private boolean hasNature;
        private Nature[] natures;
        private boolean hasGrowth;
        private boolean hasAbility;
        private Ability[] abilities;
        private boolean hasOT;
        private boolean hasPokeball;
        private boolean hasEV;
        private boolean hasIV;
        private boolean hasMoveset;
        private boolean isTrainer;

        public Build hasNickname() { this.hasNickname = true; return this; }
        public Build hasLevel()    { this.hasLevel = true; return this; }
        public Build hasNature()   { this.hasNature = true; return this; }
        public Build hasGrowth()   { this.hasGrowth = true; return this; }
        public Build hasAbility()  { this.hasAbility = true; return this; }
        public Build hasOT()       { this.hasOT = true; return this; }
        public Build hasPokeball() { this.hasPokeball = true; return this; }
        public Build hasEV()       { this.hasEV = true; return this; }
        public Build hasIV()       { this.hasIV = true; return this; }
        public Build hasMoveset()  { this.hasMoveset = true; return this; }
        public Build isTrainer()   { this.isTrainer = true; return this; }
        public Build natures(Nature... natures) {
            this.natures = natures;
            return this;
        }
        public Build abilities(Ability... abilities) {
            this.abilities = abilities;
            return this;
        }
    }
}