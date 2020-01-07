package US.bittiez.KillTracker;

public class STATIC {
    public static String MONSTER_KILLS = "mkills";
    public static String PLAYER_NAME = "name";

    public static String COMBINE_PATH(String... args){
        return String.join(".", args);
    }
}
