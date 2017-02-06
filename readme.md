This is a 'fork' of the mediawork opencms-maven-plugin
* http://opencms.mediaworx.com/maven-plugin/

All dependencies are integrated into one single project
* com.mediaworx.opencms:opencms-maven-plugin:1.6
   * http://opencms.mediaworx.com/artifactory/opencms-public/com/mediaworx/
* com.mediaworx.opencms:opencms-xmlutils:1.5
   * https://github.com/mediaworx/opencms-manifestgenerator
* com.mediaworx.opencms:opencms-manifestgenerator:1.5
   * https://github.com/mediaworx/opencms-xmlutils
   
Some Improvements are done
* Integrate folders outside the project
    * generated css classes
    * minified javascript
    * images
    * jars
    
Example pom.xml (partial)

    <properties>
        <version.opencms-maven-plugin>...</version.opencms-maven-plugin>
    </properties>
        
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>de.zebrajaeger</groupId>
                    <artifactId>opencms-maven-plugin</artifactId>
                    <extensions>true</extensions>
                    <version>${version.opencms-maven-plugin}</version>
                    <configuration>
                        <moduleName>${project.artifactId}</moduleName>
                        <manifestMetaDir>${project.basedir}/src/main/opencms/manifest</manifestMetaDir>
                        <vfsRoot>${project.basedir}/src/main/opencms/vfs</vfsRoot>
                        <replaceMetaVariables>true</replaceMetaVariables>
                        <addDependencies>true</addDependencies>
                        <addClasses>false</addClasses>
                        <failOnMissingResource>true</failOnMissingResource>
                    </configuration>
                </plugin>
        </pluginManagement>
    </build>

    <plugin>
        <groupId>de.zebrajaeger</groupId>
        <artifactId>opencms-maven-plugin</artifactId>
        <configuration>
            <srcResources>
                <resource>
                    <directory>${project.build.directory}/frontend/css</directory>
                    <targetPath>system/modules/${modulename}/resources/css</targetPath>
                </resource>
                <resource>
                    <directory>${project.build.directory}/frontend/js</directory>
                    <targetPath>system/modules/${modulename}/resources/js</targetPath>
                </resource>
                <resource>
                    <directory>${project.build.sourceFrontend}/src/assets/fonts</directory>
                    <targetPath>system/modules/${modulename}/resources/fonts</targetPath>
                </resource>
                <resource>
                    <directory>${project.build.sourceFrontend}/src/assets/images</directory>
                    <targetPath>system/modules/${modulename}/resources/images</targetPath>
                </resource>
            </srcResources>
        </configuration>
    </plugin>    