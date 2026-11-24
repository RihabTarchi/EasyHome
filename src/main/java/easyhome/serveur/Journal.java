package easyhome.serveur;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public final class Journal {

    private static final int MAX_LIGNES = 200;
    private static final LinkedList<String> lignes = new LinkedList<>();

    private Journal() { }

    public static synchronized void ajouter(String message) {
        String horaire = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String ligne = "[" + horaire + "] " + message;
        System.out.println(ligne);
        lignes.add(ligne);
        while (lignes.size() > MAX_LIGNES) {
            lignes.removeFirst();
        }
        PublieurEvenements p = publieur;
        if (p != null) {
            p.publier(ligne);   // diffusion JMS de l'evenement
        }
    }

    public static synchronized List<String> dernieres(int n) {
        int debut = Math.max(0, lignes.size() - n);
        return new ArrayList<>(lignes.subList(debut, lignes.size()));
    }
    private static volatile PublieurEvenements publieur;

    public static void setPublieur(PublieurEvenements p) {
        publieur = p;
    }
}