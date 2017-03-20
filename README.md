### Backup

 - backups to local temp folder and upload zip to s3

 - backup all active -  `GET http://localhost:9091/backup`
 - backup one by id - `GET http://localhost:9091/backup/id/{}`

## Requirements
 -  cli `mongodump` util in classpath