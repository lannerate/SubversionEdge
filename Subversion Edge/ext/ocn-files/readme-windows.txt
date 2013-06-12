CollabNet Subversion Edge - Windows 32/64-bit
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
   Certified platforms: Windows 2003/2008 Server
   
   NOTE: These are the platforms we formally certify when testing.  Since this
   is a server application, we focus on the Windows server OS.  CollabNet
   Subversion Edge works on all versions of Windows XP and later, including
   Windows Vista and Windows 7.
   
   There are separate downloads for 32-bit and 64-bit Windows.
   
   Prerequisite: You must have administrative privileges to install
   and uninstall CollabNet Subversion Edge.
   
3. Requirements
   
   Java 1.6+ JRE/JDK must be installed.  The installer will install Java 1.6 if
   does not detect it as already installed.  The installer will create
   the JAVA_HOME environment variable so that it points to the Java 1.6 JRE.

   CollabNet Subversion Edge uses HTML5, CSS3 and JavaScript. Your browser must allow
   JavaScript to run for the web UI to function properly.  We test and support
   the following browsers:

   Chrome
   Firefox
   IE 8/9
   Safari

4. Installation Notes

   The installer will update the system PATH variable to include the path to
   the "bin" and "Python25" folders that are installed by the installer and
   it will also create or update the PYTHONHOME environment variable.

   The installer will add rules to the Windows Firewall to allow access to the 
   Apache binary and to open up ports 3343 and 4434.

   The installer will add two Windows services set to start automatically when
   the system starts.

   1. CollabNet Subversion Edge - a Java-based web application that provides a
      browser UI for configuring and managing your Apache Subversion server.
   2. CollabNet Subversion Server - the actual Apache Subversion server that
      the management console manages for you, and that your Subversion users
      will access.

   You must login to the CollabNet Subversion Edge browser-based management
   console and configure the Apache server before it can be run for the first
   time.  The UI of the management console writes the needed Apache
   configuration files based on the information you provide.

   The default administrator login is:

   Address: http://localhost:3343/csvn
   Username: admin
   Password: admin

   The installer provides the option to start the application at the end of 
   the install.  This will open your browser to a local page that will detect
   when the server has finished starting.
   
   Subversion Edge also starts an SSL-protected version using a self-signed SSL
   certificate.  You can access the SSL version on this URL:
   
   Address: https://localhost:4434/csvn
 
   You can force users to use SSL from the Server configuration.  This will cause
   attempts to access the site via plain HTTP on port 3343 to be redirected to the
   secure port on 4434.

5. Updates

   CollabNet Subversion Edge includes a built-in mechanism for discovering and 
   installing updates.  You should use this facility to install updates.

   The update mechanism will require you to restart the servers at the end of
   the process, but it will do it for you.
   
   As of the 2.0.0 release you can also update the application by running the
   installer for the new release.  For the upgrade from the 1.x releases to the
   2.x releases you should do the following:
   
   * Uninstall the 1.x version from the Windows control panel
   * The uninstall will leave behind the C:\csvn folder and any files updated
     after the original install.  Delete all of the folders left behind EXCEPT
     for the C:\csvn\data folder.
   * Install the 2.x version
   
   For future updates to the 2.x versions you should be able to just run the
   installer.

6. Documentation
   
   Documentation for CollabNet Subversion Edge is available here:

   http://help.collab.net/
   
   Context-sensitive help is also linked to this location from within the
   application.
  

7. Known issues

   - For the latest FAQ, visit the project home page here:
   
     https://ctf.open.collab.net/sf/projects/svnedge

   - When trying to access a repository via ViewVC, you might see an error 
     trace that ends with this message:

      ImportError: DLL load failed with error code 182

     This error occurs when you have an older version of the OpenSSL DLLs in 
     your PATH ahead of the CollabNet Subversion bin folder. Many Windows 
     applications ship the OpenSSL DLLs and many of them also use older 
     versions. To fix this problem:

      1. Edit your PATH so that the CollabNet Subversion bin folder is at or 
         near the beginning of your PATH. 
      2. Reboot so that your Apache service can pick up the change.

   - If you try to access an existing BDB (Berkeley DB) based repository
     through CollabNet Subversion Edge, then you will receive an alert "Failed
     to load module for FS type 'bdb'." This is because CollabNet Subversion
     Edge does not support BDB.  CollabNet recommends FSFS over BDB for ease
     of maintenance and supportability.

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