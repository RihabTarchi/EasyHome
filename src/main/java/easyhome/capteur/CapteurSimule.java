package easyhome.capteur;

import easyhome.common.Constantes;
import easyhome.common.Mesure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class CapteurSimule {

    private final String type;
    private final String lieu;
    private final InetAddress hote;
    private final long periodeMs;
    private final Random alea = new Random();

    private volatile double valeur;
    private volatile Double valeurForcee = null;

    private final double min, max, pas;

    public CapteurSimule(String type, String lieu, InetAddress hote, long periodeMs) {
        this.type = type;
        this.lieu = lieu;
        this.hote = hote;
        this.periodeMs = periodeMs;
        switch (type) {
            case Constantes.LUMIERE:     min = 0;  max = 1000; valeur = 500; pas = 80;  break;
            case Constantes.TEMPERATURE: min = 10; max = 45;   valeur = 24;  pas = 1.2; break;
            case Constantes.HUMIDITE:    min = 20; max = 95;   valeur = 55;  pas = 4;   break;
            case Constantes.PLUIE:       min = 0;  max = 100;  valeur = 0;   pas = 12;  break;
            default: throw new IllegalArgumentException("Type de capteur inconnu : " + type
                    + " (choix : LUMIERE, TEMPERATURE, HUMIDITE, PLUIE)");
        }
    }

    private double prochaineValeur() {
        double v = valeur;
        if (valeurForcee != null) {
            v = valeurForcee;
            valeurForcee = null;
        } else {
            double delta = (alea.nextDouble() - 0.5) * 2 * pas;
            if (Constantes.PLUIE.equals(type)) {
                delta -= pas * 0.3;
            }
            v += delta;
        }
        v = Math.max(min, Math.min(max, v));
        valeur = v;
        return v;
    }


    private void ecouterConsole() {
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                String ligne;
                while ((ligne = in.readLine()) != null) {
                    try {
                        valeurForcee = Double.parseDouble(ligne.trim().replace(',', '.'));
                        System.out.println("[" + type + "] valeur forcee a " + valeurForcee);
                    } catch (NumberFormatException e) {
                        System.out.println("Tape un nombre pour forcer la valeur.");
                    }
                }
            } catch (IOException e) {

            }
        }, "console-" + type);
        t.setDaemon(true);
        t.start();
    }

    public void demarrer() throws IOException, InterruptedException {
        ecouterConsole();
        try (DatagramSocket socket = new DatagramSocket()) {     // port source libre, choisi par l'OS
            System.out.println("Capteur " + type + "@" + lieu + " -> " + hote.getHostAddress()
                    + ":" + Constantes.PORT_UDP + " (toutes les " + periodeMs + " ms)");
            while (!Thread.currentThread().isInterrupted()) {
                Mesure m = new Mesure(type, lieu, prochaineValeur(), System.currentTimeMillis());
                byte[] data = m.toMessage().getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, hote, Constantes.PORT_UDP));
                System.out.println("envoye : " + m.toMessage());
                Thread.sleep(periodeMs);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage : <TYPE> <lieu> [hote] [periodeMs]");
            System.out.println("TYPE : LUMIERE | TEMPERATURE | HUMIDITE | PLUIE");
            return;
        }
        String hote = args.length > 2 ? args[2] : "localhost";
        long periode = args.length > 3 ? Long.parseLong(args[3]) : 2000;
        new CapteurSimule(args[0].toUpperCase(), args[1], InetAddress.getByName(hote), periode).demarrer();
    }
}