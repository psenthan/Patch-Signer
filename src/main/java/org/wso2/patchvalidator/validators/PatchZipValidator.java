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

import java.sql.Timestamp;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import org.apache.commons.io.filefilter.HiddenFileFilter;



import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.tmatesoft.svn.core.SVNURL.parseURIDecoded;
import static org.tmatesoft.svn.core.SVNURL.parseURIEncoded;


public class PatchZipValidator implements CommonValidator {
    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOG = LoggerFactory.getLogger(CommonValidator.class);
    static final Properties prop = new Properties();
    private String username = "patchsigner@wso2.com";
    private String password = "xcbh8=cfj0mfgsOekDbh";
    private boolean securityPatch = true;
    private boolean isPatchEmpty = false;


    private boolean isResourcesFileEmpty = false;

    public void setSecurityPatch(boolean state) {
        //todo: check licence
        securityPatch = state;
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static boolean isDirEmpty(final File directory) throws IOException {
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath());
        return !dirStream.iterator().hasNext();
    }

    private static String SVNConnection(String svnURL, String svnUser, String svnPass) {
        DAVRepositoryFactory.setup();
        SVNRepository repository;
        try {

            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnURL));
            LOG.info("SVNurl:" + svnURL);
            ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[]{
                    new SVNPasswordAuthentication(svnUser, svnPass, false, parseURIEncoded(svnURL), false)});
            repository.setAuthenticationManager(authManager);
            repository.testConnection();
            LOG.info("Connection done..");
            return Constants.CONNECTION_SUCCESSFUL;


        } catch (SVNException e) {
            LOG.info("Not connected");


            e.printStackTrace();
            return Constants.SVN_CONNECTION_FAIL_STATE;
        }

    }

    @Override
    public String checkReadMe(String filePath, String patchId) throws IOException {
        StringBuilder errorMessage = new StringBuilder();
        Boolean jar = false;
        Boolean war = false;
        Boolean jag = false;
        //todo: complete .jar .war and jag
        File dir = new File(filePath + "/resources");
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith((".jar")))
                    jar = true;
                if (file.getName().endsWith((".war"))) {
                    war = true;
                }
                if (file.getName().endsWith((".jag"))) {
                    jag = true;

                }
            }
        }


        String filepath = filePath + "README.txt";
        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant README.txt does not exist\n";
        }

        List<String> lines = FileUtils.readLines(file, "UTF-8");

        String[] line = lines.get(0).split("-");
        if (!Objects.equals(patchId, line[4]) || Objects.equals(lines.get(0), "Patch ID         : patchId"))
            errorMessage = new StringBuilder("'Patch ID' line in the README.txt has an error\n");

        line = lines.get(1).split(": ");
        if (line.length < 2 || Objects.equals(line[1], "productList"))
            errorMessage.append("'Applies To' line in the README.txt has an error\n");

        line = lines.get(2).split(":");
        if (line.length == 2 && Objects.equals(line[1], "publicJIRA"))
            errorMessage.append("'Associated JIRA' line in the README.txt has an error\n");
        else if (line.length == 1 && securityPatch)
            LOG.info("This is identified as a Security patch");

        for (int i = 3; i < lines.size(); i++) {
            if (lines.get(i).startsWith("DESCRIPTION")) {
                if (lines.get(i + 1).startsWith("Patch description goes here") || lines.get(i).isEmpty())
                    errorMessage.append("DESCRIPTION section in the README.txt is not in the correct format\n");
                i++;
            }
            if (lines.get(i).startsWith("INSTALLATION INSTRUCTIONS")) {
                boolean jaggeryInstruction = false;
                for (int j = i + 1; j < lines.size(); j++) {
                    if (jag &&
                            lines.get(j).contains(
                                    " Merge and Replace resource/store to " +
                                            "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/store") &&
                            lines.get(j + 1).contains(
                                    " Merge and Replace resource/publisher to " +
                                            "<CARBON_SERVER>/repository/deployment/server/jaggeryapps/publisher")) {
                        jaggeryInstruction = true;

                    }
                    if (lines.get(j).contains("Copy the patchNumber to")) {
                        errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in the correct format: Check patchNumber\n");
                    } else if (lines.get(j).contains("Copy the patch")) {
                        if (!lines.get(j).contains("Copy the patch" + patchId + " to"))
                            errorMessage.append("INSTALLATION INSTRUCTIONS section " + "in the README.txt is not in the correct format: Check patchNumber\n");
                    }
                    i++;
                }
                if (jag && !jaggeryInstruction) {
                    errorMessage.append("Jaggery instructions are not in the correct format");
                }
            }
        }
        return errorMessage.toString();
    }

    @Override
    public String checkLicense(String filepath) throws IOException {
        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String license = prop.getProperty("license");
        LOG.info(filepath);

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant LICENSE.txt  does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if (Objects.equals(md5, license)) return "";
        return "LICENSE.txt is not in the correct format";
    }


    @Override
    public String checkNotAContribution(String filepath) throws IOException {
        prop.load(PatchZipValidator.class.getClassLoader().getResourceAsStream("application.properties"));
        final String notAContribution = prop.getProperty("notAContribution");

        File file = new File(filepath);
        if (!file.exists()) {
            return "Relevant NOT_A_CONTRIBUTION.txt does not exist\n";
        }
        FileInputStream fis = new FileInputStream(new File(filepath));
        String md5 = md5Hex(fis);
        fis.close();
        if (Objects.equals(md5, notAContribution)) return "";
        return "NOT_A_CONTRIBUTION.txt is not in the correct format";
    }

    @Override
    public boolean checkPatch(String filepath, String patchId) {
        Boolean jar = false;

        StringBuilder errorMessage = new StringBuilder();

        File dir = new File(filepath);
        if (!dir.exists()) {
            isPatchEmpty = false;
            LOG.error("patch" + patchId + " is empty!!");
            errorMessage.append("Patch folder is empty!");
        }

        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith((".jar"))) {
                    jar = true;
                } else {
                    jar = false;
                    errorMessage.append("Inappropriate " + file.getName() + "found");
                }

            }
        }
        return jar;

    }

    @Override
    public void unZip(File zipFilePath, String destFilePath) throws IOException {

        if (!zipFilePath.exists()) {
            return;
        }
        File destDir = new File(destFilePath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            String filePath = destDir + File.separator + zipEntry.getName();
            if (!zipEntry.isDirectory()) {
                new File(filePath).getParentFile().mkdirs();
                extractFile(zipInputStream, filePath);
                LOG.info("Extracting " + filePath);

            } else {
                File dir = new File(filePath);
                LOG.info("Extracting " + filePath);
                dir.mkdirs();
            }
            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    @Override
    public String checkContent(String filePath, String patchId) throws IOException {
        StringBuilder errorMessage = new StringBuilder();

        File destDir = new File(filePath);
        if (!destDir.exists()) {
            return "patch" + patchId + " content does not exist\n";
        } else {
            boolean check = new File(filePath + "LICENSE.txt").exists();
            if (!check) errorMessage.append("LICENSE.txt does not exist\n");

            check = new File(filePath + "README.txt").exists();
            if (!check) errorMessage.append("README.txt does not exist\n");

            check = new File(filePath + "NOT_A_CONTRIBUTION.txt").exists();
            if (!check) errorMessage.append("NOT_A_CONTRIBUTION.txt does not exist\n");

            check = new File(filePath + "patch" + patchId).exists();
            if (!check) errorMessage.append("patch folder does not exist\n");

            check = new File(filePath + "wso2carbon-version.txt").exists();
            if (check) errorMessage.append("Unexpected file found: wso2carbon-version.txt\n");

            String[] extensions = new String[]{"tmp", "swp", "DS_Dstore", "_MAX_OS"};
            List<File> files = (List<File>) FileUtils.listFiles(destDir, extensions, true);

            if (files.size() > 0)
                errorMessage.append("Unexpected file found: check for temporary, hidden, etc.\n");

            File[] hiddenFiles = destDir.listFiles((FileFilter) HiddenFileFilter.HIDDEN);
            assert hiddenFiles != null;
            for (File hiddenFile : hiddenFiles) {
                errorMessage.append("hidden file: ").append(hiddenFile.getName()).append("\n");

            }
            for (File file : destDir.listFiles()) {
                if (file.getName().endsWith(("~")))
                    errorMessage.append("Unexpected file found").append(file.getName()).append("\n");
            }

            check = new File(filePath + "resources").exists();
            if (check) {
                File resourcesFile = new File(filePath + "resources");
                isResourcesFileEmpty = isDirEmpty(resourcesFile);
                /*check = new File(filePath + "resources/store").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, store folder does not exist\n";

                check = new File(filePath + "resources/publisher").exists();
                if(!check) errorMessage = errorMessage + "inside the resources, publisher folder does not exist\n";*/
            }
            if (isResourcesFileEmpty && isPatchEmpty) {
                errorMessage.append("Both resources and patch").append(patchId).append(" folders are empty\n");
            }
            return errorMessage.toString();
        }
    }

    @Override
    public String downloadZipFile(String url, String version, String patchId, String destFilePath) {

        File destinationDirectory = new File(destFilePath);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }
        /*username = prop.getProperty("username");
        password = prop.getProperty("password");*/


        String checkConnection = SVNConnection(url, username, password);
        if (checkConnection.equals(Constants.CONNECTION_SUCCESSFUL)) {
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            try {
                final SvnCheckout checkout = svnOperationFactory.createCheckout();
                checkout.setSource(SvnTarget.fromURL(parseURIDecoded(url)));
                checkout.setSingleTarget(SvnTarget.fromFile(destinationDirectory));
                checkout.run();
            } catch (SVNException e) {
                LOG.error("Requested url not found");
                return "Requested url not found: " + url;
            }
        }
        return "";
    }


    public static String commitKeys(String url, String fileLocation) {
        File file = new File(fileLocation);
        String username = "patchsigner@wso2.com";
        String password = "xcbh8=cfj0mfgsOekDbh";

        String checkConnectivity = SVNConnection(url, username, password);
        if (checkConnectivity.trim().equals(Constants.CONNECTION_SUCCESSFUL)) {
            //todo: delete unzipped folder
            SVNRepository repository = null;
            for (File f : file.listFiles()) {
                try {
                    repository = SVNRepositoryFactory.create(parseURIEncoded(url));
                    ISVNEditor editor = repository.getCommitEditor(null, null, true, null);
                    editor.addFile(fileLocation, null, -1);
                } catch (SVNException e) {
                    LOG.error("Committing Keys failed", e);
                    return Constants.COMMIT_KEYS_FAILURE;
                }
            }

        }
        return null;
    }



}
