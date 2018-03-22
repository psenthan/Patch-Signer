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
package org.wso2.patchvalidator.commiter;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.updator.PMTUpdator;
import org.wso2.patchvalidator.validators.PatchValidator;
import org.wso2.patchvalidator.validators.PatchZipValidator;

/**
 * TODO: Class level comment.
 */
public class KeysCommitter {
    private static final Logger LOG = LoggerFactory.getLogger(PatchValidator.class);
    public static void validateKeysCommit(String patchUrl,String patchDestination) throws IOException {
        PatchZipValidator.commitKeys(patchUrl, patchDestination);


    }
    public static void updatePMT(String patchName,int state) throws IOException {
        String patchStatus;
        if (state == 1) patchStatus = "ReleasedNotInPublicSVN";
        else if (state == 2) patchStatus = "ReleasedNotAutomated";
        else if (state == 3) patchStatus = "Promote";
        else patchStatus = "Error in patch status";
        LOG.info("patchStatus = " + patchStatus);
        PMTUpdator.sendRequest(patchName,state,true, patchStatus);
    }


}


