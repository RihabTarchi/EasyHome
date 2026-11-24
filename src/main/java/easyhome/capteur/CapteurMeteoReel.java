package easyhome.capteur;

import easyhome.common.Constantes;
import easyhome.common.Mesure;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CapteurMeteoReel {

    private static final String URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=34.74&longitude=10.76"
            + "&current=temperature_2m,relative_humidity_2m,cloud_cover,wind_speed_10m,precipitation"
            + "&timezone=auto";

    private static final String LIEU = Constantes.LIEU_EXTERIEUR;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    static String appelerApi() throws Exception {
        HttpRequest requete = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> reponse = CLIENT.send(requete, HttpResponse.BodyHandlers.ofString());
        if (reponse.statusCode() != 200) {
            throw new IllegalStateException("API meteo : code HTTP " + reponse.statusCode());
        }
        return reponse.body();
    }

    static double lireChamp(String json, String nom) {
        int debutBloc = json.indexOf("\"current\":{");
        if (debutBloc < 0) {
            throw new IllegalArgumentException("Bloc \"current\" introuvable dans la reponse");
        }
        String bloc = json.substring(debutBloc);
        String cle = "\"" + nom + "\":";
        int debut = bloc.indexOf(cle);
        if (debut < 0) {
            throw new IllegalArgumentException("Champ introuvable : " + nom);
        }
        debut += cle.length();
        int fin = debut;
        while (fin < bloc.length() && "-0123456789.".indexOf(bloc.charAt(fin)) >= 0) {
            fin++;
        }
        return Double.parseDouble(bloc.substring(debut, fin));
    }

    static List<Mesure> lireMesures(String json) {
        long maintenant = System.currentTimeMillis();
        List<Mesure> mesures = new ArrayList<>();
        mesures.add(new Mesure(Constantes.TEMPERATURE, LIEU, lireChamp(json, "temperature_2m"), maintenant));
        mesures.add(new Mesure(Constantes.HUMIDITE, LIEU, lireChamp(json, "relative_humidity_2m"), maintenant));
        mesures.add(new Mesure(Constantes.NUAGES, LIEU, lireChamp(json, "cloud_cover"), maintenant));
        mesures.add(new Mesure(Constantes.VENT, LIEU, lireChamp(json, "wind_speed_10m"), maintenant));
        mesures.add(new Mesure(Constantes.PRECIPITATION, LIEU, lireChamp(json, "precipitation"), maintenant));
        return mesures;
    }

    public static void main(String[] args) throws Exception {
        InetAddress hote = InetAddress.getByName(args.length > 0 ? args[0] : "localhost");
        final long periodeApiMs = 60_000;      // on interroge l'API une fois par minute
        final long periodeEnvoiMs = 2_000;     // on renvoie la derniere mesure connue toutes les 2 s

        List<Mesure> dernieres = new ArrayList<>();
        long prochainAppel = 0;

        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Capteur meteo reel (" + LIEU + ") -> UDP "
                    + hote.getHostAddress() + ":" + Constantes.PORT_UDP);
            while (true) {
                if (System.currentTimeMillis() >= prochainAppel) {
                    try {
                        dernieres = lireMesures(appelerApi());
                        System.out.println("API meteo : mesures mises a jour");
                        for (Mesure m : dernieres) {
                            System.out.println("   " + m.toMessage());
                        }
                        prochainAppel = System.currentTimeMillis() + periodeApiMs;
                    } catch (Exception e) {
                        System.err.println("API meteo indisponible : " + e.getMessage()
                                + " (on garde les dernieres valeurs, nouvel essai dans 10 s)");
                        prochainAppel = System.currentTimeMillis() + 10_000;
                    }
                }
                for (Mesure m : dernieres) {
                    byte[] data = m.toMessage().getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(data, data.length, hote, Constantes.PORT_UDP));
                }
                Thread.sleep(periodeEnvoiMs);
            }
        }
    }
}