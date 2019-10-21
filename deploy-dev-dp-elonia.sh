#
# Copyright (C) 2018-2019 toop.eu
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#/bin/sh

DATE=`date +%Y%m%d`
BASEDIR=/opt/tomcat/webapps
FILENAME=toop-demo-ui-dp-0.10.5-SNAPSHOT.war

if [ -f ~/$FILENAME ]
  then
    echo stopping Tomcat
    sudo service tomcat stop

    echo Cleaning exisiting dirs
    cd $BASEDIR
    sudo mv ROOT/ ~/dp.$DATE
    sudo chmod 777 ~/dp.$DATE

    echo ROOT
    APPDIR=$BASEDIR/ROOT
    [ ! -d $APPDIR ] && sudo mkdir $APPDIR
    sudo cp ~/$FILENAME $APPDIR
    cd $APPDIR
    sudo unzip -q $FILENAME && sudo rm $FILENAME
    sudo rm $APPDIR/WEB-INF/classes/private-*.properties
    sudo rm $APPDIR/WEB-INF/classes/dcui.properties
    sudo mv $APPDIR/WEB-INF/classes/dcui.elonia-dev.properties $APPDIR/WEB-INF/classes/dcui.properties
    sudo rm $APPDIR/WEB-INF/classes/toop-interface.properties
    sudo mv $APPDIR/WEB-INF/classes/toop-interface.elonia-dev.properties $APPDIR/WEB-INF/classes/toop-interface.properties
    sudo mv $APPDIR/WEB-INF/classes/datasets.elonia.xml $APPDIR/WEB-INF/classes/datasets.xml

    echo Done!
  else
    echo "Source file ~/$FILENAME not existing!"
fi
