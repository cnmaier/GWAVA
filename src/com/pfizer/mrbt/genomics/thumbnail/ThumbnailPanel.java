/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfizer.mrbt.genomics.thumbnail;

import com.pfizer.mrbt.genomics.Singleton;
import com.pfizer.mrbt.genomics.state.AxisChangeEvent;
import com.pfizer.mrbt.genomics.state.StateListener;
import com.pfizer.mrbt.genomics.state.View;
import com.pfizer.mrbt.genomics.state.ViewData;
import com.pfizer.mrbt.genomics.state.ViewListener;
import com.pfizer.mrbt.genomics.userpref.UserPrefListener;
import com.pfizer.mrbt.axis.AxisScale;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author henstockpv
 */
public class ThumbnailPanel extends JComponent {
    public final static int DEFAULT_ROW_HEIGHT = 80;
    private ArrayList<View> views = new ArrayList<View>();
    private AbstractTableModel thumbnailTableModel;
    private JTable thumbnailTable;
    private JComponent thumbnailPanel;
    private JTextField numColumnsField;
    private AbstractButton applyButton;
    private int rowHeight = DEFAULT_ROW_HEIGHT;
    private JSlider heightSlider;
    private JSlider dotSizeSlider;
    private JComboBox bandComboBox;
    private static Object[] BAND_LEVEL_STRINGS = new String[] {"None", "1","2","4","5","8"};
    private static int[] BAND_LEVELS = new int[] {0, 1, 2, 4, 5, 8};
    
    public ThumbnailPanel() {
        super();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 10;
        gbc.gridy = 10;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        add(new JScrollPane(getThumbnailTable()), gbc);
        
        gbc.gridx = 10;
        gbc.gridy = 20;
        gbc.weighty = 0.0;
        add(getThumbnailPanel(), gbc);
        
        StateController stateController = new StateController();
        Singleton.getState().addListener(stateController);
        
        ViewController viewController = new ViewController();
        Singleton.getState().getMainView().addListener(viewController);
        
        UserPrefController userPrefController = new UserPrefController();
        Singleton.getUserPreferences().addListener(userPrefController);
    }
    
    protected JTable getThumbnailTable() {
        if(thumbnailTable==null) {
            thumbnailTableModel = new ThumbnailTableModel();
            thumbnailTable = new JTable(thumbnailTableModel);
            thumbnailTable.setCellSelectionEnabled(true);
            for(int coli = 0; coli < thumbnailTable.getColumnCount(); coli++) {
                thumbnailTable.getColumnModel().getColumn(coli).setCellRenderer(new ThumbnailRenderer());
            }
            //for(int rowi = 0; rowi < thumbnailTable.getRowCount(); rowi++) {
            thumbnailTable.setRowHeight(rowHeight);
            thumbnailTable.addMouseListener(new MouseAdapter() {
                @Override
               public void mouseClicked(MouseEvent me) {
                   int row = thumbnailTable.rowAtPoint(me.getPoint());
                   int col = thumbnailTable.columnAtPoint(me.getPoint());
                   System.out.println("Clicked on (" + row + "\t" + col + ")");
                   Singleton.getState().setMainView( (ViewData) thumbnailTable.getValueAt(row, col));
               } 
            });
        }
        return thumbnailTable;
    }
    
    protected JComponent getThumbnailPanel() {
        if(thumbnailPanel == null) {
            thumbnailPanel = new JPanel();
            thumbnailPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 10;
            gbc.gridy = 10;
            gbc.weightx = 0.0;
            JLabel numColsLabel = new JLabel("Number of columns:");
            thumbnailPanel.add(numColsLabel, gbc);
            
            gbc.gridx = 10;
            gbc.gridy = 20;
            gbc.weightx = 0.0;
            numColumnsField = new JTextField(5);
            numColumnsField.setHorizontalAlignment(JTextField.CENTER);
            numColumnsField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    int keyCode = ke.getKeyCode();
                    if (keyCode == KeyEvent.VK_ENTER) {
                        processNumColumnsSettings();                        
                    }
                }
            });

            thumbnailPanel.add(numColumnsField, gbc);

            gbc.gridx = 30;
            gbc.gridy = 10;
            gbc.weightx = 1.0;
            JLabel heightSliderLabel = new JLabel("Row Height");
            thumbnailPanel.add(heightSliderLabel, gbc);
            
            gbc.gridx = 30;
            gbc.gridy = 20;
            thumbnailPanel.add(getHeightSlider(), gbc);
            
            
            gbc.gridx = 40;
            gbc.gridy = 10;
            JLabel dotSizeLabel = new JLabel("Dot Size");
            thumbnailPanel.add(dotSizeLabel, gbc);
            
            gbc.gridx = 40;
            gbc.gridy = 20;
            thumbnailPanel.add(getDotSizeSlider(), gbc);
            
            
            gbc.gridx = 50;
            gbc.gridy = 10;
            gbc.weightx = 0.0;
            JLabel bandLabel = new JLabel("Bands Interval");
            bandLabel.setToolTipText("Band start at 0 and repeat every specified -logP units");
            thumbnailPanel.add(bandLabel, gbc);
            
            gbc.gridx = 50;
            gbc.gridy = 20;
            thumbnailPanel.add(getBandComboBox(), gbc);
            
            gbc.gridx = 100;
            gbc.gridy = 20;
            gbc.weightx = 0.0;
            //thumbnailPanel.add(getApplyButton(), gbc);
        }
        return thumbnailPanel;
    }
    
    protected AbstractButton getApplyButton() {
        if(applyButton == null) {
            applyButton = new JButton("Apply");
            applyButton.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                   processNumColumnsSettings();
               }
            });
        }
        return applyButton;
    }
    
    protected JComboBox getBandComboBox() {
        if(bandComboBox == null) {
            bandComboBox = new JComboBox(BAND_LEVEL_STRINGS);
            bandComboBox.setToolTipText("Band start at 0 and repeat every specified -logP units");
            bandComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    int selectedValue = BAND_LEVELS[bandComboBox.getSelectedIndex()];
                    Singleton.getState().setThumbnailBandLevel(selectedValue);
                }
            });
        } 
        return bandComboBox;
    }
    
    protected void processNumColumnsSettings() {
        System.out.println("Clicked apply button");
        try {
            int numCols = Integer.parseInt(numColumnsField.getText());
            ((ThumbnailTableModel) thumbnailTableModel).setColumnCount(numCols);
            System.out.println("Thumbnailtable cols " + thumbnailTable.getColumnCount());
            for (int coli = 0; coli < thumbnailTable.getColumnCount(); coli++) {
                thumbnailTable.getColumnModel().getColumn(coli).setCellRenderer(new ThumbnailRenderer());
            }
        } catch (NumberFormatException nfe) {
              JOptionPane.showMessageDialog( (JFrame) SwingUtilities.getWindowAncestor(numColumnsField),
                                            "Number of columns must be a positive integer: [" + numColumnsField.getText() + "]",
                                            "Invalid Number",
                                            JOptionPane.ERROR_MESSAGE);            
        }
    }
    
    protected JSlider getHeightSlider() {
        if(heightSlider == null) {
            heightSlider = new JSlider(40, 300, DEFAULT_ROW_HEIGHT);
            heightSlider.addChangeListener(new ChangeListener() {
                @Override
               public void stateChanged(ChangeEvent ce) {
                   thumbnailTable.setRowHeight(heightSlider.getValue());
               } 
            });
        }
        return heightSlider;
    }
    
    protected JSlider getDotSizeSlider() {
        if(dotSizeSlider == null) {
            dotSizeSlider = new JSlider(1, 10, ThumbnailRenderer.DOT_WIDTH);
            dotSizeSlider.addChangeListener(new ChangeListener() {
                @Override
               public void stateChanged(ChangeEvent ce) {
                   Singleton.getState().setThumbnailDotSize(dotSizeSlider.getValue());
               } 
            });
        }
        return dotSizeSlider;
    }
    
    
    public class StateController implements StateListener {
        @Override
        public void mainPlotChanged(ChangeEvent ce) { }
        @Override
        public void currentChanged(ChangeEvent ce) { }
        @Override
        public void thumbnailsChanged(ChangeEvent ce) { 
            //thumbnailTableModelf.fireTableStructureChanged();
            thumbnailTableModel.fireTableDataChanged();
        }
        @Override
        public void currentAnnotationChanged(ChangeEvent ce) { }

        @Override
        public void selectedAnnotationChanged(ChangeEvent ce) {
            thumbnailTableModel.fireTableDataChanged();
        }
        
        @Override
        public void averagingWindowChanged(ChangeEvent ce) {  }
        
        @Override
        public void legendSelectedRowChanged(ChangeEvent ce) { }
        
        @Override
        public void heatmapChanged(ChangeEvent ce) { }

    }
    
    /**
     * Class to respond to the changes in the main axis zoom
     */
    public class ViewController implements ViewListener {
        @Override
        public void zoomChanged(AxisChangeEvent ce) {
            switch (ce.getAxisChanged()) {
                case AxisChangeEvent.XAXIS:
                    // Updates each cell WITH THE SAME GENE as the main view  
                    // by setting the x-axis scale to the main view
                    AxisScale mainXAxis = Singleton.getState().getMainView().getXAxis();
                    String mainGene     = Singleton.getState().getMainView().getDataSet().getGeneRange().getName();
                    int numChanged = 0;
                    for (int rowi = 0; rowi < thumbnailTable.getRowCount(); rowi++) {
                        for (int coli = 0; coli < thumbnailTable.getColumnCount(); coli++) {
                            ViewData viewData = (ViewData) thumbnailTable.getValueAt(rowi, coli);
                            if (viewData != null && viewData.getDataSet().getGeneRange().getName().equals(mainGene)) {
                                viewData.setXAxis(mainXAxis);
                                numChanged++;
                            }
                        }
                    }
                    if(numChanged > 0) {
                        thumbnailTableModel.fireTableDataChanged();
                    }
                    break;
                case AxisChangeEvent.YAXIS:
                    // Updates each cell by setting the y-axis scale to the main view
                    AxisScale mainYAxis = Singleton.getState().getMainView().getYAxis();
                    for (int rowi = 0; rowi < thumbnailTable.getRowCount(); rowi++) {
                        for (int coli = 0; coli < thumbnailTable.getColumnCount(); coli++) {
                            ViewData viewData = (ViewData) thumbnailTable.getValueAt(rowi, coli);
                            if (viewData != null) {
                                viewData.setYAxis(mainYAxis);
                            }
                        }
                    }
                    thumbnailTableModel.fireTableDataChanged();
                    break;
                case AxisChangeEvent.RIGHT_YAXIS:
                    // recombi rate not shown so ignored
                    break;
                default:
            }
        }
    }
    
    public class UserPrefController implements UserPrefListener {
        public void colorChanged(ChangeEvent ce) {
            thumbnailTableModel.fireTableDataChanged();
        }
    }
    
}
