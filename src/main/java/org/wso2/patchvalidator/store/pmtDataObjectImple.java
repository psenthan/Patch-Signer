/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.store;


import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.interfaces.PmtDataObject;
import org.wso2.patchvalidator.service.SyncService;

/**
 * TODO: Class level comment.
 */
public class pmtDataObjectImple implements PmtDataObject {
    private static final Logger LOG = LoggerFactory.getLogger(pmtDataObjectImple.class);
    private Properties prop = new Properties();
    private Connection connectDB;

    {
        try {
            prop.load( SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
            String dbURL=prop.getProperty("dbURL");
            String dbUser=prop.getProperty("dbUser");
            String dbPassword=prop.getProperty("dbPassword");
            connectDB = DriverManager.getConnection(dbURL,dbUser,dbPassword);
        } catch (SQLException | IOException e) {
            LOG.error("Database connection failure!!!!!");
        }
    }
    @Override
    public int getProductType(String product) throws SQLException {



        Statement create=connectDB.createStatement();

        String productTypeChooser= "select type from wumlivesync.PRODUCT_WITH_WUM where PRODUCT='"+product+"'";
        ResultSet result=create.executeQuery(productTypeChooser);

        int type=0;

        while(result.next()){
            type = result.getInt("type");

        }
        return type;
    }
    @Override
    public void insertPostRequestParam(String patchId,String version,int state,int type,String product,String developedBy,
                                       String status) throws SQLException {


        Statement create=connectDB.createStatement();

        switch (version) {

            case "wilkes":
                version = "4.4.0";
                break;

            case "perlis":
                version = "4.3.0";
                break;

            case "turing":
                version ="4.2.0";
                break;

            default:
                LOG.error("Error in version of the product!!");
        }

        String patchType=null;

        switch (type)
        {
            case 1:
                patchType="patch";
                break;

            case 2:
                patchType="update";
                break;

            case 3:
                patchType="PatchAndUpdate";
                break;

            default:
                LOG.info("Error in patch type");
        }

        String ProcessStatus="select * from wumlivesync.TRACK_PATCH_VALIDATE_RESULTS where STATUS='"+Constants.PROCESSING+"'";
        ResultSet inProcess=create.executeQuery(ProcessStatus);


        if(inProcess.next() ){
            String PostParametersInserter="insert into wumlivesync.TRACK_PATCH_VALIDATE_RESULTS(PATCH_ID,VERSION,STATE,TYPE," +
                    "PRODUCT,DEVELOPED_BY,STATUS) values('"+patchId+"','"+version+"','"+state+"','"+patchType+"','"+product
                    +"','"+developedBy+"','"+status+"')";
            PreparedStatement proceed=connectDB.prepareStatement(PostParametersInserter,Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
            updatePostRequestStatus(product,patchId,Constants.QUEUE);
        }
        else
        {
            String PostParametersInserter="insert into wumlivesync.TRACK_PATCH_VALIDATE_RESULTS(PATCH_ID,VERSION,STATE,TYPE," +
                    "PRODUCT,DEVELOPED_BY,STATUS) values('"+patchId+"','"+version+"','"+state+"','"+patchType+"','"+product
                    +"','"+developedBy+"','"+status+"')";
            PreparedStatement proceed=connectDB.prepareStatement(PostParametersInserter,Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
            updatePostRequestStatus(product,patchId,Constants.PROCESSING);
        }


    }
    public void updatePostRequestStatus(String product,String patchId,String status) throws SQLException {
        String changeStatus="update wumlivesync.TRACK_PATCH_VALIDATE_RESULTS set status='"+status+"' where PRODUCT='"+product
                +"' && PATCH_ID='"+patchId+"'";
        PreparedStatement proceed=connectDB.prepareStatement(changeStatus,Statement.RETURN_GENERATED_KEYS);
        proceed.executeUpdate();

    }
}


