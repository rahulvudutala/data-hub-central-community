'use strict'

const admin = require('/MarkLogic/admin.xqy');
const finalDB = require('/com.marklogic.hub/config.sjs').FINALDATABASE

var config = admin.getConfiguration()
fn.head(admin.databaseGetRangeElementIndexes(config, xdmp.database(finalDB)))
	.xpath('.[namespace-uri=""]/localname/string()')
	.toArray()
