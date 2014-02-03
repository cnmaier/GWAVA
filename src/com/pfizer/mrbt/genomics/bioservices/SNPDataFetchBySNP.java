/*
 * Queries the database through bioservices based on the query tab.
 */
package com.pfizer.mrbt.genomics.bioservices;

import com.pfizer.mrbt.genomics.TransmartClient.TransmartServicesParameters;
import com.pfizer.mrbt.genomics.data.DataModel;
import com.pfizer.mrbt.genomics.data.DataSet;
import com.pfizer.mrbt.genomics.data.GeneRange;
import com.pfizer.mrbt.genomics.data.Model;
import com.pfizer.mrbt.genomics.data.NumericRange;
import com.pfizer.mrbt.genomics.data.SNP;
import com.pfizer.tnb.api.server.util.QueryResult;
import com.pfizer.tnb.bsutil.BsServiceClientImpl;
import com.pfizer.tnb.api.server.util.BioServicesInitParams;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author henstockpv
 */
public class SNPDataFetchBySNP extends BsServiceClientImpl {

    public SNPDataFetchBySNP() {
        super();
        BioServicesInitParams initParams = new BioServicesInitParams();
        initParams.setBioServicesServer(BioservicesParameters.SERVER_URL);
        initParams.setServer(BioservicesParameters.HOST);
        initParams.setPort(BioservicesParameters.PORT);
        setInitParams(initParams);
    }

    /**
     * Main routine for loading and populating the data model. It assumes that
     * the radius has been validated
     */
    public void fetchSnpData(List<ModelOption> modelOptions,
                             DbSnpSourceOption dbSnpOption,
                             GeneSourceOption geneSourceOption,
                             List<String> geneRequestList,
                             int basePairRadius) {
        int service_id = BioservicesParameters.SNP_SEARCH_BY_SNP_SERVICE_ID;
        String queryStr = BioservicesParameters.SERVER_URL + "service=" + service_id + "&SERVICE_RENDERID=7";

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("RANGE", basePairRadius + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (ModelOption modelOption : modelOptions) {
            if (cnt > 0) {
                sb.append(",");
            }
            sb.append(modelOption.getModelId() + "");
            cnt++;
        }
        paramMap.put("MODEL_ID", sb.toString());
        paramMap.put("SOURCE_ID", dbSnpOption.getId()+"");

        // loop for each string
        for (String gene : geneRequestList) {
            paramMap.put("GENE_NAME", gene);
            //System.out.println("ParamMap for " + gene + " is " + paramMap.get("MODEL_ID"));
            String queryStrWithParams = BioservicesParameters.addParametersToUrl(queryStr, paramMap);
            System.out.println("QueryStr: " + queryStrWithParams);
            //System.out.println("ParamMap2 for " + gene + " is " + paramMap.get("MODEL_ID"));
            //System.out.println("QueryStr: [" + queryStrWithParams + "]");
            long startTime = System.currentTimeMillis();
            try {
                QueryResult queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
                long endTime = System.currentTimeMillis();
                System.out.println("Elapsed time " + (endTime - startTime)/1000);
                //System.out.println("ParamMap3 for " + gene + " is " + paramMap.get("MODEL_ID"));
                //System.out.println("QueryStr results: [" + queryResults.getData().size());
                parseQueryResultsIntoDataSet(gene, modelOptions, dbSnpOption, geneSourceOption, queryResults, basePairRadius);
                //System.out.println("ParamMap4 for " + gene + " is " + paramMap.get("MODEL_ID"));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    /**
     * Performs the Bioservice call for the single gene muliple models returning
     * the QueryResult
     */
    public QueryResult fetchSnpDataSingleSNP(List<ModelOption> modelOptions,
                                       DbSnpSourceOption dbSnpOption,
                                       GeneSourceOption geneSourceOption,
                                       String snp,
                                       int basePairRadius) {
        int service_id = BioservicesParameters.SNP_SEARCH_BY_SNP_SERVICE_ID;
        String queryStr = BioservicesParameters.SERVER_URL + "service=" + service_id + "&SERVICE_RENDERID=7";

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("RANGE", basePairRadius + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (ModelOption modelOption : modelOptions) {
            if (cnt > 0) {
                sb.append(",");
            }
            sb.append(modelOption.getModelId() + "");
            cnt++;
        }
        paramMap.put("MODEL_ID", sb.toString());
        paramMap.put("HG_VERSION", dbSnpOption.getId() + "");


        paramMap.put("SNP", snp);
        //System.out.println("ParamMap for " + gene + " is " + paramMap.get("MODEL_ID"));
        String queryStrWithParams = BioservicesParameters.addParametersToUrl(queryStr, paramMap);
        //System.out.println("ParamMap2 for " + gene + " is " + paramMap.get("MODEL_ID"));
        System.out.println("QueryStr: [" + queryStrWithParams + "]");
        long startTime = System.currentTimeMillis();
        try {
            QueryResult queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
            long endTime = System.currentTimeMillis();
            //System.out.println("Elapsed time " + (endTime - startTime) / 1000);
            //System.out.println("ParamMap3 for " + gene + " is " + paramMap.get("MODEL_ID"));
            System.out.println("QueryStr results: [" + queryResults.getData().size());
            //parseQueryResultsIntoDataSet(snp, modelOptions, dbSnpOption, geneSourceOption, queryResults, basePairRadius); // pvh removing 3/19/2013 since doesn't seem to do anything
            //System.out.println("ParamMap4 for " + gene + " is " + paramMap.get("MODEL_ID"));
            return queryResults;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    /**
     * Parses the queryResult of a single gene and multiple models into a
     * DataSet structure that it adds (or modifies) in 
     * @param queryResult 
     */
    protected void parseQueryResultsIntoDataSet(String gene, 
                                                List<ModelOption> modelOptions, 
                                                DbSnpSourceOption dbSnpOption,
                                                GeneSourceOption geneSourceOption,
                                                QueryResult queryResult, int radius) {
        DataSet dataSet = new DataSet();
        int rowIndex = 0;
        int minLoc = Integer.MAX_VALUE;
        int maxLoc = Integer.MIN_VALUE;
        int chromosome = -1;
        for(List<String> row : queryResult.getData()) {
            if(rowIndex == 0) {
                chromosome = DataModel.parseChromosomeStr(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_CHROMOSOME_COL));
                //chromosome = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_GENE_CHROMOSOME_COL));
                dataSet.setChromosome(chromosome);
            }

            String studySetModel = row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_STUDY_SET_MODEL_COL);
            String[] tokens = studySetModel.split("\\s\\-\\s");
            
            String study     = "";
            String set       = "set";
            String modelName = "";
            if(tokens.length < 3) {
                //study = row.get(TransmartServicesParameters.SNP_SEARCH_STUDY_COL);
                set   = row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_STUDY_SET_MODEL_COL);
            } else if(tokens.length == 3) {
                study     = tokens[0];
                set       = tokens[1];
                modelName = tokens[2];
            } else if(tokens.length > 3) {
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < tokens.length-2; i++) {
                    if(i > 0) {
                        sb.append(" - ");
                    }
                    sb.append(tokens[i]);
                }
                study = sb.toString();
                set   = tokens[tokens.length-2];
                modelName = tokens[tokens.length-1];
                        
            }
            // removes rs string from start of rs and converts rest to integer
            int rsId        = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_RSID_COL).substring(2));
            double logPval  = Double.parseDouble(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_LOG_PVAL_COL));
            int loc         = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_START_COL));
            if(loc < minLoc) { minLoc = loc; }
            if(loc > maxLoc) { maxLoc = loc; }
            SNP currSnp     = dataSet.checkAddSnp(rsId);
            currSnp.setLoc(loc);
            Model currModel = dataSet.checkAddModel(study, set, modelName);
            dataSet.addSnpModel2Pval(currSnp, currModel, logPval);
            rowIndex++;
        }
        int avg = (minLoc + maxLoc)/2;
        // klugu todo
        maxLoc = avg + radius;
        minLoc = avg - radius;
        dataSet.setXAxisRange(new NumericRange(minLoc, maxLoc));
        GeneRange geneRange = new GeneRange(gene, chromosome, minLoc, maxLoc);
        geneRange.setRadius(radius);
        dataSet.setGeneRange(geneRange);
        dataSet.setDbSnpOption(dbSnpOption);
        dataSet.setGeneSourceOption(geneSourceOption);
        //Singleton.getDataModel().addDataSet(gene, dataSet);  12/19/2012 pvh removed to avoid double add
    }
    
    
    /**
     * Parses the queryResult of a single gene and multiple models into a
     * DataSet structure that it adds (or modifies) in 
     * @param queryResult 
     */
    public DataSet parseQueryResultsIntoDataSet(DataSet dataSet, String gene, 
                                                List<ModelOption> modelOptions, 
                                                DbSnpSourceOption dbSnpOption,
                                                GeneSourceOption geneSourceOption,
                                                QueryResult queryResult, int radius) {
        int rowIndex = 0;
        int minLoc = Integer.MAX_VALUE;
        int maxLoc = Integer.MIN_VALUE;
        int chromosome = -1;
        for(List<String> row : queryResult.getData()) {
            if(rowIndex == 0) {
                chromosome = DataModel.parseChromosomeStr(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_CHROMOSOME_COL));
                //chromosome = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_GENE_CHROMOSOME_COL));
                dataSet.setChromosome(chromosome);
            }

            String studySetModel = row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_STUDY_SET_MODEL_COL);
            String[] tokens = studySetModel.split("\\s\\-\\s");
            String study     = "";
            String set       = "set";
            String modelName = "";
            if(tokens.length < 3) {
                //study = row.get(TransmartServicesParameters.SNP_SEARCH_STUDY_COL);
                if(tokens.length == 2) {
                    study = tokens[1];
                    set   = tokens[0];  // analysis_name
                } else {
                    set   = row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_STUDY_SET_MODEL_COL);
                }
            } else if(tokens.length == 3) {
                study     = tokens[0];
                set       = tokens[1];
                modelName = tokens[2];
            } else if(tokens.length > 3) {
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < tokens.length-2; i++) {
                    if(i > 0) {
                        sb.append(" - ");
                    }
                    sb.append(tokens[i]);
                }
                study = sb.toString();
                set   = tokens[tokens.length-2];
                modelName = tokens[tokens.length-1];
                        
            }
            // removes rs string from start of rs and converts rest to integer
            int rsId        = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_RSID_COL).substring(2));
            double logPval  = Double.parseDouble(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_LOG_PVAL_COL));
            int loc         = Integer.parseInt(row.get(BioservicesParameters.SNP_SEARCH_BY_SNP_START_COL));
            if(loc < minLoc) { minLoc = loc; }
            if(loc > maxLoc) { maxLoc = loc; }
            SNP currSnp     = dataSet.checkAddSnp(rsId);
            currSnp.setLoc(loc);
            Model currModel = dataSet.checkAddModel(study, set, modelName);
            dataSet.addSnpModel2Pval(currSnp, currModel, logPval);
            rowIndex++;
        }
        int avg = (minLoc + maxLoc)/2;
        // klugu todo
        maxLoc = avg + radius;
        minLoc = avg - radius;
        dataSet.setXAxisRange(new NumericRange(minLoc, maxLoc));
        GeneRange geneRange = new GeneRange(gene, chromosome, minLoc, maxLoc);
        geneRange.setRadius(radius);
        dataSet.setGeneRange(geneRange);
        dataSet.setDbSnpOption(dbSnpOption);
        dataSet.setGeneSourceOption(geneSourceOption);
        //Singleton.getDataModel().addDataSet(gene, dataSet);
        return dataSet;
    }
    
    
}
