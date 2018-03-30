package org.wso2.patchvalidator.interfaces;

import java.sql.SQLException;

/**
 * TODO: Class level comment.
 */
public interface PmtDataObject {
     int getProductType(String product) throws SQLException;

     void insertPostRequestParam(String patchId,String version,int state,int type,String product,String developedBy,
                                String status) throws SQLException;

     void updatePostRequestStatus(String product,String patchId,String status) throws SQLException;
}
