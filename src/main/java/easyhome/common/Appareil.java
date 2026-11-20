package easyhome.common;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface Appareil extends Remote {

    void allumer() throws RemoteException;

    void eteindre() throws RemoteException;

    boolean estAllume() throws RemoteException;

    String getNom() throws RemoteException;

    String getEtatTexte() throws RemoteException;
}