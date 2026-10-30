package fr.anger.echoes.fireforge.registry;

import fr.anger.echoes.fireforge.EchoesFireForge;
import fr.anger.echoes.fireforge.witherset.WitherProgress;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FFAttachments {

    private FFAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EchoesFireForge.MOD_ID);

    /** La progression du set du Wither : elle reste sur le joueur, même à la mort (la maladie est en lui, pas dans l'armure). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WitherProgress>> WITHER_PROGRESS = ATTACHMENTS.register("wither_progress",
            () -> AttachmentType.builder(() -> WitherProgress.NONE).serialize(WitherProgress.CODEC).copyOnDeath().build());
}
