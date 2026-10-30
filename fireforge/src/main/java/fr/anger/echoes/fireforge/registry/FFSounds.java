package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Les sons de la Forge (les fichiers sont ceux du jeu, voir assets/echoes_fireforge/sounds.json ; les sous-titres sont à nous). */
public final class FFSounds {

    private FFSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, EchoesFireForge.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_COUGH = register("entity.cendreux.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_HURT = register("entity.cendreux.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_DEATH = register("entity.cendreux.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENDREUX_EMBER = register("entity.cendreux.ember");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(EchoesFireForge.id(name)));
    }
}
