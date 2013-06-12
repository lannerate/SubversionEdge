Command-line instructions for getting started.  Refer to the project wiki docs
for more information.


GET GRAILS

Grails needs a JDK installed, we assume you already have one. The Sun 1.6 JDK
is recommended version. We are currently using Grails 1.3.4. You can download
it here: http://www.grails.org/download/archive/Grails.

Unpack Grails to a location such as $HOME/grails-1.3.4


INSTALL GRAILS PLUGINS AND USING GRAILS COMMAND LINE

Our application uses a number of Grails plugins, and if you are going to use an
IDE, you will need these installed to not have compile errors. I found the
easiest way to get all the plugins was to just run some grails commands from
command line:

There is a Getting Started guide here: http://www.grails.org/Quick+Start. Here
are the basic things you need to do: 

 * Create a JAVA_HOME environment variable pointing to root of your Java
   install 
 * Create a GRAILS_HOME environment variable pointing to root of your Grails
   install 
 * Add Grails bin folder to your PATH

Here are examples from OSX:

export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
export GRAILS_HOME=/Users/username/grails-1.3.4
export PATH=$PATH:$GRAILS_HOME/bin

Once these steps are done, navigate to your checked out project and run a
command like this:

grails test-app

This will download and install all the plugins, compile the application and run
the tests.
