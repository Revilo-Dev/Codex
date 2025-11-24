package net.revilodev.codex.data;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.revilodev.codex.Config;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.*;

public final class GuideData {
    private GuideData() {}

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String PATH_CHAPTERS = "chapters";
    private static final String PATH_CATEGORIES = "chapters/categories";

    private static final Map<String, Chapter> CHAPTERS = new LinkedHashMap<>();
    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();
    private static boolean loadedClient = false;
    private static boolean loadedServer = false;

    // ------------------------------------------------------------
    // Data types
    // ------------------------------------------------------------

    public static final class Chapter {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final String image;
        public final String category;

        public Chapter(String id, String name, String icon, String description, String image, String category) {
            this.id = Objects.requireNonNull(id);
            this.name = (name == null || name.isBlank()) ? id : name;
            this.icon = (icon == null || icon.isBlank()) ? "minecraft:book" : icon;
            this.description = description == null ? "" : description;
            this.image = image == null ? "" : image;
            this.category = (category == null || category.isBlank()) ? "misc" : category;
        }

        public Optional<Item> iconItem() {
            try {
                ResourceLocation rl = ResourceLocation.parse(icon);
                return Optional.ofNullable(BuiltInRegistries.ITEM.get(rl));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        public Optional<ResourceLocation> imageLocation() {
            if (image == null || image.isBlank()) return Optional.empty();
            try {
                return Optional.of(ResourceLocation.parse(image));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static final class Category {
        public final String id;
        public final String icon;
        public final String name;
        public final int order;
        public final boolean excludeFromAll;
        public final String dependency;

        public Category(String id, String icon, String name, int order, boolean excludeFromAll, String dependency) {
            this.id = id;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? id : name;
            this.order = order;
            this.excludeFromAll = excludeFromAll;
            this.dependency = dependency == null ? "" : dependency;
        }

        public Optional<Item> iconItem() {
            try {
                ResourceLocation rl = ResourceLocation.parse(icon);
                return Optional.ofNullable(BuiltInRegistries.ITEM.get(rl));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    private static boolean isChapterDisabled(Chapter c) {
        return Config.disabledCategories().contains(c.category);
    }

    // ------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------

    public static void forceReloadAll(MinecraftServer server) {
        loadedClient = false;
        loadedServer = false;
        loadServer(server, true);
    }

    private static synchronized void load(ResourceManager rm, boolean forceReload) {
        if ((loadedClient || loadedServer) && !forceReload && !CHAPTERS.isEmpty()) return;

        CHAPTERS.clear();
        CATEGORIES.clear();

        // Categories: data/*/chapters/categories/*.json
        Map<ResourceLocation, List<Resource>> catStacks =
                rm.listResourceStacks(PATH_CATEGORIES, rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, List<Resource>> e : catStacks.entrySet()) {
            List<Resource> stack = e.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                String id = optString(obj, "id");
                if (id == null || id.isBlank()) {
                    String p = e.getKey().getPath();
                    int lastSlash = p.lastIndexOf('/');
                    String n = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
                    id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
                }

                String icon = optString(obj, "icon");
                String cname = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);
                boolean excludeFromAll = parseBoolFlexible(obj, "exclude_from_all", false);
                String dependency = optString(obj, "dependency");

                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, cname, order, excludeFromAll, dependency));
                }
            } catch (Exception ignored) {}
        }

        // Chapters: data/*/chapters/*.json (excluding /categories/)
        Map<ResourceLocation, List<Resource>> chapterStacks =
                rm.listResourceStacks(PATH_CHAPTERS, rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, List<Resource>> e : chapterStacks.entrySet()) {
            ResourceLocation rl = e.getKey();
            if (rl.getPath().contains("/categories/")) continue;
            List<Resource> stack = e.getValue();
            if (stack.isEmpty()) continue;
            Resource top = stack.get(stack.size() - 1);
            try (Reader raw = top.openAsReader(); Reader reader = new BufferedReader(raw)) {
                JsonObject obj = safeObject(reader);
                if (obj == null) continue;
                Chapter c = parseChapterObject(obj, rl);
                if (c != null && !isChapterDisabled(c)) {
                    CHAPTERS.put(c.id, c);
                }
            } catch (Exception ignored) {}
        }

        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }
    }

    private static JsonObject safeObject(Reader reader) {
        JsonElement el = GsonHelper.fromJson(GSON, reader, JsonElement.class);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static Chapter parseChapterObject(JsonObject obj, ResourceLocation src) {
        String id = optString(obj, "id");
        if (id == null || id.isBlank()) {
            String p = src.getPath();
            int lastSlash = p.lastIndexOf('/');
            String n = (lastSlash >= 0 ? p.substring(lastSlash + 1) : p);
            id = n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
        }
        String name = optString(obj, "name");
        String icon = optString(obj, "icon");
        String description = optString(obj, "description");
        String category = optString(obj, "category");
        String image = optString(obj, "image");
        return new Chapter(id, name, icon, description, image, category);
    }

    // ------------------------------------------------------------
    // Client / server entrypoints
    // ------------------------------------------------------------

    public static synchronized void loadClient(boolean forceReload) {
        if (loadedClient && !forceReload && !CHAPTERS.isEmpty()) return;
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object rmObj = mcClass.getMethod("getResourceManager").invoke(mc);
            if (rmObj instanceof ResourceManager rm) {
                load(rm, forceReload);
                loadedClient = true;
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !CHAPTERS.isEmpty()) return;
        var resources = server.getServerResources();
        ResourceManager rm = resources.resourceManager();
        load(rm, forceReload);
        loadedServer = true;
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    public static boolean isEmpty() {
        return CHAPTERS.isEmpty();
    }

    public static Collection<Chapter> all() {
        if (!loadedClient) loadClient(false);
        return Collections.unmodifiableCollection(CHAPTERS.values());
    }

    public static Collection<Chapter> allServer(MinecraftServer server) {
        loadServer(server, false);
        return Collections.unmodifiableCollection(CHAPTERS.values());
    }

    public static Optional<Chapter> byId(String id) {
        if (!loadedClient) loadClient(false);
        return Optional.ofNullable(CHAPTERS.get(id));
    }

    public static Optional<Chapter> byIdServer(MinecraftServer server, String id) {
        if (!loadedServer) loadServer(server, false);
        return Optional.ofNullable(CHAPTERS.get(id));
    }

    public static Optional<Category> categoryById(String id) {
        if (!loadedClient) loadClient(false);
        return Optional.ofNullable(CATEGORIES.get(id));
    }

    // For now categories are always unlocked; dependency is just a future hook.
    public static boolean isCategoryUnlocked(Category c, Player player) {
        if (c == null) return true;
        if (c.dependency == null || c.dependency.isBlank()) return true;
        return true;
    }

    public static boolean includeChapterInAll(Chapter c, Player player) {
        if (c == null) return false;
        Category cat = CATEGORIES.getOrDefault(c.category, null);
        if (cat == null) return true;
        if (cat.excludeFromAll) return false;
        return isCategoryUnlocked(cat, player);
    }

    public static List<Category> categoriesOrdered() {
        if (!loadedClient) loadClient(false);
        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }
        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });
        return list;
    }

    public static synchronized List<Category> categoriesOrderedServer(MinecraftServer server) {
        loadServer(server, false);
        if (!CATEGORIES.containsKey("all")) {
            CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
        }
        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort((a, b) -> {
            if ("all".equals(a.id)) return -1;
            if ("all".equals(b.id)) return 1;
            if (a.order != b.order) return Integer.compare(a.order, b.order);
            return a.name.compareToIgnoreCase(b.name);
        });
        return list;
    }

    // ------------------------------------------------------------
    // Optional: network-style sync (server → client JSON)
    // ------------------------------------------------------------

    public static synchronized void applyNetworkJson(String json) {
        CHAPTERS.clear();
        CATEGORIES.clear();
        try {
            JsonElement rootEl = GSON.fromJson(json, JsonElement.class);
            if (rootEl == null || !rootEl.isJsonObject()) {
                loadedClient = false;
                return;
            }
            JsonObject root = rootEl.getAsJsonObject();

            if (root.has("categories") && root.get("categories").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("categories")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;
                    String icon = optString(o, "icon");
                    String name = optString(o, "name");
                    int order = parseIntFlexible(o, "order", 0);
                    boolean excludeFromAll = parseBoolFlexible(o, "excludeFromAll", false);
                    String dependency = optString(o, "dependency");
                    CATEGORIES.put(id, new Category(id, icon, name, order, excludeFromAll, dependency));
                }
            }

            if (root.has("chapters") && root.get("chapters").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("chapters")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String id = optString(o, "id");
                    if (id == null || id.isBlank()) continue;
                    String name = optString(o, "name");
                    String icon = optString(o, "icon");
                    String description = optString(o, "description");
                    String category = optString(o, "category");
                    String image = optString(o, "image");
                    Chapter c = new Chapter(id, name, icon, description, image, category);
                    if (!isChapterDisabled(c)) CHAPTERS.put(c.id, c);
                }
            }

            if (!CATEGORIES.containsKey("all")) {
                CATEGORIES.put("all", new Category("all", "minecraft:book", "All", Integer.MIN_VALUE, false, ""));
            }

            loadedClient = true;
        } catch (Exception e) {
            CHAPTERS.clear();
            CATEGORIES.clear();
            loadedClient = false;
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static String optString(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    private static int parseIntFlexible(JsonObject o, String key, int def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p.isNumber()) return p.getAsInt();
        try {
            return Integer.parseInt(p.getAsString());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static boolean parseBoolFlexible(JsonObject o, String key, boolean def) {
        if (!o.has(key)) return def;
        JsonPrimitive p = o.getAsJsonPrimitive(key);
        if (p.isBoolean()) return p.getAsBoolean();
        try {
            return Boolean.parseBoolean(p.getAsString());
        } catch (Exception ignored) {
            return def;
        }
    }
}
