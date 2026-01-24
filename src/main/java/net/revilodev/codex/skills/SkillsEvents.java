package net.revilodev.codex.skills;

public final class SkillsEvents {
    private SkillsEvents() {}

    private static boolean REGISTERED = false;

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        net.revilodev.codex.skills.logic.SkillSyncEvents.register();
        net.revilodev.codex.skills.logic.SkillEvents.register();
    }
}
