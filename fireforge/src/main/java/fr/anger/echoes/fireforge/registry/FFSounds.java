package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Les sons de la Forge (assets/echoes_fireforge/sounds.json) : ceux du jeu pour les Cendreux, ceux d'Anger pour le maître de forge. */
public final class FFSounds {

    private FFSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, EchoesFireForge.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_COUGH = register("entity.cendreux.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_HURT = register("entity.cendreux.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_DEATH = register("entity.cendreux.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_EMBER = register("entity.cendreux.ember");

    // le maître de forge : les sons d'Anger (marteau, vapeur, pas, mort), sa voix est celle du wither squelette, plus grave
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_GROUND_HIT = register("entity.forge_master.ground_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_HIT = register("entity.forge_master.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_SWING = register("entity.forge_master.swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_STEAM = register("entity.forge_master.steam");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_STEP = register("entity.forge_master.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_DEATH = register("entity.forge_master.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_AMBIENT = register("entity.forge_master.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MASTER_HURT = register("entity.forge_master.hurt");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(EchoesFireForge.id(name)));
    }
}
