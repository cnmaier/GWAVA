/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfizer.mrbt.genomics.data;

import com.pfizer.mrbt.genomics.Singleton;
import com.pfizer.mrbt.genomics.TransmartClient.TransmartDataLoaderWithThreads;
import com.pfizer.mrbt.genomics.TransmartClient.TransmartQueryParameterFetch;
import com.pfizer.mrbt.genomics.bioservices.DbSnpSourceOption;
import com.pfizer.mrbt.genomics.bioservices.GeneSourceOption;
import com.pfizer.mrbt.genomics.bioservices.ModelOption;
import com.pfizer.mrbt.genomics.state.State;
import com.pfizer.tnb.api.server.util.QueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import javax.swing.event.ChangeEvent;


/**
 *
 * @author henstockpv
 */
public class DataModel {
    public final static Integer X = 24;
    public final static Integer Y = 25;
    private HashMap<String, DataSet> name2dataset = new HashMap<String,DataSet>();
    //public static String FILENAME = "data\\1336590111384gene_snp_gwa_results.txt";
    public static String FILENAME = "1336590392598gene_snp_gwa_results.txt";
    //public static String ANNOTATIONS = "c:\\Data\\Manhattan\\ManhattanPlot\\ManhattanViz\\data\\annotatedGenes";
    //public static String ANNOTATIONS = "W:\\Work\\Manhattan\\ManhattanViz\\data\\annotatedGenes";
    //public static String RECOMB_RATE_PATH = "c:\\Data\\Manhattan\\ManhattanPlot\\ManhattanViz\\data\\recombRate";
    //public static String RECOMB_RATE_PATH = "W:\\Work\\Manhattan\\ManhattanViz\\data\\recombRate";
    //private GeneAnnotations geneAnnotations;
    //private RecombinationRates recombRates;
    private ArrayList<DataListener> listeners = new ArrayList<DataListener>();

    
    public DataModel() {
        String fileSep = System.getProperty("file.separator");
        String filename = System.getProperty("user.home") + fileSep + "My Documents" + fileSep + "gwava_data" + fileSep + FILENAME;
        System.out.println("Loading gwas data [" + filename + "]");
        if(Singleton.getState().getDataMode() == State.DEMO_MODE) {
        //if(true || Singleton.getState().getDataMode() == State.DEMO_MODE) {
            loadDataSets(filename);
        } /*else if(Singleton.getState().getDataMode() == State.TRANSMART_SERVICES_MODE) {
            
        }*/
        System.out.println("Loaded DataSets:");
        for(String dataSetName : name2dataset.keySet()) {
            DataSet dataSet = name2dataset.get(dataSetName);
            System.out.println("\t" + dataSetName + "\t" + dataSet.getChromosome());
            /*for(Model model : dataSet.getModels()) {
                System.out.println("\t\tModel: " + model.toString());
            }*/
        }
    }
    
    /**
     * Initializes the data with the command-line arguments passed in from the
     * java webstart or command-line.  The argv contain the following:
     * Argv[0]: A comma-separated list of selected model IDs
     * Argv[1]: GENE,RADIUS pairs. If more than one gene is selected, these will be separated by semicolons
     * Argv[2]: The gene annotation source – we currently only support one, so this will always be GRCh37.
     * Argv[3]: The SNP annotation source – arrives as 19 (=HG19) or 18 (=HG18), matching the IDs given by the GetSnpSources webservice.
     * Argv[4]: The selected p-value cutoff – you can ignore this if the application doesn’t support it.
     */
    public void initializeData(String[] argv) {
        ArrayList<StartupInfo> startupInfos = StartupInfo.parse(argv);
        int stateDataMode = Singleton.getState().getDataMode();
        if(startupInfos.size() > 0 && (stateDataMode == State.TRANSMART_DEV_SERVICES_MODE || 
                                       stateDataMode == State.TRANSMART_SERVICES_MODE)) {   
            List<String> geneNames = new ArrayList<String>();
            List<ModelOption> modelOptions = new ArrayList<ModelOption>();
            int geneSourceId    = startupInfos.get(0).getGeneSourceId();
            int dbSnpSourceId   = startupInfos.get(0).getSnpSourceId();
            int radius          = 0;
            int index           = 0;
            
            
            
            // create a fake modelOptions with the modelIndex common to all the startupInfos
            for(Long modelIndex : startupInfos.get(0).getStudySetModelIndexList()) {
               modelOptions.add(new ModelOption("study" + index,"set" + index,"model" + index, modelIndex));
               index++;
            }
            // get dbSnpSource options and choose the option corresponding to dbSnpSourceId
            DbSnpSourceOption dbSnpSourceOption = null;
            TransmartQueryParameterFetch transmartQueryParameterFetch = new TransmartQueryParameterFetch();
            List<DbSnpSourceOption> dbSnpSrcOptions = transmartQueryParameterFetch.getDbSnpSources();
            for(DbSnpSourceOption option : dbSnpSrcOptions) {
                if(option.getId() == dbSnpSourceId) {
                    dbSnpSourceOption = option;
                }
            }
            // get geneSource options and choose the option corresponding to genesourceId
            GeneSourceOption geneSourceOption = null;
            List<GeneSourceOption>  geneSrcOptions  = transmartQueryParameterFetch.getGeneSources();
            for(GeneSourceOption option : geneSrcOptions) {
                if(option.getId() == geneSourceId) {
                    geneSourceOption = option;
                }
            }
            
            for(StartupInfo startupInfo : startupInfos) {
                List<String> genes = new ArrayList<String>();
                genes.add(startupInfo.getGene());
                TransmartDataLoaderWithThreads tdlwt = new TransmartDataLoaderWithThreads(
                    modelOptions, dbSnpSourceOption, geneSourceOption, genes, startupInfo.getRange());
                tdlwt.fetchGeneData();
            }
        }             
    }
    
    /**
     * Returns the dataset associated with name
     * @param name
     * @return 
     */
    public DataSet getDataSet(String name) {
        return name2dataset.get(name);
    }

    /**
     * Returns a collection of all the data sets regardless of the name
     * @return 
     */
    public Collection<DataSet> getDataSets() {
        return name2dataset.values();
    }
    
    /**
     * Removes the data set specified by name from the name2dataset
     * @param name 
     */
    public void removeDataSet(String name) {
        name2dataset.remove(name);
    }
    
    /**
     * Loads in a file that is a [multi-]gene/[multi-]model query result in a
     * file that comes back as the AQG format
     * @param filename 
     */
    public void loadDataSets(String filename) {
        DataLoader loader = new DataLoader();
        loader.loadDataSets(filename);
        HashMap<String, DataSet> loadResults = loader.getLoadResults();
        for(String key : loadResults.keySet()) {
            DataSet value = loadResults.get(key);
            name2dataset.put(key, value);
        }
        fireDataChanged();
    }
    
    public void exportDataSets(String filename) {
        DataExporter exporter = new DataExporter();
        exporter.exportAllData(filename);
    }
    
    /**
     * This records checks that the gene sources have the same geneSourceId as
     * the one passed in.  If not they are removed and the list returned.
     * @param genes
     * @param geneSourceOption 
     */
    /*public ArrayList<String> removeMismatchedGeneSourceData(List<String> genes, GeneSourceOption geneSourceOption) {
        ArrayList<String> wrongSourceGenes = new ArrayList<String>();
        for(String gene : genes) {
            DataSet dataSet = name2dataset.get(gene);
            if(dataSet.getGeneSourceId() != geneSourceOption.getId()) {
                name2dataset.remove(gene);
                wrongSourceGenes.add(gene);
            }
        }
        return wrongSourceGenes;
    }*/
    
    /**
     * This records the gene sources for each of the genes in the list. 
     * @param genes
     * @param geneSourceOption 
     */
    /*public void addGeneSourceTags(List<String> genes, GeneSourceOption geneSourceOption) {
        for(String gene : genes) {
            DataSet dataSet = name2dataset.get(gene);
            dataSet.setGeneSourceId(geneSourceOption.getId());
        }
        
    }*/
    
    /**
     * Parses the chromosomeStr into an integer.  This includes X and Y as 
     * case-insensitive entities.  Other values are returned as -1.
     * @param chromosomeStr
     * @return 
     */
    public static int parseChromosomeStr(String chromosomeStr) {
        if(chromosomeStr.equalsIgnoreCase("X")) {
            return X;
        } else if(chromosomeStr.equalsIgnoreCase("Y")) {
            return Y;
        } else {
            try {
                int chromosomeNumber = Integer.parseInt(chromosomeStr);
                return chromosomeNumber;
            } catch(NumberFormatException nfe) {
                System.out.println("Failed to parse chromosome " + chromosomeStr);
                return -1;
            }
        }
    }
    
    public static String getChromosomeString(int chromosomeNumber) {
        if(chromosomeNumber == X) {
            return "X";
        } else if(chromosomeNumber == Y) {
            return "Y";
        } else {
            return chromosomeNumber + "";
        }
    }
    
    /**
     * Over-writes the dataSet if same name but adds the name->dataSet to the
     * map of datasets
     * @param name
     * @param dataSet 
     */
    public void addDataSet(String name, DataSet dataSet) {
        name2dataset.put(name, dataSet);
        fireDataChanged();
    }
    
    /**
     * Method not used...
     * @param gene
     * @param modelOptions
     * @param queryResults 
     */
    public void addBioservicesData(String gene, List<ModelOption> modelOptions, QueryResult queryResults) {
        fireDataChanged();
    }
    
   
    public void addListener(DataListener listener) {
        if(! listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(DataListener listener) {
        if(listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
    
    public void fireDataChanged() {
        ChangeEvent ce = new ChangeEvent(this);
        for(DataListener listener : listeners) {
            listener.dataChanged(ce);
        }
    }
    
}
