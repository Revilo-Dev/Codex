package net.revilodev.codex.data;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
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
            this.category = (category == null || category.isBlank()) ? "" : category;
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

        public Category(String id, String icon, String name, int order) {
            this.id = id;
            this.icon = icon == null || icon.isBlank() ? "minecraft:book" : icon;
            this.name = name == null || name.isBlank() ? id : name;
            this.order = order;
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

        loadCategories(rm);
        loadChapters(rm);
    }

    private static void loadCategories(ResourceManager rm) {
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
                    id = p.substring(lastSlash + 1).replace(".json", "");
                }

                String icon = optString(obj, "icon");
                String name = optString(obj, "name");
                int order = parseIntFlexible(obj, "order", 0);

                if (id != null && !id.isBlank()) {
                    CATEGORIES.put(id, new Category(id, icon, name, order));
                }
            } catch (Exception ignored) {}
        }
    }

    private static void loadChapters(ResourceManager rm) {
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

                // Only load chapters with a valid existing category
                if (c != null && CATEGORIES.containsKey(c.category)) {
                    if (!isChapterDisabled(c)) {
                        CHAPTERS.put(c.id, c);
                    }
                }
            } catch (Exception ignored) {}
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
            id = p.substring(lastSlash + 1).replace(".json", "");
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
            var mc = net.minecraft.client.Minecraft.getInstance();
            ResourceManager rm = mc.getResourceManager();
            load(rm, forceReload);
            loadedClient = true;
        } catch (Throwable ignored) {}
    }

    public static synchronized void loadServer(MinecraftServer server, boolean forceReload) {
        if (loadedServer && !forceReload && !CHAPTERS.isEmpty()) return;
        ResourceManager rm = server.getServerResources().resourceManager();
        load(rm, forceReload);
        loadedServer = true;
    }

    // ------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------

    public static boolean isEmpty() {
        return CHAPTERS.isEmpty();
    }

    public static Collection<Chapter> allChapters() {
        if (!loadedClient) loadClient(false);
        return Collections.unmodifiableCollection(CHAPTERS.values());
    }

    public static List<Category> allCategories() {
        if (!loadedClient) loadClient(false);
        List<Category> list = new ArrayList<>(CATEGORIES.values());
        list.sort(Comparator.comparingInt(c -> c.order));
        return list;
    }

    public static List<Chapter> chaptersInCategory(String categoryId) {
        if (!loadedClient) loadClient(false);

        List<Chapter> out = new ArrayList<>();
        for (Chapter c : CHAPTERS.values()) {
            if (c.category.equals(categoryId)) {
                out.add(c);
            }
        }
        return out;
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static boolean isChapterDisabled(Chapter c) {
        return Config.disabledCategories().contains(c.category);
    }

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
}
