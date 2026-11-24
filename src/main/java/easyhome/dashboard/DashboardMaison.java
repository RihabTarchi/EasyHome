package easyhome.dashboard;

import easyhome.common.Appareil;
import easyhome.common.Constantes;
import easyhome.common.MaisonService;
import easyhome.common.Mesure;
import easyhome.common.Regle;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DashboardMaison extends JFrame {

    private static final long serialVersionUID = 1L;

    private interface AppelRmi {
        void executer() throws Exception;
    }

    private final String hote;
    private volatile MaisonService service;
    private final Map<String, Appareil> appareils = new TreeMap<>();
    private final Map<String, JToggleButton> boutons = new HashMap<>();

    private final DefaultTableModel modeleMesures =
            new DefaultTableModel(new String[]{"Capteur", "Lieu", "Valeur", "Statut"}, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private final DefaultTableModel modeleRegles =
            new DefaultTableModel(new String[]{"#", "Condition", "Action"}, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable tableMesures = new JTable(modeleMesures);
    private final JTable tableRegles = new JTable(modeleRegles);
    private final JTextArea zoneJournal = new JTextArea();
    private final JTextArea zoneEvenements = new JTextArea();   // evenements recus par JMS
    private volatile AbonneEvenements abonne;
    private final JCheckBox caseAuto = new JCheckBox("Mode automatique", true);
    private final JLabel etat = new JLabel("Connexion...");
    private final JPanel panneauAppareils = new JPanel(new GridLayout(0, 1, 6, 6));

    // Formulaire des regles
    private final JComboBox<String> choixCapteur = new JComboBox<>(Constantes.TYPES);
    private final JComboBox<String> choixOperateur = new JComboBox<>(new String[]{">", "<"});
    private final JSpinner champSeuil = new JSpinner(new SpinnerNumberModel(30.0, -1000.0, 100000.0, 1.0));
    private final JComboBox<String> choixAppareil = new JComboBox<>();
    private final JComboBox<String> choixAction = new JComboBox<>(new String[]{"ALLUMER", "ETEINDRE"});

    private List<Regle> reglesAffichees = new ArrayList<>();
    private String signatureRegles = "";
    private String dernierJournal = "";

    private volatile boolean majEnCours = false;
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

    public DashboardMaison(String hote) {
        super("EasyHome - Tableau de bord");
        this.hote = hote;
        construireInterface();
        connecterJms();
        pool.scheduleWithFixedDelay(this::rafraichir, 0, 1, TimeUnit.SECONDS);
    }


    private void construireInterface() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel haut = new JPanel(new BorderLayout());
        JLabel titre = new JLabel("EasyHome");
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 20f));
        haut.add(titre, BorderLayout.WEST);
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        droite.add(etat);
        droite.add(caseAuto);
        haut.add(droite, BorderLayout.EAST);
        add(haut, BorderLayout.NORTH);

        caseAuto.addActionListener(e -> {
            if (majEnCours) return;
            boolean auto = caseAuto.isSelected();
            appeler(() -> service.setModeAuto(auto));
        });

        tableMesures.setRowHeight(24);
        tableMesures.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                boolean horsLigne = "HORS LIGNE".equals(t.getValueAt(r, 3));
                comp.setForeground(horsLigne ? Color.GRAY : Color.BLACK);
                return comp;
            }
        });
        JPanel gauche = new JPanel(new BorderLayout());
        gauche.setBorder(BorderFactory.createTitledBorder("Capteurs (reception UDP)"));
        gauche.add(new JScrollPane(tableMesures), BorderLayout.CENTER);

        panneauAppareils.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel droiteAppareils = new JPanel(new BorderLayout());
        droiteAppareils.setBorder(BorderFactory.createTitledBorder("Appareils (commande RMI)"));
        droiteAppareils.add(panneauAppareils, BorderLayout.NORTH);
        droiteAppareils.setPreferredSize(new Dimension(300, 0));

        JPanel centre = new JPanel(new BorderLayout(8, 8));
        centre.add(gauche, BorderLayout.CENTER);
        centre.add(droiteAppareils, BorderLayout.EAST);
        add(centre, BorderLayout.CENTER);

        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Regles automatiques", construirePanneauRegles());
        zoneJournal.setEditable(false);
        zoneJournal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        onglets.addTab("Journal", new JScrollPane(zoneJournal));
        zoneEvenements.setEditable(false);
        zoneEvenements.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        onglets.addTab("Evenements en direct (JMS)", new JScrollPane(zoneEvenements));
        onglets.setPreferredSize(new Dimension(0, 250));
        add(onglets, BorderLayout.SOUTH);
    }

    private JPanel construirePanneauRegles() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        tableRegles.setRowHeight(22);
        tableRegles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRegles.getColumnModel().getColumn(0).setMaxWidth(40);
        p.add(new JScrollPane(tableRegles), BorderLayout.CENTER);

        tableRegles.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || majEnCours) return;
            int ligne = tableRegles.getSelectedRow();
            if (ligne >= 0 && ligne < reglesAffichees.size()) {
                remplirFormulaire(reglesAffichees.get(ligne));
            }
        });

        JPanel formulaire = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        formulaire.add(new JLabel("SI"));
        formulaire.add(choixCapteur);
        formulaire.add(choixOperateur);
        formulaire.add(champSeuil);
        formulaire.add(new JLabel("ALORS"));
        formulaire.add(choixAppareil);
        formulaire.add(choixAction);
        JButton ajouter = new JButton("Ajouter");
        JButton modifier = new JButton("Modifier la selection");
        JButton supprimer = new JButton("Supprimer la selection");
        formulaire.add(ajouter);
        formulaire.add(modifier);
        formulaire.add(supprimer);
        p.add(formulaire, BorderLayout.SOUTH);

        ajouter.addActionListener(e -> {
            Regle r = lireFormulaire();
            if (r != null) {
                appeler(() -> service.ajouterRegle(r));
            }
        });
        modifier.addActionListener(e -> {
            int ligne = tableRegles.getSelectedRow();
            if (ligne < 0) {
                JOptionPane.showMessageDialog(this, "Selectionne d'abord une regle dans le tableau.",
                        "Modifier", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Regle r = lireFormulaire();
            if (r != null) {
                appeler(() -> service.modifierRegle(ligne, r));
            }
        });
        supprimer.addActionListener(e -> {
            int ligne = tableRegles.getSelectedRow();
            if (ligne < 0) {
                JOptionPane.showMessageDialog(this, "Selectionne d'abord une regle dans le tableau.",
                        "Supprimer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            appeler(() -> service.supprimerRegle(ligne));
        });
        return p;
    }

    private Regle lireFormulaire() {
        if (choixAppareil.getItemCount() == 0) {
            return null;
        }
        try {
            champSeuil.commitEdit();
        } catch (java.text.ParseException ex) {

        }
        return new Regle((String) choixCapteur.getSelectedItem(),
                (String) choixOperateur.getSelectedItem(),
                ((Number) champSeuil.getValue()).doubleValue(),
                (String) choixAppareil.getSelectedItem(),
                "ALLUMER".equals(choixAction.getSelectedItem()));
    }

    private void remplirFormulaire(Regle r) {
        choixCapteur.setSelectedItem(r.getCapteur());
        choixOperateur.setSelectedItem(r.getOperateur());
        champSeuil.setValue(r.getSeuil());
        choixAppareil.setSelectedItem(r.getAppareil());
        choixAction.setSelectedItem(r.isAllumer() ? "ALLUMER" : "ETEINDRE");
    }

    private void construireBoutonsAppareils() {
        panneauAppareils.removeAll();
        boutons.clear();
        choixAppareil.removeAllItems();
        for (String nom : appareils.keySet()) {
            JToggleButton b = new JToggleButton(nom);
            b.setPreferredSize(new Dimension(260, 44));
            b.addActionListener(e -> {
                if (majEnCours) return;
                boolean allumer = b.isSelected();
                Appareil a = appareils.get(nom);
                appeler(() -> {
                    if (allumer) a.allumer(); else a.eteindre();
                });
            });
            boutons.put(nom, b);
            panneauAppareils.add(b);
            choixAppareil.addItem(nom);
        }
        panneauAppareils.revalidate();
        panneauAppareils.repaint();
    }


    private void appeler(AppelRmi appel) {
        pool.execute(() -> {
            try {
                if (service == null) {
                    throw new IllegalStateException("Serveur injoignable");
                }
                appel.executer();
                rafraichir();
            } catch (Exception ex) {
                erreur(ex);
            }
        });
    }

    private void connecter() throws Exception {
        Registry registry = LocateRegistry.getRegistry(hote, Constantes.PORT_RMI);
        MaisonService s = (MaisonService) registry.lookup(Constantes.NOM_SERVICE);
        Map<String, Appareil> trouves = new TreeMap<>();
        for (String nom : registry.list()) {
            if (nom.startsWith(Constantes.PREFIXE_APPAREIL)) {
                trouves.put(nom.substring(Constantes.PREFIXE_APPAREIL.length()), (Appareil) registry.lookup(nom));
            }
        }
        appareils.clear();
        appareils.putAll(trouves);
        service = s;
        SwingUtilities.invokeLater(this::construireBoutonsAppareils);
    }

    private void rafraichir() {
        try {
            if (service == null) {
                connecter();
            }
            final List<Mesure> mesures = service.getMesures();
            final List<Regle> regles = service.getRegles();
            final List<String> journal = service.getJournal();
            final boolean auto = service.isModeAuto();
            final Map<String, String> textes = new HashMap<>();
            final Map<String, Boolean> etats = new HashMap<>();
            for (Map.Entry<String, Appareil> e : new ArrayList<>(appareils.entrySet())) {
                etats.put(e.getKey(), e.getValue().estAllume());
                textes.put(e.getKey(), e.getValue().getEtatTexte());
            }
            SwingUtilities.invokeLater(() -> appliquer(mesures, regles, journal, auto, etats, textes));
        } catch (Exception e) {
            service = null;
            SwingUtilities.invokeLater(() -> {
                etat.setText("Serveur injoignable (" + hote + ")");
                etat.setForeground(Color.RED);
            });
        }
    }


    private void appliquer(List<Mesure> mesures, List<Regle> regles, List<String> journal,
                           boolean auto, Map<String, Boolean> etats, Map<String, String> textes) {
        majEnCours = true;
        try {
            etat.setText("Connecte a " + hote);
            etat.setForeground(new Color(0, 120, 0));
            caseAuto.setSelected(auto);

            modeleMesures.setRowCount(0);
            for (Mesure m : mesures) {
                modeleMesures.addRow(new Object[]{
                        m.getType(), m.getLieu(),
                        String.format(Locale.US, "%.1f %s", m.getValeur(), Constantes.unite(m.getType())),
                        m.isHorsLigne() ? "HORS LIGNE" : "en ligne"});
            }

            String signature = regles.toString();
            if (!signature.equals(signatureRegles)) {
                signatureRegles = signature;
                reglesAffichees = new ArrayList<>(regles);
                int selection = tableRegles.getSelectedRow();
                modeleRegles.setRowCount(0);
                for (int i = 0; i < regles.size(); i++) {
                    modeleRegles.addRow(new Object[]{i + 1, regles.get(i).getCondition(), regles.get(i).getActionTexte()});
                }
                if (selection >= 0 && selection < modeleRegles.getRowCount()) {
                    tableRegles.setRowSelectionInterval(selection, selection);
                }
            }

            for (Map.Entry<String, Boolean> e : etats.entrySet()) {
                JToggleButton b = boutons.get(e.getKey());
                if (b != null) {
                    b.setSelected(e.getValue());
                    b.setText(e.getKey() + "  :  " + textes.get(e.getKey()));
                    b.setBackground(e.getValue() ? new Color(160, 220, 160) : null);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (String l : journal) sb.append(l).append('\n');
            String texte = sb.toString();
            if (!texte.equals(dernierJournal)) {
                dernierJournal = texte;
                zoneJournal.setText(texte);
                zoneJournal.setCaretPosition(zoneJournal.getDocument().getLength());
            }
        } finally {
            majEnCours = false;
        }
    }

    private void connecterJms() {
        pool.execute(() -> {
            try {
                AbonneEvenements a = new AbonneEvenements(texte -> SwingUtilities.invokeLater(() -> {
                    zoneEvenements.append(texte + "\n");
                    zoneEvenements.setCaretPosition(zoneEvenements.getDocument().getLength());
                }));
                a.demarrer();
                abonne = a;
                SwingUtilities.invokeLater(() ->
                        zoneEvenements.append("[JMS] abonne au Topic " + Constantes.NOM_TOPIC + "\n"));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        zoneEvenements.append("[JMS] broker injoignable, nouvel essai dans 5 s...\n"));
                pool.schedule(this::connecterJms, 5, TimeUnit.SECONDS);
            }
        });
    }

    private void erreur(Exception e) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE));
    }

    public static void main(String[] args) {
        String hote = args.length > 0 ? args[0] : "localhost";
        SwingUtilities.invokeLater(() -> new DashboardMaison(hote).setVisible(true));
    }
}