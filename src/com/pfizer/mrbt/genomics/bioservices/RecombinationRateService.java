/*
 * Queries the database through bioservices based on the query tab.
 */
package com.pfizer.mrbt.genomics.bioservices;

import com.pfizer.mrbt.genomics.data.SnpRecombRate;
import com.pfizer.tnb.api.server.util.QueryResult;
import com.pfizer.tnb.bsutil.BsServiceClientImpl;
import com.pfizer.tnb.api.server.util.BioServicesInitParams;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author henstockpv
 */
public class RecombinationRateService extends BsServiceClientImpl {
    private float maxRecombinationRate = 0f;

    
    public RecombinationRateService() {
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
    public ArrayList<SnpRecombRate> fetchRecombinationRateData(String geneName,
                                           int basePairRadius,
                                           int geneSourceId) {
        int service_id = BioservicesParameters.RECOMBINATION_RATE_BY_GENE_SERVICE_ID;
        String queryStr = BioservicesParameters.SERVER_URL + "service=" + service_id + "&SERVICE_RENDERID=7";

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("GENE_SYMBOL", geneName);
        paramMap.put("RANGE", basePairRadius + "");
        paramMap.put("GENE_SOURCE_ID", geneSourceId + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();

        String queryStrWithParams = BioservicesParameters.addParametersToUrl(queryStr, paramMap);
        //System.out.println("QueryStr: [" + queryStrWithParams + "]");
        long startTime = System.currentTimeMillis();
        ArrayList<SnpRecombRate> snpRecombRates = new ArrayList<SnpRecombRate>();
        try {
            QueryResult queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
            long endTime = System.currentTimeMillis();
            snpRecombRates = parseQueryResults(queryResults, service_id);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return snpRecombRates;
    }

    /**
     * Main routine for loading and populating the data model. It assumes that
     * the radius has been validated
     */
    public QueryResult fetchRecombinationRateByGeneQueryResult(String geneName,
                                           int basePairRadius,
                                           GeneSourceOption geneSourceOption) {
        int service_id = BioservicesParameters.RECOMBINATION_RATE_BY_GENE_SERVICE_ID;
        String queryStr = BioservicesParameters.SERVER_URL + "service=" + service_id + "&SERVICE_RENDERID=7";

        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("GENE_SYMBOL", geneName);
        paramMap.put("RANGE", basePairRadius + "");
        paramMap.put("GENE_SOURCE_ID", geneSourceOption.getId() + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();

        String queryStrWithParams = BioservicesParameters.addParametersToUrl(queryStr, paramMap);
        //System.out.println("QueryStr: [" + queryStrWithParams + "]");
        ArrayList<SnpRecombRate> snpRecombRates = new ArrayList<SnpRecombRate>();
        QueryResult queryResults = null;
        try {
            queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return queryResults;
    }

    /**
     * Main routine for loading and populating the data model. It assumes that
     * the radius has been validated
     */
    public QueryResult fetchRecombinationRateBySNPQueryResult(String snpName,
                                           int basePairRadius,
                                           GeneSourceOption geneSourceOption) {
        int service_id = BioservicesParameters.RECOMBINATION_RATE_BY_SNP_SERVICE_ID;
        String queryStr = BioservicesParameters.SERVER_URL + "service=" + service_id + "&SERVICE_RENDERID=7";
        String snpNameWithoutRS = snpName.substring(2);
        // fill param map with generalities
        HashMap<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("SNP", snpNameWithoutRS);
        paramMap.put("DISTANCE", basePairRadius + "");
        System.out.println("GeneSourceOption " + geneSourceOption.getId());
        paramMap.put("SNP_SOURCE_ID", geneSourceOption.getId() + "");

        int cnt = 0;
        StringBuilder sb = new StringBuilder();

        String queryStrWithParams = BioservicesParameters.addParametersToUrl(queryStr, paramMap);
        //System.out.println("QueryStr: [" + queryStrWithParams + "]");
        ArrayList<SnpRecombRate> snpRecombRates = new ArrayList<SnpRecombRate>();
        QueryResult queryResults = null;
        try {
            queryResults = getData(queryStrWithParams, service_id, -1, -1, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return queryResults;
    }

    /**
     * Parses each row and puts the loc and rate into snpRecombRates.  It also
     * stores maxRecombinatinoRate with the maximum rate value.  If either value
     * cannot be parsed, it puts a sysmtem.out.println call and doesn't include
     * it in the list.
     * @param queryResult
     * @return 
     */
    public ArrayList<SnpRecombRate> parseQueryResults(QueryResult queryResult, int service_ID) {
        int positionCol;
        int rateCol;
        if(service_ID == BioservicesParameters.RECOMBINATION_RATE_BY_GENE_SERVICE_ID) {
            positionCol = BioservicesParameters.RECOMBINATION_RATE_BY_GENE_POSITION_COL;
            rateCol     = BioservicesParameters.RECOMBINATION_RATE_BY_GENE_COL;
        } else {
            positionCol = BioservicesParameters.RECOMBINATION_RATE_BY_SNP_POSITION_COL;
            rateCol     = BioservicesParameters.RECOMBINATION_RATE_BY_SNP_COL;
        }
        ArrayList<SnpRecombRate> snpRecombRates = new ArrayList<SnpRecombRate>();
        maxRecombinationRate = 0f;
        for(List<String> row : queryResult.getData()) {
            try {
                int loc     = Integer.parseInt(row.get(positionCol));
                float rate = Float.parseFloat(row.get(rateCol));
                snpRecombRates.add(new SnpRecombRate(loc, rate));
                if(rate > maxRecombinationRate) {
                    maxRecombinationRate = rate;
                }
            } catch(NumberFormatException nfe) {
                System.out.println("Failed to parse [" + 
                                   row.get(positionCol) + "]\t[" +               
                    row.get(rateCol) + "]");
            }
        }
        return snpRecombRates;
    }

    public float getMaxRecombinationRate() {
        return maxRecombinationRate;
    }
    
    
    
}
