package easyhome.common;

import java.io.Serializable;
import java.util.Locale;

public class Regle implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String capteur;
    private final String operateur;
    private final double seuil;
    private final String appareil;
    private final boolean allumer;

    public Regle(String capteur, String operateur, double seuil, String appareil, boolean allumer) {
        if (!">".equals(operateur) && !"<".equals(operateur)) {
            throw new IllegalArgumentException("Operateur invalide : " + operateur);
        }
        this.capteur = capteur;
        this.operateur = operateur;
        this.seuil = seuil;
        this.appareil = appareil;
        this.allumer = allumer;
    }

    public boolean concerne(Mesure m) {
        if (!capteur.equals(m.getType())) {
            return false;
        }
        return ">".equals(operateur) ? m.getValeur() > seuil : m.getValeur() < seuil;
    }

    public String getCapteur() { return capteur; }
    public String getOperateur() { return operateur; }
    public double getSeuil() { return seuil; }
    public String getAppareil() { return appareil; }
    public boolean isAllumer() { return allumer; }

    public String getCondition() {
        return capteur + " " + operateur + " " + String.format(Locale.US, "%.1f", seuil) + " " + Constantes.unite(capteur);
    }

    public String getActionTexte() {
        return appareil + " -> " + (allumer ? "ALLUMER" : "ETEINDRE");
    }

    @Override
    public String toString() {
        return "SI " + getCondition() + " ALORS " + getActionTexte();
    }
}