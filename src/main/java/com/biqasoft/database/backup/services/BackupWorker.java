package com.biqasoft.database.backup.services;

import com.biqasoft.database.backup.distributedstorage.BackupDomain;
import com.biqasoft.database.backup.distributedstorage.ZipUtils;
import com.biqasoft.entity.constants.TOKEN_TYPES;
import com.biqasoft.entity.core.CreatedInfo;
import com.biqasoft.entity.core.Domain;
import com.biqasoft.users.domain.useraccount.UserAccount;
import com.biqasoft.microservice.database.MainDatabase;
import com.biqasoft.microservice.database.MongoTenantHelper;
import com.biqasoft.persistence.base.BiqaObjectFilterService;
import com.biqasoft.storage.DefaultStorageService;
import com.biqasoft.storage.entity.StorageFile;
import com.biqasoft.storage.s3.AmazonS3FileRepository;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by ya on 11/23/2016.
 */
@Service
public class BackupWorker {

    private ThreadPoolExecutor threadPoolExecutor;
    private Thread queueWatcher;

    private String databaseServiceName;
    private String databaseMasterTag;
    private String userName;
    private String password;

    private final ConsulClient consulClient;
    private final MongoTenantHelper mongoTenantHelper;

    private String awsAccessKey;
    private String awsSecretKey;
    private String bucketBackupsName;

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private final String outputPath;
    private String dateFormat;
    private BiqaObjectFilterService biqaObjectFilterService;

    private IQueue<BackupDomain> queue;

    private Boolean deleteFolderAfterZip;
    private Boolean deleteZipAfterUpload;
    private Boolean uploadToUserStorage;
    private volatile Integer maxBackupThreads;

    private final MongoOperations mainDatabase;

    /**
     *
     * @param databaseServiceName
     * @param databaseMasterTag
     * @param userName
     * @param password
     * @param awsAccessKey
     * @param awsSecretKey
     * @param bucketBackupsName
     * @param dateFormat
     * @param outputPath
     * @param deleteFolderAfterZip
     * @param deleteZipAfterUpload
     * @param maxBackupThreadsSpellExpression spring spell expression, for example "5", or "#availableProcessors * 2"
     * @param consulClient
     * @param biqaObjectFilterService
     * @param mongoTenantHelper
     * @param queue
     */
    @Autowired
    public BackupWorker(@Value("${db.discovery.service.name}") String databaseServiceName,
                        @Value("${biqa.backup.db.discovery.tag}") String databaseMasterTag,
                        @Value("${db.credentials.username}") String userName,
                        @Value("${db.credentials.password}") String password,
                        @Value("${aws.s3.credentials.access.key}") String awsAccessKey,
                        @Value("${aws.s3.credentials.secret.key}") String awsSecretKey,
                        @Value("${aws.s3.bucket.backups}") String bucketBackupsName,
                        @Value("${biqa.backup.date.format:yyyy-MM-dd'__'HH-mm-ss}") String dateFormat,
                        @Value("${biqa.backup.output.path}") String outputPath,
                        @Value("${biqa.backup.output.delete.folder:true}") Boolean deleteFolderAfterZip,
                        @Value("${biqa.backup.output.delete.zip:true}") Boolean deleteZipAfterUpload,
                        @Value("${biqa.backup.upload.userstorage:true}") Boolean uploadToUserStorage,
                        @Value("${biqa.backup.distributed.threadpool.active.max:#availableProcessors}") String maxBackupThreadsSpellExpression,
                        ConsulClient consulClient,
                        BiqaObjectFilterService biqaObjectFilterService,
                        MongoTenantHelper mongoTenantHelper,
                        @MainDatabase MongoOperations mainDatabase,
                        IQueue<BackupDomain> queue) {

        this.uploadToUserStorage = uploadToUserStorage;
        this.databaseServiceName = databaseServiceName;
        this.databaseMasterTag = databaseMasterTag;
        this.userName = userName;
        this.password = password;
        this.consulClient = consulClient;
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.bucketBackupsName = bucketBackupsName;
        this.biqaObjectFilterService = biqaObjectFilterService;
        this.mongoTenantHelper = mongoTenantHelper;
        this.dateFormat = dateFormat;
        this.outputPath = outputPath;
        this.deleteFolderAfterZip = deleteFolderAfterZip;
        this.deleteZipAfterUpload = deleteZipAfterUpload;
        this.queue = queue;
        this.maxBackupThreads = getMaxThreadNumber(maxBackupThreadsSpellExpression);
        logger.info("maxBackupThreads {}", this.maxBackupThreads);

        this.mainDatabase = mainDatabase;

        this.threadPoolExecutor = new ScheduledThreadPoolExecutor(this.maxBackupThreads);
        startQueueWatcherThread();
        testMongoDump();
    }

    /**
     * test that mongodump in classpath
     */
    private void testMongoDump() {
        ProcessBuilder pb = new ProcessBuilder("mongodump", "--version");
        pb.inheritIO();
        Process p;
        try {
            p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Integer getMaxThreadNumber(String spellExpression){
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("availableProcessors",Runtime.getRuntime().availableProcessors());
        return (Integer) parser.parseExpression(spellExpression).getValue(context);
    }

    /**
     * watch every second for new domains to backup from queue
     */
    private void startQueueWatcherThread() {
        this.queueWatcher = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (this.threadPoolExecutor.getActiveCount() < this.maxBackupThreads) {
                    if (!this.queue.isEmpty()) {
                        BackupDomain domain = this.queue.poll();
                        logger.info("Poll new domain {}", domain.getDomain().getDomain());
                        backupAndArchiveDomain(domain.getDomain());
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        queueWatcher.setName("Monitor And poll backup queue");
        queueWatcher.start();
    }

    private void backupAndArchiveDomain(Domain domain) {
        CatalogService mongoDBmaster = getMongoServer();

        String userName = this.userName;
        String password = this.password;
        String port = mongoDBmaster.getServicePort().toString();
        String host = mongoDBmaster.getServiceAddress();

        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        String dateAsString = formatter.format(now);

        String tempOutputPath = outputPath + File.separator + dateAsString;

//        example of executed cmd
//        mongodump --host mongodb1.example.net --port 3017 --username user --password pass --out /opt/backup/mongodump-2013-10-24
        ProcessBuilder pb = new ProcessBuilder("mongodump", "--host", host, "--port", port, "--username", userName, "--password", password, "--out", tempOutputPath
                , "--db", domain.getDomain(), "--authenticationDatabase", "admin");
        pb.inheritIO();
        logger.info("Start backup mongodb {}", domain.getDomain());
        Process p;
        try {
            p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            logger.error("Error backup mongodb {}", domain.getDomain());
            throw new RuntimeException(e.getMessage());
        }
        logger.info("End backup mongodb {}", domain.getDomain());

        String zipFileName = null;
        String sourceDirPath = tempOutputPath + File.separator + domain.getDomain();
        try {
            zipFileName = tempOutputPath + File.separator + domain.getDomain() + ".zip";
            ZipUtils.pack(sourceDirPath, zipFileName);
        } catch (Exception e) {
            logger.error("can not zip mongodb database {}", domain.getDomain(), e);
        } finally {
            if (deleteFolderAfterZip) {
                FileSystemUtils.deleteRecursively(new File(sourceDirPath));
            }
        }

        try {
            logger.info("Start: processing domain {} , file {}", domain.getDomain(), zipFileName);
            if (uploadToUserStorage){
                processBackupToUserStorage(zipFileName, domain.getDomain());
            }
            logger.info("End: domain {} , file {}", domain.getDomain(), zipFileName);
        } catch (Exception e) {
            logger.warn("Can not upload backup file to domain {} , file {}", domain.getDomain(), zipFileName, e);
        } finally {
            if (deleteZipAfterUpload) {
                Assert.notNull(zipFileName);
                FileSystemUtils.deleteRecursively(new File(zipFileName));
            }
        }
    }

    /**
     * Upload backup file to storage available to user
     * @param filePath
     * @param domain
     */
    private void processBackupToUserStorage(String filePath, String domain) {
        MongoOperations mongoOperations = mongoTenantHelper.domainDataBaseUnsafeGet(domain);
        UserAccount userAccount = MongoTenantHelper.createMockedUser(domain);

        DefaultStorageService defaultStorageService = new DefaultStorageService(biqaObjectFilterService, mongoOperations);
        AmazonS3FileRepository repository = new AmazonS3FileRepository(awsAccessKey, awsSecretKey, mongoOperations, defaultStorageService);

        StorageFile documentFile = new StorageFile();
        documentFile.setBucket(bucketBackupsName);
        documentFile.setUploadStoreType(TOKEN_TYPES.DEFAULT_STORAGE);
        documentFile.setUploaded(true);
        documentFile.setFile(true);
        documentFile.setSecured(true);
        documentFile.setCreatedInfo(new CreatedInfo(new Date()));

        File file = new File(filePath);

        documentFile.setName(file.getName());
        documentFile.setParentId(defaultStorageService.checkBackupFolder().getId());

        mongoOperations.insert(documentFile);

        Domain domain1 = new Domain();
        domain1.setDomain(domain);

        repository.uploadFile(file, documentFile, userAccount, domain1);
        mongoOperations.save(documentFile);

        backupMetaInfoToInternalStorage(documentFile);
    }

    private void backupMetaInfoToInternalStorage(StorageFile documentFile) {
        BackupEntry backupEntry = new BackupEntry();
        backupEntry.setStorageFile(documentFile);
        backupEntry.setDate(new Date());

        mainDatabase.insert(backupEntry);
    }

    private Response<List<CatalogService>> getMongoConfiguration() {
        return consulClient.getCatalogService(databaseServiceName, QueryParams.DEFAULT);
    }

    /**
     * Get mongodb server from which we will backup
     *
     * @return
     */
    private CatalogService getMongoServer() {
        CatalogService mongoDBmaster = null;
        Response<List<CatalogService>> response = getMongoConfiguration();

        if ("random".equals(databaseMasterTag) && response.getValue().size() > 0) {
            mongoDBmaster = response.getValue().get(0);
        } else {
            for (CatalogService service : response.getValue()) {
                if (service.getServiceTags().contains(databaseMasterTag)) {
                    mongoDBmaster = service;
                    break;
                }
            }
        }

        return mongoDBmaster;
    }

}
