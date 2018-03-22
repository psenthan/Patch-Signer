/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.patchvalidator.validators;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.service.SyncService;

public class PatchValidator  {
    public String patchUrl="null";
    public String patchDestination="null";
    public String patchName="null";
    public static PatchValidateFactory getPatchValidateFactory(String filepath) {
        if (filepath.endsWith(".zip")) {
            return new PatchValidateFactory();
        }
        return null;
    }
    private Properties prop = new Properties();
    private Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private static final Logger LOG = LoggerFactory.getLogger(PatchValidator.class);



    public String zipPatchValidate(String patchId, String version, int state, int type,String product,String developedBy) throws IOException {



       /*
       state >> to go state
       type >> patch/update/patch and update
       output=`./wum-uc validate /home/paraparan/Patches/$carbonversion/updates/update$2/WSO2-CARBON-UPDATE-$carbonversion-$2.zip
                /home/paraparan/Servers/am/wso2am-2.1.0.zip`
       todo: handle code based on state and type
       todo: handle inputs are not in the correct format case

       */


        LOG.info("Sync Service running\n");
        prop.load(PatchValidator.class.getClassLoader().getResourceAsStream("application.properties"));

        String typeof=null;


        if (type==1 || type==3) {
            typeof="patch";
        }


        ZipDownloadPath zipDownloadPath = new ZipDownloadPath(typeof,version,patchId);
        String filepath=zipDownloadPath .getFilepath();
        patchUrl=zipDownloadPath .getUrl();
        patchDestination=zipDownloadPath .getZipDownloadDestination();
        String destFilePath=zipDownloadPath .getDestFilePath();
        String unzippedFolderPath=zipDownloadPath .getUnzippedFolderPath();
        patchName = prop.getProperty("orgPatch") + version + "-" + patchId;


        String errorMessage = "";


        version = prop.getProperty(version);
        if(version==null){
            return "Incorrect directory";
        }

            PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
            assert patchValidateFactory != null;

            CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);


            String result = commonValidator.downloadZipFile(patchUrl, version, patchId, patchDestination);
            if (!Objects.equals(result, "")) {
                LOG.info(result);
                return result + "\n" + errorMessage;
            }


            File fl = new File(patchDestination);
//
//            for (File file : fl.listFiles()) {
//                if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
//                        || file.getName().endsWith((".sha1"))) {
//                /*
//                todo: sendRequest()
//                ("WSO2-CARBON-PATCH-4.0.0-0591","ReleasedNotInPublicSVN",true,"Promote");
//                */
//                    errorMessage = "patch" + patchId + " is already signed\n";
//                    FileUtils.deleteDirectory(new File(destFilePath));
//                    LOG.info(errorMessage + "\n");
//                    return errorMessage + "\n";
//                }
//            }


            try {
                LOG.info(filepath);
                LOG.info(patchDestination);
                LOG.info(unzippedFolderPath);

                commonValidator.unZip(new File(filepath), patchDestination);
                errorMessage = commonValidator.checkContent(unzippedFolderPath, patchId);

                errorMessage = errorMessage + commonValidator.checkLicense(unzippedFolderPath + "LICENSE.txt");
                errorMessage = errorMessage + commonValidator.checkNotAContribution(unzippedFolderPath +
                        "NOT_A_CONTRIBUTION.txt");
                if (!commonValidator.checkPatch(unzippedFolderPath+
                                        "patch" + patchId + "/",patchId)) {
                                    errorMessage = errorMessage + ".jar is not available!!";
                                }
            errorMessage = errorMessage + commonValidator.checkReadMe(unzippedFolderPath, patchId);

            } catch (IOException e) {
                LOG.error("unzipping failed", e);
                errorMessage = errorMessage + "File unzipping failed\n";
            }

        if (Objects.equals(errorMessage, "")) {
            return Constants.SUCCESSFULLY_SIGNED;

        } else {

            FileUtils.deleteDirectory(new File(destFilePath));
            LOG.info(errorMessage + "\n");
            return errorMessage + "\n";
        }

    }

}

