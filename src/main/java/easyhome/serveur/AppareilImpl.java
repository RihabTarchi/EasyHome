package easyhome.serveur;

import easyhome.common.Appareil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AppareilImpl extends UnicastRemoteObject implements Appareil {

    private static final long serialVersionUID = 1L;

    private final String nom;
    private final String texteOn;
    private final String texteOff;
    private boolean allume = false;

    public AppareilImpl(String nom, String texteOn, String texteOff) throws RemoteException {
        super();
        this.nom = nom;
        this.texteOn = texteOn;
        this.texteOff = texteOff;
    }

    @Override
    public synchronized void allumer() throws RemoteException {
        if (!allume) {
            allume = true;
            Journal.ajouter(nom + " : " + texteOn);
        }
    }

    @Override
    public synchronized void eteindre() throws RemoteException {
        if (allume) {
            allume = false;
            Journal.ajouter(nom + " : " + texteOff);
        }
    }

    @Override
    public synchronized boolean estAllume() throws RemoteException {
        return allume;
    }

    @Override
    public String getNom() throws RemoteException {
        return nom;
    }

    @Override
    public synchronized String getEtatTexte() throws RemoteException {
        return allume ? texteOn : texteOff;
    }
}