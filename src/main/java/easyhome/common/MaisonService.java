package easyhome.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MaisonService extends Remote {


    List<Mesure> getMesures() throws RemoteException;

    List<Regle> getRegles() throws RemoteException;

    void ajouterRegle(Regle regle) throws RemoteException;


    void modifierRegle(int index, Regle nouvelle) throws RemoteException;

    void supprimerRegle(int index) throws RemoteException;

    boolean isModeAuto() throws RemoteException;

    void setModeAuto(boolean auto) throws RemoteException;


    List<String> getJournal() throws RemoteException;
}