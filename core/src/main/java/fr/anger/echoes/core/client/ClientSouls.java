package fr.anger.echoes.core.client;

/** Côté client : la jauge d'âmes reçue du serveur. */
public final class ClientSouls {

    private static int souls;
    private static int max = 50;

    private ClientSouls() {}

    public static void set(int souls, int max) {
        ClientSouls.souls = souls;
        ClientSouls.max = Math.max(1, max);
    }

    public static int get() {
        return souls;
    }

    public static int max() {
        return max;
    }
}
