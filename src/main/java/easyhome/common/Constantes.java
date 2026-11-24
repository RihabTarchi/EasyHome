package easyhome.common;

public final class Constantes {

    private Constantes() { }

    public static final int PORT_UDP = 5000;

    public static final int PORT_RMI = 1099;

    public static final String NOM_SERVICE = "MaisonService";

    public static final String PREFIXE_APPAREIL = "Appareil/";

    public static final long TIMEOUT_HORS_LIGNE_MS = 10_000;

    public static final String LIEU_EXTERIEUR = "exterieur";
    public static final String NUAGES = "NUAGES";
    public static final String VENT = "VENT";
    public static final String PRECIPITATION = "PRECIPITATION";

    // --- JMS ---
    public static final String URL_BROKER = "tcp://localhost:61616";
    public static final String NOM_TOPIC = "easyhome.evenements";


    public static final String LUMIERE = "LUMIERE";
    public static final String TEMPERATURE = "TEMPERATURE";
    public static final String HUMIDITE = "HUMIDITE";
    public static final String PLUIE = "PLUIE";

    public static final String[] TYPES = { LUMIERE, TEMPERATURE, HUMIDITE, PLUIE };

    public static String unite(String type) {
        switch (type) {
            case LUMIERE:     return "lux";
            case TEMPERATURE: return "\u00B0C";
            case HUMIDITE:    return "%";
            case PLUIE:       return "%";
            case NUAGES:        return "%";
            case VENT:          return "km/h";
            case PRECIPITATION: return "mm";
            default:          return "";
        }
    }
}
