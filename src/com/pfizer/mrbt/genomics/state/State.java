/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfizer.mrbt.genomics.state;

import com.pfizer.mrbt.genomics.Singleton;
import com.pfizer.mrbt.genomics.SnpLineChoice;
import com.pfizer.mrbt.genomics.data.DataPointEntry;
import com.pfizer.mrbt.genomics.data.DataSet;
import com.pfizer.mrbt.genomics.data.GeneAnnotation;
import com.pfizer.mrbt.genomics.data.Model;
import com.pfizer.mrbt.genomics.heatmap.HeatmapParameters;
import com.pfizer.mrbt.genomics.hline.HLine;
import com.pfizer.mrbt.genomics.thumbnail.ThumbnailRenderer;
import com.pfizer.mrbt.genomics.webservices.ModelOption;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author henstockpv
 */
public class State {

    public final static int DEFAULT_THUMBNAIL_TOP_PADDING = 21;
    public final static int DEFAULT_TOP_PADDING = 17;
    public final static int DEFAULT_BOTTOM_PADDING = 0;
    public final static int DEFAULT_LEFT_PADDING = 0;
    //public final static int DEFAULT_RIGHT_PADDING = 5;
    public final static int DEFAULT_RIGHT_PADDING = 0;
    private int leftPadding = DEFAULT_LEFT_PADDING;
    private int rightPadding = DEFAULT_RIGHT_PADDING;
    private int topPadding = DEFAULT_TOP_PADDING;
    private int bottomPadding = DEFAULT_BOTTOM_PADDING;
    private int thumbnailTopPadding = DEFAULT_THUMBNAIL_TOP_PADDING;
    public static int UNSELECTED_ROW = -1;
    
    /*public final static int DEMO_MODE = 101;
    public final static int BIOSERVICES_MODE = 102; 
    public final static int TRANSMART_SERVICES_MODE = 103;
    public final static int TRANSMART_DEV_SERVICES_MODE = 104;
    private int dataMode = TRANSMART_SERVICES_MODE; // source of initial data*/
    
    public final static int FAILED_SEARCH = -5;
    //private final int dataMode = DEMO_MODE; // source of initial data
    
    private double hscale, vscale;
    private DataPointEntry currDataPointEntry = null;
    private ArrayList<ViewData> viewDatas = new ArrayList<ViewData>();
    private ArrayList<ViewData> thumbnailViewData = new ArrayList<ViewData>();
    private ArrayList<ViewData> heatmapViewData = new ArrayList<ViewData>();
    private ArrayList<Color> plotColors;
    private Color DEFAULT_PLOT_COLOR = Color.BLACK;
    private int thumbnailDotSize = ThumbnailRenderer.DOT_WIDTH;
    private boolean showRecombinationRate = true;
    private ArrayList<DataPointEntry> rsIdSearchResults = new ArrayList<DataPointEntry>();
    private ArrayList<StateListener> listeners = new ArrayList<StateListener>();
    private String mainDataSet = "";
    private View mainView = null;
    private GeneAnnotation currentGeneAnnotation = null;
    private SelectedGeneAnnotation selectedGeneAnnotation = null;
    private int averagingWindowWidth = 0;
    private boolean showAveragingLines = false;
    private SnpLineChoice snpLineChoice = SnpLineChoice.NONE;
    private boolean showPoints = true;
    private int thumbnailBandLevel = 0;
    private int legendSelectedRow = UNSELECTED_ROW;
    private HashMap<String, ArrayList<HLine>> use2lineIndex = new HashMap<String, ArrayList<HLine>>();
    private HashMap<Model, String> model2legend = new HashMap<Model, String>();
    private boolean displayPrintReady = false;
    private HeatmapParameters heatmapParameters = new HeatmapParameters();
    
    private HistoryTableModel historyTableModel = new HistoryTableModel();
    
    public State() {
        initializeColors();
    }

    protected void initializeColors() {
        plotColors = new ArrayList<Color>();
        // rgb colors for qualitative 9 possible
        plotColors.add(new Color(55,126,184));
        plotColors.add(new Color(228,26,28));
        plotColors.add(new Color(77,175,74));
        plotColors.add(new Color(152,78,163));
        plotColors.add(new Color(255,127,0));
        plotColors.add(new Color(166,86,40));
        plotColors.add(new Color(247,129,191));
        plotColors.add(new Color(255,255,51));
        plotColors.add(new Color(153,153,153));
        // rgb colors for qualitative 9 possible with pastels
        plotColors.add(new Color(141,211,199));
        plotColors.add(new Color(255,255,179));
        plotColors.add(new Color(190,186,218));
        plotColors.add(new Color(251,128,114));
        plotColors.add(new Color(128,177,211));
        plotColors.add(new Color(253,180,98));
        plotColors.add(new Color(179,222,105));
        plotColors.add(new Color(252,205,229));
        plotColors.add(new Color(217,217,217));
        // rgb colors for qualitative 9 possible with pastels
        plotColors.add(new Color(251,180,174));
        plotColors.add(new Color(179,205,227));
        plotColors.add(new Color(204,235,197));
        plotColors.add(new Color(222,203,228));
        plotColors.add(new Color(254,217,166));
        plotColors.add(new Color(255,255,204));
        plotColors.add(new Color(229,216,189));
        plotColors.add(new Color(253,218,236));
    }

    /**
     * Returns the plotColor for the given index.  If the index is not 
     * [0 #plotcolors.size()-1], then it returns DEFAULT_PLOT_COLOR
     * @param index
     * @return 
     */
    public Color getPlotColor(int index) {
        if (index < 0 || index >= plotColors.size()) {
            return DEFAULT_PLOT_COLOR;
        } else {
            return plotColors.get(index);
        }
    }

    public ViewData getNewViewData(DataSet dataSet) {
        ViewData viewData = new ViewData(dataSet);
        viewDatas.add(viewData);
        return viewData;
    }

    /**
     * Adds the viewData to the list stored here viewDatas
     * @param viewData e
     */
    public void addViewData(ViewData viewData) {
        viewDatas.add(viewData);
    }

    public int getBottomPadding() {
        return bottomPadding;
    }

    public int getLeftPadding() {
        return leftPadding;
    }

    public int getRightPadding() {
        return rightPadding;
    }

    public int getTopPadding() {
        return topPadding;
    }

    public int getThumbnailTopPadding() {
        return thumbnailTopPadding;
    }

    /**
     * Returns the view corresponding to the main manhattan plot.  If it is null,
     * then it creates a dummy dataset and uses that.
     * @return 
     */
    public View getMainView() {
        if(mainView == null) {
            DataSet dataSet = DataSet.createDummyDataset();
            ViewData viewData = new ViewData(dataSet);
            mainView = new View(viewData);
        }
        return mainView;
    }

    /**
     * Sets the view belonging to the main Manhattan plot
     * @param mainView
     */
    public void setMainView(ViewData mainViewData) {
        if (mainView == null) {
            mainView = new View(mainViewData);
        } else {
            mainView.setViewData(mainViewData);
        }
        currentGeneAnnotation = null;
        rsIdSearchResults.clear();
        //selectedGeneAnnotation = null;
        fireMainPlotChanged();
    }

    /**
     * Sets the currDataEntry to the specified filename, studySetModel and
     * index
     *
     * @param filename
     * @param studySetModel
     * @param index
     */
    public void setCurrDataEntry(DataPointEntry dataPointEntry) {
        //System.out.println("Set Current Data Entry : " + dataPointEntry);
        if (currDataPointEntry == null || !currDataPointEntry.equals(dataPointEntry)) {
            currDataPointEntry = dataPointEntry;
            fireCurrChanged();
        }
    }

    /**
     * Sets the currDataEntry to null
     */
    public void clearCurrDataEntry() {
        if (currDataPointEntry != null) {
            currDataPointEntry = null;
            fireCurrChanged();
        }
    }

    public void setShowRecombinationRate(boolean showRecombinationRate) {
        if (this.showRecombinationRate != showRecombinationRate) {
            this.showRecombinationRate = showRecombinationRate;
            System.out.println("changing the value of show recombo rate in state");
            fireMainPlotChanged();
        }
    }

    public boolean getShowRecombinationRate() {
        return this.showRecombinationRate;
    }

    /**
     * Returns the currently selected entry (mouse-over
     * @return 
     */
    public DataPointEntry getCurrenDataEntry() {
        return currDataPointEntry;
    }

    public void addListener(StateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(StateListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public void fireCurrChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.currentChanged(changeEvent);
        }
    }

    public void fireCurrentAnnotationChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.currentAnnotationChanged(changeEvent);
        }
    }

    public void fireSelectedAnnotationChanged() {
        ChangeEvent ce = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.selectedAnnotationChanged(ce);
        }
    }

    public void fireThumbnailsChanged() {
        ChangeEvent ce = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.thumbnailsChanged(ce);
        }
    }

    public void fireHeatmapChanged() {
        ChangeEvent ce = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.heatmapChanged(ce);
        }
    }

    /**
     * Fires a main-plot-changed message to all the listeners indicating that
     * the main plot has a different dataset or views.
     */
    public void fireMainPlotChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.mainPlotChanged(changeEvent);
        }
    }

    /**
     * Fires a main-plot-changed message to all the listeners indicating that
     * the main plot has a different dataset or views.
     */
    public void fireAveragingWindowChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.averagingWindowChanged(changeEvent);
        }
    }
    
    public void fireLegendSelectedRowChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (StateListener listener : listeners) {
            listener.legendSelectedRowChanged(changeEvent);
        }
    }

    /**
     * Adds viewData to the end of the list thumbnailViewData
     * @param viewData 
     */
    public void addThumbnailViewData(ViewData viewData) {
        thumbnailViewData.add(viewData);
    }

    /**
     * Adds viewData to the end of the list heatmapViewData
     * @param viewData 
     */
    public void addHeatmapViewData(ViewData viewData) {
        heatmapViewData.add(viewData);
    }
    
    /**
     * Returns an arraylist of ViewData corresponding to all the selected
     * data for making the heatmap
     * @return 
     */
    public ArrayList<ViewData> getHeatmapViewData() {
        return heatmapViewData;
    }

    public HeatmapParameters getHeatmapParameters() {
        return heatmapParameters;
    }

    public void setHeatmapParameters(HeatmapParameters heatmapParameters) {
        this.heatmapParameters = heatmapParameters;
    }
    
    /**
     * Removes viewData to the end of the list thumbnailViewData
     * @param viewData 
     */
    public void removeThumbnailViewData(ViewData viewData) {
        thumbnailViewData.remove(viewData);
    }

    public void clearThumbnailViewData() {
        thumbnailViewData.clear();
    }

    public void clearHeatmapData() {
        heatmapViewData.clear();
    }

    /**
     * If the index is in range, it returns the corresponding thumbnailViewData
     * entry else it returns null
     * @param index
     * @return 
     */
    public ViewData getThumbnailViewData(int index) {
        if (index >= 0 && index < thumbnailViewData.size()) {
            return thumbnailViewData.get(index);
        } else {
            return null;
        }
    }

    public int getNumThumbnails() {
        return thumbnailViewData.size();
    }

    public int getThumbnailDotSize() {
        return thumbnailDotSize;
    }

    public void setThumbnailDotSize(int thumbnailDotSize) {
        if (this.thumbnailDotSize != thumbnailDotSize) {
            this.thumbnailDotSize = thumbnailDotSize;
            fireThumbnailsChanged();
        }
    }

    public GeneAnnotation getCurrentGeneAnnotation() {
        return currentGeneAnnotation;
    }

    public void setCurrentGeneAnnotation(GeneAnnotation currentGeneAnnotation) {
        if (this.currentGeneAnnotation != currentGeneAnnotation) {
            this.currentGeneAnnotation = currentGeneAnnotation;
            fireCurrentAnnotationChanged();
        }
    }

    public SelectedGeneAnnotation getSelectedGeneAnnotation() {
        return selectedGeneAnnotation;
    }

    public void setSelectedGeneAnnotation(SelectedGeneAnnotation selectedGeneAnnotation) {
        if (this.selectedGeneAnnotation != selectedGeneAnnotation) {
            this.selectedGeneAnnotation = selectedGeneAnnotation;
            fireSelectedAnnotationChanged();
        }
    }

    /**
     * Sets the list of search values found with an RsId text search
     * @param newRsIdSearchResults 
     */
    public void setRsIdSearchResults(ArrayList<DataPointEntry> newRsIdSearchResults) {
        this.rsIdSearchResults = newRsIdSearchResults;
        fireCurrChanged();
    }

    public ArrayList<DataPointEntry> getRsIdSearchResults() {
        return rsIdSearchResults;
    }

    public void clearRsIdSearchResults() {
        if (rsIdSearchResults.size() > 0) {
            rsIdSearchResults.clear();
            fireCurrChanged();
        }
    }

    public int getThumbnailBandLevel() {
        return thumbnailBandLevel;
    }

    public void setThumbnailBandLevel(int thumbnailBandLevel) {
        if (this.thumbnailBandLevel != thumbnailBandLevel) {
            this.thumbnailBandLevel = thumbnailBandLevel;
            fireThumbnailsChanged();
        }
    }

    public int getAveragingWindowWidth() {
        return averagingWindowWidth;
    }

    public void setAveragingWindowWidth(int averagingWindowWidth) {
        if (this.averagingWindowWidth != averagingWindowWidth) {
            this.averagingWindowWidth = averagingWindowWidth;
            fireAveragingWindowChanged();
        }
    }

    public boolean getShowAveragingLines() {
        return showAveragingLines;
    }

    public void setShowAveragingLines(boolean showAveragingLines) {
        if (this.showAveragingLines != showAveragingLines) {
            this.showAveragingLines = showAveragingLines;
            fireAveragingWindowChanged();
        }
    }
    
    public void setSNPLineChoice(SnpLineChoice snpLineChoice) {
        if(this.snpLineChoice != snpLineChoice) {
            this.snpLineChoice = snpLineChoice;
            fireAveragingWindowChanged();
        }
    }
    
    /**
     * Returns snpLineChoice indicating whether the lines connecting SNPS are
     * none, averaged, or connecting lines
     * @return 
     */
    public SnpLineChoice getSnpLineChoice() {
        return this.snpLineChoice;
    }

    public boolean getShowPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        if (this.showPoints != showPoints) {
            this.showPoints = showPoints;
            fireMainPlotChanged();
        }
    }
    
    /**
     * Adds an hline to the use2lineIndex hashmap that enables a plot to query
     * based on the gene and model whether it should be included.  For the
     * models, it includes the modelID as a string.  For the this plot, it
     * adds it to the view
     * @param hLine
     * @param gene
     * @param model 
     */
    public void addHorizontalLine(HLine hLine) {
        String geneName = mainView.getDataSet().getGeneRange().getName();
        ArrayList<Model> models = mainView.getModels();
        int lineScope = hLine.getLineScope();
        switch(lineScope) {
            case HLine.SCOPE_THIS_PLOT:
                //mainView.getViewData().addHLine(hLine);
                ArrayList<HLine> hlineList = null;
                String modelIdsStr = modelIds2String(models);
                String geneModelKey = getGeneModelKey(geneName, modelIdsStr);
                if(use2lineIndex.get(geneModelKey) != null) {
                    hlineList = use2lineIndex.get(geneModelKey);
                } else {
                    hlineList = new ArrayList<HLine>();
                }
                hlineList.add(hLine);
                use2lineIndex.put(geneModelKey, hlineList); 
                break;
            case HLine.SCOPE_SAME_GENE:
                ArrayList<HLine> geneHLines = use2lineIndex.get(geneName);
                if(geneHLines == null) {
                    geneHLines = new ArrayList<HLine>();
                }
                geneHLines.add(hLine);
                use2lineIndex.put(geneName, geneHLines);
                break;
            case HLine.SCOPE_SAME_MODEL:
                String modelKey = models.get(0).getId() + "";
                ArrayList<HLine> scopeHLines = use2lineIndex.get(modelKey);
                if(scopeHLines == null) {
                    scopeHLines = new ArrayList<HLine>();
                }
                scopeHLines.add(hLine);
                use2lineIndex.put(modelKey, scopeHLines);
                break;
            case HLine.SCOPE_GLOBAL:
                ArrayList<HLine> hLineList = use2lineIndex.get("global");
                if(hLineList == null) {
                    hLineList = new ArrayList<HLine>();
                }
                hLineList.add(hLine);
                use2lineIndex.put("global", hLineList);
                break;
            default:
                System.out.println("unknown scope entered in state.addLine");
        }
        fireMainPlotChanged();
    }
    
    /**
     * Converts models to a list of + delimited modelIds
     * @param models
     * @return 
     */
    private String modelIds2String(ArrayList<Model> models) {
        StringBuilder modelSb = new StringBuilder();
        int modelIndex = 0;
        for (Model model : models) {
            if (modelIndex > 0) {
                modelSb.append("+");
            }
            modelSb.append(model.getId());
        }
        return modelSb.toString();
    }
    
    public void replaceHorizontalLine(int index, HLine line) {
        ArrayList<HLine> mainViewLines = mainView.getViewData().getHLines();
        int relativeIndex = 0;
        if(mainViewLines == null) {
        } else if(index < mainViewLines.size()) {
            mainViewLines.set(index, line);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex = index - mainViewLines.size();
        }
        
        ArrayList<HLine> globalLines = use2lineIndex.get("global");
        if(globalLines == null) {
        } else if(relativeIndex < globalLines.size()) {
            globalLines.set(relativeIndex, line);
            use2lineIndex.put("global", globalLines);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex -= globalLines.size();
        }
        
        String currGene = mainView.getDataSet().getGeneRange().getName();
        ArrayList<HLine> geneLines = use2lineIndex.get(currGene);
        if(geneLines == null) {
        } else if(relativeIndex < geneLines.size()) {
            geneLines.set(relativeIndex, line);
            use2lineIndex.put(currGene, geneLines);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex -= geneLines.size();
        }
        
        View view = Singleton.getState().getMainView();
        ArrayList<Model> models = view.getViewData().getModels();
        for(Model model : models) {
            ArrayList<HLine> modelLines = use2lineIndex.get(model.getId() + "");
            if(modelLines == null) { 
            } else if(relativeIndex < modelLines.size()) {
                modelLines.set(relativeIndex, line);
                use2lineIndex.put(model.getId() + "", modelLines);
                      fireMainPlotChanged();
                      return;
            } else {
                relativeIndex -= modelLines.size();
            }
        }

        // replaces the gene::model1+model2...Modeln) --> ArrayList<HLine>
        String modelIdsStr = modelIds2String(models);
        ArrayList<HLine> foundLines = use2lineIndex.get(currGene + "::" + modelIdsStr);
        if(relativeIndex < foundLines.size()) {
            foundLines.set(relativeIndex, line);
            fireMainPlotChanged();
        }

    
    }
    
    public void removeHorizontalLine(int index) {
        ArrayList<HLine> mainViewLines = mainView.getViewData().getHLines();
        int relativeIndex = 0;
        if(mainViewLines == null) {
        } else if(index < mainViewLines.size()) {
            mainViewLines.remove(index);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex = index - mainViewLines.size();
        }
        
        ArrayList<HLine> globalLines = use2lineIndex.get("global");
        if(globalLines == null) {
        } else if(relativeIndex < globalLines.size()) {
            globalLines.remove(relativeIndex);
            use2lineIndex.put("global", globalLines);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex -= globalLines.size();
        }
        
        String currGene = mainView.getDataSet().getGeneRange().getName();
        ArrayList<HLine> geneLines = use2lineIndex.get(currGene);
        if(geneLines == null) {
        } else if(relativeIndex < geneLines.size()) {
            geneLines.remove(relativeIndex);
            use2lineIndex.put(currGene, geneLines);
            fireMainPlotChanged();
            return;
        } else {
            relativeIndex -= geneLines.size();
        }
        
        View view = Singleton.getState().getMainView();
        ArrayList<Model> models = view.getViewData().getModels();
        for(Model model : models) {
            ArrayList<HLine> modelLines = use2lineIndex.get(model.getId() + "");
            if(modelLines == null) { 
            } else if(relativeIndex < modelLines.size()) {
                modelLines.remove(relativeIndex);
                use2lineIndex.put(model.getId() + "", modelLines);
            } else {
                relativeIndex -= modelLines.size();
            }
        }
        
        // remove the gene::model1+model2...Modeln) --> ArrayList<HLine>
        String modelIdsStr = modelIds2String(models);
        ArrayList<HLine> foundLines = use2lineIndex.get(currGene + "::" + modelIdsStr);
        if(relativeIndex < foundLines.size()) {
            foundLines.remove(relativeIndex);
            fireMainPlotChanged();
        }
    }
    
    /**
     * Returns a unique list of the HLines corresponding to the plot specified by the
     * gene and model
     * @param gene
     * @param model
     * @return 
     */
    public ArrayList<HLine> getLines(String gene, ArrayList<Model> models) {
        ArrayList<HLine> hLines = new ArrayList<HLine>();
        for(HLine hLine : mainView.getViewData().getHLines()) {
            hLines.add(hLine);
        }
        ArrayList<HLine> globalLines = use2lineIndex.get("global");
        if(globalLines != null) {
            for(HLine hLine : globalLines) {
                hLines.add(hLine);
            }
        }
        
        // find lines for current gene
        String currGene = mainView.getDataSet().getGeneRange().getName();
        ArrayList<HLine> geneLines = use2lineIndex.get(currGene);
        if(geneLines != null) {
            for(HLine hLine : geneLines) {
                hLines.add(hLine);
            }
        }

        // find lines for any of models shown
        for(Model model : models) {
            ArrayList<HLine> modelLines = use2lineIndex.get(model.getId() + "");
            if(modelLines != null) {
                for(HLine hLine : modelLines) {
                    hLines.add(hLine);
                }
            }
        }
        
        // find if exact match of gene::model+model+...+model
        String modelIdStr = modelIds2String(models);
        
        ArrayList<HLine> foundHLines = use2lineIndex.get(gene + "::" + modelIdStr);
        if(foundHLines != null) {
            for(HLine foundHLine : foundHLines) {
                hLines.add(foundHLine);
            }
        }
        
        return hLines;
    }
    
    /**
     * Fetches the lines for the current view whether in the range of the
     * display (y-axis) or not.
     * @return 
     */
    public ArrayList<HLine> getLines() {
        View view = Singleton.getState().getMainView();
        String gene = view.getDataSet().getGeneRange().getName();
        ArrayList<Model> models = view.getViewData().getModels();
        ArrayList<HLine> lines = getLines(gene, models);
        return lines;
    }
    
    private String getGeneModelKey(String gene, String model) {
        return gene + "::" + model;
    }
    
    /**
     * Assigns a key-value in th emodel2legend where legend is the displayed
     * value 
     * @param model
     * @param legend 
     */
    public void  assignModel2Legend(Model model, String legend) {
        model2legend.put(model, legend);
    }
    
    /**
     * Returns the legend display from the mdoel.  If it is not assigned, then
     * it returns the model
     * @param model
     * @return 
     */
    public String getLegendFromModel(Model model) {
        if(model2legend.get(model) == null) {
            return model.toString();
        } else {
            return model2legend.get(model);
        }
    }
    
    /**
     * Sets the legendSelectedRow to rowIndex.  If none are selected, it should
     * set it to UNSELECTED_ROW
     * @param rowIndex 
     */
    public void setLegendSelectedRow(int rowIndex) {
        if(legendSelectedRow != rowIndex) {
            legendSelectedRow = rowIndex;
            fireLegendSelectedRowChanged();
        }
    }
    
    /**
     * Returns UNSELECTED_ROW if none are selected else returns the index of
     * the selected row of the legend modification pane
     * @return 
     */
    public int getLegendSelectedRow() {
        return legendSelectedRow;
    }
    
    /**
     * Returns whether the program is running demo mode or bioservices mode
     * @return 
     */
    /*public int getDataMode() {
        return dataMode;
    }*/
    
    /*******************************************************************
     * Sets the dataMode to be dataModel where dataMode shouldbe one of
     * BIOSERVICES_MODE or TRANSMART_SERVICES_MODE.  This must be done
     * at the beginning else we aren't responsible for the behavior.  Actually
     * it just has to be done before the calls are made for the query window
     * initialization, etc.
     * @param dataMode 
     */
    /*public void setDataMode(int dataMode) {
        this.dataMode = dataMode;
    }*/
    
    public HistoryTableModel getHistoryTableModel() {
        return historyTableModel;
    }
    
    /**
     * Updates the searchHistory table with the following information of the
     * search.  Fires an update to the historyTableModel
     */
    public void retrievalInitialized(String gene, List<ModelOption> modelOptions, int range, int queryId) {
        History historyElement = new History(gene, modelOptions, range, SearchStatus.WAITING, queryId);
        historyTableModel.addHistory(historyElement);
        //System.out.println("Adding search initialized for gene " + gene + " query# " + queryId);
    }
    
    /**
     * Updates the searchHistory table with the following information of the
     * search.  Fires an update to the historyTableModel
     */
    public void retrievalStarted(String gene, int numModels, int range, int queryId) {
        //int index = historyTableModel.findFirstInTable(gene, SearchStatus.WAITING);
        int index = historyTableModel.findRowByQueryId(queryId);
        if(index >= 0) { // could have cleared history before starts retrieval
            History oneHistory = historyTableModel.getHistory(index);
            oneHistory.update(SearchStatus.WORKING, 0);
            historyTableModel.setHistory(index, oneHistory);
        }
        //System.out.println("Adding search started for gene " + gene + " queryId " + queryId);
    }
    
    /**
     * Updates the historyTableModel.  If it failed, then numSnp = FAILED_SEARCH
     * @param gene
     * @param numSnp 
     * @param searchStatus is the modified result of the search
     * @param queryId
     */
    public void retrievalCompleted(String gene, int numSnp, SearchStatus searchStatus, int queryId) {
        //int index = historyTableModel.findFirstInTable(gene, SearchStatus.WORKING);
        int index = historyTableModel.findRowByQueryId(queryId);
        if(index >= 0) { // could have cleared the history before data comes back
            History oneHistory = historyTableModel.getHistory(index);
            oneHistory.update( searchStatus, numSnp );
            historyTableModel.setHistory(index, oneHistory);
        }
    }

    /**
     * Returns true if the display should be print-ready
     */
    public boolean isDisplayPrintReady() {
        return displayPrintReady;
    }

    /**
     * Sets the displayPrintReady to true if the plots should be print-ready
     * @param displayPrintReady 
     */
    public void setDisplayPrintReady(boolean displayPrintReady) {
        if(this.displayPrintReady != displayPrintReady) {
            this.displayPrintReady = displayPrintReady;
            fireMainPlotChanged();
        }
    }
    
    /**
     * Returns friendly string describing the current mode (dev/stage/demo, etc.)
     * @return 
     */
    /*public String getDataServicesModeName() {
        switch(dataMode) {
            case DEMO_MODE: 
                return "DEMO_MODE = " + DEMO_MODE;
            case BIOSERVICES_MODE: 
                return "BIOSERVICES_MODE = " + BIOSERVICES_MODE;
            case TRANSMART_SERVICES_MODE: 
                return "TRANSMART_SERVICES_MODE = " + TRANSMART_SERVICES_MODE;
            case TRANSMART_DEV_SERVICES_MODE: 
                return "TRANSMART_DEV_SERVICES_MODE = " + TRANSMART_DEV_SERVICES_MODE;
            default: 
                return "Unknown model " + dataMode;
        }
    } */   
}
