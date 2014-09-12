/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pfizer.mrbt.genomics.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author henstockpv
 */
public class DataLoader {

    private HashMap<String, DataSet> loadResults = new HashMap<String, DataSet>();
    public final static String SNP_START = "SNP start";
    public final static String SNP_FUNC = "SNP func";
    public final static String GENE_COL = "GeneId";
    public final static String GENE_SYMBOL = "Symb";
    public final static String CHR_COL = "Chr";
    public final static String RSID_COL = "rs Id";
    public final static String GENE_START = "Start";
    public final static String GENE_STOP = "Stop";

    public DataLoader() {
    }


    /**
     * Reads a tab-delimited file that came from the gene search into the data
     * model
     *
     * @param filename
     */
   /* public void loadDataSets(String filename) {
        loadResults.clear();
        ArrayList<ArrayList<String>> matrix = loadMatrix(filename);
        int snpCol = findColName(matrix, SNP_START);
        ArrayList<Integer> snpLoc = extractColumnInteger(matrix, snpCol);
        int snpStartCol = findColName(matrix, SNP_START);
        int snpFuncCol = findColName(matrix, SNP_FUNC);
        int rsIdCol = findColName(matrix, RSID_COL);
        int geneIdCol = findColName(matrix, GENE_COL);
        int geneSymbolCol = findColName(matrix, GENE_SYMBOL);
        int chromosomeCol = findColName(matrix, CHR_COL);
        int geneStartCol = findColName(matrix, GENE_START);
        int geneStopCol = findColName(matrix, GENE_STOP);
        int currChromosome = -1;
        
        DataSet dataSet = null;
        ArrayList<Model> models = null;
        ArrayList<SNP> snps = null;
        //SnpModel2PvalMap snpModel2Pval = null;
                    
        int numCols = matrix.get(0).size();
        String prevGeneName = "";
        int minGeneRange = -1;
        int maxGeneRange = -1;
        int rowIndex = 0;
        for (ArrayList<String> rowData : matrix) {
            if (rowIndex == 0) {
                models = initializeModels(rowData, snpFuncCol+1);
            } else {
                if( ! rowData.get(geneSymbolCol).equals(prevGeneName)) {
                    if (dataSet != null) {
                        System.out.println("Dataset is not null " + prevGeneName + "\t" + rowData.get(geneSymbolCol));
                        dataSet = new DataSet();
                        dataSet.setModels(models);
                        dataSet.setSNPs(snps);
                        NumericRange range = new NumericRange(minGeneRange, maxGeneRange);
                        dataSet.setChromosome(currChromosome);
                        dataSet.setXAxisRange(range);
                        GeneRange geneRange = new GeneRange(prevGeneName, currChromosome, (int) range.getMin(), (int) range.getMax());
                        dataSet.setGeneRange(geneRange);
                        dataSet.setSnpModel2PvalMap(snpModel2Pval);
                        loadResults.put(prevGeneName, dataSet);
                    } else {
                        System.out.println("Dataset is null" + prevGeneName + "\t" + rowData.get(geneSymbolCol));
                        dataSet = new DataSet();
                        dataSet.setChromosome(currChromosome);
                   }
                    snps = new ArrayList<SNP>();
                    snpModel2Pval = new SnpModel2PvalMap();
                    prevGeneName = rowData.get(geneSymbolCol);
                    minGeneRange = Integer.parseInt( rowData.get(geneStartCol) );
                    maxGeneRange = Integer.parseInt( rowData.get(geneStopCol) );
                }
                int snpStart = Integer.parseInt(rowData.get(snpStartCol));
                int rsId     = Integer.parseInt(rowData.get(rsIdCol));
                currChromosome = parseChromosome(rowData.get(chromosomeCol));
                SNP currSnp  = new SNP(rsId, snpStart);
                snps.add(currSnp);
                minGeneRange = Math.min(minGeneRange, snpStart);
                maxGeneRange = Math.max(maxGeneRange, snpStart);
                int rowNumCols = rowData.size();
                int modelIndex = 0;
                for(int col = snpFuncCol+1; col < rowNumCols; col += 2) {
                    // assumes starts with logp and then has non-logp for each feature
                    if(! rowData.get(col).startsWith("N")) {
                        try {
                            double logp = Float.parseFloat(rowData.get(col));
                            snpModel2Pval.put(currSnp, models.get(modelIndex), logp);
                        } catch(NumberFormatException nfe) {
                            System.out.println("Failed to parse float " + rowData.get(col));
                        }
                    }
                    modelIndex++;
                }

            }
            rowIndex++;
        }
        if (dataSet != null) {
            System.out.println("Final dataset is not null " + prevGeneName);
            dataSet = new DataSet();
            dataSet.setModels(models);
            dataSet.setSNPs(snps);
            dataSet.setChromosome(currChromosome);
            NumericRange range = new NumericRange(minGeneRange, maxGeneRange);
            dataSet.setXAxisRange(range);
            dataSet.setSnpModel2PvalMap(snpModel2Pval);
            GeneRange geneRange = new GeneRange(prevGeneName, currChromosome, (int) range.getMin(), (int) range.getMax());
            dataSet.setGeneRange(geneRange);
            loadResults.put(prevGeneName, dataSet);
        }
        
    }*/
    
    /**
     * Returns the integer chromosome or the DataModel.X or DataModel.Y.  If
     * it's none of those, it returns -1
     * @param str
     * @return 
     */
    public int parseChromosome(String str) {
        int chromosome = -1;
        try {
            chromosome = Integer.parseInt(str);
        } catch(NumberFormatException nfe) {
            if(str.equalsIgnoreCase("X")) {
                chromosome = DataModel.X;   
            } else if(str.equalsIgnoreCase("Y")) {
                chromosome = DataModel.Y;
            }
        }
        return chromosome;
    }

    /**
     * Initializes the list of models based on the header row columns
     * SNP_FUNC + 1 to the end
     * @param header 
     */
    private ArrayList<Model> initializeModels(ArrayList<String> header, int startCol) {
        ArrayList<Model> models = new ArrayList<Model>();
        for(int col = startCol; col < header.size(); col+=2) {
            String[] tokens = header.get(col).split("\\/");
            String study    = tokens[0];
            String endpoint = tokens[1];
            String type     = tokens[2];
            Model model = new Model(study, endpoint, type);
            models.add(model);
        }
        return models;
    }

    /**
     * Loads matrix into a list of lists of Strings with one row per line.
     */
    private ArrayList<ArrayList<String>> loadMatrix(String filename) {
        ArrayList<ArrayList<String>> matrix = new ArrayList<ArrayList<String>>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(filename);
            br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {
                    String[] tokens = line.trim().split("\\t");
                    int numTokens = tokens.length;
                    ArrayList<String> tokenList = new ArrayList<String>();
                    for (int tokeni = 0; tokeni < numTokens; tokeni++) {
                        tokenList.add(tokens[tokeni]);
                    }
                    matrix.add(tokenList);
                }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not found " + filename);
        } catch (IOException ioe) {
            System.out.println("IO exception for file " + filename);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                System.out.println("Failed to close br " + filename);
            }
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ioe) {
                System.out.println("Failed to close fr " + filename);
            }
        }
        return matrix;
    }

    /**
     * Returns the index of mtx.get(0) where the list entry is "SNP start"
     * else returns -1 with a printed message
     *
     * @param mtx
     * @return
     */
    protected int findColName(ArrayList<ArrayList<String>> mtx, String nameToFind) {
        int foundCol = -1;
        int col = 0;
        for (String colName : mtx.get(0)) {
            if (colName.equals(nameToFind)) {
                return col;
            }
            col++;
        }
        System.out.println("Failed to find column " + nameToFind);
        return foundCol;
    }

    /**
     * Returns a vertical slice of matrix excluding the header row for each
     * row at column wantCol. If the value is NaN or blank, it fills in 0
     * since 0 doesn't exist in log scale. It fills in 0 for non-numeric
     * values as well.
     */
    /*protected ArrayList<Double> extractColumn(ArrayList<ArrayList<String>> mtx, int wantCol) {
        ArrayList<Double> vec = new ArrayList<Double>();
        int rowNum = 0;
        for (ArrayList<String> row : mtx) {
            if (rowNum > 0) { // ignore header
                if (wantCol < row.size()) {
                    try {
                        double value = Double.parseDouble(row.get(wantCol));
                        vec.add(value);
                    } catch (NumberFormatException nfe) {
                        vec.add(NO_VALUE);
                    }
                } else {
                    vec.add(NO_VALUE);
                }
            }
            rowNum++;
        }
        return vec;
    }*/

    /**
     * Returns a vertical slice of matrix excluding the header row for each
     * row at column wantCol. If the value is NaN or blank, it fills in 0
     * since 0 doesn't exist in log scale. It fills in 0 for non-numeric
     * values as well.
     */
    protected ArrayList<Integer> extractColumnInteger(ArrayList<ArrayList<String>> mtx, int wantCol) {
        ArrayList<Integer> vec = new ArrayList<Integer>();
        int rowNum = 0;
        for (ArrayList<String> row : mtx) {
            if (rowNum > 0) {  // ignore header
                if (wantCol < row.size()) {
                    try {
                        int value = Integer.parseInt(row.get(wantCol));
                        vec.add(value);
                    } catch (NumberFormatException nfe) {
                        vec.add(-1);
                    }
                } else {
                    vec.add(-1);
                }
            }
            rowNum++;
        }
        return vec;
    }
    
    /**
     * Returns the results obtained from loading in the data
     * @return 
     */
    public HashMap<String, DataSet> getLoadResults() {
        return loadResults;
    }
}
