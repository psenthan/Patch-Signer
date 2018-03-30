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
package org.wso2.patchvalidator.productMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.service.SyncService;


/**
 * TODO: Class level comment.
 */
public class ProductDefiner {
    private static final Logger LOG = LoggerFactory.getLogger(ProductDefiner.class);
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
    public String findProduct(String productName,String productVersion) throws SQLException {

        Statement create=connectDB.createStatement();

        String productChooser= "select PRODUCT_ABBREVIATION from wumlivesync.PRODUCT_DETAILS where PRODUCT_NAME='"+ productName
                +"' AND PRODUCT_VERSION='"+productVersion+"'";
        ResultSet result=create.executeQuery(productChooser);


        while(result.next()){
            productName = result.getString("PRODUCT_ABBREVIATION");

        }
        return productName;
    }
    }

