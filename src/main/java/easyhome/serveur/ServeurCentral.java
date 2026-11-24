package easyhome.serveur;

import easyhome.common.Appareil;
import easyhome.common.Constantes;
import easyhome.common.Mesure;
import easyhome.common.Regle;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


public class ServeurCentral {

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.setProperty("java.rmi.server.hostname", args[0]);
        }


        Registry registry = LocateRegistry.createRegistry(Constantes.PORT_RMI);


        Map<String, Appareil> appareils = new TreeMap<>();
        appareils.put("Climatiseur",  new AppareilImpl("Climatiseur", "ALLUME", "ETEINT"));
        appareils.put("Fenetre",      new AppareilImpl("Fenetre", "FERMEE", "OUVERTE"));
        appareils.put("Lampe",        new AppareilImpl("Lampe", "ALLUMEE", "ETEINTE"));
        appareils.put("Ventilateur",  new AppareilImpl("Ventilateur", "ALLUME", "ETEINT"));

        for (Map.Entry<String, Appareil> e : appareils.entrySet()) {
            registry.rebind(Constantes.PREFIXE_APPAREIL + e.getKey(), e.getValue());
        }

        Map<String, Mesure> mesures = new ConcurrentHashMap<>();
        MoteurRegles moteur = new MoteurRegles(appareils);
        registry.rebind(Constantes.NOM_SERVICE, new MaisonServiceImpl(mesures, moteur));


        chargerReglesParDefaut(moteur);


        BrokerJms broker = new BrokerJms();
        PublieurEvenements publieur = new PublieurEvenements();
        try {
            broker.demarrer();
            publieur.demarrer();
            Journal.setPublieur(publieur);
        } catch (Exception e) {

            System.err.println("JMS indisponible, le serveur continue sans : " + e);
        }


        RecepteurUDP recepteur = new RecepteurUDP(mesures, moteur);
        Thread t = new Thread(recepteur, "recepteur-udp");
        t.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            recepteur.arreter();
            publieur.arreter();
            broker.arreter();
        }));
        Journal.ajouter("Serveur pret. Registry RMI sur le port " + Constantes.PORT_RMI
                + " - " + appareils.size() + " appareils publies.");
    }

    private static void chargerReglesParDefaut(MoteurRegles m) {
        m.ajouter(new Regle(Constantes.TEMPERATURE, ">", 30, "Climatiseur", true));
        m.ajouter(new Regle(Constantes.TEMPERATURE, "<", 27, "Climatiseur", false));

        m.ajouter(new Regle(Constantes.PLUIE, ">", 20, "Fenetre", true));
        m.ajouter(new Regle(Constantes.PLUIE, "<", 5, "Fenetre", false));

        m.ajouter(new Regle(Constantes.LUMIERE, "<", 200, "Lampe", true));
        m.ajouter(new Regle(Constantes.LUMIERE, ">", 400, "Lampe", false));

        m.ajouter(new Regle(Constantes.HUMIDITE, ">", 70, "Ventilateur", true));
        m.ajouter(new Regle(Constantes.HUMIDITE, "<", 55, "Ventilateur", false));
    }
}