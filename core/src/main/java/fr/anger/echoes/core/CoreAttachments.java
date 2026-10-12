package fr.anger.echoes.core;

import fr.anger.echoes.core.soul.SoulData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;

/** Les données que le Core attache aux joueurs. On y accède par Souls et Lore, pas directement. */
public final class CoreAttachments {

    private CoreAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EchoesCore.MOD_ID);

    /** Âmes et bonus de jauge. À la mort, les âmes sont perdues mais pas les bonus (voir SoulEvents). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SoulData>> SOULS =
            ATTACHMENT_TYPES.register("souls", () -> AttachmentType.builder(SoulData::new)
                    .serialize(SoulData.CODEC)
                    .build());

    /** Entrées de lore lues (tablettes, pages...), tous épisodes confondus. Conservé à la mort. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<ResourceLocation>>> LORE =
            ATTACHMENT_TYPES.register("lore", () -> AttachmentType.<Set<ResourceLocation>>builder(() -> Set.of())
                    .serialize(ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf))
                    .copyOnDeath()
                    .build());
}
