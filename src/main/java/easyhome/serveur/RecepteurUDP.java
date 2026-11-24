package easyhome.serveur;

import easyhome.common.Constantes;
import easyhome.common.Mesure;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RecepteurUDP implements Runnable {

    private final Map<String, Mesure> mesures;
    private final MoteurRegles moteur;
    private volatile boolean actif = true;
    private DatagramSocket socket;

    public RecepteurUDP(Map<String, Mesure> mesures, MoteurRegles moteur) {
        this.mesures = mesures;
        this.moteur = moteur;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(Constantes.PORT_UDP);
            Journal.ajouter("Recepteur UDP en ecoute sur le port " + Constantes.PORT_UDP);
            byte[] tampon = new byte[256];
            while (actif) {
                DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);
                socket.receive(paquet);                                  // bloquant
                String message = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);
                try {
                    Mesure m = Mesure.parse(message);
                    mesures.put(m.getCle(), m);
                    moteur.evaluer(m);
                } catch (IllegalArgumentException e) {
                    Journal.ajouter("Paquet ignore : " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            if (actif) {
                Journal.ajouter("Erreur socket UDP : " + e.getMessage()
                        + " (le port " + Constantes.PORT_UDP + " est-il deja utilise ?)");
            }
        } catch (IOException e) {
            Journal.ajouter("Erreur reseau UDP : " + e.getMessage());
        }
    }

    public void arreter() {
        actif = false;
        if (socket != null) {
            socket.close();
        }
    }
}