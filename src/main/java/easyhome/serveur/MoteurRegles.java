package easyhome.serveur;

import easyhome.common.Appareil;
import easyhome.common.Mesure;
import easyhome.common.Regle;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class MoteurRegles {

    private final List<Regle> regles = new CopyOnWriteArrayList<>();
    private final Map<String, Appareil> appareils;
    private volatile boolean modeAuto = true;

    public MoteurRegles(Map<String, Appareil> appareils) {
        this.appareils = appareils;
    }

    public void evaluer(Mesure m) {
        if (!modeAuto) {
            return;
        }
        for (Regle r : regles) {
            if (!r.concerne(m)) {
                continue;
            }
            Appareil a = appareils.get(r.getAppareil());
            if (a == null) {
                continue;
            }
            try {

                if (a.estAllume() != r.isAllumer()) {
                    if (r.isAllumer()) {
                        a.allumer();
                    } else {
                        a.eteindre();
                    }
                    Journal.ajouter("[AUTO] " + r + "   (mesure = " + m.getValeur() + ")");
                }
            } catch (RemoteException e) {
                Journal.ajouter("Erreur sur l'appareil " + r.getAppareil() + " : " + e.getMessage());
            }
        }
    }

    public void ajouter(Regle r) {
        regles.add(r);
        Journal.ajouter("Regle ajoutee : " + r);
    }

    public void modifier(int index, Regle nouvelle) {
        if (index >= 0 && index < regles.size()) {
            regles.set(index, nouvelle);
            Journal.ajouter("Regle modifiee : " + nouvelle);
        }
    }

    public void supprimer(int index) {
        if (index >= 0 && index < regles.size()) {
            Regle r = regles.remove(index);
            Journal.ajouter("Regle supprimee : " + r);
        }
    }

    public List<Regle> getRegles() {
        return new ArrayList<>(regles);
    }

    public boolean isModeAuto() { return modeAuto; }

    public void setModeAuto(boolean auto) {
        this.modeAuto = auto;
        Journal.ajouter("Mode " + (auto ? "AUTOMATIQUE" : "MANUEL"));
    }
}