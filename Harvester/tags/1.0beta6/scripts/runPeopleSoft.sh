#!/bin/bash

# Copyright (c) 2010 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the new BSD license
# which accompanies this distribution, and is available at
# http://www.opensource.org/licenses/bsd-license.html
# 
# Contributors:
#     Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams - initial API and implementation

# Set working directory
set -e

DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
cd ..

HARVESTER_TASK=peoplesoft

if [ -f scripts/env ]; then
  . scripts/env
else
  exit 1
fi

CHECKEMPTY=false

#clear old fetches
rm -rf XMLVault/h2ps/XML

# Execute Fetch
$JDBCFetch -X config/tasks/PeopleSoftFetch.xml

# backup fetch
date=`date +%Y-%m-%d_%T`
tar -czpf backups/ps.xml.$date.tar.gz XMLVault/h2ps/XML
rm -rf backups/ps.xml.latest.tar.gz
ln -s ps.xml.$date.tar.gz backups/ps.xml.latest.tar.gz

exit

# uncomment to restore previous fetch
#tar -xzpf backups/ps.xml.latest.tar.gz XMLVault/h2ps/XML

# clear old translates
rm -rf XMLVault/h2ps/RDF

# Execute Translate
$XSLTranslator -i config/recordHandlers/PeopleSoft-Merge.xml -o config/recordHandlers/PeopleSoft-RDF.xml -x config/datamaps/PeopleSoftToVivo.xsl

# backup translate
date=`date +%Y-%m-%d_%T`
tar -czpf backups/ps.rdf.$date.tar.gz XMLVault/h2ps/RDF
rm -rf backups/ps.rdf.latest.tar.gz
ln -s ps.rdf.$date.tar.gz backups/ps.rdf.latest.tar.gz

# uncomment to restore previous translate
#tar -xzpf backups/ps.rdf.latest.tar.gz XMLVault/h2ps/RDF

# Clear old H2 transfer model
rm -rf XMLVault/h2ps/all

# Execute Transfer to import from record handler into local temp model
$Transfer -o config/jenaModels/h2.xml -O modelName=peopleSoftTempTransfer -O checkEmpty=$CHECKEMPTY -O dbUrl=jdbc:h2:XMLVault/h2ps/all/store -h config/recordHandlers/PeopleSoft-RDF.xml -n http://vivo.ufl.edu/individual/

# backup H2 transfer Model
date=`date +%Y-%m-%d_%T`
tar -czpf backups/ps.all.$date.tar.gz XMLVault/h2ps/all
rm -rf backups/ps.all.latest.tar.gz
ln -s ps.all.$date.tar.gz backups/ps.all.latest.tar.gz

# uncomment to restore previous H2 transfer Model
tar -xzpf backups/ps.all.latest.tar.gz XMLVault/h2ps/all

SCOREINPUT="-i config/jenaModels/h2.xml -ImodelName=peopleSoftTempTransfer -IdbUrl=jdbc:h2:XMLVault/h2ps/all/store -IcheckEmpty=$CHECKEMPTY"
SCOREDATA="-s config/jenaModels/h2.xml -SmodelName=peopleSoftScoreData -SdbUrl=jdbc:h2:XMLVault/h2ps/scoreData/store -ScheckEmpty=$CHECKEMPTY"
TEMPCOPY="-t XMLVault/h2ps/tempCopy"
SCOREMODELS="$SCOREINPUT -v $VIVOCONFIG -VcheckEmpty=$CHECKEMPTY $SCOREDATA $TEMPCOPY"
EQTEST="org.vivoweb.harvester.score.algorithm.EqualityTest"

# Clear old H2 temp copy
rm -rf XMLVault/h2ps/tempCopy

# Execute Score for People
$Score $SCOREMODELS -n http://vivoweb.org/harvest/ufl/peoplesoft/person/ -Aufid=$EQTEST -Wufid=1.0 -Fufid=http://vivo.ufl.edu/ontology/vivo-ufl/ufid -Pufid=http://vivo.ufl.edu/ontology/vivo-ufl/ufid

# Execute Score for Departments
$Score $SCOREMODELS -n http://vivoweb.org/harvest/ufl/peoplesoft/org/ -AdeptId=$EQTEST -WdeptId=1.0 -FdeptId=http://vivo.ufl.edu/ontology/vivo-ufl/deptID -PdeptId=http://vivo.ufl.edu/ontology/vivo-ufl/deptID

# Find matches using scores and rename nodes to matching uri
$Match $SCOREINPUT $SCOREDATA -t 1.0 -r

# Execute Score for Positions
$Score $SCOREMODELS -n http://vivoweb.org/harvest/ufl/peoplesoft/position/ -AposOrg=$EQTEST -WposOrg=1.0 -FposOrg=http://vivoweb.org/ontology/core#positionInOrganization -PposOrg=http://vivoweb.org/ontology/core#positionInOrganization -AposPer=$EQTEST -WposPer=1.0 -FposPer=http://vivoweb.org/ontology/core#positionForPerson -PposPer=http://vivoweb.org/ontology/core#positionForPerson -AdeptPos=$EQTEST -WdeptPos=1.0 -FdeptPos=http://vivo.ufl.edu/ontology/vivo-ufl/deptIDofPosition -PdeptPos=http://vivo.ufl.edu/ontology/vivo-ufl/deptIDofPosition

# Find matches using scores and rename nodes to matching uri
$Match $SCOREINPUT $SCOREDATA -t 1.0 -r

# Clear old H2 temp copy
rm -rf XMLVault/h2ps/tempCopy

# backup H2 score data Model
date=`date +%Y-%m-%d_%T`
tar -czpf backups/ps.scoredata.$date.tar.gz XMLVault/h2ps/scoreData
rm -rf backups/ps.scoredata.latest.tar.gz
ln -s ps.scoredata.$date.tar.gz backups/ps.scoredata.latest.tar.gz

# uncomment to restore previous H2 matched Model
#tar -xzpf backups/ps.scoredata.latest.tar.gz XMLVault/h2ps/scoreData

CNFLAGS="$SCOREINPUT -v $VIVOCONFIG -VcheckEmpty=$CHECKEMPTY -n http://vivo.ufl.edu/individual/"
# Execute ChangeNamespace to get unmatched People into current namespace
$ChangeNamespace $CNFLAGS -o http://vivoweb.org/harvest/ufl/peoplesoft/person/
# Execute ChangeNamespace to get unmatched Departments into current namespace
$ChangeNamespace $CNFLAGS -o http://vivoweb.org/harvest/ufl/peoplesoft/org/ -e
# Execute ChangeNamespace to get unmatched Positions into current namespace
$ChangeNamespace $CNFLAGS -o http://vivoweb.org/harvest/ufl/peoplesoft/position/

# backup H2 matched Model
date=`date +%Y-%m-%d_%T`
tar -czpf backups/ps.matched.$date.tar.gz XMLVault/h2ps/all
rm -rf backups/ps.matched.latest.tar.gz
ln -s ps.matched.$date.tar.gz backups/ps.matched.latest.tar.gz

# uncomment to restore previous H2 matched Model
#tar -xzpf backups/ps.matched.latest.tar.gz XMLVault/h2ps/all

# Backup pretransfer vivo database, symlink latest to latest.sql
date=`date +%Y-%m-%d_%T`
mysqldump -h $SERVER -u $USERNAME -p$PASSWORD $DBNAME > backups/$DBNAME.ps.pretransfer.$date.sql
rm -rf backups/$DBNAME.ps.pretransfer.latest.sql
ln -s $DBNAME.ps.pretransfer.$date.sql backups/$DBNAME.ps.pretransfer.latest.sql

# Restore pretransfer vivo database
#mysql -h $SERVER -u $USERNAME -p$PASSWORD -e "drop database $DBNAME;"
#mysql -h $SERVER -u $USERNAME -p$PASSWORD -e "create database $DBNAME;"
#mysql -h $SERVER -u $USERNAME -p$PASSWORD $DBNAME < backups/$DBNAME.ps.pretransfer.latest.sql

#PREVHARVESTMODEL="http://vivoweb.org/ingest/ufl/peoplesoft"
PREVHARVESTMODEL="uflPeopleSoft"
# Find Subtractions
$Diff -m $VIVOCONFIG -MmodelName=$PREVHARVESTMODEL -McheckEmpty=$CHECKEMPTY -s config/jenaModels/h2.xml -ScheckEmpty=$CHECKEMPTY -SdbUrl=jdbc:h2:XMLVault/h2ps/all/store -SmodelName=peopleSoftTempTransfer -d XMLVault/update_Subtractions.rdf.xml
# Find Additions
$Diff -m config/jenaModels/h2.xml -McheckEmpty=$CHECKEMPTY -MdbUrl=jdbc:h2:XMLVault/h2ps/all/store -MmodelName=peopleSoftTempTransfer -s $VIVOCONFIG -ScheckEmpty=$CHECKEMPTY -SmodelName=$PREVHARVESTMODEL -d XMLVault/update_Additions.rdf.xml
# Apply Subtractions to Previous model
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -OmodelName=http://vivoweb.org/ingest/ufl/peoplesoft -r XMLVault/update_Subtractions.rdf.xml -m
# Apply Additions to Previous model
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -OmodelName=http://vivoweb.org/ingest/ufl/peoplesoft -r XMLVault/update_Additions.rdf.xml
# Apply Subtractions to VIVO
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -r XMLVault/update_Subtractions.rdf.xml -m
# Apply Additions to VIVO
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -r XMLVault/update_Additions.rdf.xml

# Backup posttransfer vivo database, symlink latest to latest.sql
date=`date +%Y-%m-%d_%T`
mysqldump -h $SERVER -u $USERNAME -p$PASSWORD $DBNAME > backups/$DBNAME.ps.posttransfer.$date.sql
rm -rf backups/$DBNAME.ps.posttransfer.latest.sql
ln -s $DBNAME.ps.posttransfer.$date.sql backups/$DBNAME.ps.posttransfer.latest.sql

# Restore post transfer vivo database
#mysql -h $SERVER -u $USERNAME -p$PASSWORD -e "drop database $DBNAME;"
#mysql -h $SERVER -u $USERNAME -p$PASSWORD -e "create database $DBNAME;"
#mysql -h $SERVER -u $USERNAME -p$PASSWORD $DBNAME < backups/$DBNAME.ps.posttransfer.latest.sql

# Tomcat must be restarted in order for the harvested data to appear in VIVO
/etc/init.d/tomcat restart