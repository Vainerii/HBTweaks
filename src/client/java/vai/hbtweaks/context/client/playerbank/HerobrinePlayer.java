package vai.hbtweaks.context.client.playerbank;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.json.JSONObject;
import vai.hbtweaks.context.client.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// Not used
public class HerobrinePlayer {

    private static final WeakHashMap<Player, HerobrinePlayer> CACHE = new WeakHashMap<>();

    private final Integer hbId;
    private final String mcName;
    private final String hbName;
    private final Component rpName;
    private final String hbTitle;

    private final byte bulbLit;
    private final byte bulbOff;
    private final byte bulbBroken;
    private final byte bulbShattered;

    private final String mcLevel;

    public HerobrinePlayer(Player player) throws Exception {
        this.mcName = Util.getMCName(player);
        this.rpName = Util.getRpName(player);

        this.hbId = fetchHbId(player);
        if (this.hbId == null)
            throw new Exception("This player has no herobrine_id property");

        Map<String, Object> data = fetchById(hbId);

        this.hbName = (String) data.get("pseudo");

        // Titre choisi
        List<?> titles = (List<?>) data.get("titles");
        String chosenTitle = null;
        for (Object t : titles) {
            Map<?, ?> entry = (Map<?, ?>) t;
            if (Boolean.TRUE.equals(entry.get("choosen"))) {
                Map<?, ?> titleObj = (Map<?, ?>) entry.get("title");
                chosenTitle = titleObj.get("title").toString();
                break;
            }
        }
        this.hbTitle = chosenTitle;

        // Santé mentale (ampoules)
        Map<?, ?> sanity = (Map<?, ?>) data.get("sanity");
        this.bulbLit = ((Number) sanity.get("lit")).byteValue();
        this.bulbOff = ((Number) sanity.get("off")).byteValue();
        this.bulbBroken = ((Number) sanity.get("broken")).byteValue();
        this.bulbShattered = ((Number) sanity.get("shattered")).byteValue();

        // Niveau
        this.mcLevel = data.get("player_level").toString();
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static Integer fetchHbId(Player player) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) return null;
        PlayerInfo pi = me.connection.getPlayerInfo(player.getUUID());
        if (pi == null) return null;
        for (Property property : pi.getProfile().properties().get("herobrine_id")) {
            try {
                return Integer.parseInt(property.value());
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    public static HerobrinePlayer getHerobrinePlayer(Player player) {
        return CACHE.get(player);
    }

    public static boolean isProcessed(Player player) {
        return CACHE.containsKey(player);
    }

    public static void processPlayer(Player player) {
        try {
            HerobrinePlayer.CACHE.put(player, new HerobrinePlayer(player));
        } catch (Exception ignored) {
            HerobrinePlayer.CACHE.put(player, null);
        }
    }

    private static Map<String, Object> fetchById(int memberId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.herobrine.fr/api/players/" + memberId))
                .GET()
                .build();

        String json = HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return new JSONObject(json).toMap();
    }

    public Integer getHbId() {
        return hbId;
    }

    public String getMcName() {
        return mcName;
    }

    public Component getRpName() {
        return rpName;
    }

    public String getHbName() {
        return hbName;
    }

    public String getHbTitle() {
        return hbTitle;
    }

    public byte getBulbLit() {
        return bulbLit;
    }

    public byte getBulbOff() {
        return bulbOff;
    }

    public byte getBulbBroken() {
        return bulbBroken;
    }

    public byte getBulbShattered() {
        return bulbShattered;
    }

    public String getMcLevel() {
        return mcLevel;
    }
}
