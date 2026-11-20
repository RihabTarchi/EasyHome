package easyhome.common;

import java.io.Serializable;
import java.util.Locale;

public class Mesure implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String lieu;
    private final double valeur;
    private final long horodatage;
    private boolean horsLigne;

    public Mesure(String type, String lieu, double valeur, long horodatage) {
        this.type = type;
        this.lieu = lieu;
        this.valeur = valeur;
        this.horodatage = horodatage;
    }

    public static Mesure parse(String message) {
        String[] p = message.trim().split(";");
        if (p.length != 3) {
            throw new IllegalArgumentException("Format invalide (attendu TYPE;lieu;valeur) : " + message);
        }
        try {
            double v = Double.parseDouble(p[2].trim().replace(',', '.'));
            return new Mesure(p[0].trim().toUpperCase(), p[1].trim(), v, System.currentTimeMillis());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur non numerique : " + message);
        }
    }

    public String toMessage() {
        return type + ";" + lieu + ";" + String.format(Locale.US, "%.1f", valeur);
    }

    public String getCle() {
        return type + "@" + lieu;
    }

    public String getType() { return type; }
    public String getLieu() { return lieu; }
    public double getValeur() { return valeur; }
    public long getHorodatage() { return horodatage; }
    public boolean isHorsLigne() { return horsLigne; }
    public void setHorsLigne(boolean horsLigne) { this.horsLigne = horsLigne; }

    @Override
    public String toString() {
        return type + "@" + lieu + " = " + String.format(Locale.US, "%.1f", valeur) + " " + Constantes.unite(type);
    }
}