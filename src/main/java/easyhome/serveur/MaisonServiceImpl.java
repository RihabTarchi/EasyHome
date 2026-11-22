package easyhome.serveur;

import easyhome.common.Constantes;
import easyhome.common.MaisonService;
import easyhome.common.Mesure;
import easyhome.common.Regle;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MaisonServiceImpl extends UnicastRemoteObject implements MaisonService {

    private static final long serialVersionUID = 1L;

    private final transient Map<String, Mesure> mesures;
    private final transient MoteurRegles moteur;

    public MaisonServiceImpl(Map<String, Mesure> mesures, MoteurRegles moteur) throws RemoteException {
        super();
        this.mesures = mesures;
        this.moteur = moteur;
    }

    @Override
    public List<Mesure> getMesures() throws RemoteException {
        long maintenant = System.currentTimeMillis();
        List<Mesure> resultat = new ArrayList<>();
        for (Mesure m : mesures.values()) {
            // On envoie une COPIE avec le statut "hors ligne" calcule avec l'horloge du serveur
            Mesure copie = new Mesure(m.getType(), m.getLieu(), m.getValeur(), m.getHorodatage());
            copie.setHorsLigne(maintenant - m.getHorodatage() > Constantes.TIMEOUT_HORS_LIGNE_MS);
            resultat.add(copie);
        }
        resultat.sort(Comparator.comparing(Mesure::getType).thenComparing(Mesure::getLieu));
        return resultat;
    }

    @Override
    public List<Regle> getRegles() throws RemoteException {
        return moteur.getRegles();
    }

    @Override
    public void ajouterRegle(Regle regle) throws RemoteException {
        moteur.ajouter(regle);
    }

    @Override
    public void modifierRegle(int index, Regle nouvelle) throws RemoteException {
        moteur.modifier(index, nouvelle);
    }

    @Override
    public void supprimerRegle(int index) throws RemoteException {
        moteur.supprimer(index);
    }

    @Override
    public boolean isModeAuto() throws RemoteException {
        return moteur.isModeAuto();
    }

    @Override
    public void setModeAuto(boolean auto) throws RemoteException {
        moteur.setModeAuto(auto);
    }

    @Override
    public List<String> getJournal() throws RemoteException {
        return Journal.dernieres(60);
    }
}