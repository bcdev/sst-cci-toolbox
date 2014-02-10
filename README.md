# ESA SST-CCI Toolbox

## Overview

To be completed.

## Setting up the developer environment on CEMS

1. Append the following snippet to your '.bash_profile'

>
  export JAVA_HOME='/usr/java/jdk1.7.0_51'
  export PATH=$HOME/bin:$JAVA_HOME/bin:$PATH
  # To Access HTTP or HTTPS you need to go through the RAL site web
  # proxies. How this is configured varies with the application you
  # are using to access the web but commonly you need to set
  export http_proxy=wwwcache.rl.ac.uk:8080
  export https_proxy=wwwcache.rl.ac.uk:8080

2. Include the following snippet in your '.m2/settings.xml' file and adapt the username

>
<settings>
  <localRepository>/data/mboettcher/mms/m2/repository/</localRepository>
  <servers>
    <server>
      <id>bc-mvn-repo-public</id>
      <username>maven</username>
      <!-- the BC maven private key is only needed for deployment -->
      <privateKey>/home/<ADAPT USERNAME>/.m2/id_rsa</privateKey>
      <passphrase><ADAPT PASSPHRASE></passphrase>
      -->
      <filePermissions>664</filePermissions>
      <directoryPermissions>775</directoryPermissions>
    </server>
    <server>
      <id>bc-mvn-repo-closed</id>
      <username>maven-cs</username>
      <!-- the BC maven private key -->
      <privateKey>/home/<ADAPT USERNAME>/.m2/id_rsa</privateKey>
      <passphrase><ADAPT PASSPHRASE></passphrase>
      -->
      <filePermissions>664</filePermissions>
      <directoryPermissions>775</directoryPermissions>
    </server>
  </servers>
  <proxies>
    <proxy>
      <active>true</active>
      <protocol>http</protocol>
      <host>wwwcache.rl.ac.uk</host>
      <port>8080</port>
      <!--
      <username>proxyuser</username>
      <password>somepassword</password>
      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
      -->
    </proxy>
    <proxy>
      <active>true</active>
      <protocol>https</protocol>
      <host>wwwcache.rl.ac.uk</host>
      <port>8080</port>
      <!--
      <username>proxyuser</username>
      <password>somepassword</password>
      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
      -->
    </proxy>
  </proxies>
  <profiles>
    <profile>
      <id>compiler</id>
        <properties>
          <java.home>/usr/java/jdk1.7.0_51</java.home>
        </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>compiler</activeProfile>
  </activeProfiles>
</settings>

## How to build an assembly

1. Change to the project root directory
2. Type 'mvn clean package install'
3. Type 'mvn -DskipTests=true -f mms/pom.xml assembly:assembly'
4. Type 'mvn -DskipTests=true -f tools/pom.xml assembly:assembly'

## Contact information

* Ralf Quast (ralf.quast@brockmann-consult.de)
