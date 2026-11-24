package easyhome.capteur;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CapteurMeteoReel {

    private static final String URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=34.74&longitude=10.76"
            + "&current=temperature_2m,relative_humidity_2m,precipitation,cloud_cover,is_day"
            + "&timezone=auto";

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

    public static void main(String[] args) throws Exception {
        String json = appelerApi();
        System.out.println("Temperature : " + lireChamp(json, "temperature_2m") + " C");
        System.out.println("Humidite    : " + lireChamp(json, "relative_humidity_2m") + " %");
        System.out.println("Pluie       : " + lireChamp(json, "precipitation") + " mm");
        System.out.println("Nuages      : " + lireChamp(json, "cloud_cover") + " %");
        System.out.println("Jour (1/0)  : " + lireChamp(json, "is_day"));
    }
}