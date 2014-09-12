/*
 * This is a small panel that allows quick user controls to guide the plot panel
 */
package com.pfizer.mrbt.genomics;

//import com.pfizer.mrbt.genomics.annotation.AnnotationPanel;
import com.pfizer.mrbt.genomics.hline.LineCreationPane;
import com.pfizer.mrbt.genomics.data.DataPointEntry;
import com.pfizer.mrbt.genomics.data.DataSet;
import com.pfizer.mrbt.genomics.data.GeneAnnotation;
import com.pfizer.mrbt.genomics.data.Model;
import com.pfizer.mrbt.genomics.data.SNP;
import com.pfizer.mrbt.genomics.hline.HLine;
import com.pfizer.mrbt.genomics.hline.LineModificationPane;
import com.pfizer.mrbt.genomics.legend.LegendPanel;
import com.pfizer.mrbt.genomics.state.SelectedGeneAnnotation;
import com.pfizer.mrbt.genomics.state.StateListener;
import com.pfizer.mrbt.genomics.state.View;
import com.pfizer.mrbt.axis.AxisScale;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author henstockpv
 */
public class ControlsPanel extends JComponent {

    private JTextField geneSearchField;
    private AbstractButton geneSearchButton;
    private JCheckBox showRecombinationRateCheckBox;
    private JSlider averagingSlider;
    private JTextField averagingTextField;
    private JComponent averagingPanel;
    private JComponent checkBoxPanel;
    private JComponent lineButtonPanel;
    private JCheckBox showPointsCheckBox;
    private JCheckBox showAveragingLinesCheckBox;
    private JComboBox showSNPLinesComboBox;
    private AbstractButton adjustImageButton;
    private AbstractButton buildLegendButton;
    private PlotPanel plotPanel;
    private AbstractButton addLineButton;
    private AbstractButton editLinesButton;
    private JCheckBox printReadyCheckBox;
    private JComponent imageOptionsPanel;
    private boolean snpSearchRequest = true; // if search for SNP using search button, true else false

    private String[] SNP_LINES_CHOICES = {"No Lines", "Average Lines", "Connecting Lines"};

    
    public ControlsPanel(PlotPanel plotPanel) {
        super();
        this.plotPanel = plotPanel;  // kluge so have access to the Manhattan plot for resizing
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 10;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.NONE;
        add(getGeneSearchField(), gbc);

        gbc.gridx = 20;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.NONE;
        add(getGeneSearchButton(), gbc);

        gbc.gridx = 30;
        gbc.gridy = 10;
        gbc.insets = new Insets(5, 20, 5, 5);
        add(getShowRecombinationRateCheckBox(), gbc);


        gbc.gridx = 40;
        gbc.gridy = 10;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(getAveragingPanel(), gbc);

        gbc.gridx = 50;
        gbc.gridy = 10;
        add(getCheckBoxPanel(), gbc);

        gbc.gridx = 60;
        gbc.gridy = 10;
        add(getLineButtonPanel(), gbc);

        gbc.gridx = 80;
        gbc.gridy = 10;
        //add(getBuildLegendButton(), gbc);

        gbc.gridx = 90;
        gbc.gridy = 10;
        add(getImageOptionsPanel(), gbc);
        //add(getAdjustImageButton(), gbc);
        
        StateController stateController = new StateController();
        Singleton.getState().addListener(stateController);

        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    }

    /**
     * Sets up the field for searching for the gene. If you click return
     * in it then it will find the requested gene in the view
     *
     * @return
     */
    protected JTextField getGeneSearchField() {
        if (geneSearchField == null) {
            geneSearchField = new JTextField(10);
            Dimension preferredSize = geneSearchField.getPreferredSize();
            geneSearchField.setMinimumSize(preferredSize);
            geneSearchField.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent ke) {
                    int keyCode = ke.getKeyCode();
                    if (keyCode == KeyEvent.VK_ENTER) {
                        geneSearchButton.doClick();
                    }
                }
            });
        }
        return geneSearchField;
    }

    protected AbstractButton getGeneSearchButton() {
        if (geneSearchButton == null) {
            geneSearchButton = new JButton("Search");
            geneSearchButton.setToolTipText("Searches for gene in current view");
            geneSearchButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    String searchTerm = geneSearchField.getText().trim();
                    if(searchTerm.trim().isEmpty()) {
                        clearSnpSearch();
                    } else if(isGeneSearch(searchTerm)) {
                        GeneAnnotation foundGene = findSearchTerm(geneSearchField.getText().trim());
                        if(foundGene != null) {
                            ControlsPanel.this.plotPanel.ensureGeneAnnotationIsVisible(foundGene.getGene());
                        }
                    } else {
                        performSNPSearch(searchTerm);
                    }
                }
            });
        }
        return geneSearchButton;
    }

    /**
     * Allows formats in the form of a single gene with letters or a
     * multi-rsid with numbers, separating punctuation and spaces. It will
     * also return true if the search string is empty
     *
     * @param searchTerm
     * @return
     */
    private boolean isGeneSearch(String searchTerm) {
        if (searchTerm.length() == 0) {
            return true;
        } else if(searchTerm.matches("^\\s*[rR][sS]\\-*[0-9]+.*$")) {
            return false;
        } else if (searchTerm.matches("^.*[a-zA-Z]+.*$")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Search term may be any white-space or punctuation (, ; / |)
     * delimited list. It will set the list of SNPs in the state.
     *
     * @param searchTerm
     */
    protected void performSNPSearch(String searchTerm) {
        String[] tokens = searchTerm.split("[\\/\\;\\,\\|\\s]+");
        ArrayList<Integer> searchRsIds = validateSearchTerms(tokens);
        int numSearchTerms = searchRsIds.size();
        if (numSearchTerms == 0) {
            JOptionPane.showMessageDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    "Search for single genes or [|,;/] separated Rs-Ids",
                    "Illegal entry",
                    JOptionPane.ERROR_MESSAGE);
            System.out.println("Invalid search term list");
            return;
        }
        ArrayList<DataPointEntry> foundRsIdEntries = new ArrayList<DataPointEntry>();
        View view = Singleton.getState().getMainView();
        DataSet dataSet = view.getDataSet();
        CopyOnWriteArrayList<SNP> snps = dataSet.getSnps();
        ArrayList<Model> models = view.getModels();
        for (SNP snp : snps) {
            int oneRsId = snp.getRsId();
            for (Integer rsid : searchRsIds) {
                if (rsid == oneRsId) {
                    for (Model model : models) {
                        Double logpval = dataSet.getPvalFromSnpModel(snp, model);
                        if (logpval != null) { // found snp in model
                            DataPointEntry foundDPE = new DataPointEntry(dataSet, model, snp);
                            foundRsIdEntries.add(foundDPE);
                        }
                    }
                }
            }
        }
        if (foundRsIdEntries.isEmpty() && snpSearchRequest) {
            System.out.println("No rsId entries found");
            Singleton.getState().clearRsIdSearchResults();
            JOptionPane.showMessageDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    "No matching RsID SNP entries were found in this view",
                    "Not found",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            Singleton.getState().setRsIdSearchResults(foundRsIdEntries);
        }
    }
    
    protected void clearSnpSearch() {
        ArrayList<DataPointEntry> emptyList = new ArrayList<DataPointEntry>();
        Singleton.getState().setRsIdSearchResults(emptyList);
    }

    /**
     * Returns the integer value of each of the terms. If any of them are
     * not valid, then it returns a 0-length list.
     *
     * @param terms
     */
    private ArrayList<Integer> validateSearchTerms(String[] terms) {
        int numTerms = terms.length;
        ArrayList<Integer> searchTerms = new ArrayList<Integer>();
        for (int termi = 0; termi < numTerms; termi++) {
            try {
                String oneTermStr = terms[termi].replaceFirst("[rR][sS]\\-*", "");
                int oneTerm = Integer.parseInt(oneTermStr);
                searchTerms.add(oneTerm);
            } catch (NumberFormatException nfe) {
                searchTerms.clear();
                return searchTerms;
            }
        }
        return searchTerms;
    }

    /**
     * Performs a case-insensitive search on the genes in the current
     * view. If found, it currently will just select the gene regardless
     * of whether it is in the view
     *
     * @param gene
     * @return
     */
    protected GeneAnnotation findSearchTerm(String gene) {
        View mainView = Singleton.getState().getMainView();
        int chromosome = mainView.getDataSet().getChromosome();
        AxisScale xAxisScale = mainView.getXAxis();
        //double minx = xAxisScale.getMinDisplayValue();
        //double maxx = xAxisScale.getMaxDisplayValue();
        DataSet dataSet = mainView.getDataSet();
        //GeneAnnotations geneAnnotations = Singleton.getDataModel().getGeneAnnotations();
        //List<GeneAnnotation> geneAnnotationList = geneAnnotations.getAnnotations(chromosome, (int) Math.round(minx), (int) Math.round(maxx));
        List<GeneAnnotation> geneAnnotationList = dataSet.getGeneAnnotations();
        boolean found = false;
        for (GeneAnnotation geneAnnotation : geneAnnotationList) {
            if (gene.equalsIgnoreCase(geneAnnotation.getGene())) {
                Singleton.getState().setSelectedGeneAnnotation(new SelectedGeneAnnotation(geneAnnotation, chromosome));
                return geneAnnotation;
            }
        }
        // not found
        JOptionPane.showMessageDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Could not find gene " + gene + " in this view",
                "Not Found",
                JOptionPane.WARNING_MESSAGE);
        return null;
    }

    protected JCheckBox getShowRecombinationRateCheckBox() {
        if (showRecombinationRateCheckBox == null) {
            showRecombinationRateCheckBox = new JCheckBox("<html>Show<br/>Recombination<br/>Rate</html>");
            showRecombinationRateCheckBox.setSelected(Singleton.getState().getShowRecombinationRate());
            showRecombinationRateCheckBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    Singleton.getState().setShowRecombinationRate(showRecombinationRateCheckBox.isSelected());
                    System.out.println("action listener on show recombo rate hit");
                }
            });
        }
        return showRecombinationRateCheckBox;
    }

    protected JComponent getAveragingPanel() {
        if (averagingPanel == null) {
            averagingPanel = new JPanel();
            averagingPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 10;
            gbc.gridy = 10;
            averagingPanel.add(getAveragingSlider(), gbc);

            gbc.gridx = 20;
            gbc.gridy = 10;
            averagingPanel.add(getAveragingTextField(), gbc);
            averagingPanel.setBorder(BorderFactory.createTitledBorder("Averaging Filter Width"));
        }
        return averagingPanel;
    }

    protected JComponent getCheckBoxPanel() {
        if (checkBoxPanel == null) {
            checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 10;
            gbc.gridy = 10;
            gbc.anchor = GridBagConstraints.WEST;
            checkBoxPanel.add(getShowPointsCheckBox(), gbc);

            /*gbc.gridx = 10;
            gbc.gridy = 20;
            checkBoxPanel.add(getShowAveragingLinesCheckBox(), gbc);*/
            
            gbc.gridx = 10;
            gbc.gridy = 30;
            checkBoxPanel.add(getShowSNPLinesComboBox(), gbc);
            //displayPanel.setBorder(BorderFactory.createTitledBorder("Averaging Filter Width"));
        }
        return checkBoxPanel;
    }

    
    protected JComponent getLineButtonPanel() {
        if (lineButtonPanel == null) {
            lineButtonPanel = new JPanel();
            lineButtonPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 10;
            gbc.gridy = 10;
            lineButtonPanel.add(getAddLineButton(), gbc);

            gbc.gridx = 10;
            gbc.gridy = 20;
            lineButtonPanel.add(getEditLinesButton(), gbc);
        }
        return lineButtonPanel;
    }
    
    protected JComponent getImageOptionsPanel() {
        if (imageOptionsPanel == null) {
            imageOptionsPanel = new JPanel();
            imageOptionsPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 10;
            gbc.gridy = 10;
            imageOptionsPanel.add(getAdjustImageButton(), gbc);

            gbc.gridx = 10;
            gbc.gridy = 20;
            imageOptionsPanel.add(getPrintReadyCheckBox(), gbc);
        }
        return imageOptionsPanel;
    }


    protected JSlider getAveragingSlider() {
        if (averagingSlider == null) {
            averagingSlider = new JSlider(0, 30000);
            averagingSlider.setMajorTickSpacing(10000);
            averagingSlider.setMinorTickSpacing(2500);
            averagingSlider.setPaintTicks(true);
            // averagingSlider.setPaintLabels(true);
            averagingSlider.setValue(Singleton.getState().getAveragingWindowWidth());
            averagingSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent ce) {
                    if (!averagingSlider.getValueIsAdjusting()) {
                        int val = averagingSlider.getValue();
                        averagingTextField.setText(val + "");
                        System.out.println("Value set to " + val + " out of " + averagingSlider.getMaximum());
                        Singleton.getState().setAveragingWindowWidth(val);
                    }
                }
            });
        }
        return averagingSlider;
    }

    /**
     * creaetes averagingTextField that displays the number of the slider
     * or if a number is typed, it will register it
     *
     * @return
     */
    protected JTextField getAveragingTextField() {
        if (averagingTextField == null) {
            averagingTextField = new JTextField(8);
            averagingTextField.setHorizontalAlignment(JTextField.CENTER);
            averagingTextField.setToolTipText("Sets the width of averaging window; 0 = no line");
            averagingTextField.setText(Singleton.getState().getAveragingWindowWidth() + "");
            averagingTextField.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent ke) {
                    int keyCode = ke.getKeyCode();
                    if (keyCode == KeyEvent.VK_ENTER) {
                        try {
                            int value = Integer.parseInt(averagingTextField.getText());
                            Singleton.getState().setAveragingWindowWidth(value);
                            if (value < 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showMessageDialog(
                                    (JFrame) SwingUtilities.getWindowAncestor(ControlsPanel.this),
                                    "Must be a positive integer: " + averagingTextField.getText(),
                                    "Invalid integer error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
        return averagingTextField;
    }

    private JCheckBox getShowPointsCheckBox() {
        if (showPointsCheckBox == null) {
            showPointsCheckBox = new JCheckBox("Show SNP p-value points");
            showPointsCheckBox.setSelected(true);
            showPointsCheckBox.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    Singleton.getState().setShowPoints(showPointsCheckBox.isSelected());
                }
            });
        }
        return showPointsCheckBox;
    }
    
    private JComboBox getShowSNPLinesComboBox() {
        if(showSNPLinesComboBox == null) {
            showSNPLinesComboBox = new JComboBox(SNP_LINES_CHOICES);
            showSNPLinesComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    SnpLineChoice[] choices = {SnpLineChoice.NONE, SnpLineChoice.AVERAGE, SnpLineChoice.CONNECTING};
                    Singleton.getState().setSNPLineChoice(choices[showSNPLinesComboBox.getSelectedIndex()]);;
                }
            });
        }
        return showSNPLinesComboBox;
    }

    private JCheckBox getShowAveragingLinesCheckBox() {
        if (showAveragingLinesCheckBox == null) {
            showAveragingLinesCheckBox = new JCheckBox("Show average lines");
            showAveragingLinesCheckBox.setSelected(false);
            showAveragingLinesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    Singleton.getState().setShowAveragingLines(showAveragingLinesCheckBox.isSelected());
                }
            });
        }
        return showAveragingLinesCheckBox;
    }

    protected AbstractButton getAdjustImageButton() {
        if (adjustImageButton == null) {
            adjustImageButton = new JButton("Adjust Img");
            adjustImageButton.setToolTipText("Cleans up image in case stretching window causes distortion");
            adjustImageButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    ControlsPanel.this.plotPanel.getManhattanPlot().adjustImage();
                    ControlsPanel.this.plotPanel.revalidate();
                    ControlsPanel.this.plotPanel.getYAxisRegion().repaint();
                }
            });
        }
        return adjustImageButton;
    }

    protected AbstractButton getPrintReadyCheckBox() {
        if (printReadyCheckBox == null) {
            printReadyCheckBox = new JCheckBox("Print Ready");
            printReadyCheckBox.setSelected(false);
            printReadyCheckBox.setToolTipText("Changes the dot size for printing");
            printReadyCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    Singleton.getState().setDisplayPrintReady(printReadyCheckBox.isSelected());
                }
            });
        }
        return printReadyCheckBox;
    }

    protected AbstractButton getAddLineButton() {
        if (addLineButton == null) {
            addLineButton = new JButton("Add Line");
            addLineButton.setToolTipText("Allows addition of a horizontal line to the view[s]");
            addLineButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    LineCreationPane lineCreationPane = new LineCreationPane();

                    java.net.URL imgURL = this.getClass().getResource("/images/guava_48.jpg");
                    //Image icon = new ImageIcon(imgURL).getImage();

                    int optionPaneReturn = JOptionPane.showConfirmDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(ControlsPanel.this),
                            lineCreationPane, "Create new line",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                            new ImageIcon(imgURL));
                    if (optionPaneReturn == JOptionPane.OK_OPTION) {
                        try {
                            Float yValue = Float.parseFloat(lineCreationPane.getLogPValue());
                            HLine hline = new HLine(
                                    lineCreationPane.getLineName(),
                                    yValue,
                                    lineCreationPane.getLineColor(),
                                    lineCreationPane.getLineStyle(),
                                    lineCreationPane.getLineScopeIndex());
                            Singleton.getState().addHorizontalLine(hline);
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showMessageDialog(
                                    (JFrame) SwingUtilities.getWindowAncestor(ControlsPanel.this),
                                    "Y level should be numeric: " + lineCreationPane.getLogPValue(),
                                    "Invalid y-level",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }
        return addLineButton;
    }

    protected AbstractButton getEditLinesButton() {
        if (editLinesButton == null) {
            editLinesButton = new JButton("Edit Lines");
            editLinesButton.setToolTipText("Allows for the modification or removal of horizontal lines in the plot");
            editLinesButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    LineModificationPane lineModificationPane = new LineModificationPane(ControlsPanel.this);

                    java.net.URL imgURL = this.getClass().getResource("/images/guava_48.jpg");
                    //Image icon = new ImageIcon(imgURL).getImage();

                    /*int optionPaneReturn = JOptionPane.showConfirmDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(ControlsPanel.this),
                            lineModificationPane, "Edit lines",
                            JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE,
                            new ImageIcon(imgURL));*/
                    JOptionPane.showMessageDialog(
                            ControlsPanel.this.getTopLevelAncestor(),
                            lineModificationPane, "Edit lines",
                            JOptionPane.PLAIN_MESSAGE,
                            new ImageIcon(imgURL));
                            
                }
            });
        }
        return editLinesButton;
    }

    /**
     * Constructs the legend that can be saved or captured
     * @return 
     */
    protected AbstractButton getBuildLegendButton() {
        if (buildLegendButton == null) {
            buildLegendButton = new JButton("Build Legend");
            buildLegendButton.setToolTipText("Constructs the legend that can be saved or copied to the clipboard");
            buildLegendButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    LegendPanel legendPanel = new LegendPanel();

                    /*java.net.URL imgURL = this.getClass().getResource("/images/guava_48.jpg");
                    int optionPaneReturn = JOptionPane.showConfirmDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(ControlsPanel.this),
                    legendPanel, "Build Legend",
                    JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, 
                    new ImageIcon(imgURL));
                    }*/

                    JFrame frame = new JFrame("Legend Setup");
                    frame.setContentPane(legendPanel);
                    try {
                        System.out.println("Class path" + System.getProperty("java.class.path"));
                        java.net.URL imgURL = this.getClass().getResource("/images/guava_16.jpg");
                        frame.setIconImage(new ImageIcon(imgURL).getImage());
                    } catch (NullPointerException npe) {
                        System.out.println("Failed to load in the icon.");
                    }

                    frame.pack();
                    frame.setVisible(true);
                }
            });

        }
        return buildLegendButton;
    }
    
    public class StateController implements StateListener {

        @Override
        public void currentChanged(ChangeEvent ce) {        }

        @Override
        public void mainPlotChanged(ChangeEvent ce) { 
            String selectedGeneSnp = Singleton.getState().getMainView().getDataSet().getGeneRange().getName();
            if(selectedGeneSnp.startsWith("rs")) {
                snpSearchRequest = false;
                geneSearchField.setText(selectedGeneSnp);
                geneSearchButton.doClick();
                snpSearchRequest = true;
            }
        }

        @Override
        public void thumbnailsChanged(ChangeEvent ce) {
        }

        @Override
        public void currentAnnotationChanged(ChangeEvent ce) {
        }

        @Override
        public void selectedAnnotationChanged(ChangeEvent ce) {
        }

        @Override
        public void averagingWindowChanged(ChangeEvent ce) {
        }

        @Override
        public void legendSelectedRowChanged(ChangeEvent ce) {
        }

        @Override
        public void heatmapChanged(ChangeEvent ce) {
        }

    }

}