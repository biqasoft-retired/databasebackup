# Backup microservice

Backup:
 - mongodb user tenants(domain)
 - consul
 
to local temp folder and upload zip to S3

#### Endpoints

 - backup all active domains -  `GET http://localhost:9091/v1/backup`
 - backup one domain by id - `GET http://localhost:9091/v1/backup/id/{}`
 - backup all consul K/V -  `GET http://localhost:9091/v1/backup/consul`

## Requirements
 -  cli `mongodump` util in classpath