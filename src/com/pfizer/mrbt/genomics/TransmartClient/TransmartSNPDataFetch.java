/*
 * Queries the database through bioservices based on the query tab.
 */
package com.pfizer.mrbt.genomics.TransmartClient;

import com.pfizer.mrbt.genomics.bioservices.*;
import com.pfizer.mrbt.genomics.Singleton;
import com.pfizer.mrbt.genomics.data.DataModel;
import com.pfizer.mrbt.genomics.data.DataSet;
import com.pfizer.mrbt.genomics.data.GeneRange;
import com.pfizer.mrbt.genomics.data.Model;
import com.pfizer.mrbt.genomics.data.NumericRange;
import com.pfizer.mrbt.genomics.data.SNP;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author henstockpv
 */
public class TransmartSNPDataFetch {

    public TransmartSNPDataFetch() {
    }

    /**
     * Performs the transmart call to get the SNPs and returns the XML file
     * associated with the call.
     */
    public String fetchSnpDataSingleGene(List<ModelOption> modelOptions,
                                       DbSnpSourceOption dbSnpOption,
                                       GeneSourceOption geneSourceOption,
                                       String gene,
                                       int basePairRadius) {
        String queryStr = TransmartServicesParameters.getServerURL() + TransmartServicesParameters.SNP_SEARCH_METHOD;

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("range", basePairRadius + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (ModelOption modelOption : modelOptions) {
            if (cnt > 0) {
                sb.append(",");
            }
            sb.append(modelOption.getModelId() + "");
            cnt++;
        }
        paramMap.put("modelId", sb.toString());
        //paramMap.put("SOURCE_ID", dbSnpOption.getId() + ""); // unused

        paramMap.put("geneName", gene);
        //System.out.println("ParamMap for " + gene + " is " + paramMap.get("modelId"));
        String queryStrWithParams = TransmartUtil.addParametersToUrl(queryStr, paramMap);
        //System.out.println("ParamMap2 for " + gene + " is " + paramMap.get("modelId"));
        System.out.println("QueryStr: [" + queryStrWithParams + "]");
        long startTime = System.currentTimeMillis();
        System.out.println("QueryStrWithParams " + queryStrWithParams);
        try {
            //QueryResult queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
            String xmlResult = TransmartUtil.fetchResult(queryStrWithParams);
            long endTime = System.currentTimeMillis();
            //System.out.println("Elapsed time " + (endTime - startTime) / 1000);
            //System.out.println("ParamMap3 for " + gene + " is " + paramMap.get("modelId"));
            //System.out.println("QueryStr results: [" + queryResults.getData().size());
            //parseQueryResultsIntoDataSet(gene, modelOptions, dbSnpOption, geneSourceOption, xmlResults, basePairRadius);
            //System.out.println("ParamMap4 for " + gene + " is " + paramMap.get("modelId"));
            //return queryResults;
            return xmlResult;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * Performs the transmart call to get the SNPs and returns the XML file
     * associated with the call.
     */
    public String fetchSnpDataSingleSNP(List<ModelOption> modelOptions,
                                       DbSnpSourceOption dbSnpOption,
                                       GeneSourceOption geneSourceOption,
                                       String gene,
                                       int basePairRadius) {
        String queryStr = TransmartServicesParameters.getServerURL() + TransmartServicesParameters.SNP_SEARCH_METHOD;

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("range", basePairRadius + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();
        for (ModelOption modelOption : modelOptions) {
            if (cnt > 0) {
                sb.append(",");
            }
            sb.append(modelOption.getModelId() + "");
            cnt++;
        }
        paramMap.put("modelId", sb.toString());

        paramMap.put("geneName", gene);
        //System.out.println("ParamMap for " + gene + " is " + paramMap.get("modelId"));
        String queryStrWithParams = TransmartUtil.addParametersToUrl(queryStr, paramMap);
        //System.out.println("QueryStr: [" + queryStrWithParams + "]");
        long startTime = System.currentTimeMillis();
        try {
            String xmlResult = TransmartUtil.fetchResult(queryStrWithParams);
            long endTime = System.currentTimeMillis();
            //System.out.println("Elapsed time " + (endTime - startTime) / 1000);
            return xmlResult;
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
    protected DataSet parseQueryResultsIntoDataSet(DataSet dataSet, String gene, 
                                                List<ModelOption> modelOptions, 
                                                DbSnpSourceOption dbSnpOption,
                                                GeneSourceOption geneSourceOption,
                                                String xmlResult, int radius) {
        int rowIndex = 0;
        int minLoc = Integer.MAX_VALUE;
        int maxLoc = Integer.MIN_VALUE;
        int chromosome = -1;
        ArrayList<ArrayList<String>> queryResults = TransmartUtil.parseXml(xmlResult);
        
        for(List<String> row : queryResults) {
            if(rowIndex == 0) {
                chromosome = DataModel.parseChromosomeStr(row.get(TransmartServicesParameters.SNP_SEARCH_CHROMOSOME_COL));
                dataSet.setChromosome(chromosome);
            }
            String studySetModel = row.get(TransmartServicesParameters.SNP_SEARCH_STUDY_SET_MODEL_NAME_COL);
            String[] tokens = studySetModel.split("\\s+\\-\\s+");
            String study = "study";
            String set = "set";
            String model = "model";
            if(tokens.length == 3) {
                study = tokens[0];
                set   = tokens[1];
                model = tokens[2];
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
                model = tokens[tokens.length-1];
            } else {
                study = row.get(TransmartServicesParameters.SNP_SEARCH_STUDY_COL);
                set   = row.get(TransmartServicesParameters.SNP_SEARCH_SET_COL);
            }
            
            //String study    = row.get(TransmartServicesParameters.SNP_SEARCH_STUDY_COL);
            //String set      = row.get(TransmartServicesParameters.SNP_SEARCH_SET_COL);
            // removes rs string from start of rs and converts rest to integer
            int rsId        = Integer.parseInt(row.get(TransmartServicesParameters.SNP_SEARCH_RSID_COL).substring(2));
            double logPval  = Double.parseDouble(row.get(TransmartServicesParameters.SNP_SEARCH_LOG_PVAL_COL));
            int loc         = Integer.parseInt(row.get(TransmartServicesParameters.SNP_SEARCH_START_COL));
            if(loc < minLoc) { minLoc = loc; }
            if(loc > maxLoc) { maxLoc = loc; }
            SNP currSnp     = dataSet.checkAddSnp(rsId);
            currSnp.setLoc(loc);
            Model currModel = dataSet.checkAddModel(study, set, model);
            dataSet.addSnpModel2Pval(currSnp, currModel, logPval);
            rowIndex++;
        }
        int avg = (minLoc + maxLoc)/2;
        // kluge todo
        System.out.println("MaxLoc " + maxLoc + "\tminLoc " + minLoc + "\tAvg " + avg + "\tradius " + radius);
        //maxLoc = avg + radius;
        //minLoc = avg - radius;
        dataSet.setXAxisRange(new NumericRange(minLoc, maxLoc));
        GeneRange geneRange = new GeneRange(gene, chromosome, minLoc, maxLoc);
        geneRange.setRadius(radius);
        dataSet.setGeneRange(geneRange);
        dataSet.setDbSnpOption(dbSnpOption);
        dataSet.setGeneSourceOption(geneSourceOption);
        return dataSet;
    }
}
