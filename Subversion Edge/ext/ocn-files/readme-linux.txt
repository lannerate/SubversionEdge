CollabNet Subversion Edge - Linux 32/64-bit
Version 4.0.0

Contents

   1. Overview
   2. Platform and configuration
   3. Requirements
   4. Installation notes
   5. Updates
   6. Documentation
   7. Known issues
   8. Support for CollabNet Subversion Edge
   9. About Subversion and CollabNet
   
1. Overview

   CollabNet Subversion Edge includes everything you need to install, manage
   and operate a Subversion server.  It includes all of the Subversion and
   Apache binaries needed to run a Subversion server.  It also integrates the
   popular ViewVC repository browsing tool so that users can view repository
   history from a web browser.

   CollabNet Subversion Edge also includes a powerful web-based management
   console that makes it easy to configure and manage your Apache server and
   Subversion repositories.  You can easily setup the server to use SSL
   connections and even connect the server to a corporate LDAP repository,
   including Microsoft Active Directory.

   CollabNet Subversion Edge only supports serving Subversion repositories via
   Apache httpd and it only supports the Subversion fsfs repository format.
  
2. Platform and configuration

   Product: CollabNet Subversion Edge
   License: GNU Affero General Public License 3.0 (AGPLv3)
   Certified platforms: Red Hat Enterprise Linux 5.x - 6.x 
                        CentOS 5.x - 6.x
                        SuSE Linux Enterprise 11.x
   
   NOTE: These are the platforms we formally certify when testing.  CollabNet
   Subversion Edge is known to work on virtually all Linux distributions and is
   informally tested on others such as Ubuntu and Fedora.
   
   There are separate downloads for 32-bit and 64-bit Linux.  Download the 
   appropriate version for your distribution and kernel.
   
3. Requirements
   
   * Java 1.6+ JRE/JDK must be installed. We recommend the JRE provided by
     Sun/Oracle.  When testing on 64-bit Linux we have used the 64-bit JVM.
   
   * Python 2.4 to 2.6 must be installed. Subversion Edge uses mod_python which
     requires that your system Python provides shared libraries. Many recent Linux
     distributions include the shared libraries in an optional packages named
     "python-libs" or something similar.  This package needs to be installed.
     In addition, some Linux distributions may package some Python modules in
     optional packages, such as "python-xml". You may need some of these modules
     installed, particularly if you connect your Subversion Edge server to
     CollabNet TeamForge.

   * CollabNet Subversion Edge uses HTML5, CSS3 and JavaScript. Your browser must allow
     JavaScript to run for the web UI to function properly.  We test and support
     the following browsers:

       Chrome
       Firefox
       IE 8/9
       Safari

4. Installation Notes

   IMPORTANT: Do not untar CollabNet Subversion Edge using root or sudo.  This
   will cause the UID/GID on the files to carry the values from our build
   system instead of being reset.
   
   1. Set the JAVA_HOME environment variable, and point it to your Java 6 JRE
      home.  For example:

      export JAVA_HOME=/usr/java/default
   
      Test the variable:
   
      $ $JAVA_HOME/bin/java -version
      java version "1.6.0_20"
      Java(TM) SE Runtime Environment (build 1.6.0_20-b02)
      Java HotSpot(TM) Client VM (build 16.3-b01, mixed mode, sharing)
   
   2. Switch to the folder where you want to install CollabNet Subversion
      Edge.  You must have write permissions to this folder.
      
      $ cd /opt
      
   3. Untar the file you downloaded from CollabNet.
   
      $ tar zxf CollabNetSubversionEdge-x.y.z_linux-x86.tar.gz
      
      This will create a folder named "csvn" in the current directory. You can
      rename this folder if desired.
      
   4. Optional. Install the application so that it will start automatically
      when the server restarts.  This command generally requires root/sudo to
      execute.
      
      $ cd csvn
      $ sudo -E bin/csvn install
      
      In addition to configuring your system so that the server is started
      with the system, it will also write the current JAVA_HOME and the
      current username in to the file data/conf/csvn.conf.  You can edit this
      file if needed as it controls the startup settings for the application.
      By setting the JAVA_HOME and RUN_AS_USER variables in this file, it
      ensures they are set correctly when the application is run.
      
   5. Start the server.  Be sure that you are logged in as your own userid and
      not running as root.
      
      $ bin/csvn start
      
      This will take a few minutes and the script will loop until it sees that
      the server is running.  If the server does not start, then try starting
      the server with this command:
      
      $ bin/csvn console
      
      This will start the server but output the initial startup messages to
      the console.

      You must login to the CollabNet Subversion Edge browser-based management
      console and configure the Apache server before it can be run for the first
      time.  The UI of the management console writes the needed Apache
      configuration files based on the information you provide.

      The default administrator login is:

      Address: http://localhost:3343/csvn
      Username: admin
      Password: admin

      Subversion Edge also starts an SSL-protected version using a self-signed SSL
      certificate.  You can access the SSL version on this URL:

      Address: https://localhost:4434/csvn

      You can force users to use SSL from the Server configuration.  This will cause
      attempts to access the site via plain HTTP on port 3343 to be redirected to the
      secure port on 4434.

   6. Optional. Configure the Apache Subversion server to start automatically when
      the system boots.
      
      $ cd csvn
      $ sudo bin/csvn-httpd install
      
      It is recommend that you login to the Edge console and configure and start the
      Apache server via the web UI before you perform this step.


5. Updates

   CollabNet Subversion Edge includes a built-in mechanism for discovering and 
   installing updates.  You must use this facility to install updates.  Do not
   download and run a new version of the application installer.

   The update mechanism will require you to restart the servers at the end of
   the process, but it will do it for you.

6. Documentation
   
   Documentation for CollabNet Subversion Edge is available here:

   http://help.collab.net/
   
   Context-sensitive help is also linked to this location from within the
   application.
  

7. Known issues

   - For the latest FAQ, visit the project home page here:
   
     https://ctf.open.collab.net/sf/projects/svnedge

   - If you try to access an existing BDB (Berkeley DB) based repository
     through CollabNet Subversion Edge, then you will receive an alert "Failed
     to load module for FS type 'bdb'." This is because CollabNet Subversion
     Edge does not support BDB.  CollabNet recommends FSFS over BDB for ease
     of maintenance and supportability.
     
    - You can access the application from localhost, but not other computers
    on your LAN.  This usually means that you need to configure Firewall rules
    for you server to open the firewall for port 3343:
    
    edit /etc/sysconfig/iptables
    edit /etc/sysconfig/ip6tables
        
    Add the following line to the above files, it line be will the line before
    the last 2 lines.
        
    -A RH-Firewall-1-INPUT -m state --state NEW -m tcp -p tcp --dport 3343 -j ACCEPT

    /etc/init.d/iptables restart
    /etc/init.d/ip6tables restart

8. Support for CollabNet Subversion Edge

   Ask questions and get assistance with using CollabNet Subversion Edge via
   the community forums on openCollabNet.  The forum for CollabNet Subversion
   Edge questions is available here:

   http://subversion.open.collab.net/ds/viewForumSummary.do?dsForumId=3

   Find out about CollabNet Technical Support at 
   http://www.open.collab.net/support/
   
   Information about CollabNet Training, Consulting, and Migration
   services is at http://www.open.collab.net/training/
   
   Join openCollabNet for community support: http://open.collab.net

9. About Subversion and CollabNet

   CollabNet launched the Subversion project in 2000 in response to the demand 
   for an open standard for Web-based software configuration management that 
   could support distributed development. CollabNet continues to be strongly 
   involved with the Subversion project and offers CollabNet Subversion
   Support, Training, and Consulting services. 
   
   CollabNet also provides the most widely used collaborative development 
   environment in the world. More than 1,400,000 developers and IT projects 
   managers collaborate online through CollabNet. The company is transforming 
   the way software is developed by enabling organizations to leverage global 
   development talents to deliver better products and innovate faster. 
   
   Visit CollabNet at http://www.collab.net for more information.
   
   Subversion is a registered trademark of the Apache Software Foundation.
   http://subversion.apache.org/